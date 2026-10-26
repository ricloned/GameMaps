package com.gamemaps.app.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.DisplayMetrics
import com.gamemaps.app.style.GameStyle
import com.gamemaps.app.style.TexPack
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

/**
 * Процедурные текстуры для карты (паттерны MapLibre).
 * Слоты: land, urban, park, wood, water, building, farm, sand, road.
 * Картинки рисуются кодом, никаких файлов не нужно.
 */
object Textures {

    /** Какие слоты есть у каждого набора — по ним стиль карты решает, где паттерн, а где цвет. */
    fun slots(p: TexPack): Set<String> = when (p) {
        TexPack.NONE -> emptySet()
        TexPack.BLOCKS -> setOf("land", "urban", "park", "wood", "water", "building", "farm", "sand", "road")
        TexPack.GRAIN -> setOf("land", "urban", "park", "wood", "water", "building", "farm", "sand")
        TexPack.POCKET -> setOf("land", "urban", "park", "wood", "water", "building", "farm", "sand")
        TexPack.PAPER -> setOf("land", "urban", "park", "wood", "water", "farm")
        TexPack.BLUEPRINT -> setOf("building", "park", "wood", "water")
        TexPack.PIP -> setOf("land", "building", "water", "wood")
        TexPack.CYBER -> setOf("land", "building", "water")
        TexPack.CHROME -> setOf("land", "building", "water")
        TexPack.SYNTH -> setOf("land", "water", "building")
        TexPack.TACTICAL -> setOf("land", "urban", "park", "wood", "building")
        TexPack.MATRIX -> setOf("land", "building")
        TexPack.VICE -> setOf("land", "water", "building")
        TexPack.CARBON -> setOf("land", "building")
        TexPack.VAPOR -> setOf("land", "water")
    }

    fun generate(g: GameStyle, d: Float): Map<String, Bitmap> {
        val t = Gen(d)
        val m = LinkedHashMap<String, Bitmap>()
        fun put(slot: String, b: Bitmap) { if (slot in slots(g.texture)) m["tex_$slot"] = b }
        when (g.texture) {
            TexPack.NONE -> {}
            TexPack.BLOCKS -> {
                val grass = intArrayOf(0xFF7FB238.toInt(), 0xFF6FA22F.toInt(), 0xFF8CC142.toInt(), 0xFF79AE35.toInt(), 0xFF5E9426.toInt())
                put("land", t.pixel(16, 4f, 11) { _, _, r -> grass[r.nextInt(grass.size)] })
                put("urban", t.pixel(16, 4f, 12) { _, _, r -> shade(grass[r.nextInt(grass.size)], -0.06f) })
                put("park", t.pixel(16, 4f, 13) { _, _, r -> shade(grass[r.nextInt(grass.size)], -0.12f) })
                val leaves = intArrayOf(0xFF2F6E1A.toInt(), 0xFF3A7F22.toInt(), 0xFF24591A.toInt(), 0xFF46902A.toInt(), 0xFF1B4612.toInt())
                put("wood", t.pixel(16, 4f, 14) { _, _, r -> leaves[r.nextInt(leaves.size)] })
                val water = intArrayOf(0xFF3F55E0.toInt(), 0xFF3A4FD8.toInt(), 0xFF4A62EE.toInt(), 0xFF3346C8.toInt())
                put("water", t.pixel(16, 4f, 15) { x, y, r ->
                    if ((y == 3 || y == 11) && (x + y) % 8 < 3) 0xFF6F84F5.toInt() else water[r.nextInt(water.size)]
                })
                put("farm", t.pixel(16, 4f, 16) { _, y, r ->
                    when (y % 4) {
                        0 -> 0xFF7A4A1E.toInt()
                        1 -> if (r.nextInt(5) == 0) 0xFF5C9A2A.toInt() else 0xFFC68A3A.toInt()
                        else -> if (r.nextBoolean()) 0xFFC68A3A.toInt() else 0xFFB8782C.toInt()
                    }
                })
                val sand = intArrayOf(0xFFE8D9A0.toInt(), 0xFFDCCB8C.toInt(), 0xFFF0E3B0.toInt(), 0xFFD5C282.toInt())
                put("sand", t.pixel(16, 4f, 17) { _, _, r -> sand[r.nextInt(sand.size)] })
                put("building", t.pixel(16, 4f, 18) { x, y, r ->
                    val plank = intArrayOf(0xFFA07840.toInt(), 0xFF8F6A35.toInt(), 0xFFB08848.toInt())
                    when {
                        y % 4 == 3 -> 0xFF5E4220.toInt()
                        (x + (y / 4) * 5) % 8 == 7 -> 0xFF6E4E28.toInt()
                        else -> plank[r.nextInt(plank.size)]
                    }
                })
                val stone = intArrayOf(0xFF8F8F8F.toInt(), 0xFF7A7A7A.toInt(), 0xFFA0A0A0.toInt(), 0xFF6A6A6A.toInt(), 0xFF858585.toInt())
                put("road", t.pixel(16, 2f, 19) { _, _, r -> stone[r.nextInt(stone.size)] })
            }
            TexPack.GRAIN -> {
                put("land", t.grain(g.land, 0.07f, 1))
                put("urban", t.grain(g.landUrban, 0.06f, 2))
                put("park", t.grain(g.park, 0.10f, 3))
                put("wood", t.grain(g.wood, 0.14f, 4))
                put("water", t.grain(g.water, 0.05f, 5))
                put("building", t.grain(g.building, 0.08f, 6))
                put("farm", t.stripes(shade(g.park, 0.08f), shade(g.park, -0.08f), 6f, 7))
                put("sand", t.grain(shade(g.land, 0.25f), 0.08f, 8))
            }
            TexPack.POCKET -> {
                val c0 = 0xFF0F380F.toInt(); val c1 = 0xFF306230.toInt(); val c2 = 0xFF8BAC0F.toInt(); val c3 = 0xFF9BBC0F.toInt()
                put("land", t.pixel(16, 3f, 21) { x, y, _ -> if ((x == 3 && y == 4) || (x == 4 && y == 3) || (x == 11 && y == 12) || (x == 12 && y == 11)) c1 else c3 })
                put("urban", t.pixel(8, 3f, 22) { x, y, _ -> if (x == 0 || y == 0) c2 else c3 })
                put("park", t.pixel(8, 3f, 23) { x, y, _ -> if ((x + y) % 4 == 0 && y % 2 == 0) c1 else c2 })
                put("wood", t.pixel(16, 3f, 24) { x, y, _ -> tree16(x, y, c0, c1, c2) })
                put("water", t.pixel(16, 3f, 25) { x, y, _ -> if (y % 8 == 2 && x % 8 in 1..4) c2 else if (y % 8 == 3 && x % 8 in 4..6) c2 else c1 })
                put("building", t.pixel(8, 3f, 26) { x, y, _ -> when { y % 4 == 3 -> c0; (x + y / 4 * 4) % 8 == 0 -> c0; else -> c1 } })
                put("farm", t.pixel(8, 3f, 27) { _, y, _ -> if (y % 2 == 0) c1 else c2 })
                put("sand", t.pixel(8, 3f, 28) { x, y, _ -> if ((x * 3 + y * 5) % 7 == 0) c2 else c3 })
            }
            TexPack.PAPER -> {
                val ink = g.label
                put("land", t.paper(g.land, 31))
                put("urban", t.paper(g.landUrban, 32))
                put("park", t.ticks(t.paper(g.park, 33), shade(ink, 0f), 34))
                put("wood", t.trees(t.paper(g.wood, 35), ink, 36))
                put("water", t.waves(g.water, shade(g.waterLine, -0.1f), 37))
                put("farm", t.hatch(t.paper(g.park, 38), withAlpha(ink, 0x55), 10f, 1.2f, false))
            }
            TexPack.BLUEPRINT -> {
                val w = 0x88FFFFFF.toInt()
                put("building", t.hatch(t.solid(g.building), w, 7f, 1.2f, false))
                put("park", t.hatch(t.solid(g.park), 0x55FFFFFF, 10f, 1f, true))
                put("wood", t.dots(g.wood, 0x88FFFFFF.toInt(), 8f, 1.2f))
                put("water", t.lines(g.water, 0x66CFE3FF, 6f, 1f))
            }
            TexPack.PIP -> {
                put("land", t.lines(g.land, withAlpha(g.uiAccent, 0x18), 4f, 1f))
                put("building", t.dots(g.building, withAlpha(g.uiAccent, 0x99), 5f, 1f))
                put("water", t.lines(g.water, withAlpha(g.uiAccent, 0x40), 3f, 1f))
                put("wood", t.dots(g.wood, withAlpha(g.uiAccent, 0x44), 7f, 1.6f))
            }
            TexPack.CYBER -> {
                put("land", t.dots(g.land, withAlpha(g.uiAccent, 0x30), 12f, 0.9f))
                put("building", t.grid(g.building, withAlpha(g.uiAccent, 0x55), 8f, 0.8f))
                put("water", t.lines(g.water, withAlpha(g.uiAccent, 0x22), 5f, 1f))
            }
            TexPack.CHROME -> {
                put("land", t.grain(g.land, 0.12f, 41))
                put("building", t.hatch(t.solid(g.building), 0x55FF003C, 9f, 2f, false))
                put("water", t.lines(g.water, 0x4400F0FF, 4f, 1f))
            }
            TexPack.SYNTH -> {
                put("land", t.grid(g.land, 0x55FF2E97, 24f, 1.2f))
                put("water", t.lines(g.water, 0x662DE2E6, 6f, 1.2f))
                put("building", t.grid(g.building, 0x882DE2E6.toInt(), 8f, 1f))
            }
            TexPack.TACTICAL -> {
                put("land", t.camo(g.land, shade(g.land, 0.08f), shade(g.land, -0.08f), 51))
                put("urban", t.camo(g.landUrban, shade(g.landUrban, 0.06f), shade(g.landUrban, -0.06f), 52))
                put("park", t.camo(g.park, shade(g.park, 0.1f), shade(g.park, -0.1f), 53))
                put("wood", t.camo(g.wood, shade(g.wood, 0.1f), shade(g.wood, -0.12f), 54))
                put("building", t.hatch(t.solid(g.building), withAlpha(g.buildingLine, 0xAA), 6f, 1f, false))
            }
            TexPack.MATRIX -> {
                put("land", t.glyphs(g.land, withAlpha(g.uiAccent, 0x22), 61))
                put("building", t.glyphs(g.building, withAlpha(g.uiAccent, 0x77), 62))
            }
            TexPack.VICE -> {
                put("land", t.hatch(t.solid(g.land), 0x14FF4FA3, 14f, 3f, false))
                put("water", t.waves(g.water, 0x6639E8FF, 63))
                put("building", t.grid(g.building, 0x44FF4FA3, 10f, 1f))
            }
            TexPack.CARBON -> {
                put("land", t.grain(g.land, 0.06f, 71))
                put("building", t.carbon(g.building))
            }
            TexPack.VAPOR -> {
                put("land", t.checker(g.land, shade(g.land, -0.05f), 16f))
                put("water", t.waves(g.water, 0x88FFFFFF.toInt(), 81))
            }
        }
        return m
    }

    // ---------------- цветовые помощники ----------------

    fun shade(c: Int, k: Float): Int {
        val a = (c ushr 24) and 0xFF
        fun ch(v: Int): Int = if (k >= 0) (v + (255 - v) * k).toInt() else (v * (1 + k)).toInt()
        val r = ch((c shr 16) and 0xFF).coerceIn(0, 255)
        val gg = ch((c shr 8) and 0xFF).coerceIn(0, 255)
        val b = ch(c and 0xFF).coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (gg shl 8) or b
    }

    fun withAlpha(c: Int, a: Int): Int = (c and 0x00FFFFFF) or ((a and 0xFF) shl 24)

    /** Пиксельное «дерево» 16×16 для стиля Pocket. */
    private fun tree16(x: Int, y: Int, dark: Int, mid: Int, light: Int): Int {
        val cx = 7.5f
        val cy = 6.5f
        val dx = x - cx
        val dy = y - cy
        val inCrown = dx * dx + dy * dy < 30f
        return when {
            y in 12..14 && x in 6..9 -> dark
            inCrown && dx * dx + dy * dy > 22f -> dark
            inCrown && dx < -1 && dy < -1 -> light
            inCrown -> mid
            else -> light
        }
    }

    // ---------------- генераторы ----------------

    private class Gen(val d: Float) {

        fun bitmap(sizeDp: Float): Bitmap {
            val px = (sizeDp * d).toInt().coerceAtLeast(4)
            return Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888).also {
                it.density = (DisplayMetrics.DENSITY_DEFAULT * d).toInt()
            }
        }

        private fun paint(color: Int, aa: Boolean = true) = Paint(if (aa) Paint.ANTI_ALIAS_FLAG else 0).apply { this.color = color }

        fun solid(color: Int, sizeDp: Float = 32f): Bitmap {
            val b = bitmap(sizeDp)
            Canvas(b).drawColor(color)
            return b
        }

        /** Пиксель-арт: n×n текселей, каждый texDp по размеру. */
        fun pixel(n: Int, texDp: Float, seed: Long, color: (Int, Int, Random) -> Int): Bitmap {
            val b = bitmap(n * texDp)
            val c = Canvas(b)
            val r = Random(seed)
            val u = b.width / n.toFloat()
            val p = paint(0, false)
            for (y in 0 until n) for (x in 0 until n) {
                p.color = color(x, y, r)
                c.drawRect(x * u, y * u, (x + 1) * u + 0.5f, (y + 1) * u + 0.5f, p)
            }
            return b
        }

        /** Зернистая «спутниковая» текстура. */
        fun grain(base: Int, amp: Float, seed: Long, sizeDp: Float = 48f, cellDp: Float = 2f): Bitmap {
            val b = bitmap(sizeDp)
            val c = Canvas(b)
            c.drawColor(base)
            val r = Random(seed)
            val cell = cellDp * d
            val n = (b.width / cell).toInt().coerceAtLeast(1)
            val u = b.width / n.toFloat()
            val p = paint(0, false)
            for (y in 0 until n) for (x in 0 until n) {
                p.color = shade(base, (r.nextFloat() * 2 - 1) * amp)
                c.drawRect(x * u, y * u, (x + 1) * u + 0.5f, (y + 1) * u + 0.5f, p)
            }
            // крупные пятна (с переносом через край — бесшовно)
            val blot = paint(0)
            repeat(5) {
                blot.color = withAlpha(shade(base, (r.nextFloat() * 2 - 1) * amp * 2), 0x40)
                val bx = r.nextFloat() * b.width
                val by = r.nextFloat() * b.height
                val rad = (4 + r.nextFloat() * 8) * d
                for (ox in -1..1) for (oy in -1..1) c.drawCircle(bx + ox * b.width, by + oy * b.height, rad, blot)
            }
            return b
        }

        fun stripes(a: Int, bCol: Int, rowDp: Float, seed: Long, sizeDp: Float = 48f): Bitmap {
            val b = bitmap(sizeDp)
            val c = Canvas(b)
            val row = rowDp * d
            var y = 0f
            var i = 0
            val p = paint(0, false)
            while (y < b.height) {
                p.color = if (i % 2 == 0) a else bCol
                c.drawRect(0f, y, b.width.toFloat(), y + row, p)
                y += row; i++
            }
            return b
        }

        /** Бумага: зерно + волокна. */
        fun paper(base: Int, seed: Long): Bitmap {
            val b = grain(base, 0.05f, seed, 64f, 1.5f)
            val c = Canvas(b)
            val r = Random(seed + 100)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.7f * d }
            repeat(10) {
                p.color = withAlpha(shade(base, -0.25f), 0x30)
                val x = r.nextFloat() * b.width
                val y = r.nextFloat() * b.height
                val len = (6 + r.nextFloat() * 14) * d
                val ang = r.nextFloat() * PI.toFloat()
                c.drawLine(x, y, x + len * kotlin.math.cos(ang), y + len * kotlin.math.sin(ang), p)
            }
            return b
        }

        fun hatch(bg: Bitmap, line: Int, spacingDp: Float, widthDp: Float, cross: Boolean): Bitmap {
            val b = bg.copy(Bitmap.Config.ARGB_8888, true).also { it.density = bg.density }
            val c = Canvas(b)
            val s = b.width.toFloat()
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; strokeWidth = widthDp * d; style = Paint.Style.STROKE }
            val n = (s / (spacingDp * d)).toInt().coerceAtLeast(1)
            val step = s / n
            var k = -s
            while (k <= 2 * s) {
                c.drawLine(k, 0f, k + s, s, p)
                if (cross) c.drawLine(k + s, 0f, k, s, p)
                k += step
            }
            return b
        }

        fun lines(bg: Int, line: Int, spacingDp: Float, widthDp: Float, sizeDp: Float = 24f): Bitmap {
            val b = bitmap(sizeDp)
            val c = Canvas(b)
            c.drawColor(bg)
            val n = (b.height / (spacingDp * d)).toInt().coerceAtLeast(1)
            val step = b.height / n.toFloat()
            val p = paint(line).apply { strokeWidth = widthDp * d }
            for (i in 0 until n) c.drawLine(0f, i * step + step / 2, b.width.toFloat(), i * step + step / 2, p)
            return b
        }

        fun grid(bg: Int, line: Int, spacingDp: Float, widthDp: Float): Bitmap {
            val b = bitmap(spacingDp)
            val c = Canvas(b)
            c.drawColor(bg)
            val p = paint(line).apply { strokeWidth = widthDp * d }
            c.drawLine(0f, 0f, b.width.toFloat(), 0f, p)
            c.drawLine(0f, 0f, 0f, b.height.toFloat(), p)
            return b
        }

        fun dots(bg: Int, dot: Int, spacingDp: Float, rDp: Float): Bitmap {
            val b = bitmap(spacingDp)
            val c = Canvas(b)
            c.drawColor(bg)
            c.drawCircle(b.width / 2f, b.height / 2f, rDp * d, paint(dot))
            return b
        }

        fun waves(bg: Int, line: Int, seed: Long, sizeDp: Float = 32f): Bitmap {
            val b = bitmap(sizeDp)
            val c = Canvas(b)
            c.drawColor(bg)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = line; style = Paint.Style.STROKE; strokeWidth = 1.2f * d; strokeCap = Paint.Cap.ROUND }
            val w = b.width.toFloat()
            for (row in 0 until 3) {
                val y0 = b.height * (row + 0.5f) / 3f
                val path = Path()
                var x = 0f
                path.moveTo(0f, y0)
                while (x <= w) {
                    path.lineTo(x, y0 + 2.2f * d * sin(2 * PI * (x / w) * 2 + row).toFloat())
                    x += 1.5f
                }
                c.drawPath(path, p)
            }
            if (seed < 0) c.drawColor(0)
            return b
        }

        /** Деревья-символы, как рисуют на старых картах. */
        fun trees(bg: Bitmap, ink: Int, seed: Long): Bitmap {
            val b = bg.copy(Bitmap.Config.ARGB_8888, true).also { it.density = bg.density }
            val c = Canvas(b)
            val s = b.width.toFloat()
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(ink, 0xB0); style = Paint.Style.STROKE; strokeWidth = 1.1f * d; strokeJoin = Paint.Join.ROUND }
            val r = Random(seed)
            val spots = listOf(0.22f to 0.25f, 0.72f to 0.2f, 0.45f to 0.6f, 0.15f to 0.8f, 0.8f to 0.72f)
            for ((fx, fy) in spots) {
                val x = fx * s + (r.nextFloat() - 0.5f) * 4 * d
                val y = fy * s + (r.nextFloat() - 0.5f) * 4 * d
                val h = 7 * d
                val path = Path().apply {
                    moveTo(x, y - h); lineTo(x + h * 0.55f, y + h * 0.35f); lineTo(x - h * 0.55f, y + h * 0.35f); close()
                }
                c.drawPath(path, p)
                c.drawLine(x, y + h * 0.35f, x, y + h * 0.75f, p)
            }
            return b
        }

        /** Штрихи травы. */
        fun ticks(bg: Bitmap, ink: Int, seed: Long): Bitmap {
            val b = bg.copy(Bitmap.Config.ARGB_8888, true).also { it.density = bg.density }
            val c = Canvas(b)
            val s = b.width.toFloat()
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(ink, 0x70); strokeWidth = 0.9f * d; strokeCap = Paint.Cap.ROUND }
            val r = Random(seed)
            repeat(7) {
                val x = (0.1f + r.nextFloat() * 0.8f) * s
                val y = (0.1f + r.nextFloat() * 0.8f) * s
                val h = 3 * d
                c.drawLine(x, y, x - h * 0.4f, y - h, p)
                c.drawLine(x, y, x, y - h * 1.2f, p)
                c.drawLine(x, y, x + h * 0.4f, y - h, p)
            }
            return b
        }

        fun camo(base: Int, a: Int, bCol: Int, seed: Long, sizeDp: Float = 64f): Bitmap {
            val b = bitmap(sizeDp)
            val c = Canvas(b)
            c.drawColor(base)
            val r = Random(seed)
            val p = paint(0)
            repeat(14) { i ->
                p.color = if (i % 2 == 0) a else bCol
                val x = r.nextFloat() * b.width
                val y = r.nextFloat() * b.height
                val rad = (5 + r.nextFloat() * 9) * d
                for (ox in -1..1) for (oy in -1..1) {
                    c.drawOval(x + ox * b.width - rad, y + oy * b.height - rad * 0.6f, x + ox * b.width + rad, y + oy * b.height + rad * 0.6f, p)
                }
            }
            return b
        }

        fun glyphs(bg: Int, fg: Int, seed: Long): Bitmap {
            val b = bitmap(40f)
            val c = Canvas(b)
            c.drawColor(bg)
            val r = Random(seed)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fg; typeface = Typeface.MONOSPACE; textSize = 7f * d }
            val cell = b.width / 5f
            val chars = "01アイウカキ<>"
            for (y in 0 until 5) for (x in 0 until 5) {
                if (r.nextInt(3) == 0) continue
                c.drawText(chars[r.nextInt(chars.length)].toString(), x * cell + 1, (y + 1) * cell - 2, p)
            }
            return b
        }

        fun carbon(base: Int): Bitmap {
            val b = bitmap(8f)
            val c = Canvas(b)
            val h = b.width / 2f
            val p1 = paint(shade(base, 0.08f), false)
            val p2 = paint(shade(base, -0.1f), false)
            c.drawRect(0f, 0f, h, h, p1); c.drawRect(h, h, 2 * h, 2 * h, p1)
            c.drawRect(h, 0f, 2 * h, h, p2); c.drawRect(0f, h, h, 2 * h, p2)
            return b
        }

        fun checker(a: Int, bCol: Int, cellDp: Float): Bitmap {
            val b = bitmap(cellDp * 2)
            val c = Canvas(b)
            val h = b.width / 2f
            c.drawColor(a)
            val p = paint(bCol, false)
            c.drawRect(h, 0f, 2 * h, h, p); c.drawRect(0f, h, h, 2 * h, p)
            return b
        }
    }
}
