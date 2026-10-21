package com.gamemaps.app.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.DisplayMetrics
import androidx.core.content.res.ResourcesCompat
import com.gamemaps.app.model.PoiCategory
import com.gamemaps.app.style.BlipShape
import com.gamemaps.app.style.DestShape
import com.gamemaps.app.style.GameStyle
import com.gamemaps.app.style.PlayerShape
import com.gamemaps.app.style.UiFont
import kotlin.math.cos
import kotlin.math.sin

/** Загрузка шрифтов стилей для Canvas (значки, текст на радаре). */
object GameFonts {
    private val cache = HashMap<Int, Typeface?>()

    private fun load(ctx: Context, res: Int): Typeface? =
        cache.getOrPut(res) { runCatching { ResourcesCompat.getFont(ctx, res) }.getOrNull() }

    private fun fallback(g: GameStyle): Typeface {
        val st = if (g.uiItalic) Typeface.BOLD_ITALIC else Typeface.BOLD
        val base = when (g.uiFont) {
            UiFont.SANS -> Typeface.SANS_SERIF
            UiFont.SERIF -> Typeface.SERIF
            UiFont.MONO -> Typeface.MONOSPACE
            UiFont.CONDENSED -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        }
        return Typeface.create(base, st)
    }

    fun ui(ctx: Context, g: GameStyle): Typeface = g.uiFontRes?.let { load(ctx, it) } ?: fallback(g)
    fun title(ctx: Context, g: GameStyle): Typeface = g.titleFontRes?.let { load(ctx, it) } ?: ui(ctx, g)
}

/** Рисует значки (стрелка игрока, метка, «блипы» мест) под выбранный игровой стиль. */
object MarkerIcons {

    private fun bitmap(w: Int, h: Int, d: Float): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            it.density = (DisplayMetrics.DENSITY_DEFAULT * d).toInt()
        }

    private fun fill(color: Int, aa: Boolean = true) = Paint(if (aa) Paint.ANTI_ALIAS_FLAG else 0).apply {
        style = Paint.Style.FILL; this.color = color
    }

    private fun stroke(color: Int, w: Float, aa: Boolean = true) = Paint(if (aa) Paint.ANTI_ALIAS_FLAG else 0).apply {
        style = Paint.Style.STROKE; this.color = color; strokeWidth = w
        strokeJoin = if (aa) Paint.Join.ROUND else Paint.Join.MITER
        strokeCap = if (aa) Paint.Cap.ROUND else Paint.Cap.SQUARE
    }

    fun starPath(cx: Float, cy: Float, ro: Float, ri: Float): Path = Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) ro else ri
            val a = Math.toRadians(-90.0 + i * 36.0)
            val x = cx + r * cos(a).toFloat()
            val y = cy + r * sin(a).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    /** Стрелка игрока, смотрит вверх (поворачивается слоем карты). */
    fun player(g: GameStyle, d: Float): Bitmap {
        val s = (44 * d).toInt()
        val bmp = bitmap(s, s, d)
        val c = Canvas(bmp)
        val f = s.toFloat()
        val aa = g.playerShape != PlayerShape.PIXEL
        val p = Path()
        when (g.playerShape) {
            PlayerShape.ARROW -> {
                p.moveTo(f / 2, f * 0.08f); p.lineTo(f * 0.82f, f * 0.88f)
                p.lineTo(f / 2, f * 0.68f); p.lineTo(f * 0.18f, f * 0.88f); p.close()
            }
            PlayerShape.TRIANGLE -> {
                p.moveTo(f / 2, f * 0.1f); p.lineTo(f * 0.8f, f * 0.84f); p.lineTo(f * 0.2f, f * 0.84f); p.close()
            }
            PlayerShape.CHEVRON -> {
                p.moveTo(f / 2, f * 0.12f); p.lineTo(f * 0.86f, f * 0.7f); p.lineTo(f * 0.66f, f * 0.84f)
                p.lineTo(f / 2, f * 0.56f); p.lineTo(f * 0.34f, f * 0.84f); p.lineTo(f * 0.14f, f * 0.7f); p.close()
            }
            PlayerShape.PIXEL -> {
                // ступенчатая «пиксельная» стрелка
                val u = f / 11f
                val rows = listOf(5 to 6, 4 to 7, 4 to 7, 3 to 8, 3 to 8, 2 to 9, 2 to 9, 1 to 10, 1 to 4, 7 to 10)
                rows.forEachIndexed { i, (a, b) ->
                    p.addRect(a * u, (i + 0.5f) * u, b * u, (i + 1.5f) * u, Path.Direction.CW)
                }
            }
        }
        c.drawPath(p, fill(g.player, aa))
        c.drawPath(p, stroke(g.playerLine, 2.5f * d, aa))
        return bmp
    }

    /** Метка назначения: булавка, крестик «X», флажок финиша или ромб. */
    fun waypoint(g: GameStyle, d: Float): Bitmap {
        val w = (36 * d).toInt()
        val h = (46 * d).toInt()
        val bmp = bitmap(w, h, d)
        val c = Canvas(bmp)
        val cx = w / 2f
        when (g.destShape) {
            DestShape.PIN -> {
                val r = w / 2f - 3 * d
                val cy = r + 3 * d
                val head = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
                val tip = Path().apply {
                    moveTo(cx - r * 0.62f, cy + r * 0.72f); lineTo(cx, h - 2 * d); lineTo(cx + r * 0.62f, cy + r * 0.72f); close()
                }
                head.op(tip, Path.Op.UNION)
                c.drawPath(head, fill(g.waypoint))
                c.drawPath(head, stroke(0xFFFFFFFF.toInt(), 2.5f * d))
                c.drawCircle(cx, cy, r * 0.32f, fill(0xFFFFFFFF.toInt()))
            }
            DestShape.X -> {
                val m = 6 * d
                val b = h - 6 * d
                val outline = stroke(g.routeCasing, 9 * d)
                val line = stroke(g.waypoint, 5.5f * d)
                c.drawLine(m, b - (w - 2 * m), w - m, b, outline); c.drawLine(w - m, b - (w - 2 * m), m, b, outline)
                c.drawLine(m, b - (w - 2 * m), w - m, b, line); c.drawLine(w - m, b - (w - 2 * m), m, b, line)
            }
            DestShape.FLAG -> {
                val pole = 7 * d
                c.drawRect(pole - 1.5f * d, 3 * d, pole + 1.5f * d, h - 2f * d, fill(g.routeCasing))
                val cell = 5 * d
                for (row in 0 until 4) for (col in 0 until 5) {
                    val color = if ((row + col) % 2 == 0) g.waypoint else g.routeCasing
                    c.drawRect(pole + col * cell, 4 * d + row * cell, pole + (col + 1) * cell, 4 * d + (row + 1) * cell, fill(color, false))
                }
                c.drawRect(pole, 4 * d, pole + 5 * cell, 4 * d + 4 * cell, stroke(g.routeCasing, 1.5f * d, false))
            }
            DestShape.DIAMOND -> {
                val cy = h / 2f
                val r = w / 2f - 4 * d
                val p = Path().apply { moveTo(cx, cy - r * 1.3f); lineTo(cx + r, cy); lineTo(cx, cy + r * 1.3f); lineTo(cx - r, cy); close() }
                c.drawPath(p, fill(g.waypoint))
                c.drawPath(p, stroke(g.routeCasing, 3 * d))
                c.drawCircle(cx, cy, r * 0.3f, fill(g.routeCasing))
            }
        }
        return bmp
    }

    /** Звёздочка «популярного места». */
    fun landmark(g: GameStyle, d: Float): Bitmap {
        val s = (20 * d).toInt()
        val bmp = bitmap(s, s, d)
        val c = Canvas(bmp)
        val p = starPath(s / 2f, s / 2f, s / 2f - 1.5f * d, s / 4.6f)
        c.drawPath(p, fill(g.landmark))
        c.drawPath(p, stroke(g.labelHalo, 1.5f * d))
        return bmp
    }

    private fun shapePath(shape: BlipShape, cx: Float, cy: Float, r: Float): Path = Path().apply {
        when (shape) {
            BlipShape.CIRCLE -> addCircle(cx, cy, r, Path.Direction.CW)
            BlipShape.SQUARE, BlipShape.PIXEL -> addRect(cx - r * 0.9f, cy - r * 0.9f, cx + r * 0.9f, cy + r * 0.9f, Path.Direction.CW)
            BlipShape.DIAMOND -> {
                moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close()
            }
            BlipShape.HEX -> {
                for (i in 0 until 6) {
                    val a = Math.toRadians(60.0 * i - 90.0)
                    val x = cx + r * cos(a).toFloat()
                    val y = cy + r * sin(a).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        }
    }

    /** «Блип» категории места. */
    fun blip(g: GameStyle, cat: PoiCategory, d: Float, tf: Typeface): Bitmap {
        val s = (28 * d).toInt()
        val bmp = bitmap(s, s, d)
        val c = Canvas(bmp)
        val cx = s / 2f
        val cy = s / 2f
        val r = s / 2f - 2 * d
        val pixel = g.blipShape == BlipShape.PIXEL
        val path = shapePath(g.blipShape, cx, cy, r)
        val bg: Int
        val line: Int
        val glyph: Int
        if (g.blipColored) {
            bg = cat.color; line = g.blipLine; glyph = 0xFFFFFFFF.toInt()
        } else {
            bg = g.blipBg; line = cat.color; glyph = cat.color
        }
        c.drawPath(path, fill(bg, !pixel))
        c.drawPath(path, stroke(line, (if (pixel) 3f else 2f) * d, !pixel))
        if (pixel) {
            // блик как у кубиков
            c.drawRect(cx - r * 0.9f + 2 * d, cy - r * 0.9f + 2 * d, cx + r * 0.9f - 2 * d, cy - r * 0.9f + 4 * d, fill(0x55FFFFFF, false))
        }
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glyph
            typeface = tf
            textAlign = Paint.Align.CENTER
            textSize = (if (cat.glyph.length > 1) 10.5f else 15f) * d
        }
        val y = cy - (tp.descent() + tp.ascent()) / 2f
        c.drawText(cat.glyph, cx, y, tp)
        return bmp
    }

    /** Все картинки, нужные слоям карты для этого стиля. */
    fun all(ctx: Context, g: GameStyle, d: Float): Map<String, Bitmap> {
        val tf = GameFonts.ui(ctx, g)
        val m = LinkedHashMap<String, Bitmap>()
        m["player"] = player(g, d)
        m["waypoint"] = waypoint(g, d)
        m["landmark"] = landmark(g, d)
        PoiCategory.entries.forEach { m["blip_${it.name}"] = blip(g, it, d, tf) }
        return m
    }
}
