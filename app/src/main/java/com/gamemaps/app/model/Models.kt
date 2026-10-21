package com.gamemaps.app.model

import org.json.JSONArray
import org.json.JSONObject

data class GeoPoint(val lat: Double, val lon: Double)

/** Прямоугольник на карте: юг, запад, север, восток. */
data class Box(val s: Double, val w: Double, val n: Double, val e: Double) {
    fun contains(o: Box) = o.s >= s && o.n <= n && o.w >= w && o.e <= e
    fun expand(f: Double): Box {
        val dLat = (n - s) * f
        val dLon = (e - w) * f
        return Box(s - dLat, w - dLon, n + dLat, e + dLon)
    }
}

enum class ViewMode { MAP, RADAR }

enum class Profile(val title: String, val server: String) {
    CAR("Авто", "routed-car"),
    BIKE("Вело", "routed-bike"),
    FOOT("Пешком", "routed-foot")
}

/** Категории мест. glyph — значок-«блип» как на радаре в играх. */
enum class PoiCategory(val title: String, val glyph: String, val color: Int, val overpass: List<String>) {
    GROCERY("Продукты", "24", 0xFF3FA34D.toInt(), listOf(
        "[\"shop\"~\"^(supermarket|convenience|grocery|greengrocer|bakery|butcher|alcohol|beverages|deli|dairy|pastry|confectionery|seafood|frozen_food)\$\"]",
        "[\"amenity\"=\"marketplace\"]")),
    MALL("Торговые центры", "M", 0xFFE0B020.toInt(), listOf("[\"shop\"~\"^(mall|department_store)\$\"]")),
    CLOTHES("Одежда и обувь", "T", 0xFF9C4FD6.toInt(), listOf("[\"shop\"~\"^(clothes|shoes|fashion|boutique|bag|jewelry|fabric|sports|tailor)\$\"]")),
    ELECTRONICS("Электроника", "E", 0xFF2E86DE.toInt(), listOf("[\"shop\"~\"^(electronics|mobile_phone|computer|hifi|appliance|video_games)\$\"]")),
    SHOP("Другие магазины", "S", 0xFF8E8E8E.toInt(), listOf("[\"shop\"]")),
    FOOD("Еда", "F", 0xFFE8742A.toInt(), listOf("[\"amenity\"~\"^(restaurant|fast_food|food_court)\$\"]")),
    CAFE("Кафе", "C", 0xFFB5793B.toInt(), listOf("[\"amenity\"=\"cafe\"]")),
    BAR("Бары и клубы", "Y", 0xFFD6336C.toInt(), listOf("[\"amenity\"~\"^(bar|pub|biergarten|nightclub)\$\"]")),
    FUEL("Заправки", "G", 0xFFD93A2B.toInt(), listOf("[\"amenity\"=\"fuel\"]")),
    CAR("Автосервис", "A", 0xFF607D8B.toInt(), listOf("[\"shop\"~\"^(car|car_repair|car_parts|tyres|motorcycle)\$\"]", "[\"amenity\"=\"car_wash\"]")),
    PHARMACY("Аптеки", "+", 0xFF20B26B.toInt(), listOf("[\"amenity\"=\"pharmacy\"]")),
    HOSPITAL("Больницы", "H", 0xFFE53935.toInt(), listOf("[\"amenity\"~\"^(hospital|clinic|doctors)\$\"]")),
    BANK("Банки и банкоматы", "\$", 0xFF2EAD5B.toInt(), listOf("[\"amenity\"~\"^(bank|atm|bureau_de_change)\$\"]")),
    POLICE("Полиция", "★", 0xFF2B4FD9.toInt(), listOf("[\"amenity\"=\"police\"]")),
    GYM("Спортзалы", "W", 0xFFFF8F1F.toInt(), listOf("[\"leisure\"=\"fitness_centre\"]"));

    companion object {
        private val GROCERY_SHOPS = setOf(
            "supermarket", "convenience", "grocery", "greengrocer", "bakery", "butcher", "alcohol",
            "beverages", "deli", "dairy", "pastry", "confectionery", "seafood", "frozen_food"
        )
        private val CLOTHES_SHOPS = setOf("clothes", "shoes", "fashion", "boutique", "bag", "jewelry", "fabric", "sports", "tailor")
        private val TECH_SHOPS = setOf("electronics", "mobile_phone", "computer", "hifi", "appliance", "video_games")
        private val CAR_SHOPS = setOf("car", "car_repair", "car_parts", "tyres", "motorcycle")

        fun categorize(tags: Map<String, String>): PoiCategory? {
            when (tags["amenity"].orEmpty()) {
                "restaurant", "fast_food", "food_court" -> return FOOD
                "cafe" -> return CAFE
                "bar", "pub", "biergarten", "nightclub" -> return BAR
                "fuel" -> return FUEL
                "pharmacy" -> return PHARMACY
                "hospital", "clinic", "doctors" -> return HOSPITAL
                "bank", "atm", "bureau_de_change" -> return BANK
                "police" -> return POLICE
                "car_wash" -> return CAR
                "marketplace" -> return GROCERY
            }
            if (tags["leisure"] == "fitness_centre") return GYM
            val shop = tags["shop"] ?: return null
            return when (shop) {
                in GROCERY_SHOPS -> GROCERY
                "mall", "department_store" -> MALL
                in CLOTHES_SHOPS -> CLOTHES
                in TECH_SHOPS -> ELECTRONICS
                in CAR_SHOPS -> CAR
                "chemist" -> PHARMACY
                "", "no", "vacant", "yes;no" -> null
                else -> SHOP
            }
        }
    }
}

data class Place(
    val id: String,
    val name: String,
    val point: GeoPoint,
    val category: PoiCategory? = null,
    val kind: String = "",
    val address: String = "",
    val hours: String = "",
    val phone: String = ""
)

data class ReverseInfo(val street: String, val district: String, val city: String, val label: String)

data class RouteStep(
    val text: String,
    val arrow: String,
    val location: GeoPoint,
    val pointIndex: Int
)

data class RouteResult(
    val points: List<GeoPoint>,
    val cumulative: DoubleArray,
    val distance: Double,
    val duration: Double,
    val steps: List<RouteStep>,
    val profile: Profile
)

data class RoutePoint(val lat: Double, val lon: Double, val label: String) {
    val point: GeoPoint get() = GeoPoint(lat, lon)
}

data class SavedRoute(
    val id: Long,
    val name: String,
    val profile: Profile,
    val points: List<RoutePoint>,
    val distance: Double
) {
    fun toJson(): JSONObject {
        val pts = JSONArray()
        points.forEach { pts.put(JSONObject().put("lat", it.lat).put("lon", it.lon).put("label", it.label)) }
        return JSONObject().put("id", id).put("name", name).put("profile", profile.name)
            .put("distance", distance).put("points", pts)
    }

    companion object {
        fun fromJson(o: JSONObject): SavedRoute {
            val arr = o.optJSONArray("points") ?: JSONArray()
            val pts = (0 until arr.length()).map {
                val p = arr.getJSONObject(it)
                RoutePoint(p.getDouble("lat"), p.getDouble("lon"), p.optString("label"))
            }
            val profile = runCatching { Profile.valueOf(o.optString("profile")) }.getOrDefault(Profile.CAR)
            return SavedRoute(o.optLong("id"), o.optString("name"), profile, pts, o.optDouble("distance", 0.0))
        }
    }
}

data class Settings(
    val styleId: String = "v",
    val labels: Boolean = true,
    val labelLang: String = "local",
    val buildings: Boolean = true,
    val buildings3d: Boolean = false,
    val poiEnabled: Boolean = true,
    val poiNames: Boolean = true,
    val hiddenCats: Set<String> = emptySet(),
    val blipScale: Float = 1f,
    val profile: Profile = Profile.CAR,
    val headingUp: Boolean = true,
    val tilt3d: Boolean = true,
    val keepScreenOn: Boolean = true,
    val showClock: Boolean = true,
    val showZone: Boolean = true,
    val showBars: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject()
        .put("styleId", styleId).put("labels", labels).put("labelLang", labelLang)
        .put("buildings", buildings).put("buildings3d", buildings3d)
        .put("poiEnabled", poiEnabled).put("poiNames", poiNames)
        .put("hiddenCats", JSONArray(hiddenCats.toList())).put("blipScale", blipScale.toDouble())
        .put("profile", profile.name).put("headingUp", headingUp).put("tilt3d", tilt3d)
        .put("keepScreenOn", keepScreenOn).put("showClock", showClock)
        .put("showZone", showZone).put("showBars", showBars)

    companion object {
        fun fromJson(o: JSONObject): Settings {
            val d = Settings()
            val hidden = o.optJSONArray("hiddenCats")
            return Settings(
                styleId = o.optString("styleId", d.styleId),
                labels = o.optBoolean("labels", d.labels),
                labelLang = o.optString("labelLang", d.labelLang),
                buildings = o.optBoolean("buildings", d.buildings),
                buildings3d = o.optBoolean("buildings3d", d.buildings3d),
                poiEnabled = o.optBoolean("poiEnabled", d.poiEnabled),
                poiNames = o.optBoolean("poiNames", d.poiNames),
                hiddenCats = if (hidden == null) emptySet() else (0 until hidden.length()).map { hidden.getString(it) }.toSet(),
                blipScale = o.optDouble("blipScale", 1.0).toFloat(),
                profile = runCatching { Profile.valueOf(o.optString("profile")) }.getOrDefault(d.profile),
                headingUp = o.optBoolean("headingUp", d.headingUp),
                tilt3d = o.optBoolean("tilt3d", d.tilt3d),
                keepScreenOn = o.optBoolean("keepScreenOn", d.keepScreenOn),
                showClock = o.optBoolean("showClock", d.showClock),
                showZone = o.optBoolean("showZone", d.showZone),
                showBars = o.optBoolean("showBars", d.showBars)
            )
        }
    }
}
