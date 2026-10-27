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
    FOOT("Пешком", "routed-foot"),
    /** Метро + пешком до станций; пешие куски строит routed-foot. */
    METRO("Метро", "routed-foot")
}

/** Категории мест. glyph — значок-«блип» как на радаре в играх. */
enum class PoiCategory(val title: String, val glyph: String, val color: Int, val overpass: List<String>) {
    GROCERY("Продукты", "24", 0xFF3FA34D.toInt(), listOf(
        "[\"shop\"~\"^(supermarket|convenience|grocery|greengrocer|bakery|butcher|alcohol|beverages|deli|dairy|pastry|confectionery|seafood|frozen_food)\$\"]",
        "[\"amenity\"=\"marketplace\"]")),
    MALL("Торговые центры", "ТЦ", 0xFFE0B020.toInt(), listOf("[\"shop\"~\"^(mall|department_store)\$\"]")),
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
    GYM("Спортзалы", "W", 0xFFFF8F1F.toInt(), listOf("[\"leisure\"=\"fitness_centre\"]")),
    TRANSIT("Метро и вокзалы", "M", 0xFFD32F2F.toInt(), listOf("[\"railway\"~\"^(station|halt)\$\"]", "[\"station\"=\"subway\"]"));

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
            if (tags["railway"] == "station" || tags["railway"] == "halt" || tags["station"] == "subway") return TRANSIT
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

/** Кусок маршрута для отрисовки: пешком (пунктир) или поездка по линии метро своего цвета. */
data class RouteSeg(
    val points: List<GeoPoint>,
    val walk: Boolean,
    val color: Int? = null,
    /** Для поездки: название линии. */
    val line: String = "",
    /** Для поездки: сколько перегонов. */
    val stops: Int = 0,
    val duration: Double = 0.0
)

data class RouteResult(
    val points: List<GeoPoint>,
    val cumulative: DoubleArray,
    val distance: Double,
    val duration: Double,
    val steps: List<RouteStep>,
    val profile: Profile,
    /** Пусто — обычный маршрут одной линией. */
    val segments: List<RouteSeg> = emptyList(),
    /** Короткое описание варианта: улицы или линии метро. */
    val summary: String = ""
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
    val showBars: Boolean = true,
    val trail: Boolean = true,
    val effects: Boolean = true,
    val gameHud: Boolean = true,
    val textures: Boolean = true,
    val satellite: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject()
        .put("styleId", styleId).put("labels", labels).put("labelLang", labelLang)
        .put("buildings", buildings).put("buildings3d", buildings3d)
        .put("poiEnabled", poiEnabled).put("poiNames", poiNames)
        .put("hiddenCats", JSONArray(hiddenCats.toList())).put("blipScale", blipScale.toDouble())
        .put("profile", profile.name).put("headingUp", headingUp).put("tilt3d", tilt3d)
        .put("keepScreenOn", keepScreenOn).put("showClock", showClock)
        .put("showZone", showZone).put("showBars", showBars)
        .put("trail", trail).put("effects", effects).put("gameHud", gameHud).put("textures", textures).put("satellite", satellite)

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
                showBars = o.optBoolean("showBars", d.showBars),
                trail = o.optBoolean("trail", d.trail),
                effects = o.optBoolean("effects", d.effects),
                gameHud = o.optBoolean("gameHud", d.gameHud),
                textures = o.optBoolean("textures", d.textures),
                satellite = o.optBoolean("satellite", d.satellite)
            )
        }
    }
}

/** Сопоставление POI из векторных тайлов (схема OpenMapTiles) с категориями. */
object TileCats {
    val bySubclass: Map<PoiCategory, List<String>> = linkedMapOf(
        PoiCategory.GROCERY to listOf("supermarket", "convenience", "grocery", "greengrocer", "bakery", "butcher",
            "alcohol", "beverages", "deli", "dairy", "pastry", "confectionery", "seafood", "frozen_food",
            "marketplace", "cheese", "wine", "tea", "coffee", "farm"),
        PoiCategory.MALL to listOf("mall", "department_store"),
        PoiCategory.CLOTHES to listOf("clothes", "shoes", "fashion", "boutique", "bag", "jewelry", "fabric",
            "sports", "tailor", "fashion_accessories", "watches"),
        PoiCategory.ELECTRONICS to listOf("electronics", "mobile_phone", "computer", "hifi", "appliance", "video_games", "camera"),
        PoiCategory.CAR to listOf("car", "car_repair", "car_parts", "tyres", "motorcycle", "car_wash"),
        PoiCategory.FOOD to listOf("restaurant", "fast_food", "food_court"),
        PoiCategory.CAFE to listOf("cafe", "ice_cream"),
        PoiCategory.BAR to listOf("bar", "pub", "biergarten", "nightclub"),
        PoiCategory.FUEL to listOf("fuel", "charging_station"),
        PoiCategory.PHARMACY to listOf("pharmacy", "chemist"),
        PoiCategory.HOSPITAL to listOf("hospital", "clinic", "doctors", "dentist"),
        PoiCategory.BANK to listOf("bank", "atm", "bureau_de_change"),
        PoiCategory.POLICE to listOf("police"),
        PoiCategory.GYM to listOf("fitness_centre", "sports_centre"),
        PoiCategory.TRANSIT to listOf("station", "subway", "halt")
    )
    val byClass: Map<PoiCategory, List<String>> = linkedMapOf(
        PoiCategory.GROCERY to listOf("grocery", "alcohol_shop", "bakery"),
        PoiCategory.CLOTHES to listOf("clothing_store", "jewelry"),
        PoiCategory.ELECTRONICS to listOf("mobile_phone"),
        PoiCategory.CAR to listOf("car"),
        PoiCategory.FOOD to listOf("restaurant", "fast_food"),
        PoiCategory.CAFE to listOf("cafe", "ice_cream"),
        PoiCategory.BAR to listOf("bar", "beer"),
        PoiCategory.FUEL to listOf("fuel"),
        PoiCategory.PHARMACY to listOf("pharmacy"),
        PoiCategory.HOSPITAL to listOf("hospital", "doctor", "dentist"),
        PoiCategory.BANK to listOf("bank", "atm"),
        PoiCategory.POLICE to listOf("police"),
        PoiCategory.TRANSIT to listOf("railway"),
        PoiCategory.SHOP to listOf("shop", "furniture", "gift", "hardware", "florist", "beauty", "hairdresser",
            "optician", "toys", "music", "books", "garden", "stationery", "bicycle", "laundry", "pet", "doityourself")
    )

    /** Классы, которые считаем «популярными местами» и подписываем названиями. */
    val landmarks: Map<String, String> = linkedMapOf(
        "attraction" to "Достопримечательность", "museum" to "Музей", "theatre" to "Театр", "castle" to "Замок",
        "monument" to "Памятник", "stadium" to "Стадион", "college" to "Учебное заведение",
        "place_of_worship" to "Храм", "town_hall" to "Администрация", "zoo" to "Зоопарк", "cinema" to "Кинотеатр",
        "art_gallery" to "Галерея", "library" to "Библиотека", "park" to "Парк", "aquarium" to "Аквариум",
        "lodging" to "Отель", "embassy" to "Посольство", "post" to "Почта"
    )

    fun categoryOf(cls: String, subclass: String): PoiCategory? {
        for ((cat, list) in bySubclass) if (subclass in list) return cat
        for ((cat, list) in byClass) if (cls in list) return cat
        return null
    }
}

/** Игровая статистика: деньги, пробег, достижения. Хранится локально. */
data class GameStats(
    val money: Long = 0,
    val distance: Double = 0.0,
    val walk: Double = 0.0,
    val routesDone: Int = 0,
    val cheats: Int = 0,
    val encounters: Int = 0,
    val achievements: Set<String> = emptySet(),
    val secret: Boolean = false
) {
    val steps: Long get() = (walk / 0.75).toLong()
    val xp: Long get() = (distance / 10).toLong() + routesDone * 50L + encounters * 10L
    val level: Int get() = kotlin.math.sqrt(xp / 40.0).toInt() + 1
    val levelProgress: Float
        get() {
            val l = level - 1
            val from = 40.0 * l * l
            val to = 40.0 * (l + 1) * (l + 1)
            return ((xp - from) / (to - from)).toFloat().coerceIn(0f, 1f)
        }

    fun toJson(): JSONObject = JSONObject().put("money", money).put("distance", distance).put("walk", walk)
        .put("routesDone", routesDone).put("cheats", cheats).put("encounters", encounters)
        .put("achievements", JSONArray(achievements.toList())).put("secret", secret)

    companion object {
        fun fromJson(o: JSONObject): GameStats {
            val a = o.optJSONArray("achievements")
            return GameStats(
                money = o.optLong("money"), distance = o.optDouble("distance", 0.0), walk = o.optDouble("walk", 0.0),
                routesDone = o.optInt("routesDone"), cheats = o.optInt("cheats"), encounters = o.optInt("encounters"),
                achievements = if (a == null) emptySet() else (0 until a.length()).map { a.getString(it) }.toSet(),
                secret = o.optBoolean("secret")
            )
        }
    }
}

object Achievements {
    val all: List<Pair<String, String>> = listOf(
        "m100" to "Первые шаги — пройти 100 м",
        "km1" to "Разминка — 1 км",
        "km10" to "Дальнобойщик — 10 км",
        "km42" to "Марафонец — 42 км",
        "km100" to "Путешественник — 100 км",
        "route1" to "Первая миссия — доехать по маршруту",
        "route10" to "Профи — 10 маршрутов",
        "rich" to "Миллионер — \$1 000 000",
        "cheater" to "Читер — ввести чит-код",
        "secret" to "Знаток кодов — открыть секретный стиль",
        "encounter" to "Первая встреча — дикий магазин"
    )

    fun title(id: String): String = all.firstOrNull { it.first == id }?.second?.substringBefore(" —") ?: id
}
