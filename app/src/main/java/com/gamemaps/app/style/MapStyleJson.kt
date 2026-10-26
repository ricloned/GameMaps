package com.gamemaps.app.style

import com.gamemaps.app.model.PoiCategory
import com.gamemaps.app.model.Settings
import com.gamemaps.app.model.TileCats
import com.gamemaps.app.map.Textures
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Собирает стиль MapLibre (JSON) из игрового стиля.
 * Векторные тайлы — OpenFreeMap (схема OpenMapTiles), бесплатно и без ключа.
 */
object MapStyleJson {

    const val EMPTY_FC = "{\"type\":\"FeatureCollection\",\"features\":[]}"
    private const val TILES = "https://tiles.openfreemap.org/planet"
    private const val SAT_TILES = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
    private const val GLYPHS = "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf"

    private fun a(vararg v: Any): JSONArray {
        val j = JSONArray()
        for (x in v) j.put(x)
        return j
    }

    private fun o(vararg p: Pair<String, Any>): JSONObject {
        val j = JSONObject()
        for ((k, v) in p) j.put(k, v)
        return j
    }

    fun css(c: Int): String {
        val al = (c ushr 24) and 0xFF
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        return if (al == 255) String.format(Locale.US, "#%02X%02X%02X", r, g, b)
        else "rgba($r,$g,$b,${String.format(Locale.US, "%.3f", al / 255.0)})"
    }

    private fun classIn(vararg c: String): JSONArray =
        a("match", a("get", "class"), JSONArray(c.toList()), true, false)

    private fun zoomLin(vararg s: Number): JSONArray {
        val arr = JSONArray()
        arr.put("interpolate"); arr.put(a("linear")); arr.put(a("zoom"))
        s.forEach { arr.put(it) }
        return arr
    }

    /** Ширина дорог по зуму: пары (zoom, width). */
    private fun widths(scale: Double, extra: Double, stops: DoubleArray): JSONArray {
        val arr = JSONArray()
        arr.put("interpolate"); arr.put(a("exponential", 1.5)); arr.put(a("zoom"))
        var i = 0
        while (i < stops.size - 1) {
            arr.put(stops[i]); arr.put(stops[i + 1] * scale + extra)
            i += 2
        }
        return arr
    }

    private fun lineLayout() = o("line-cap" to "round", "line-join" to "round")

    private fun layer(
        id: String,
        type: String,
        sourceLayer: String? = null,
        source: String? = "omt",
        filter: JSONArray? = null,
        minzoom: Int? = null,
        paint: JSONObject = JSONObject(),
        layout: JSONObject = JSONObject()
    ): JSONObject {
        val l = JSONObject()
        l.put("id", id)
        l.put("type", type)
        if (type != "background" && source != null) l.put("source", source)
        if (sourceLayer != null) l.put("source-layer", sourceLayer)
        if (filter != null) l.put("filter", filter)
        if (minzoom != null) l.put("minzoom", minzoom)
        l.put("paint", paint)
        l.put("layout", layout)
        return l
    }

    private class Tier(val id: String, val filter: JSONArray, val stops: DoubleArray, val color: Int, val minzoom: Int)

    /** Выражение: подкласс/класс POI из тайлов → имя иконки блипа ("" — не показывать). */
    private fun tileIconExpr(s: Settings): Any? {
        val hidden = s.hiddenCats
        val subPairs = ArrayList<Any>()
        val seenSub = HashSet<String>()
        for ((cat, list) in TileCats.bySubclass) {
            if (cat.name in hidden) continue
            for (v in list) if (seenSub.add(v)) { subPairs.add(v); subPairs.add("blip_${cat.name}") }
        }
        val clsPairs = ArrayList<Any>()
        val seenCls = HashSet<String>()
        for ((cat, list) in TileCats.byClass) {
            if (cat.name in hidden) continue
            for (v in list) if (seenCls.add(v)) { clsPairs.add(v); clsPairs.add("blip_${cat.name}") }
        }
        if (subPairs.isEmpty() && clsPairs.isEmpty()) return null
        val clsExpr: Any = if (clsPairs.isEmpty()) "" else {
            val m = JSONArray()
            m.put("match"); m.put(a("coalesce", a("get", "class"), ""))
            clsPairs.forEach { m.put(it) }
            m.put("")
            m
        }
        if (subPairs.isEmpty()) return clsExpr
        val m = JSONArray()
        m.put("match"); m.put(a("coalesce", a("get", "subclass"), ""))
        subPairs.forEach { m.put(it) }
        m.put(clsExpr)
        return m
    }

    @Suppress("unused")
    private val allCats = PoiCategory.entries

    fun build(g: GameStyle, s: Settings): String {
        val layers = JSONArray()
        val sc = g.roadScale.toDouble()

        val name: JSONArray = when (s.labelLang) {
            "ru" -> a("coalesce", a("get", "name:ru"), a("get", "name"), "")
            "en" -> a("coalesce", a("get", "name:en"), a("get", "name_en"), a("get", "name"), "")
            else -> a("coalesce", a("get", "name"), "")
        }
        val text: JSONArray = if (g.labelUpper) a("upcase", name) else name
        val font = a(g.labelFont)
        val bold = a("Noto Sans Bold")
        val spacing = if (g.labelUpper) 0.08 else 0.02

        // --- Текстуры стиля (паттерны) ---
        val sat = s.satellite
        val tex: Set<String> = if (s.textures && !sat) Textures.slots(g.texture) else emptySet()
        fun fillPaint(slot: String, color: Int, opacity: Double = 1.0): JSONObject {
            val p = if (slot in tex) o("fill-pattern" to "tex_$slot") else o("fill-color" to css(color))
            if (opacity < 1.0) p.put("fill-opacity", opacity)
            return p
        }

        if (sat) {
            // --- Спутник: снимки + подкраска под игровой стиль ---
            val look = SatelliteLook.of(g.id)
            layers.put(layer("bg", "background", source = null, paint = o("background-color" to "#000000")))
            layers.put(layer("satellite", "raster", source = "sat",
                paint = o(
                    "raster-saturation" to look.saturation,
                    "raster-contrast" to look.contrast,
                    "raster-brightness-min" to look.brightnessMin,
                    "raster-brightness-max" to look.brightnessMax,
                    "raster-hue-rotate" to look.hue,
                    "raster-fade-duration" to 150
                )))
            if (look.tint != null && look.tintOpacity > 0) {
                layers.put(layer("sat-tint", "background", source = null,
                    paint = o("background-color" to css(look.tint), "background-opacity" to look.tintOpacity)))
            }
        } else {
        // --- Подложка ---
        layers.put(layer("bg", "background", source = null,
            paint = if ("land" in tex) o("background-pattern" to "tex_land") else o("background-color" to css(g.land))))
        layers.put(layer("landuse", "fill", "landuse",
            filter = classIn("residential", "commercial", "industrial", "retail", "suburb", "neighbourhood", "quarry", "railway"),
            paint = fillPaint("urban", g.landUrban)))
        layers.put(layer("landuse-farm", "fill", "landuse", filter = classIn("farmland", "allotments", "orchard"),
            paint = fillPaint("farm", g.park)))
        layers.put(layer("landcover-grass", "fill", "landcover",
            filter = classIn("grass", "wetland", "scrub"),
            paint = fillPaint("park", g.park, 0.9)))
        layers.put(layer("landcover-farm", "fill", "landcover", filter = classIn("farmland"),
            paint = fillPaint("farm", g.park, 0.95)))
        layers.put(layer("landcover-sand", "fill", "landcover", filter = classIn("sand", "beach"),
            paint = fillPaint("sand", Textures.shade(g.land, 0.25f))))
        layers.put(layer("park", "fill", "park", paint = fillPaint("park", g.park)))
        layers.put(layer("landcover-wood", "fill", "landcover",
            filter = classIn("wood", "forest"),
            paint = fillPaint("wood", g.wood, 0.95)))
        layers.put(layer("water", "fill", "water", paint = fillPaint("water", g.water)))
        layers.put(layer("waterway", "line", "waterway",
            paint = o("line-color" to css(g.waterLine), "line-width" to widths(1.0, 0.0, doubleArrayOf(8.0, 0.5, 14.0, 2.0, 18.0, 6.0)))))
        }

        // --- Здания ---
        if (s.buildings) {
            val blocky = g.blockBuildings
            if (s.buildings3d || blocky || sat) {
                // В «кубическом» стиле высота кратна блоку, а стены — из текстуры досок
                val rawH = a("coalesce", a("get", "render_height"), 6)
                val height: Any = if (blocky) a("*", a("ceil", a("/", rawH, 4)), 4) else rawH
                val paint = o(
                    "fill-extrusion-height" to height,
                    "fill-extrusion-base" to a("coalesce", a("get", "render_min_height"), 0),
                    "fill-extrusion-opacity" to (if (blocky && !sat) 1.0 else if (sat) 0.78 else 0.9),
                    "fill-extrusion-vertical-gradient" to true
                )
                if ("building" in tex) paint.put("fill-extrusion-pattern", "tex_building")
                else paint.put("fill-extrusion-color", css(if (sat) Textures.shade(g.building, 0.35f) else Textures.shade(g.building, 0.12f)))
                if (sat && !s.buildings3d) {
                    // спутник без 3D: только контуры домов
                    layers.put(layer("building-outline", "line", "building", minzoom = 15,
                        paint = o("line-color" to css(g.buildingLine), "line-opacity" to 0.55, "line-width" to 1)))
                } else {
                    layers.put(layer("building-3d", "fill-extrusion", "building", minzoom = 14, paint = paint))
                }
            } else {
                val paint = fillPaint("building", g.building)
                paint.put("fill-outline-color", css(g.buildingLine))
                layers.put(layer("building", "fill", "building", minzoom = 13, paint = paint))
            }
        }

        // --- Дороги ---
        layers.put(layer("road-path", "line", "transportation", filter = classIn("path", "track"), minzoom = 14,
            paint = o(
                "line-opacity" to (if (sat) 0.5 else 1.0),
                "line-color" to css(g.path),
                "line-width" to widths(sc, 0.0, doubleArrayOf(14.0, 0.6, 18.0, 2.5)),
                "line-dasharray" to a(2, 1.5)
            )))
        layers.put(layer("rail", "line", "transportation", filter = classIn("rail", "transit"), minzoom = 11,
            paint = o(
                "line-color" to css(g.rail),
                "line-width" to widths(1.0, 0.0, doubleArrayOf(11.0, 0.6, 18.0, 2.5)),
                "line-dasharray" to a(3, 2)
            )))

        val tiers = listOf(
            Tier("minor", classIn("minor", "service", "raceway", "busway"),
                doubleArrayOf(12.0, 0.5, 14.0, 1.6, 16.0, 4.0, 18.0, 11.0), g.roadMinor, 12),
            Tier("major", classIn("primary", "secondary", "tertiary"),
                doubleArrayOf(8.0, 0.5, 11.0, 1.2, 13.0, 2.2, 15.0, 5.0, 18.0, 16.0), g.roadMajor, 7),
            Tier("highway", classIn("motorway", "trunk"),
                doubleArrayOf(5.0, 0.6, 9.0, 1.4, 12.0, 2.6, 14.0, 4.5, 16.0, 9.0, 18.0, 20.0), g.roadHighway, 4)
        )
        val casing = g.roadCasing
        if (casing != null) {
            for (t in tiers) {
                layers.put(layer("${t.id}-casing", "line", "transportation", filter = t.filter, minzoom = maxOf(t.minzoom, 11),
                    paint = o("line-color" to css(casing), "line-width" to widths(sc, 2.5, t.stops), "line-opacity" to (if (sat) 0.5 else 1.0)),
                    layout = lineLayout()))
            }
        }
        for (t in tiers) {
            layers.put(layer("road-${t.id}", "line", "transportation", filter = t.filter, minzoom = t.minzoom,
                paint = (if ("road" in tex) o("line-pattern" to "tex_road", "line-width" to widths(sc, 0.0, t.stops))
                else o("line-color" to css(t.color), "line-width" to widths(sc, 0.0, t.stops)))
                    .also { if (sat) it.put("line-opacity", if (t.id == "minor") 0.45 else 0.7) },
                layout = lineLayout()))
        }
        layers.put(layer("boundary", "line", "boundary",
            filter = a("<=", a("get", "admin_level"), 4),
            paint = o("line-color" to css(g.roadMajor), "line-opacity" to 0.45, "line-width" to 1.2, "line-dasharray" to a(4, 3))))

        // --- Подписи ---
        if (s.labels) {
            val textPaint = o("text-color" to css(g.label), "text-halo-color" to css(g.labelHalo), "text-halo-width" to 1.4)
            layers.put(layer("water-name", "symbol", "water_name",
                layout = o("text-field" to text, "text-font" to a("Noto Sans Italic"), "text-size" to 12, "text-max-width" to 8),
                paint = o("text-color" to css(g.label), "text-opacity" to 0.75, "text-halo-color" to css(g.labelHalo), "text-halo-width" to 1.0)))
            layers.put(layer("road-label", "symbol", "transportation_name", minzoom = 13,
                layout = o(
                    "symbol-placement" to "line",
                    "text-field" to text,
                    "text-font" to font,
                    "text-size" to zoomLin(13, 10, 18, 14),
                    "text-letter-spacing" to spacing
                ),
                paint = textPaint))
            layers.put(layer("place-small", "symbol", "place", minzoom = 11,
                filter = classIn("suburb", "neighbourhood", "quarter", "village", "hamlet"),
                layout = o("text-field" to text, "text-font" to font, "text-size" to 12, "text-letter-spacing" to spacing, "text-max-width" to 8),
                paint = o("text-color" to css(g.label), "text-halo-color" to css(g.labelHalo), "text-halo-width" to 1.4)))
            layers.put(layer("place-town", "symbol", "place", filter = classIn("town"),
                layout = o("text-field" to text, "text-font" to bold, "text-size" to 14, "text-letter-spacing" to spacing),
                paint = o("text-color" to css(g.label), "text-halo-color" to css(g.labelHalo), "text-halo-width" to 1.6)))
            layers.put(layer("place-city", "symbol", "place", filter = classIn("city"),
                layout = o("text-field" to text, "text-font" to bold, "text-size" to zoomLin(4, 12, 10, 20), "text-letter-spacing" to spacing),
                paint = o("text-color" to css(g.label), "text-halo-color" to css(g.labelHalo), "text-halo-width" to 2.0)))
        }

        // --- Магазины и заведения прямо из векторных тайлов ---
        val iconExpr = tileIconExpr(s)
        if (s.poiEnabled && iconExpr != null) {
            val tileLayout = o(
                "icon-image" to iconExpr,
                "icon-size" to s.blipScale.toDouble(),
                "icon-allow-overlap" to false,
                "icon-padding" to 2,
                "symbol-sort-key" to a("coalesce", a("get", "rank"), 99)
            )
            if (s.poiNames) {
                tileLayout.put("text-field", a("step", a("zoom"), "", 16, name))
                tileLayout.put("text-font", bold)
                tileLayout.put("text-size", 10)
                tileLayout.put("text-offset", a(0, 1.4))
                tileLayout.put("text-anchor", "top")
                tileLayout.put("text-optional", true)
                tileLayout.put("text-max-width", 8)
            }
            layers.put(layer("poi-tiles", "symbol", "poi", minzoom = 14,
                filter = a("!=", iconExpr, ""),
                layout = tileLayout,
                paint = o("text-color" to css(g.label), "text-halo-color" to css(g.labelHalo), "text-halo-width" to 1.2)))
        }

        // --- Популярные места: музеи, театры, стадионы, храмы, парки, отели ---
        layers.put(layer("poi-landmark", "symbol", "poi", minzoom = 13,
            filter = a("match", a("coalesce", a("get", "class"), ""), JSONArray(TileCats.landmarks.keys.toList()), true, false),
            layout = o(
                "icon-image" to "landmark",
                "icon-size" to 0.9,
                "icon-allow-overlap" to false,
                "text-field" to name,
                "text-font" to bold,
                "text-size" to a("interpolate", a("linear"), a("zoom"), 13, 10, 18, 13),
                "text-offset" to a(0, 1.1),
                "text-anchor" to "top",
                "text-max-width" to 9,
                "text-optional" to true,
                "symbol-sort-key" to a("coalesce", a("get", "rank"), 99)
            ),
            paint = o("text-color" to css(g.landmark), "text-halo-color" to css(g.labelHalo), "text-halo-width" to 1.6)))

        if (s.labels) {
            layers.put(layer("housenumber", "symbol", "housenumber", minzoom = 17,
                layout = o("text-field" to a("get", "housenumber"), "text-font" to a("Noto Sans Regular"), "text-size" to 10),
                paint = o("text-color" to css(g.label), "text-opacity" to 0.55, "text-halo-color" to css(g.labelHalo), "text-halo-width" to 1.0)))
        }

        // --- Динамические слои приложения ---
        val trailColor = g.trailColor
        if (s.trail && trailColor != null) {
            val trailPaint = o("line-color" to css(trailColor), "line-width" to zoomLin(10, 2, 18, 6))
            if (g.trailDashed) trailPaint.put("line-dasharray", a(0.6, 1.6))
            layers.put(layer("trail", "line", source = "trail", paint = trailPaint, layout = lineLayout()))
        }
        layers.put(layer("route-casing", "line", source = "route",
            paint = o("line-color" to css(g.routeCasing), "line-width" to zoomLin(10, 7, 18, 16)),
            layout = lineLayout()))
        layers.put(layer("route-line", "line", source = "route",
            paint = o("line-color" to css(g.route), "line-width" to zoomLin(10, 4, 18, 10)),
            layout = lineLayout()))
        layers.put(layer("draft-line", "line", source = "draft",
            filter = a("==", a("geometry-type"), "LineString"),
            paint = o("line-color" to css(g.waypoint), "line-width" to 5, "line-dasharray" to a(1.4, 0.9)),
            layout = lineLayout()))
        layers.put(layer("draft-pt", "circle", source = "draft",
            filter = a("==", a("geometry-type"), "Point"),
            paint = o("circle-radius" to 11, "circle-color" to css(g.waypoint), "circle-stroke-color" to "#FFFFFF", "circle-stroke-width" to 2)))
        layers.put(layer("draft-num", "symbol", source = "draft",
            filter = a("==", a("geometry-type"), "Point"),
            layout = o(
                "text-field" to a("to-string", a("get", "n")),
                "text-font" to bold,
                "text-size" to 12,
                "text-allow-overlap" to true,
                "text-ignore-placement" to true
            ),
            paint = o("text-color" to "#FFFFFF")))

        val poiLayout = o(
            "icon-image" to a("get", "icon"),
            "icon-size" to s.blipScale.toDouble(),
            "icon-allow-overlap" to false,
            "icon-padding" to 1
        )
        if (s.poiNames) {
            poiLayout.put("text-field", a("step", a("zoom"), "", 16, a("get", "name")))
            poiLayout.put("text-font", bold)
            poiLayout.put("text-size", 10)
            poiLayout.put("text-offset", a(0, 1.4))
            poiLayout.put("text-anchor", "top")
            poiLayout.put("text-optional", true)
            poiLayout.put("text-max-width", 8)
        }
        layers.put(layer("poi-blips", "symbol", source = "pois", layout = poiLayout,
            paint = o("text-color" to css(g.label), "text-halo-color" to css(g.labelHalo), "text-halo-width" to 1.2)))

        layers.put(layer("dest", "symbol", source = "dest",
            layout = o(
                "icon-image" to "waypoint",
                "icon-anchor" to "bottom",
                "icon-allow-overlap" to true,
                "icon-ignore-placement" to true
            )))
        layers.put(layer("player-glow", "circle", source = "player",
            paint = o("circle-radius" to 20, "circle-color" to css(g.player), "circle-opacity" to 0.18, "circle-pitch-alignment" to "map")))
        layers.put(layer("player-dot", "circle", source = "player",
            paint = o("circle-radius" to 7, "circle-color" to css(g.player), "circle-stroke-color" to css(g.playerLine), "circle-stroke-width" to 2.5)))
        layers.put(layer("player", "symbol", source = "player",
            layout = o(
                "icon-image" to "player",
                "icon-rotate" to a("get", "bearing"),
                "icon-rotation-alignment" to "map",
                "icon-pitch-alignment" to "map",
                "icon-allow-overlap" to true,
                "icon-ignore-placement" to true
            )))

        // --- Источники ---
        val sources = JSONObject()
        sources.put("omt", o("type" to "vector", "url" to TILES))
        if (sat) {
            sources.put("sat", o(
                "type" to "raster",
                "tiles" to a(SAT_TILES),
                "tileSize" to 256,
                "maxzoom" to 19,
                "attribution" to "Esri, Maxar, Earthstar Geographics"
            ))
        }
        for (id in listOf("route", "draft", "dest", "pois", "player", "trail")) {
            sources.put(id, o("type" to "geojson", "data" to JSONObject(EMPTY_FC)))
        }

        val root = JSONObject()
        root.put("version", 8)
        root.put("name", g.name)
        root.put("glyphs", GLYPHS)
        root.put("light", o("anchor" to "viewport", "color" to "#FFFFFF", "intensity" to 0.4, "position" to a(1.15, 210, 30)))
        root.put("sources", sources)
        root.put("layers", layers)
        return root.toString()
    }
}
