package com.gamemaps.app.map

import android.content.Context
import com.gamemaps.app.model.Box
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

/** Скачанная область для офлайна. */
data class OfflineArea(
    val id: Long,
    val name: String,
    val progress: Float,
    val bytes: Long,
    val complete: Boolean,
    val active: Boolean,
    val error: String? = null
)

/**
 * Офлайн-карты через MapLibre OfflineManager.
 * Регион качается по стилю OpenFreeMap «liberty» — он ссылается на те же тайлы
 * (tiles.openfreemap.org/planet) и шрифты, что и все игровые стили приложения,
 * поэтому скачанные данные работают для любого стиля.
 */
class Offline(ctx: Context, private val onChange: (List<OfflineArea>) -> Unit) {

    private val om = OfflineManager.getInstance(ctx.applicationContext)
    private val regions = LinkedHashMap<Long, OfflineRegion>()
    private val areas = LinkedHashMap<Long, OfflineArea>()

    init {
        om.setOfflineMapboxTileCountLimit(200_000)
        // то, что просто смотрел, тоже остаётся в кэше (до 300 МБ)
        om.setMaximumAmbientCacheSize(300L * 1024 * 1024, null)
        refresh()
    }

    private fun publish() = onChange(areas.values.toList())

    private fun nameOf(r: OfflineRegion): String =
        runCatching { JSONObject(String(r.metadata)).optString("name") }.getOrNull()?.ifBlank { null } ?: "Область ${r.id}"

    fun refresh() {
        om.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                regions.clear()
                val old = HashMap(areas)
                areas.clear()
                offlineRegions?.forEach { r ->
                    regions[r.id] = r
                    areas[r.id] = old[r.id] ?: OfflineArea(r.id, nameOf(r), 0f, 0, false, false)
                    r.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                        override fun onStatus(status: OfflineRegionStatus?) {
                            if (status != null) update(r, status)
                        }
                        override fun onError(error: String?) {}
                    })
                }
                publish()
            }
            override fun onError(error: String) {}
        })
    }

    private fun update(r: OfflineRegion, s: OfflineRegionStatus) {
        val req = s.requiredResourceCount.coerceAtLeast(1)
        val prev = areas[r.id]
        areas[r.id] = OfflineArea(
            id = r.id,
            name = prev?.name ?: nameOf(r),
            progress = (s.completedResourceCount.toFloat() / req).coerceIn(0f, 1f),
            bytes = s.completedResourceSize,
            complete = s.isComplete,
            active = s.downloadState == OfflineRegion.STATE_ACTIVE && !s.isComplete,
            error = prev?.error
        )
        publish()
    }

    private fun observe(r: OfflineRegion) {
        r.setObserver(object : OfflineRegion.OfflineRegionObserver {
            override fun onStatusChanged(status: OfflineRegionStatus) {
                update(r, status)
                if (status.isComplete) r.setDownloadState(OfflineRegion.STATE_INACTIVE)
            }
            override fun onError(error: OfflineRegionError) {
                val a = areas[r.id] ?: return
                areas[r.id] = a.copy(error = error.message.ifBlank { error.reason })
                publish()
            }
            override fun mapboxTileCountLimitExceeded(limit: Long) {
                val a = areas[r.id] ?: return
                areas[r.id] = a.copy(error = "Слишком большая область")
                publish()
                r.setDownloadState(OfflineRegion.STATE_INACTIVE)
            }
        })
        r.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }

    /** Скачать область (south, west, north, east) под именем name. */
    fun download(name: String, box: Box, pixelRatio: Float, onError: (String) -> Unit) {
        val bounds = LatLngBounds.Builder()
            .include(LatLng(box.n, box.e))
            .include(LatLng(box.s, box.w))
            .build()
        val def = OfflineTilePyramidRegionDefinition(STYLE_URL, bounds, 6.0, 14.0, pixelRatio)
        val meta = JSONObject().put("name", name).toString().toByteArray()
        om.createOfflineRegion(def, meta, object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion) {
                regions[offlineRegion.id] = offlineRegion
                areas[offlineRegion.id] = OfflineArea(offlineRegion.id, name, 0f, 0, false, true)
                publish()
                observe(offlineRegion)
            }
            override fun onError(error: String) = onError(error)
        })
    }

    fun resume(id: Long) {
        val r = regions[id] ?: return
        areas[id]?.let { areas[id] = it.copy(error = null, active = true) }
        publish()
        observe(r)
    }

    fun pause(id: Long) {
        regions[id]?.setDownloadState(OfflineRegion.STATE_INACTIVE)
        areas[id]?.let { areas[id] = it.copy(active = false) }
        publish()
    }

    fun delete(id: Long) {
        val r = regions[id] ?: return
        r.setDownloadState(OfflineRegion.STATE_INACTIVE)
        r.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
            override fun onDelete() {
                regions.remove(id)
                areas.remove(id)
                publish()
            }
            override fun onError(error: String) {}
        })
    }

    companion object {
        const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
    }
}
