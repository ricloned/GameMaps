package com.gamemaps.app.net

import com.gamemaps.app.model.GeoPoint
import com.gamemaps.app.model.Profile
import com.gamemaps.app.model.RouteResult
import com.gamemaps.app.model.RouteSeg
import com.gamemaps.app.model.RouteStep
import com.gamemaps.app.nav.Geo
import org.json.JSONObject
import java.io.IOException
import java.util.PriorityQueue

/**
 * Маршруты на метро по данным OpenStreetMap:
 * линии (route=subway/monorail/light_rail) и их станции грузятся из Overpass,
 * дальше — граф «станция на линии», перегоны и пересадки, поиск кратчайшего пути.
 * До станции и от станции — пешком по OSRM.
 */
object Metro {
    private class Line(val name: String, val color: Int)
    private class St(val name: String, val p: GeoPoint, val line: Int)
    private class Edge(val to: Int, val t: Double, val ride: Boolean)
    private class Net(val center: GeoPoint, val lines: List<Line>, val st: List<St>, val adj: List<List<Edge>>)

    private const val SPEED = 11.0      // м/с — средняя с разгонами и торможениями (~40 км/ч)
    private const val DWELL = 35.0      // стоянка на станции, с
    private const val ENTRY = 240.0     // вход, спуск, ожидание поезда, с
    private const val EXIT = 90.0       // выход в город, с
    private const val TRANSFER = 180.0  // пересадка: переход + ожидание, с
    private const val WALK = 1.3        // скорость пешехода, м/с

    private val FALLBACK = intArrayOf(
        0xFFE42313.toInt(), 0xFF4BAF4F.toInt(), 0xFF0072BA.toInt(), 0xFF1EBCEF.toInt(), 0xFF915133.toInt(),
        0xFFF07E24.toInt(), 0xFF943E90.toInt(), 0xFFFFD803.toInt(), 0xFFADACAC.toInt(), 0xFFBED12C.toInt()
    )

    private var cache: Net? = null

    @Synchronized
    private fun net(near: GeoPoint): Net {
        cache?.let { if (Geo.dist(it.center, near) < 25_000) return it }
        val q = "[out:json][timeout:90];" +
            "rel[\"route\"~\"^(subway|monorail|light_rail)\$\"](around:35000,${near.lat},${near.lon})->.r;" +
            ".r out body;node(r.r);out body;"
        val els = JSONObject(Api.overpassRaw(q)).getJSONArray("elements")
        val nodes = HashMap<Long, Pair<GeoPoint, String>>()
        val rels = ArrayList<JSONObject>()
        for (i in 0 until els.length()) {
            val e = els.getJSONObject(i)
            when (e.optString("type")) {
                "node" -> nodes[e.getLong("id")] = GeoPoint(e.getDouble("lat"), e.getDouble("lon")) to
                    (e.optJSONObject("tags")?.optString("name") ?: "")
                "relation" -> rels += e
            }
        }
        val lines = ArrayList<Line>()
        val lineIdx = HashMap<String, Int>()
        val st = ArrayList<St>()
        val stIdx = HashMap<String, Int>()
        val adj = ArrayList<MutableList<Edge>>()

        fun vertex(line: Int, nodeId: Long): Int? {
            val (p, name) = nodes[nodeId] ?: return null
            val key = if (name.isNotBlank()) "$line|$name" else "$line|#$nodeId"
            return stIdx.getOrPut(key) {
                st += St(name.ifBlank { "станция" }, p, line)
                adj += ArrayList()
                st.lastIndex
            }
        }

        for (r in rels) {
            val tags = r.optJSONObject("tags") ?: JSONObject()
            val colour = tags.optString("colour")
            val ref = tags.optString("ref")
            val fullName = tags.optString("name")
            val shortName = fullName.substringBefore(":").substringBefore(" (").trim()
            val key = if (colour.isNotBlank() || ref.isNotBlank()) "$colour|$ref" else shortName
            val li = lineIdx.getOrPut(key) {
                val c = runCatching { android.graphics.Color.parseColor(colour) }.getOrNull() ?: FALLBACK[lines.size % FALLBACK.size]
                lines += Line(shortName.ifBlank { if (ref.isNotBlank()) "Линия $ref" else "Линия" }, c)
                lines.lastIndex
            }
            val mem = r.optJSONArray("members") ?: continue
            val stops = ArrayList<Long>()
            val platforms = ArrayList<Long>()
            for (k in 0 until mem.length()) {
                val m = mem.getJSONObject(k)
                if (m.optString("type") != "node") continue
                val role = m.optString("role")
                if (role.startsWith("stop")) stops += m.getLong("ref")
                else if (role.startsWith("platform")) platforms += m.getLong("ref")
            }
            val seq = (if (stops.size >= 2) stops else platforms).mapNotNull { vertex(li, it) }
            for (k in 1 until seq.size) {
                val a = seq[k - 1]; val b = seq[k]
                if (a == b) continue
                val t = Geo.dist(st[a].p, st[b].p) / SPEED + DWELL
                adj[a] += Edge(b, t, true)
                adj[b] += Edge(a, t, true)
            }
        }
        // пересадки между линиями: станции рядом друг с другом
        for (i in st.indices) for (j in i + 1 until st.size) {
            if (st[i].line == st[j].line) continue
            val d = Geo.dist(st[i].p, st[j].p)
            if (d < 320) {
                val t = TRANSFER + d / WALK
                adj[i] += Edge(j, t, false)
                adj[j] += Edge(i, t, false)
            }
        }
        return Net(near, lines, st, adj).also { cache = it }
    }

    private class Path(val total: Double, val start: Int, val verts: List<Int>, val rides: List<Boolean>, val sig: String)

    private fun dijkstra(n: Net, src: Int): Pair<DoubleArray, IntArray> {
        val dist = DoubleArray(n.st.size) { Double.MAX_VALUE }
        val prev = IntArray(n.st.size) { -1 }
        dist[src] = 0.0
        val pq = PriorityQueue<Pair<Double, Int>>(compareBy { it.first })
        pq += 0.0 to src
        while (pq.isNotEmpty()) {
            val (d, v) = pq.poll()!!
            if (d > dist[v]) continue
            for (e in n.adj[v]) {
                val nd = d + e.t
                if (nd < dist[e.to]) { dist[e.to] = nd; prev[e.to] = v; pq += nd to e.to }
            }
        }
        return dist to prev
    }

    private fun estWalk(a: GeoPoint, b: GeoPoint) = Geo.dist(a, b) * 1.25 / WALK

    /** До трёх вариантов на метро, от быстрого к медленному. */
    fun routes(a: GeoPoint, b: GeoPoint): List<RouteResult> {
        val mid = GeoPoint((a.lat + b.lat) / 2, (a.lon + b.lon) / 2)
        val n = net(mid)
        if (n.st.isEmpty()) throw IOException("в этом городе метро не найдено")
        fun near(p: GeoPoint) = n.st.indices.filter { Geo.dist(n.st[it].p, p) < 2500 }
            .sortedBy { Geo.dist(n.st[it].p, p) }.take(8)
        val sa = near(a)
        val sb = near(b).toSet()
        if (sa.isEmpty()) throw IOException("рядом с началом нет станций метро")
        if (sb.isEmpty()) throw IOException("рядом с концом нет станций метро")

        val best = HashMap<String, Path>()
        for (s in sa) {
            val (dist, prev) = dijkstra(n, s)
            for (e in sb) {
                if (dist[e] == Double.MAX_VALUE || e == s) continue
                val verts = ArrayList<Int>()
                var v = e
                while (v != -1) { verts += v; if (v == s) break; v = prev[v] }
                verts.reverse()
                val rides = (1 until verts.size).map { k -> n.st[verts[k - 1]].line == n.st[verts[k]].line }
                if (rides.none { it } || !rides.first() || !rides.last()) continue // начинать и заканчивать поездкой
                val sig = buildString {
                    var k = 0
                    while (k < rides.size) {
                        if (rides[k]) {
                            val from = verts[k]
                            while (k < rides.size && rides[k]) k++
                            append(n.st[from].line).append(':').append(n.st[from].name).append('>').append(n.st[verts[k]].name).append(';')
                        } else k++
                    }
                }
                val total = estWalk(a, n.st[s].p) + ENTRY + dist[e] + EXIT + estWalk(n.st[e].p, b)
                val old = best[sig]
                if (old == null || total < old.total) best[sig] = Path(total, s, verts, rides, sig)
            }
        }
        if (best.isEmpty()) throw IOException("не нашёл путь по метро")
        val sorted = best.values.sortedBy { it.total }
        val top = sorted.first().total
        // разные по линиям варианты, не сильно медленнее лучшего
        val picked = ArrayList<Path>()
        val lineSets = HashSet<String>()
        for (p in sorted) {
            if (p.total > top * 1.7 + 300) break
            val ls = p.verts.map { n.st[it].line }.distinct().joinToString(",")
            if (picked.isNotEmpty() && !lineSets.add(ls) && picked.size >= 2) continue
            lineSets += ls
            picked += p
            if (picked.size == 3) break
        }
        return picked.map { build(n, a, b, it) }.sortedBy { it.duration }
    }

    private fun build(n: Net, a: GeoPoint, b: GeoPoint, path: Path): RouteResult {
        val segs = ArrayList<RouteSeg>()
        val steps = ArrayList<RouteStep>()
        val points = ArrayList<GeoPoint>()
        fun add(seg: RouteSeg) {
            if (points.isNotEmpty() && seg.points.isNotEmpty() && points.last() == seg.points.first()) points += seg.points.drop(1)
            else points += seg.points
            segs += seg
        }
        val v = path.verts
        val first = n.st[v.first()]
        val last = n.st[v.last()]
        val (w1, t1) = Api.walk(a, first.p)
        steps += RouteStep("Пешком до станции «${first.name}» · ${Geo.fmtDur(t1)}", "↑", a, 0)
        add(RouteSeg(w1, true, duration = t1))
        var total = t1 + ENTRY
        var k = 0
        val lineNames = ArrayList<String>()
        var transfers = 0
        while (k < path.rides.size) {
            if (path.rides[k]) {
                val from = k
                var t = 0.0
                while (k < path.rides.size && path.rides[k]) {
                    t += n.adj[v[k]].filter { it.to == v[k + 1] && it.ride }.minOfOrNull { it.t } ?: 0.0
                    k++
                }
                val line = n.lines[n.st[v[from]].line]
                val pts = (from..k).map { n.st[v[it]].p }
                val s0 = n.st[v[from]].name
                val s1 = n.st[v[k]].name
                val stops = k - from
                steps += RouteStep("${line.name}: «$s0» → «$s1», ${stops} ост.", "M", pts.first(), points.size.coerceAtLeast(1) - 1)
                add(RouteSeg(pts, false, line.color, line.name, stops, t + if (from == 0) ENTRY else 0.0))
                lineNames += line.name
                total += t
            } else {
                val p0 = n.st[v[k]].p
                val p1 = n.st[v[k + 1]].p
                val t = n.adj[v[k]].filter { it.to == v[k + 1] && !it.ride }.minOfOrNull { it.t } ?: TRANSFER
                transfers++
                val to = n.st[v[k + 1]]
                steps += RouteStep("Пересадка на ${n.lines[to.line].name} («${to.name}»)", "⇄", p0, points.size - 1)
                add(RouteSeg(listOf(p0, p1), true, duration = t))
                total += t
                k++
            }
        }
        val (w2, t2) = Api.walk(last.p, b)
        steps += RouteStep("Выход на «${last.name}», дальше пешком · ${Geo.fmtDur(t2)}", "↑", last.p, points.size - 1)
        add(RouteSeg(w2, true, duration = t2))
        total += EXIT + t2
        steps += RouteStep("Вы на месте", "◉", b, points.size - 1)
        val cum = Geo.cumulative(points)
        val tr = when (transfers) { 0 -> "без пересадок"; 1 -> "1 пересадка"; in 2..4 -> "$transfers пересадки"; else -> "$transfers пересадок" }
        return RouteResult(points, cum, cum.lastOrNull() ?: 0.0, total, steps, Profile.METRO, segs,
            summary = lineNames.distinct().joinToString(" → ") + " · " + tr)
    }
}
