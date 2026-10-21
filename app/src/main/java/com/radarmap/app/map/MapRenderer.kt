package com.radarmap.app.map

import android.content.Context
import android.graphics.RectF
import com.radarmap.app.model.Box
import com.radarmap.app.model.GeoPoint
import com.radarmap.app.model.PoiCategory
import com.radarmap.app.model.Settings
import com.radarmap.app.style.GameStyle
import com.radarmap.app.style.MapStyleJson
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource

/** Всё, что касается MapLibre: стиль, источники данных, камера, жесты. */
class MapRenderer(ctx: Context) {

    interface Listener {
        fun onTap(lat: Double, lon: Double, poiId: String?)
        fun onLongPress(lat: Double, lon: Double)
        fun onCameraIdle()
        fun onCameraMove(bearing: Float)
        fun onUserGesture()
    }

    var listener: Listener? = null
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var styleJson: String? = null
    private val sourceData = HashMap<String, String>()
    private val density = ctx.resources.displayMetrics.density

    fun attach(m: MapLibreMap, g: GameStyle, s: Settings, start: GeoPoint?) {
        map = m
        style = null
        styleJson = null
        m.uiSettings.isCompassEnabled = false
        m.uiSettings.isLogoEnabled = false
        m.uiSettings.isAttributionEnabled = false

        m.addOnMapClickListener { ll ->
            val pt = m.projection.toScreenLocation(ll)
            val r = 18 * density
            val id = runCatching {
                m.queryRenderedFeatures(RectF(pt.x - r, pt.y - r, pt.x + r, pt.y + r), "poi-blips")
                    .firstOrNull()?.getStringProperty("pid")
            }.getOrNull()
            listener?.onTap(ll.latitude, ll.longitude, id)
            true
        }
        m.addOnMapLongClickListener { ll ->
            listener?.onLongPress(ll.latitude, ll.longitude)
            true
        }
        m.addOnCameraIdleListener { listener?.onCameraIdle() }
        m.addOnCameraMoveListener { listener?.onCameraMove(m.cameraPosition.bearing.toFloat()) }
        m.addOnCameraMoveStartedListener { reason ->
            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) listener?.onUserGesture()
        }

        m.cameraPosition = if (start != null)
            CameraPosition.Builder().target(LatLng(start.lat, start.lon)).zoom(16.0).build()
        else
            CameraPosition.Builder().target(LatLng(50.0, 20.0)).zoom(3.5).build()

        apply(g, s)
    }

    /** Применяет игровой стиль и настройки. Пересобирает стиль, только если он реально поменялся. */
    fun apply(g: GameStyle, s: Settings) {
        val m = map ?: return
        val json = MapStyleJson.build(g, s)
        if (json == styleJson) return
        styleJson = json
        style = null
        m.setStyle(Style.Builder().fromJson(json)) { st ->
            if (styleJson != json) return@setStyle
            st.addImage("player", MarkerIcons.player(g, density))
            st.addImage("waypoint", MarkerIcons.waypoint(g, density))
            PoiCategory.entries.forEach { st.addImage("blip_${it.name}", MarkerIcons.blip(g, it, density)) }
            style = st
            for ((id, data) in sourceData) {
                st.getSourceAs<GeoJsonSource>(id)?.setGeoJson(data)
            }
        }
    }

    /** Обновить GeoJSON источника (route, draft, dest, pois, player). */
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
        return Box(b.latSouth, b.lonWest, b.latNorth, b.lonEast) to m.cameraPosition.zoom
    }
}
