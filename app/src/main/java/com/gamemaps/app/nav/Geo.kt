package com.gamemaps.app.nav

import com.gamemaps.app.model.GeoPoint
import java.util.Locale
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val R = 6371000.0

    fun dist(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
        return 2 * R * asin(min(1.0, sqrt(h)))
    }

    fun cumulative(pts: List<GeoPoint>): DoubleArray {
        val c = DoubleArray(pts.size)
        for (i in 1 until pts.size) c[i] = c[i - 1] + dist(pts[i - 1], pts[i])
        return c
    }

    /** Ближайшая вершина линии, начиная с индекса from (шаги маршрута идут по порядку). */
    fun nearestVertex(p: GeoPoint, pts: List<GeoPoint>, from: Int): Int {
        var best = Double.MAX_VALUE
        var bi = from.coerceIn(0, maxOf(0, pts.size - 1))
        for (i in bi until pts.size) {
            val d = dist(p, pts[i])
            if (d < best) { best = d; bi = i }
            if (d < 0.5) break
        }
        return bi
    }

    data class Snap(val index: Int, val distance: Double, val along: Double)

    /** Привязка точки к линии маршрута: номер сегмента, отступ от линии и пройденное расстояние. */
    fun snap(p: GeoPoint, pts: List<GeoPoint>, cum: DoubleArray): Snap {
        if (pts.size < 2) return Snap(0, if (pts.isEmpty()) Double.MAX_VALUE else dist(p, pts[0]), 0.0)
        val kx = cos(Math.toRadians(p.lat)) * R * PI / 180.0
        val ky = R * PI / 180.0
        var best = Double.MAX_VALUE
        var bi = 0
        var bt = 0.0
        for (i in 0 until pts.size - 1) {
            val ax = (pts[i].lon - p.lon) * kx
            val ay = (pts[i].lat - p.lat) * ky
            val bx = (pts[i + 1].lon - p.lon) * kx
            val by = (pts[i + 1].lat - p.lat) * ky
            val dx = bx - ax
            val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 > 0) ((-ax * dx - ay * dy) / len2).coerceIn(0.0, 1.0) else 0.0
            val x = ax + t * dx
            val y = ay + t * dy
            val d = sqrt(x * x + y * y)
            if (d < best) { best = d; bi = i; bt = t }
        }
        val along = cum[bi] + bt * (cum[bi + 1] - cum[bi])
        return Snap(bi, best, along)
    }

    fun fmtDist(m: Double): String =
        if (m < 1000) "${((m / 10).roundToInt() * 10)} м"
        else String.format(Locale("ru"), "%.1f км", m / 1000)

    fun fmtDur(s: Double): String {
        val min = (s / 60).roundToInt().coerceAtLeast(1)
        return if (min < 60) "$min мин" else "${min / 60} ч ${min % 60} мин"
    }
}
