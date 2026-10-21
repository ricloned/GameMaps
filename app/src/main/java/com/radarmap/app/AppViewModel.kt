package com.radarmap.app

import android.app.Application
import android.location.Location
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radarmap.app.data.Storage
import com.radarmap.app.location.LocationTracker
import com.radarmap.app.map.GeoJson
import com.radarmap.app.map.MapRenderer
import com.radarmap.app.model.Box
import com.radarmap.app.model.GeoPoint
import com.radarmap.app.model.Place
import com.radarmap.app.model.PoiCategory
import com.radarmap.app.model.Profile
import com.radarmap.app.model.RoutePoint
import com.radarmap.app.model.RouteResult
import com.radarmap.app.model.SavedRoute
import com.radarmap.app.model.Settings
import com.radarmap.app.model.ViewMode
import com.radarmap.app.nav.Geo
import com.radarmap.app.net.Api
import com.radarmap.app.style.GameStyle
import com.radarmap.app.style.GameStyles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.MapLibreMap
import kotlin.math.abs

data class NavState(
    val route: RouteResult,
    val targets: List<GeoPoint>,
    val stepIndex: Int = 1,
    val distToStep: Double = 0.0,
    val remaining: Double = route.distance,
    val remainingTime: Double = route.duration
)

class AppViewModel(app: Application) : AndroidViewModel(app), MapRenderer.Listener {

    private val storage = Storage(app)

    var settings by mutableStateOf(storage.loadSettings())
        private set
    val style: GameStyle get() = GameStyles.byId(settings.styleId)

    val renderer = MapRenderer(app)
    private val tracker = LocationTracker(app, { onLocation(it) }, { onHeading(it) })

    // --- вид и камера ---
    var viewMode by mutableStateOf(ViewMode.MAP)
        private set
    var following by mutableStateOf(true)
        private set
    var location by mutableStateOf<Location?>(null)
        private set
    var heading by mutableFloatStateOf(0f)
        private set
    var cameraBearing by mutableFloatStateOf(0f)
        private set
    var hasPermission by mutableStateOf(false)
        private set

    // --- экраны ---
    var menuOpen by mutableStateOf(false)
    var menuTab by mutableIntStateOf(0)
    var searchOpen by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    var results by mutableStateOf<List<Place>>(emptyList())
        private set
    var searching by mutableStateOf(false)
        private set
    var searchTitle by mutableStateOf<String?>(null)
        private set
    var selected by mutableStateOf<Place?>(null)
        private set
    var message by mutableStateOf<String?>(null)

    // --- маршруты ---
    var editing by mutableStateOf(false)
        private set
    val draft = mutableStateListOf<RoutePoint>()
    var draftResult by mutableStateOf<RouteResult?>(null)
        private set
    var activeRoute by mutableStateOf<RouteResult?>(null)
        private set
    private var activeTargets: List<GeoPoint> = emptyList()
    var nav by mutableStateOf<NavState?>(null)
        private set
    var routeLoading by mutableStateOf(false)
        private set
    val savedRoutes = mutableStateListOf<SavedRoute>()

    // --- места ---
    private val pois = LinkedHashMap<String, Place>()
    private var poiBox: Box? = null
    private var poiJob: Job? = null
    var poiLoading by mutableStateOf(false)
        private set

    // --- название улицы (как районы в San Andreas) ---
    var zoneName by mutableStateOf<String?>(null)
        private set
    var zoneStamp by mutableIntStateOf(0)
        private set
    private var zoneAt: GeoPoint? = null
    private var zoneTime = 0L

    init {
        renderer.listener = this
        savedRoutes.addAll(storage.loadRoutes())
    }

    val canGoBack: Boolean
        get() = menuOpen || searchOpen || editing || selected != null || (activeRoute != null && nav == null)

    fun back() {
        when {
            menuOpen -> menuOpen = false
            searchOpen -> closeSearch()
            editing -> closeEditor()
            selected != null -> clearSelection()
            activeRoute != null && nav == null -> stopRoute()
        }
    }

    // ================= Карта =================

    fun attachMap(m: MapLibreMap) {
        val l = location
        renderer.attach(m, style, settings, l?.let { GeoPoint(it.latitude, it.longitude) })
        updatePlayer()
        renderPois()
        renderRoute()
        renderDraft()
        renderDest()
    }

    fun updateSettings(f: (Settings) -> Settings) {
        val n = f(settings)
        if (n == settings) return
        settings = n
        storage.saveSettings(n)
        renderer.apply(style, n)
        renderPois()
    }

    fun toggleCat(cat: PoiCategory) = updateSettings {
        val h = it.hiddenCats.toMutableSet()
        if (!h.remove(cat.name)) h.add(cat.name)
        it.copy(hiddenCats = h)
    }

    fun setProfile(p: Profile) {
        updateSettings { it.copy(profile = p) }
        if (draft.size >= 2) recomputeDraft()
    }

    fun openMenu() {
        searchOpen = false
        menuOpen = true
    }

    fun toggleMode() {
        viewMode = if (viewMode == ViewMode.MAP) ViewMode.RADAR else ViewMode.MAP
        if (viewMode == ViewMode.RADAR) {
            following = true
            followCamera(force = true)
        } else {
            val c = myPoint().takeIf { following } ?: renderer.center() ?: return
            renderer.moveTo(c.lat, c.lon, null, if (nav != null) null else 0f, if (nav != null) null else 0.0, 600)
        }
    }

    fun locate() {
        following = true
        val z = renderer.zoom() ?: 0.0
        followCamera(force = true, zoom = if (z < 14.0) 16.0 else null)
    }

    fun resetNorth() = renderer.resetNorth()

    override fun onCameraMove(bearing: Float) {
        cameraBearing = bearing
    }

    override fun onUserGesture() {
        following = false
    }

    override fun onTap(lat: Double, lon: Double, poiId: String?) {
        if (editing) {
            addDraftPoint(lat, lon, poiId?.let { pois[it]?.name })
            return
        }
        if (poiId != null) {
            pois[poiId]?.let { select(it, fly = false) }
            return
        }
        if (selected != null) clearSelection()
    }

    override fun onLongPress(lat: Double, lon: Double) {
        if (editing) {
            addDraftPoint(lat, lon, null)
            return
        }
        val p = Place(id = "wp", name = "Метка", point = GeoPoint(lat, lon), kind = "Своя точка")
        select(p, fly = false)
        viewModelScope.launch {
            val r = runCatching { withContext(Dispatchers.IO) { Api.reverse(lat, lon) } }.getOrNull()
            val cur = selected
            if (r != null && cur != null && cur.id == "wp" && cur.point == p.point) {
                selected = cur.copy(address = r.label)
            }
        }
    }

    // ================= GPS =================

    fun onPermission(granted: Boolean) {
        hasPermission = granted
        if (granted) tracker.start() else message = "Без доступа к геопозиции GPS не работает"
    }

    fun startTracking() {
        if (hasPermission) tracker.start()
    }

    fun stopTracking() = tracker.stop()

    override fun onCleared() {
        tracker.stop()
        super.onCleared()
    }

    fun myPoint(): GeoPoint? = location?.let { GeoPoint(it.latitude, it.longitude) }

    private fun myPointOrCenter(): GeoPoint? = myPoint() ?: renderer.center()

    private fun onLocation(l: Location) {
        val first = location == null
        location = l
        updatePlayer()
        if (following) followCamera(force = first, zoom = if (first) 16.0 else null)
        if (nav != null) updateNav(l)
        checkZone(l)
    }

    private var lastHeadingPush = 0L
    private var lastHeadingVal = 0f

    private fun onHeading(h: Float) {
        heading = h
        val now = SystemClock.elapsedRealtime()
        if (now - lastHeadingPush < 120) return
        var diff = abs(h - lastHeadingVal) % 360f
        if (diff > 180f) diff = 360f - diff
        if (diff < 2f) return
        lastHeadingPush = now
        lastHeadingVal = h
        updatePlayer()
        if (following && headingUpActive()) followCamera()
    }

    /** Направление: при движении — по GPS, стоя на месте — по компасу. */
    fun currentHeading(): Float {
        val l = location
        return if (l != null && l.hasBearing() && l.hasSpeed() && l.speed > 2.5f) l.bearing else heading
    }

    private fun headingUpActive() = settings.headingUp && (viewMode == ViewMode.RADAR || nav != null)

    private fun followCamera(force: Boolean = false, zoom: Double? = null) {
        val l = location ?: return
        val radar = viewMode == ViewMode.RADAR
        val navOn = nav != null
        val bearing: Float? = when {
            headingUpActive() -> currentHeading()
            radar -> 0f
            else -> null
        }
        val tilt: Double? = when {
            (radar || navOn) && settings.tilt3d -> 50.0
            radar || navOn -> 0.0
            else -> null
        }
        val z = zoom ?: if (force && (radar || navOn)) 17.0 else null
        renderer.moveTo(l.latitude, l.longitude, z, bearing, tilt, if (force) 900 else 650)
    }

    private fun updatePlayer() {
        val l = location ?: return
        renderer.set("player", GeoJson.fc(listOf(
            GeoJson.point(l.latitude, l.longitude, mapOf("bearing" to currentHeading().toDouble()))
        )))
    }

    private fun checkZone(l: Location) {
        if (!settings.showZone) return
        val me = GeoPoint(l.latitude, l.longitude)
        val now = SystemClock.elapsedRealtime()
        val last = zoneAt
        if (last != null && (Geo.dist(me, last) < 120 || now - zoneTime < 15_000)) return
        zoneAt = me
        zoneTime = now
        viewModelScope.launch {
            val r = runCatching { withContext(Dispatchers.IO) { Api.reverse(me.lat, me.lon) } }.getOrNull() ?: return@launch
            val z = r.street.ifBlank { r.district }.ifBlank { r.city }
            if (z.isNotBlank() && z != zoneName) {
                zoneName = z
                zoneStamp++
            }
        }
    }

    // ================= Места =================

    override fun onCameraIdle() {
        val (box, zoom) = renderer.viewport() ?: return
        if (!settings.poiEnabled || zoom < 14.0) return
        if (poiBox?.contains(box) == true) return
        poiJob?.cancel()
        poiJob = viewModelScope.launch {
            delay(500)
            poiLoading = true
            try {
                val big = box.expand(0.3)
                val list = withContext(Dispatchers.IO) { Api.poisInBox(big) }
                if (pois.size > 5000) pois.clear()
                list.forEach { pois[it.id] = it }
                poiBox = big
                renderPois()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = "Места не загрузились (${e.message}). Подвинь карту ещё раз."
            } finally {
                poiLoading = false
            }
        }
    }

    private fun renderPois() {
        if (!settings.poiEnabled) {
            renderer.set("pois", GeoJson.EMPTY)
            return
        }
        val hidden = settings.hiddenCats
        val feats = pois.values.mapNotNull { p ->
            val cat = p.category ?: return@mapNotNull null
            if (cat.name in hidden) return@mapNotNull null
            GeoJson.point(p.point.lat, p.point.lon, mapOf("pid" to p.id, "icon" to "blip_${cat.name}", "name" to p.name))
        }
        renderer.set("pois", GeoJson.fc(feats))
    }

    private fun select(p: Place, fly: Boolean) {
        selected = p
        following = following && !fly
        renderDest()
        if (fly) renderer.moveTo(p.point.lat, p.point.lon, 16.5, null, null, 900)
    }

    fun clearSelection() {
        selected = null
        renderDest()
    }

    private fun renderDest() {
        val p = selected?.point ?: activeTargets.lastOrNull()
        renderer.set("dest", if (p == null) GeoJson.EMPTY else GeoJson.fc(listOf(GeoJson.point(p.lat, p.lon))))
    }

    // ================= Поиск =================

    private var searchJob: Job? = null

    fun openSearch() {
        searchOpen = true
    }

    fun closeSearch() {
        searchOpen = false
        searchJob?.cancel()
        searching = false
    }

    fun clearSearch() {
        query = ""
        searchTitle = null
        results = emptyList()
        searchJob?.cancel()
        searching = false
    }

    fun onQuery(q: String) {
        query = q
        searchTitle = null
        searchJob?.cancel()
        if (q.isBlank()) {
            results = emptyList()
            searching = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            searching = true
            try {
                val c = myPointOrCenter()
                results = withContext(Dispatchers.IO) { Api.photon(q, c?.lat, c?.lon) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = "Поиск не удался: ${e.message}"
            } finally {
                searching = false
            }
        }
    }

    fun searchCategory(cat: PoiCategory) {
        query = ""
        searchTitle = cat.title
        results = emptyList()
        searchJob?.cancel()
        val c = myPointOrCenter() ?: return
        searchJob = viewModelScope.launch {
            searching = true
            try {
                val list = withContext(Dispatchers.IO) { Api.nearby(cat, c.lat, c.lon, 3000) }
                list.forEach { pois[it.id] = it }
                renderPois()
                results = list.sortedBy { Geo.dist(c, it.point) }.take(80)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = "Не удалось найти: ${e.message}"
            } finally {
                searching = false
            }
        }
    }

    fun pickResult(p: Place) {
        searchOpen = false
        if (p.category != null) {
            pois[p.id] = p
            renderPois()
        }
        following = false
        select(p, fly = true)
    }

    // ================= Маршрут до точки и навигация =================

    private var routeJob: Job? = null
    private var offCount = 0

    fun routeToSelected() {
        val p = selected ?: return
        val me = myPoint()
        if (me == null) {
            message = "Жду сигнал GPS…"
            return
        }
        selected = null
        buildRoute(listOf(me, p.point), fit = true, startNav = false)
    }

    private fun buildRoute(targets: List<GeoPoint>, fit: Boolean, startNav: Boolean) {
        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            routeLoading = true
            try {
                val r = withContext(Dispatchers.IO) { Api.route(settings.profile, targets) }
                activeRoute = r
                activeTargets = targets.drop(1)
                renderRoute()
                renderDest()
                if (startNav) startNavigation() else if (fit) renderer.fit(r.points)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = "Маршрут не построен: ${e.message}"
            } finally {
                routeLoading = false
            }
        }
    }

    fun startNavigation() {
        val r = activeRoute ?: return
        nav = NavState(r, activeTargets)
        offCount = 0
        selected = null
        following = true
        followCamera(force = true, zoom = 17.0)
        location?.let { updateNav(it) }
    }

    fun stopRoute() {
        routeJob?.cancel()
        activeRoute = null
        activeTargets = emptyList()
        nav = null
        renderRoute()
        renderDest()
        if (viewMode == ViewMode.MAP) {
            val c = renderer.center() ?: return
            renderer.moveTo(c.lat, c.lon, null, 0f, 0.0, 500)
        }
    }

    private fun renderRoute() {
        val r = activeRoute
        renderer.set("route", if (r == null) GeoJson.EMPTY else GeoJson.fc(listOf(GeoJson.line(r.points))))
    }

    private fun updateNav(l: Location) {
        val n = nav ?: return
        val me = GeoPoint(l.latitude, l.longitude)
        val targets = n.targets.toMutableList()
        if (targets.isNotEmpty() && Geo.dist(me, targets.last()) < 30) {
            stopRoute()
            message = "Вы на месте!"
            return
        }
        if (targets.size > 1 && Geo.dist(me, targets.first()) < 40) targets.removeAt(0)

        val r = n.route
        val snap = Geo.snap(me, r.points, r.cumulative)
        if (snap.distance > 60) {
            offCount++
            if (offCount >= 3 && !routeLoading) {
                offCount = 0
                message = "Перестраиваю маршрут…"
                nav = n.copy(targets = targets)
                buildRoute(listOf(me) + targets, fit = false, startNav = true)
                return
            }
        } else {
            offCount = 0
        }
        if (r.steps.isEmpty()) return
        var stepIdx = r.steps.indexOfFirst { it.pointIndex > snap.index }
        if (stepIdx < 0) stepIdx = r.steps.lastIndex
        val total = r.cumulative.lastOrNull() ?: 0.0
        val remaining = if (total > 0) (total - snap.along).coerceAtLeast(0.0) * r.distance / total else 0.0
        val remTime = if (r.distance > 0) r.duration * remaining / r.distance else 0.0
        nav = n.copy(
            targets = targets,
            stepIndex = stepIdx,
            distToStep = Geo.dist(me, r.steps[stepIdx].location),
            remaining = remaining,
            remainingTime = remTime
        )
    }

    // ================= Свои маршруты =================

    private var draftJob: Job? = null

    fun startEditor() {
        menuOpen = false
        searchOpen = false
        selected = null
        editing = true
        renderDest()
        renderDraft()
        if (draft.isEmpty()) message = "Тапай по карте — каждая точка станет остановкой"
    }

    fun closeEditor() {
        editing = false
        draftJob?.cancel()
        draft.clear()
        draftResult = null
        renderDraft()
    }

    fun addSelectedToDraft() {
        val p = selected ?: return
        selected = null
        renderDest()
        editing = true
        addDraftPoint(p.point.lat, p.point.lon, p.name)
    }

    private fun addDraftPoint(lat: Double, lon: Double, label: String?) {
        draft.add(RoutePoint(lat, lon, label ?: "Точка ${draft.size + 1}"))
        recomputeDraft()
    }

    fun undoDraft() {
        if (draft.isNotEmpty()) draft.removeAt(draft.lastIndex)
        recomputeDraft()
    }

    fun clearDraft() {
        draft.clear()
        recomputeDraft()
    }

    private fun recomputeDraft() {
        draftJob?.cancel()
        draftResult = null
        renderDraft()
        if (draft.size < 2) return
        val pts = draft.map { it.point }
        draftJob = viewModelScope.launch {
            delay(300)
            routeLoading = true
            try {
                draftResult = withContext(Dispatchers.IO) { Api.route(settings.profile, pts) }
                renderDraft()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = "Не удалось проложить: ${e.message}"
            } finally {
                routeLoading = false
            }
        }
    }

    private fun renderDraft() {
        if (draft.isEmpty()) {
            renderer.set("draft", GeoJson.EMPTY)
            return
        }
        val feats = ArrayList<org.json.JSONObject>()
        val line = draftResult?.points ?: if (draft.size >= 2) draft.map { it.point } else null
        if (line != null) feats += GeoJson.line(line)
        draft.forEachIndexed { i, p -> feats += GeoJson.point(p.lat, p.lon, mapOf("n" to i + 1)) }
        renderer.set("draft", GeoJson.fc(feats))
    }

    fun saveDraft(name: String) {
        if (draft.size < 2) {
            message = "Нужно минимум 2 точки"
            return
        }
        val r = SavedRoute(
            id = System.currentTimeMillis(),
            name = name.trim().ifBlank { "Маршрут ${savedRoutes.size + 1}" },
            profile = settings.profile,
            points = draft.toList(),
            distance = draftResult?.distance ?: 0.0
        )
        savedRoutes.add(0, r)
        storage.saveRoutes(savedRoutes)
        message = "Маршрут «${r.name}» сохранён"
    }

    /** Поехали: от моей позиции через все точки своего маршрута. */
    fun startDraft() {
        if (draft.isEmpty()) return
        val me = myPoint()
        val targets = (if (me != null) listOf(me) else emptyList()) + draft.map { it.point }
        if (targets.size < 2) {
            message = "Добавь ещё точку или дождись GPS"
            return
        }
        editing = false
        draftJob?.cancel()
        draft.clear()
        draftResult = null
        renderDraft()
        buildRoute(targets, fit = me == null, startNav = me != null)
    }

    fun showSaved(r: SavedRoute) {
        menuOpen = false
        updateSettings { it.copy(profile = r.profile) }
        editing = true
        selected = null
        draft.clear()
        draft.addAll(r.points)
        recomputeDraft()
        following = false
        renderer.fit(r.points.map { it.point })
    }

    fun startSaved(r: SavedRoute) {
        menuOpen = false
        updateSettings { it.copy(profile = r.profile) }
        draft.clear()
        draft.addAll(r.points)
        startDraft()
    }

    fun deleteSaved(r: SavedRoute) {
        savedRoutes.remove(r)
        storage.saveRoutes(savedRoutes)
    }
}
