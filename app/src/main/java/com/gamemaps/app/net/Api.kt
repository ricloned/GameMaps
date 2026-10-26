package com.gamemaps.app.net

import com.gamemaps.app.model.Box
import com.gamemaps.app.model.GeoPoint
import com.gamemaps.app.model.Place
import com.gamemaps.app.model.PoiCategory
import com.gamemaps.app.model.Profile
import com.gamemaps.app.model.ReverseInfo
import com.gamemaps.app.model.RouteResult
import com.gamemaps.app.model.RouteStep
import com.gamemaps.app.nav.Geo
import com.gamemaps.app.nav.Instructions
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Бесплатные открытые сервисы, без ключей и аккаунтов:
 *  - Photon (komoot) — поиск адресов и мест
 *  - Overpass API — все магазины и заведения из OpenStreetMap
 *  - OSRM (FOSSGIS) — маршруты для авто, велосипеда и пешком
 * Все вызовы блокирующие — запускать в Dispatchers.IO.
 */
object Api {
    private const val UA = "GameMaps/1.0 (Android; personal map app)"
    private val OVERPASS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
    )

    private fun http(url: String, body: String? = null): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15000
            c.readTimeout = 40000
            c.setRequestProperty("User-Agent", UA)
            c.setRequestProperty("Accept-Language", "ru,en;q=0.8")
            if (body != null) {
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IOException("сервер ответил $code")
            return text
        } finally {
            c.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun f(d: Double) = String.format(Locale.US, "%.6f", d)

    // ---------- Поиск ----------

    fun photon(q: String, lat: Double?, lon: Double?): List<Place> {
        val sb = StringBuilder("https://photon.komoot.io/api/?limit=15&q=").append(enc(q))
        if (lat != null && lon != null) sb.append("&lat=").append(f(lat)).append("&lon=").append(f(lon))
        val root = JSONObject(http(sb.toString()))
        val arr = root.optJSONArray("features") ?: return emptyList()
        val out = ArrayList<Place>()
        for (i in 0 until arr.length()) {
            val ft = arr.getJSONObject(i)
            val c = ft.getJSONObject("geometry").getJSONArray("coordinates")
            val p = ft.getJSONObject("properties")
            val street = listOf(p.optString("street"), p.optString("housenumber")).filter { it.isNotBlank() }.joinToString(" ")
            val city = p.optString("city")
            val name = p.optString("name").ifBlank { street.ifBlank { city.ifBlank { "Точка" } } }
            val address = listOf(street, p.optString("district"), city)
                .filter { it.isNotBlank() && it != name }.distinct().joinToString(", ")
            val key = p.optString("osm_key")
            val value = p.optString("osm_value")
            val cat = PoiCategory.categorize(mapOf(key to value))
            out += Place(
                id = "ph_${p.optString("osm_type")}${p.optLong("osm_id")}",
                name = name,
                point = GeoPoint(c.getDouble(1), c.getDouble(0)),
                category = cat,
                kind = cat?.title ?: kindRu(key, value),
                address = address
            )
        }
        return out
    }

    /** Границы города по названию (для офлайн-загрузки): имя и Box. */
    fun cityBox(q: String): Pair<String, Box>? {
        val url = "https://photon.komoot.io/api/?limit=8&q=" + enc(q)
        val arr = JSONObject(http(url)).optJSONArray("features") ?: return null
        var fallback: Pair<String, Box>? = null
        for (i in 0 until arr.length()) {
            val ft = arr.getJSONObject(i)
            val p = ft.getJSONObject("properties")
            val name = p.optString("name").ifBlank { q }
            val ext = p.optJSONArray("extent")
            val isPlace = p.optString("osm_key") == "place" || p.optString("osm_value") in setOf("city", "town", "administrative")
            if (ext != null && ext.length() == 4) {
                // extent: [minLon, maxLat, maxLon, minLat]
                val box = Box(ext.getDouble(3), ext.getDouble(0), ext.getDouble(1), ext.getDouble(2))
                if (isPlace) return name to box
                if (fallback == null) fallback = name to box
            } else if (fallback == null) {
                val c = ft.getJSONObject("geometry").getJSONArray("coordinates")
                val lat = c.getDouble(1); val lon = c.getDouble(0)
                fallback = name to Box(lat - 0.12, lon - 0.2, lat + 0.12, lon + 0.2)
            }
        }
        return fallback
    }

    fun reverse(lat: Double, lon: Double): ReverseInfo? {
        val root = JSONObject(http("https://photon.komoot.io/reverse?lat=${f(lat)}&lon=${f(lon)}&limit=1"))
        val arr = root.optJSONArray("features") ?: return null
        if (arr.length() == 0) return null
        val p = arr.getJSONObject(0).getJSONObject("properties")
        val street = p.optString("street").ifBlank {
            if (p.optString("osm_key") == "highway") p.optString("name") else ""
        }
        val hn = p.optString("housenumber")
        val district = p.optString("district").ifBlank { p.optString("locality") }
        val city = p.optString("city")
        val label = listOf(listOf(street, hn).filter { it.isNotBlank() }.joinToString(" "), district, city)
            .filter { it.isNotBlank() }.joinToString(", ")
        return ReverseInfo(street, district, city, label.ifBlank { p.optString("name") })
    }

    private fun kindRu(key: String, value: String): String = when (value) {
        "house", "building" -> "Дом"
        "street", "primary", "secondary", "tertiary", "residential" -> if (key == "highway") "Улица" else value
        "city" -> "Город"
        "town" -> "Город"
        "village" -> "Посёлок"
        "suburb", "district", "neighbourhood" -> "Район"
        "station" -> "Станция"
        "bus_stop" -> "Остановка"
        "park" -> "Парк"
        else -> value.replace('_', ' ')
    }

    // ---------- Магазины и заведения (Overpass) ----------

    private fun overpass(query: String): String {
        var last: Exception? = null
        for (u in OVERPASS) {
            try {
                return http(u, "data=" + enc(query))
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IOException("Overpass недоступен")
    }

    private const val AMENITY_RE =
        "^(restaurant|fast_food|food_court|cafe|bar|pub|biergarten|nightclub|fuel|pharmacy|hospital|clinic|doctors|bank|atm|bureau_de_change|police|car_wash|marketplace)\$"

    fun poisInBox(b: Box): List<Place> {
        val bb = "${f(b.s)},${f(b.w)},${f(b.n)},${f(b.e)}"
        val q = "[out:json][timeout:25];(" +
                "nwr[\"shop\"]($bb);" +
                "nwr[\"amenity\"~\"$AMENITY_RE\"]($bb);" +
                "nwr[\"leisure\"=\"fitness_centre\"]($bb);" +
                ");out tags center 1500;"
        return parseOverpass(overpass(q))
    }

    fun nearby(cat: PoiCategory, lat: Double, lon: Double, radius: Int): List<Place> {
        val around = "around:$radius,${f(lat)},${f(lon)}"
        val body = cat.overpass.joinToString("") { "nwr$it($around);" }
        val q = "[out:json][timeout:25];($body);out tags center 400;"
        return parseOverpass(overpass(q)).filter { it.category == cat }
    }

    private fun parseOverpass(text: String): List<Place> {
        val els = JSONObject(text).optJSONArray("elements") ?: return emptyList()
        val out = ArrayList<Place>(els.length())
        for (i in 0 until els.length()) {
            val e = els.getJSONObject(i)
            val tagsO = e.optJSONObject("tags") ?: continue
            val center = e.optJSONObject("center")
            val lat: Double
            val lon: Double
            if (e.has("lat")) {
                lat = e.getDouble("lat"); lon = e.getDouble("lon")
            } else if (center != null) {
                lat = center.getDouble("lat"); lon = center.getDouble("lon")
            } else continue
            val tags = HashMap<String, String>()
            tagsO.keys().forEach { k -> tags[k] = tagsO.optString(k) }
            val cat = PoiCategory.categorize(tags) ?: continue
            val street = listOfNotNull(tags["addr:street"], tags["addr:housenumber"]).joinToString(" ")
            val address = listOf(street, tags["addr:city"].orEmpty()).filter { it.isNotBlank() }.joinToString(", ")
            val sub = tags["shop"] ?: tags["amenity"] ?: tags["leisure"] ?: ""
            out += Place(
                id = "${e.optString("type").take(1)}${e.optLong("id")}",
                name = tags["name"] ?: tags["brand"] ?: cat.title,
                point = GeoPoint(lat, lon),
                category = cat,
                kind = if (sub.isNotBlank()) "${cat.title} · ${sub.replace('_', ' ')}" else cat.title,
                address = address,
                hours = tags["opening_hours"].orEmpty(),
                phone = tags["phone"] ?: tags["contact:phone"] ?: ""
            )
        }
        return out
    }

    /** Подробности о месте по названию рядом с точкой: часы, телефон, адрес. */
    fun details(lat: Double, lon: Double, name: String): Place? {
        if (name.isBlank()) return null
        val safe = name.replace("\\", "").replace("\"", "\\\"")
        val q = "[out:json][timeout:15];nwr(around:80,${f(lat)},${f(lon)})[\"name\"=\"$safe\"];out tags center 3;"
        val els = JSONObject(overpass(q)).optJSONArray("elements") ?: return null
        for (i in 0 until els.length()) {
            val e = els.getJSONObject(i)
            val t = e.optJSONObject("tags") ?: continue
            val tags = HashMap<String, String>()
            t.keys().forEach { k -> tags[k] = t.optString(k) }
            val street = listOfNotNull(tags["addr:street"], tags["addr:housenumber"]).joinToString(" ")
            return Place(
                id = "d",
                name = name,
                point = GeoPoint(lat, lon),
                category = PoiCategory.categorize(tags),
                address = listOf(street, tags["addr:city"].orEmpty()).filter { it.isNotBlank() }.joinToString(", "),
                hours = tags["opening_hours"].orEmpty(),
                phone = tags["phone"] ?: tags["contact:phone"] ?: ""
            )
        }
        return null
    }

    /** Расстояние до ближайшего отделения полиции в радиусе (или null). */
    fun policeNear(lat: Double, lon: Double, radius: Int): Double? {
        val q = "[out:json][timeout:15];nwr[\"amenity\"=\"police\"](around:$radius,${f(lat)},${f(lon)});out center 10;"
        val els = JSONObject(overpass(q)).optJSONArray("elements") ?: return null
        var best: Double? = null
        val me = GeoPoint(lat, lon)
        for (i in 0 until els.length()) {
            val e = els.getJSONObject(i)
            val c = e.optJSONObject("center")
            val p = when {
                e.has("lat") -> GeoPoint(e.getDouble("lat"), e.getDouble("lon"))
                c != null -> GeoPoint(c.getDouble("lat"), c.getDouble("lon"))
                else -> continue
            }
            val d = Geo.dist(me, p)
            if (best == null || d < best) best = d
        }
        return best
    }

    // ---------- Маршруты (OSRM) ----------

    fun route(profile: Profile, pts: List<GeoPoint>): RouteResult {
        require(pts.size >= 2) { "нужно минимум 2 точки" }
        val coords = pts.joinToString(";") { "${f(it.lon)},${f(it.lat)}" }
        val url = "https://routing.openstreetmap.de/${profile.server}/route/v1/driving/$coords" +
                "?overview=full&geometries=geojson&steps=true"
        val root = JSONObject(http(url))
        if (root.optString("code") != "Ok") throw IOException(root.optString("message", "маршрут не найден"))
        val r = root.getJSONArray("routes").getJSONObject(0)
        val ca = r.getJSONObject("geometry").getJSONArray("coordinates")
        val points = (0 until ca.length()).map {
            val c = ca.getJSONArray(it)
            GeoPoint(c.getDouble(1), c.getDouble(0))
        }
        val steps = ArrayList<RouteStep>()
        var from = 0
        val legs = r.getJSONArray("legs")
        for (li in 0 until legs.length()) {
            val st = legs.getJSONObject(li).getJSONArray("steps")
            val lastLeg = li == legs.length() - 1
            for (si in 0 until st.length()) {
                val s = st.getJSONObject(si)
                val m = s.getJSONObject("maneuver")
                val type = m.optString("type")
                if (type == "depart" && li > 0) continue
                val mod = m.optString("modifier")
                val loc = m.getJSONArray("location")
                val gp = GeoPoint(loc.getDouble(1), loc.getDouble(0))
                val idx = Geo.nearestVertex(gp, points, from)
                from = idx
                steps += RouteStep(
                    text = Instructions.text(type, mod, s.optString("name"), m.optInt("exit", 0), lastLeg),
                    arrow = Instructions.arrow(type, mod),
                    location = gp,
                    pointIndex = idx
                )
            }
        }
        return RouteResult(points, Geo.cumulative(points), r.optDouble("distance", 0.0), r.optDouble("duration", 0.0), steps, profile)
    }
}
