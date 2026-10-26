package com.gamemaps.app.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.gamemaps.app.model.Box
import com.gamemaps.app.model.GeoPoint
import com.gamemaps.app.model.Settings
import com.gamemaps.app.style.GameStyle
import com.gamemaps.app.style.MapStyleJson
import org.json.JSONObject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource

/** Объект, по которому тапнули: слой, свойства и координата. */
data class MapHit(val layer: String, val props: JSONObject, val lat: Double, val lon: Double)

/** Всё, что касается MapLibre: стиль, источники данных, камера, жесты. */
class MapRenderer(ctx: Context) {

    interface Listener {
        fun onTap(lat: Double, lon: Double, hit: MapHit?)
        fun onLongPress(lat: Double, lon: Double)
        fun onCameraIdle()
        fun onCameraMove(bearing: Float, tilt: Float)
        fun onUserGesture()
    }

    private val app = ctx.applicationContext
    var listener: Listener? = null
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var styleJson: String? = null
    private var images: Map<String, Bitmap> = emptyMap()
    private var imagesFor: String? = null
    private val sourceData = HashMap<String, String>()
    private val density = ctx.resources.displayMetrics.density

    private val tapLayers = arrayOf("poi-blips", "poi-tiles", "poi-landmark")

    fun attach(m: MapLibreMap, g: GameStyle, s: Settings, start: GeoPoint?) {
        map = m
        style = null
        styleJson = null
        m.uiSettings.isCompassEnabled = false
        m.uiSettings.isLogoEnabled = false
        m.uiSettings.isAttributionEnabled = false
        // Лёгкий поворот двумя пальцами: не блокируем поворот при одновременном зуме
        m.uiSettings.isRotateGesturesEnabled = true
        m.uiSettings.isIncreaseRotateThresholdWhenScaling = false
        m.uiSettings.isDisableRotateWhenScaling = false
        m.uiSettings.isIncreaseScaleThresholdWhenRotating = true
        runCatching { m.gesturesManager.rotateGestureDetector.angleThreshold = 4f }

        m.addOnMapClickListener { ll ->
            listener?.onTap(ll.latitude, ll.longitude, hitAt(m, ll))
            true
        }
        m.addOnMapLongClickListener { ll ->
            listener?.onLongPress(ll.latitude, ll.longitude)
            true
        }
        m.addOnCameraIdleListener { listener?.onCameraIdle() }
        m.addOnCameraMoveListener { listener?.onCameraMove(m.cameraPosition.bearing.toFloat(), m.cameraPosition.tilt.toFloat()) }
        m.addOnCameraMoveStartedListener { reason ->
            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) listener?.onUserGesture()
        }

        m.cameraPosition = if (start != null)
            CameraPosition.Builder().target(LatLng(start.lat, start.lon)).zoom(16.0).build()
        else
            CameraPosition.Builder().target(LatLng(55.75, 37.62)).zoom(4.0).build()

        apply(g, s)
    }

    private fun hitAt(m: MapLibreMap, ll: LatLng): MapHit? {
        val pt = m.projection.toScreenLocation(ll)
        val r = 18 * density
        for (layer in tapLayers) {
            val f = runCatching {
                m.queryRenderedFeatures(RectF(pt.x - r, pt.y - r, pt.x + r, pt.y + r), layer).firstOrNull()
            }.getOrNull() ?: continue
            return featureToHit(layer, f.toJson(), ll.latitude, ll.longitude)
        }
        return null
    }

    private fun featureToHit(layer: String, json: String, lat: Double, lon: Double): MapHit? = runCatching {
        val o = JSONObject(json)
        val props = o.optJSONObject("properties") ?: JSONObject()
        val geom = o.optJSONObject("geometry")
        var la = lat
        var lo = lon
        if (geom != null && geom.optString("type") == "Point") {
            val c = geom.getJSONArray("coordinates")
            lo = c.getDouble(0); la = c.getDouble(1)
        }
        MapHit(layer, props, la, lo)
    }.getOrNull()

    /** POI из тайлов вокруг точки (для «встреч» и профайлера). */
    fun tilePoisNear(lat: Double, lon: Double, meters: Double): List<MapHit> {
        val m = map ?: return emptyList()
        if (style == null) return emptyList()
        return runCatching {
            val pt = m.projection.toScreenLocation(LatLng(lat, lon))
            val mpp = m.projection.getMetersPerPixelAtLatitude(lat)
            val r = (meters / mpp).toFloat() * density
            m.queryRenderedFeatures(RectF(pt.x - r, pt.y - r, pt.x + r, pt.y + r), "poi-tiles", "poi-landmark")
                .mapNotNull { featureToHit("poi-tiles", it.toJson(), lat, lon) }
        }.getOrDefault(emptyList())
    }

    fun metersPerDp(): Double? {
        val m = map ?: return null
        val t = m.cameraPosition.target ?: return null
        return runCatching { m.projection.getMetersPerPixelAtLatitude(t.latitude) }.getOrNull()
    }

    /** Применяет игровой стиль и настройки. Пересобирает стиль, только если он реально поменялся. */
    fun apply(g: GameStyle, s: Settings) {
        val m = map ?: return
        val json = MapStyleJson.build(g, s)
        if (json == styleJson) return
        styleJson = json
        style = null
        if (imagesFor != g.id) {
            images = MarkerIcons.all(app, g, density) + Textures.generate(g, density)
            imagesFor = g.id
        }
        val imgs = images
        val builder = Style.Builder().fromJson(json)
        for ((id, bmp) in imgs) builder.withImage(id, bmp)
        m.setStyle(builder) { st ->
            if (styleJson != json) return@setStyle
            for ((id, bmp) in imgs) {
                if (st.getImage(id) == null) st.addImage(id, bmp)
            }
            style = st
            for ((id, data) in sourceData) {
                st.getSourceAs<GeoJsonSource>(id)?.setGeoJson(data)
            }
        }
    }

    /** Обновить GeoJSON источника (route, draft, dest, pois, player, trail). */
    fun set(id: String, geojson: String) {
        if (sourceData[id] == geojson) return
        sourceData[id] = geojson
        val st = style ?: return
        if (!st.isFullyLoaded) return
        st.getSourceAs<GeoJsonSource>(id)?.setGeoJson(geojson)
    }

    fun moveTo(lat: Double, lon: Double, zoom: Double?, bearing: Float?, tilt: Double?, ms: Int) {
        val m = map ?: return
        val cur = m.cameraPosition
        val pos = CameraPosition.Builder()
            .target(LatLng(lat, lon))
            .zoom(zoom ?: cur.zoom)
            .bearing(bearing?.toDouble() ?: cur.bearing)
            .tilt(tilt ?: cur.tilt)
            .build()
        if (ms <= 0) m.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
        else m.easeCamera(CameraUpdateFactory.newCameraPosition(pos), ms)
    }

    fun setTilt(t: Double) {
        val c = center() ?: return
        moveTo(c.lat, c.lon, null, null, t, 600)
    }

    fun resetNorth() {
        val c = center() ?: return
        moveTo(c.lat, c.lon, null, 0f, 0.0, 500)
    }

    fun fit(points: List<GeoPoint>, padDp: Int = 70) {
        val m = map ?: return
        if (points.isEmpty()) return
        if (points.size == 1) {
            moveTo(points[0].lat, points[0].lon, 16.0, null, null, 800)
            return
        }
        try {
            val b = LatLngBounds.Builder()
            points.forEach { b.include(LatLng(it.lat, it.lon)) }
            m.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), (padDp * density).toInt()), 900)
        } catch (e: Exception) {
            moveTo(points[0].lat, points[0].lon, 15.0, null, null, 800)
        }
    }

    fun center(): GeoPoint? {
        val t = map?.cameraPosition?.target ?: return null
        return GeoPoint(t.latitude, t.longitude)
    }

    fun zoom(): Double? = map?.cameraPosition?.zoom

    fun viewport(): Pair<Box, Double>? {
        val m = map ?: return null
        val b = m.projection.visibleRegion.latLngBounds
        return Box(b.latitudeSouth, b.longitudeWest, b.latitudeNorth, b.longitudeEast) to m.cameraPosition.zoom
    }
}
