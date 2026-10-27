package com.gamemaps.app

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
import com.gamemaps.app.data.Storage
import com.gamemaps.app.location.LocationTracker
import com.gamemaps.app.map.GeoJson
import com.gamemaps.app.map.MapHit
import com.gamemaps.app.map.MapRenderer
import com.gamemaps.app.model.Achievements
import com.gamemaps.app.model.GameStats
import com.gamemaps.app.model.GeoPoint
import com.gamemaps.app.model.Place
import com.gamemaps.app.model.PoiCategory
import com.gamemaps.app.model.Profile
import com.gamemaps.app.model.RoutePoint
import com.gamemaps.app.model.RouteResult
import com.gamemaps.app.model.SavedRoute
import com.gamemaps.app.model.Settings
import com.gamemaps.app.model.TileCats
import com.gamemaps.app.model.ViewMode
import com.gamemaps.app.nav.Geo
import com.gamemaps.app.net.Api
import com.gamemaps.app.style.GameStyle
import com.gamemaps.app.style.GameStyles
import com.gamemaps.app.style.HudKind
import com.gamemaps.app.update.Release
import com.gamemaps.app.update.Updater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    val remainingTime: Double = route.duration,
    val startedAt: Long = SystemClock.elapsedRealtime()
)

enum class UpdateState { IDLE, CHECKING, AVAILABLE, DOWNLOADING, READY, NONE, ERROR }

enum class BannerKind { PASS, WASTED, ACHIEVEMENT, ENCOUNTER }

data class Banner(val kind: BannerKind, val title: String, val sub: String, val id: Long = SystemClock.elapsedRealtimeNanos())

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
    var cameraTilt by mutableFloatStateOf(0f)
        private set
    var metersPerDp by mutableStateOf(1.0)
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
    private var activeOrigin: GeoPoint? = null
    /** Варианты пути (от быстрого к медленному) и какой выбран. */
    var routeOptions by mutableStateOf<List<RouteResult>>(emptyList())
        private set
    var routeChoice by mutableIntStateOf(0)
        private set
    var nav by mutableStateOf<NavState?>(null)
        private set
    var routeLoading by mutableStateOf(false)
        private set
    val savedRoutes = mutableStateListOf<SavedRoute>()

    // --- результаты поиска на карте ---
    private val pois = LinkedHashMap<String, Place>()

    // --- название улицы ---
    var zoneName by mutableStateOf<String?>(null)
        private set
    var zoneStamp by mutableIntStateOf(0)
        private set
    private var zoneAt: GeoPoint? = null
    private var zoneTime = 0L

    // --- игровой слой ---
    var stats by mutableStateOf(storage.loadStats())
        private set
    var wanted by mutableIntStateOf(0)
        private set
    private var wantedOff = false
    private var wantedBoostUntil = 0L
    var banner by mutableStateOf<Banner?>(null)
    var forceNight by mutableStateOf<Boolean?>(null)
        private set
    var nearbyCount by mutableIntStateOf(0)
        private set
    private val trail = ArrayList<GeoPoint>()
    private var lastFix: Location? = null
    private var moneyCarry = 0.0
    private var lastStatsSave = 0L
    private var lastPoliceCheck = 0L
    private var policeAt: GeoPoint? = null
    private var lastEncounter = 0L
    private val seenNames = HashSet<String>()
    private var lastGpsLostBanner = 0L

    // --- офлайн-карты ---
    var offlineAreas by mutableStateOf<List<com.gamemaps.app.map.OfflineArea>>(emptyList())
        private set
    var offlineBusy by mutableStateOf(false)
        private set
    private val offline = com.gamemaps.app.map.Offline(app) { list -> offlineAreas = list }

    /** Скачать город по названию. */
    fun downloadCity(q: String) {
        if (q.isBlank()) return
        viewModelScope.launch {
            offlineBusy = true
            try {
                val res = withContext(Dispatchers.IO) { Api.cityBox(q.trim()) }
                if (res == null) message = "Город «$q» не найден"
                else {
                    val (name, box) = res
                    offline.download(name, box, getApplication<Application>().resources.displayMetrics.density) { e ->
                        message = "Не удалось начать загрузку: $e"
                    }
                    message = "Загружаю «$name» для офлайна"
                }
            } catch (e: Exception) {
                message = "Нужен интернет, чтобы найти город: ${e.message}"
            } finally {
                offlineBusy = false
            }
        }
    }

    /** Скачать то, что сейчас видно на экране (с запасом). */
    fun downloadVisible() {
        val (box, _) = renderer.viewport() ?: return
        val name = zoneName?.let { "Район: $it" } ?: "Область ${java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale("ru")).format(java.util.Date())}"
        offline.download(name, box.expand(0.15), getApplication<Application>().resources.displayMetrics.density) { e ->
            message = "Не удалось начать загрузку: $e"
        }
        message = "Загружаю видимую область"
    }

    fun pauseOffline(id: Long) = offline.pause(id)
    fun resumeOffline(id: Long) = offline.resume(id)
    fun deleteOffline(id: Long) = offline.delete(id)

    // --- автообновление из GitHub Releases ---
    var update by mutableStateOf<Release?>(null)
        private set
    var updateState by mutableStateOf(UpdateState.IDLE)
        private set
    var updateProgress by mutableFloatStateOf(0f)
        private set
    private var updateFile: java.io.File? = null

    init {
        renderer.listener = this
        savedRoutes.addAll(storage.loadRoutes())
        viewModelScope.launch { gameTicker() }
        checkUpdate(silent = true)
    }

    fun checkUpdate(silent: Boolean = false) {
        if (updateState == UpdateState.CHECKING || updateState == UpdateState.DOWNLOADING) return
        viewModelScope.launch {
            updateState = UpdateState.CHECKING
            try {
                val r = withContext(Dispatchers.IO) { Updater.latest() }
                if (r != null && Updater.isNewer(r)) {
                    update = r
                    updateState = UpdateState.AVAILABLE
                } else {
                    updateState = UpdateState.NONE
                    if (!silent) message = "Установлена последняя версия"
                }
            } catch (e: Exception) {
                updateState = if (silent) UpdateState.IDLE else UpdateState.ERROR
                if (!silent) message = "Не удалось проверить: ${e.message}"
            }
        }
    }

    fun downloadUpdate() {
        val r = update ?: return
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            updateState = UpdateState.DOWNLOADING
            updateProgress = 0f
            try {
                val f = withContext(Dispatchers.IO) { Updater.download(ctx, r) { p -> updateProgress = p } }
                updateFile = f
                updateState = UpdateState.READY
                installUpdate()
            } catch (e: Exception) {
                updateState = UpdateState.AVAILABLE
                message = "Ошибка загрузки: ${e.message}"
            }
        }
    }

    fun installUpdate() {
        val f = updateFile ?: return
        if (!Updater.install(getApplication(), f)) message = "Разреши установку из GameMaps и нажми «Установить» ещё раз"
    }

    fun dismissUpdate() {
        updateState = UpdateState.IDLE
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
        renderTrail()
    }

    fun updateSettings(f: (Settings) -> Settings) {
        val n = f(settings)
        if (n == settings) return
        settings = n
        storage.saveSettings(n)
        renderer.apply(style, n)
        renderPois()
    }

    fun selectStyle(id: String) {
        updateSettings { it.copy(styleId = id) }
        if (viewMode == ViewMode.RADAR) followCamera(force = true)
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
            setFollow(true)
            followCamera(force = true)
        } else {
            val c = myPoint().takeIf { following } ?: renderer.center() ?: return
            val b = if (following && settings.headingUp) currentHeading() else 0f
            renderer.moveTo(c.lat, c.lon, null, b, if (nav != null) null else 0.0, 600)
        }
    }

    fun locate() {
        setFollow(true)
        val z = renderer.zoom() ?: 0.0
        followCamera(force = true, zoom = if (z < 14.0) 16.0 else null)
    }

    fun resetNorth() = renderer.resetNorth()

    /** Спутник вкл/выкл. При включении сразу 3D-дома и наклон, как в навигаторах. */
    fun toggleSatellite() {
        val on = !settings.satellite
        updateSettings { it.copy(satellite = on, buildings3d = if (on) true else it.buildings3d) }
        if (on && cameraTilt < 10f) renderer.setTilt(45.0)
        message = if (on) "Спутник" else "Схема"
    }

    /** Наклон камеры для 3D-вида домов. */
    fun toggleTilt() {
        if (cameraTilt > 10f) {
            renderer.setTilt(0.0)
        } else {
            if (!settings.buildings3d) updateSettings { it.copy(buildings3d = true) }
            renderer.setTilt(55.0)
            val z = renderer.zoom() ?: 0.0
            if (z < 15.5) renderer.center()?.let { renderer.moveTo(it.lat, it.lon, 16.5, null, 55.0, 700) }
        }
    }

    override fun onCameraMove(bearing: Float, tilt: Float) {
        cameraBearing = bearing
        cameraTilt = tilt
    }

    override fun onCameraIdle() {
        renderer.metersPerDp()?.let { metersPerDp = it }
    }

    override fun onUserGesture() {
        setFollow(false)
    }

    override fun onTap(lat: Double, lon: Double, hit: MapHit?) {
        if (editing) {
            val label = hit?.props?.optString("name")?.takeIf { it.isNotBlank() }
            addDraftPoint(hit?.lat ?: lat, hit?.lon ?: lon, label)
            return
        }
        if (hit != null) {
            val p = placeFromHit(hit)
            if (p != null) {
                select(p, fly = false)
                if (hit.layer != "poi-blips") enrich(p)
                return
            }
        }
        if (selected != null) clearSelection()
    }

    private fun localName(props: org.json.JSONObject): String {
        val base = props.optString("name")
        return when (settings.labelLang) {
            "ru" -> props.optString("name:ru").ifBlank { base }
            "en" -> props.optString("name:en").ifBlank { props.optString("name_en").ifBlank { base } }
            else -> base
        }
    }

    private fun placeFromHit(hit: MapHit): Place? {
        if (hit.layer == "poi-blips") return pois[hit.props.optString("pid")]
        val cls = hit.props.optString("class")
        val sub = hit.props.optString("subclass")
        val landmark = TileCats.landmarks[cls]
        val cat = if (landmark != null) null else TileCats.categoryOf(cls, sub)
        val title = cat?.title ?: landmark ?: "Место"
        val name = localName(hit.props).ifBlank { title }
        val subTxt = sub.replace('_', ' ').takeIf { it.isNotBlank() && it != cls }
        return Place(
            id = "t_${cls}_${hit.lat}_${hit.lon}",
            name = name,
            point = GeoPoint(hit.lat, hit.lon),
            category = cat,
            kind = listOfNotNull(title, subTxt).joinToString(" · ")
        )
    }

    /** Догружаем адрес, часы работы и телефон для места, по которому тапнули. */
    private fun enrich(p: Place) {
        viewModelScope.launch {
            val d = runCatching { withContext(Dispatchers.IO) { Api.details(p.point.lat, p.point.lon, p.name) } }.getOrNull()
            var cur = selected ?: return@launch
            if (cur.id != p.id) return@launch
            if (d != null) {
                cur = cur.copy(
                    address = d.address.ifBlank { cur.address },
                    hours = d.hours.ifBlank { cur.hours },
                    phone = d.phone.ifBlank { cur.phone }
                )
                selected = cur
            }
            if (cur.address.isBlank()) {
                val r = runCatching { withContext(Dispatchers.IO) { Api.reverse(p.point.lat, p.point.lon) } }.getOrNull()
                val now = selected
                if (r != null && now != null && now.id == p.id) selected = now.copy(address = r.label)
            }
        }
    }

    override fun onLongPress(lat: Double, lon: Double) {
        if (editing) {
            addDraftPoint(lat, lon, null)
            return
        }
        val p = Place(id = "wp_${lat}_$lon", name = "Метка", point = GeoPoint(lat, lon), kind = "Своя точка")
        select(p, fly = false)
        viewModelScope.launch {
            val r = runCatching { withContext(Dispatchers.IO) { Api.reverse(lat, lon) } }.getOrNull()
            val cur = selected
            if (r != null && cur != null && cur.id == p.id) selected = cur.copy(address = r.label)
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

    fun stopTracking() {
        tracker.stop()
        storage.saveStats(stats)
    }

    override fun onCleared() {
        tracker.stop()
        storage.saveStats(stats)
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
        trackProgress(l)
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

    private fun headingUpActive() = settings.headingUp

    /** Пока карта следует за игроком, стрелку рисует оверлей в центре экрана (без мигания). */
    private fun setFollow(v: Boolean) {
        if (following == v) return
        following = v
        updatePlayer()
    }

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
            (radar || navOn) && settings.tilt3d && style.radarTilt -> 50.0
            radar || navOn -> 0.0
            else -> null
        }
        val z = zoom ?: if (force && (radar || navOn)) 17.0 else null
        renderer.moveTo(l.latitude, l.longitude, z, bearing, tilt, if (force) 900 else 650)
    }

    private fun updatePlayer() {
        val l = location ?: return
        if (following) {
            renderer.set("player", GeoJson.EMPTY)
            return
        }
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

    // ================= Игровой слой =================

    /** Пробег, деньги, шаги, след и достижения. */
    private fun trackProgress(l: Location) {
        val prev = lastFix
        if (l.hasAccuracy() && l.accuracy > 40f) return
        lastFix = l
        if (prev == null) return
        val d = Geo.dist(GeoPoint(prev.latitude, prev.longitude), GeoPoint(l.latitude, l.longitude))
        if (d < 2.0 || d > 300.0) return
        val walking = !l.hasSpeed() || l.speed < 2.5f
        moneyCarry += d / 10.0
        val earned = moneyCarry.toLong()
        moneyCarry -= earned
        stats = stats.copy(
            money = stats.money + earned,
            distance = stats.distance + d,
            walk = if (walking) stats.walk + d else stats.walk
        )
        // след
        val me = GeoPoint(l.latitude, l.longitude)
        if (trail.isEmpty() || Geo.dist(trail.last(), me) > 5) {
            trail.add(me)
            if (trail.size > 3000) trail.subList(0, 500).clear()
            renderTrail()
        }
        val km = stats.distance / 1000
        if (stats.distance >= 100) unlock("m100")
        if (km >= 1) unlock("km1")
        if (km >= 10) unlock("km10")
        if (km >= 42.2) unlock("km42")
        if (km >= 100) unlock("km100")
        if (stats.money >= 1_000_000) unlock("rich")
        val now = SystemClock.elapsedRealtime()
        if (now - lastStatsSave > 30_000) {
            lastStatsSave = now
            storage.saveStats(stats)
        }
    }

    private fun renderTrail() {
        renderer.set("trail", if (trail.size < 2) GeoJson.EMPTY else GeoJson.fc(listOf(GeoJson.line(trail))))
    }

    fun clearTrail() {
        trail.clear()
        renderTrail()
        message = "След очищен"
    }

    private fun unlock(id: String) {
        if (id in stats.achievements) return
        stats = stats.copy(achievements = stats.achievements + id)
        storage.saveStats(stats)
        banner = Banner(BannerKind.ACHIEVEMENT, "Достижение получено!", Achievements.title(id))
    }

    private fun addMoney(v: Long) {
        stats = stats.copy(money = (stats.money + v).coerceAtLeast(0))
        if (stats.money >= 1_000_000) unlock("rich")
        storage.saveStats(stats)
    }

    private fun wasted(sub: String) {
        val g = style
        banner = Banner(BannerKind.WASTED, g.wastedTitle, sub)
    }

    /** Фоновый цикл: розыск у полиции, «встречи» с магазинами, потеря GPS. */
    private suspend fun gameTicker() {
        while (viewModelScope.isActive) {
            delay(5000)
            val l = location ?: continue
            val now = SystemClock.elapsedRealtime()
            val me = GeoPoint(l.latitude, l.longitude)

            // потеря GPS → «WASTED»
            val age = (SystemClock.elapsedRealtimeNanos() - l.elapsedRealtimeNanos) / 1_000_000L
            if (hasPermission && age > 45_000 && now - lastGpsLostBanner > 10 * 60_000) {
                lastGpsLostBanner = now
                wasted("Сигнал GPS потерян")
            }

            // розыск: чем ближе отделение полиции, тем больше звёзд
            if (wantedOff) {
                wanted = 0
            } else if (now < wantedBoostUntil) {
                wanted = maxOf(wanted, 4)
            } else {
                val pa = policeAt
                if (now - lastPoliceCheck > 60_000 && (pa == null || Geo.dist(pa, me) > 120)) {
                    lastPoliceCheck = now
                    policeAt = me
                    val d = runCatching { withContext(Dispatchers.IO) { Api.policeNear(me.lat, me.lon, 400) } }.getOrNull()
                    wanted = when {
                        d == null -> 0
                        d < 100 -> 3
                        d < 220 -> 2
                        else -> 1
                    }
                } else if (pa != null && Geo.dist(pa, me) > 500) {
                    wanted = 0
                }
            }

            // «встречи» и профайлер: магазины рядом с игроком
            val hud = style.hud
            if (hud == HudKind.POCKET || hud == HudKind.HACKER) {
                val near = renderer.tilePoisNear(me.lat, me.lon, 45.0)
                nearbyCount = near.size
                if (now - lastEncounter > 45_000) {
                    val fresh = near.map { localName(it.props) }.firstOrNull { it.isNotBlank() && it !in seenNames }
                    if (fresh != null) {
                        lastEncounter = now
                        seenNames.add(fresh)
                        stats = stats.copy(encounters = stats.encounters + 1)
                        unlock("encounter")
                        banner = if (hud == HudKind.POCKET)
                            Banner(BannerKind.ENCOUNTER, "Дикий «$fresh» появился!", "Что будешь делать? ▶ПРОЙТИ МИМО")
                        else
                            Banner(BannerKind.ENCOUNTER, "ПРОФАЙЛЕР: $fresh", "Wi-Fi найден · уязвимостей: ${1 + fresh.length % 4}")
                    }
                }
            }
        }
    }

    /** Чит-коды из игр (вводятся в строку поиска). Возвращает true, если код сработал. */
    private fun tryCheat(raw: String): Boolean {
        val up = raw.trim().uppercase().replace(" ", "")
        val low = raw.trim().lowercase()
        val text: String = when (up) {
            "HESOYAM" -> { addMoney(250_000); "Здоровье, броня и \$250 000" }
            "AEZAKMI", "LEAVEMEALONE" -> { wantedOff = true; wanted = 0; "Розыск больше не грозит" }
            "ASNAEB" -> { wantedOff = false; wantedBoostUntil = 0; wanted = 0; "Розыск сброшен" }
            "OSRBLHH", "BRINGITON" -> { wantedOff = false; wantedBoostUntil = SystemClock.elapsedRealtime() + 120_000; wanted = 4; "Полиция в бешенстве на 2 минуты" }
            "BAGUVIX" -> "Бесконечное здоровье"
            "YECGAA" -> "Джетпак выдан. Смотри в небо"
            "PANZER", "AIWPRTON" -> "Танк уже едет к тебе"
            "IDDQD" -> "Режим бога включён"
            "IDKFA" -> "Все ключи и оружие"
            "ROSEBUD" -> { addMoney(1_000); "+1 000 симолеонов" }
            "MOTHERLODE" -> { addMoney(50_000); "+50 000 симолеонов" }
            "SZCMAWO" -> { wasted("Чит-код"); "Ну ты сам попросил" }
            "GROVESTREET", "SANANDREAS" -> { selectStyle("sa"); "Grove Street. Home." }
            "LOSSANTOS" -> { selectStyle("v"); "Добро пожаловать в Лос-Сантос" }
            "VICECITY" -> { selectStyle("vice"); "Добро пожаловать в 1986-й" }
            "REDPILL" -> { selectStyle("matrix"); "Добро пожаловать в реальный мир" }
            "BLUEPILL" -> { selectStyle("v"); "Ты проснёшься в своей кровати" }
            "WARNEVERCHANGES" -> { selectStyle("pip"); "War. War never changes." }
            "UUDDLRLRBA", "KONAMI", "↑↑↓↓←→←→BA" -> {
                stats = stats.copy(secret = true)
                unlock("secret")
                selectStyle("vapor")
                "Секретный стиль открыт"
            }
            else -> when (low) {
                "/time set night" -> { forceNight = true; "Время установлено: ночь" }
                "/time set day" -> { forceNight = false; "Время установлено: день" }
                "/time reset" -> { forceNight = null; "Время снова реальное" }
                "/gamemode creative" -> "Творческий режим. Летать всё равно нельзя"
                "/kill" -> { wasted("/kill"); "Игрок погиб" }
                else -> return false
            }
        }
        stats = stats.copy(cheats = stats.cheats + 1)
        storage.saveStats(stats)
        unlock("cheater")
        message = "ЧИТ-КОД АКТИВИРОВАН · $text"
        return true
    }

    // ================= Места из поиска =================

    private fun renderPois() {
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
        if (fly) setFollow(false)
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
        if (tryCheat(q)) {
            query = ""
            results = emptyList()
            searchOpen = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(450)
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
                val list = withContext(Dispatchers.IO) { Api.routes(settings.profile, targets) }
                val r = list.first()
                // при перестройке в пути варианты не показываем — сразу лучший
                routeOptions = if (startNav) emptyList() else list
                routeChoice = 0
                activeRoute = r
                activeOrigin = targets.first()
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

    /** Выбрать один из вариантов пути. */
    fun chooseRoute(i: Int) {
        val r = routeOptions.getOrNull(i) ?: return
        routeChoice = i
        activeRoute = r
        renderRoute()
    }

    /** Сменить способ передвижения у показанного маршрута и пересчитать варианты. */
    fun changeRouteMode(p: Profile) {
        updateSettings { it.copy(profile = p) }
        val from = myPoint() ?: activeOrigin ?: return
        if (activeTargets.isEmpty()) return
        buildRoute(listOf(from) + activeTargets, fit = true, startNav = false)
    }

    fun startNavigation() {
        val r = activeRoute ?: return
        val prev = nav
        nav = NavState(r, activeTargets, startedAt = prev?.startedAt ?: SystemClock.elapsedRealtime())
        renderRoute()
        offCount = 0
        selected = null
        setFollow(true)
        followCamera(force = true, zoom = 17.0)
        location?.let { updateNav(it) }
    }

    fun stopRoute() {
        routeJob?.cancel()
        activeRoute = null
        activeTargets = emptyList()
        routeOptions = emptyList()
        routeChoice = 0
        nav = null
        renderRoute()
        renderDest()
        if (viewMode == ViewMode.MAP) {
            val c = renderer.center() ?: return
            renderer.moveTo(c.lat, c.lon, null, 0f, 0.0, 500)
        }
    }

    private fun onArrived(n: NavState) {
        val reward = 500L + (n.route.distance / 2).toLong()
        addMoney(reward)
        stats = stats.copy(routesDone = stats.routesDone + 1)
        storage.saveStats(stats)
        val g = style
        val secs = (SystemClock.elapsedRealtime() - n.startedAt) / 1000
        val sub = when (g.hud) {
            HudKind.RACER -> "Время ${secs / 60}:${String.format("%02d", secs % 60)} · +${g.moneyPrefix}$reward"
            else -> "${g.passSub} ${g.moneyPrefix}$reward".trim()
        }
        banner = Banner(BannerKind.PASS, g.passTitle, sub)
        if (stats.routesDone >= 1) unlock("route1")
        if (stats.routesDone >= 10) unlock("route10")
    }

    private fun renderRoute() {
        val r = activeRoute
        renderer.set("route", when {
            r == null -> GeoJson.EMPTY
            r.segments.isEmpty() -> GeoJson.fc(listOf(GeoJson.line(r.points)))
            else -> GeoJson.fc(r.segments.filter { it.points.size >= 2 }.map { seg ->
                val p = HashMap<String, Any>()
                p["walk"] = seg.walk
                seg.color?.let { p["color"] = com.gamemaps.app.style.MapStyleJson.css(it) }
                GeoJson.line(seg.points, p)
            })
        })
        // остальные варианты — серыми линиями под выбранным
        val alts = if (nav == null) routeOptions.filter { it !== r } else emptyList()
        renderer.set("routealt", if (alts.isEmpty()) GeoJson.EMPTY else GeoJson.fc(alts.map { GeoJson.line(it.points) }))
    }

    private fun updateNav(l: Location) {
        val n = nav ?: return
        val me = GeoPoint(l.latitude, l.longitude)
        val targets = n.targets.toMutableList()
        if (targets.isNotEmpty() && Geo.dist(me, targets.last()) < 30) {
            stopRoute()
            onArrived(n)
            return
        }
        if (targets.size > 1 && Geo.dist(me, targets.first()) < 40) targets.removeAt(0)

        val r = n.route
        val snap = Geo.snap(me, r.points, r.cumulative)
        if (snap.distance > 60 && r.profile != Profile.METRO) {
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
        setFollow(false)
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
