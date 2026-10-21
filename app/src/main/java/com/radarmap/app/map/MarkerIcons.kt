package com.radarmap.app.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import com.radarmap.app.model.PoiCategory
import com.radarmap.app.style.BlipShape
import com.radarmap.app.style.GameStyle
import com.radarmap.app.style.UiFont
import kotlin.math.cos
import kotlin.math.sin

/** Рисует значки (стрелка игрока, метка, «блипы» мест) под выбранный игровой стиль. */
object MarkerIcons {

    fun typeface(g: GameStyle): Typeface {
        val st = if (g.uiItalic) Typeface.BOLD_ITALIC else Typeface.BOLD
        val base = when (g.uiFont) {
            UiFont.SANS -> Typeface.SANS_SERIF
            UiFont.SERIF -> Typeface.SERIF
            UiFont.MONO -> Typeface.MONOSPACE
            UiFont.CONDENSED -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        }
        return Typeface.create(base, st)
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; this.color = color }
    private fun stroke(color: Int, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; this.color = color; strokeWidth = w
        strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }

    /** Стрелка игрока, смотрит вверх (поворачивается слоем карты). */
    fun player(g: GameStyle, d: Float): Bitmap {
        val s = (44 * d).toInt()
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Path().apply {
            moveTo(s / 2f, s * 0.08f)
            lineTo(s * 0.82f, s * 0.88f)
            lineTo(s / 2f, s * 0.68f)
            lineTo(s * 0.18f, s * 0.88f)
            close()
        }
        c.drawPath(p, fill(g.player))
        c.drawPath(p, stroke(g.playerLine, 2.5f * d))
        return bmp
    }

    /** Метка назначения (как вейпоинт в игре). */
    fun waypoint(g: GameStyle, d: Float): Bitmap {
        val w = (34 * d).toInt()
        val h = (46 * d).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = w / 2f
        val r = w / 2f - 3 * d
        val cy = r + 3 * d
        val head = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        val tip = Path().apply {
            moveTo(cx - r * 0.62f, cy + r * 0.72f)
            lineTo(cx, h - 2 * d)
            lineTo(cx + r * 0.62f, cy + r * 0.72f)
            close()
        }
        head.op(tip, Path.Op.UNION)
        c.drawPath(head, fill(g.waypoint))
        c.drawPath(head, stroke(0xFFFFFFFF.toInt(), 2.5f * d))
        c.drawCircle(cx, cy, r * 0.32f, fill(0xFFFFFFFF.toInt()))
        return bmp
    }

    private fun shapePath(shape: BlipShape, cx: Float, cy: Float, r: Float): Path = Path().apply {
        when (shape) {
            BlipShape.CIRCLE -> addCircle(cx, cy, r, Path.Direction.CW)
            BlipShape.SQUARE -> addRect(cx - r * 0.9f, cy - r * 0.9f, cx + r * 0.9f, cy + r * 0.9f, Path.Direction.CW)
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
    fun blip(g: GameStyle, cat: PoiCategory, d: Float): Bitmap {
        val s = (28 * d).toInt()
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = s / 2f
        val cy = s / 2f
        val r = s / 2f - 2 * d
        val path = shapePath(g.blipShape, cx, cy, r)
        val bg: Int
        val line: Int
        val glyph: Int
        if (g.blipColored) {
            bg = cat.color; line = g.blipLine; glyph = 0xFFFFFFFF.toInt()
        } else {
            bg = g.blipBg; line = cat.color; glyph = cat.color
        }
        c.drawPath(path, fill(bg))
        c.drawPath(path, stroke(line, 2f * d))
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glyph
            typeface = typeface(g)
            textAlign = Paint.Align.CENTER
            textSize = (if (cat.glyph.length > 1) 11f else 15f) * d
        }
        val y = cy - (tp.descent() + tp.ascent()) / 2f
        c.drawText(cat.glyph, cx, y, tp)
        return bmp
    }
}
