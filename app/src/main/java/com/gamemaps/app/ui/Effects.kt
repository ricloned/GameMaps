package com.gamemaps.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.gamemaps.app.style.Fx
import com.gamemaps.app.style.GameStyle
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.max
import kotlin.random.Random

/** Эффекты поверх всей карты (не перехватывают касания). */
@Composable
fun MapOverlays(g: GameStyle, forceNight: Boolean?, modifier: Modifier = Modifier) {
    val fx = g.fx
    Box(modifier.fillMaxSize()) {
        if (Fx.TINT in fx) TintOverlay(Color(g.uiAccent).copy(alpha = 0.05f))
        if (Fx.PAPER in fx) TintOverlay(Color(0xFF8B5A2B).copy(alpha = 0.07f))
        if (Fx.DAY_NIGHT in fx) DayNightOverlay(forceNight)
        if (Fx.GRID_PAPER in fx) GridPaperOverlay(Color.White)
        if (Fx.LCD in fx) LcdOverlay(Color(g.uiText))
        if (Fx.SCANLINES in fx) ScanlinesOverlay()
        if (Fx.FLICKER in fx) FlickerOverlay(Color(g.uiAccent))
        if (Fx.VIGNETTE in fx || Fx.PAPER in fx) VignetteOverlay(if (Fx.PAPER in fx) Color(0xFF3A2410) else Color.Black)
        if (Fx.GLITCH in fx) GlitchOverlay(listOf(Color(g.uiAccent), Color(0xFFFF003C), Color(0xFF00F0FF)))
        if (Fx.CODE_RAIN in fx) CodeRain(Modifier.fillMaxSize(), Color(g.uiAccent), 0.10f)
    }
}

/** Фон вокруг круглого радара — свой у каждого стиля. */
@Composable
fun RadarBackdrop(g: GameStyle, modifier: Modifier) {
    when {
        Fx.SYNTH in g.fx -> SynthBackdrop(modifier)
        Fx.CODE_RAIN in g.fx -> CodeRain(modifier, Color(g.uiAccent), 0.85f)
        else -> GridBackdrop(Color(g.uiAccent), modifier)
    }
}

@Composable
private fun TintOverlay(color: Color) {
    Canvas(Modifier.fillMaxSize()) { drawRect(color) }
}

@Composable
private fun ScanlinesOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val step = 3.dp.toPx()
        val h = 1.dp.toPx() * 0.7f
        val c = Color.Black.copy(alpha = 0.16f)
        var y = 0f
        while (y < size.height) {
            drawRect(c, Offset(0f, y), Size(size.width, h))
            y += step
        }
    }
}

@Composable
private fun LcdOverlay(color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val step = 3.dp.toPx()
        val c = color.copy(alpha = 0.06f)
        var x = 0f
        while (x < size.width) {
            drawLine(c, Offset(x, 0f), Offset(x, size.height), 1f); x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(c, Offset(0f, y), Offset(size.width, y), 1f); y += step
        }
    }
}

@Composable
private fun GridPaperOverlay(color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val step = 20.dp.toPx()
        var i = 0
        var x = 0f
        while (x < size.width) {
            val a = if (i % 5 == 0) 0.22f else 0.09f
            drawLine(color.copy(alpha = a), Offset(x, 0f), Offset(x, size.height), 1f)
            x += step; i++
        }
        i = 0
        var y = 0f
        while (y < size.height) {
            val a = if (i % 5 == 0) 0.22f else 0.09f
            drawLine(color.copy(alpha = a), Offset(0f, y), Offset(size.width, y), 1f)
            y += step; i++
        }
    }
}

@Composable
private fun VignetteOverlay(color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Transparent, color.copy(alpha = 0.55f)),
                center = center,
                radius = max(size.width, size.height) * 0.75f
            )
        )
    }
}

@Composable
private fun FlickerOverlay(color: Color) {
    val t = rememberInfiniteTransition(label = "flicker")
    val a by t.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(tween(90, easing = LinearEasing), RepeatMode.Reverse),
        label = "flickerA"
    )
    Canvas(Modifier.fillMaxSize()) { drawRect(color.copy(alpha = a)) }
}

@Composable
private fun DayNightOverlay(force: Boolean?) {
    var hour by remember { mutableIntStateOf(LocalTime.now().hour) }
    LaunchedEffect(Unit) {
        while (true) {
            hour = LocalTime.now().hour
            delay(60_000)
        }
    }
    val alpha = when (force) {
        true -> 0.42f
        false -> 0f
        null -> when (hour) {
            in 21..23, in 0..4 -> 0.38f
            in 19..20, in 5..6 -> 0.18f
            else -> 0f
        }
    }
    if (alpha > 0f) Canvas(Modifier.fillMaxSize()) { drawRect(Color(0xFF0A0F3A).copy(alpha = alpha)) }
}

@Composable
private fun GlitchOverlay(colors: List<Color>) {
    var seed by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(5_000, 12_000))
            repeat(4) {
                seed = Random.nextInt(1, Int.MAX_VALUE)
                delay(70)
            }
            seed = 0
        }
    }
    if (seed != 0) {
        Canvas(Modifier.fillMaxSize()) {
            val r = Random(seed)
            repeat(5) {
                val y = r.nextFloat() * size.height
                val h = (4 + r.nextInt(26)).dp.toPx()
                val x = (r.nextFloat() - 0.5f) * 60.dp.toPx()
                drawRect(colors[r.nextInt(colors.size)].copy(alpha = 0.28f), Offset(x, y), Size(size.width, h))
            }
        }
    }
}

/** Сетка вокруг радара (по умолчанию). */
@Composable
private fun GridBackdrop(accent: Color, modifier: Modifier) {
    Canvas(modifier) {
        val step = 32.dp.toPx()
        val c = accent.copy(alpha = 0.07f)
        var x = 0f
        while (x < size.width) {
            drawLine(c, Offset(x, 0f), Offset(x, size.height), 1f); x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(c, Offset(0f, y), Offset(size.width, y), 1f); y += step
        }
    }
}

/** Синтвейв: закатное солнце в полосках и неоновая сетка до горизонта. */
@Composable
private fun SynthBackdrop(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "synth")
    val phase by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "synthPhase"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.58f
        drawRect(
            Brush.verticalGradient(
                0f to Color(0xFF0D0221), 0.6f to Color(0xFF3B0A57), 1f to Color(0xFFFF2E97),
                startY = 0f, endY = horizon
            ),
            size = Size(w, horizon)
        )
        // солнце
        val sunR = w * 0.3f
        val sunC = Offset(w / 2, horizon - sunR * 0.35f)
        drawCircle(
            Brush.verticalGradient(listOf(Color(0xFFF9C80E), Color(0xFFFF2E97)), startY = sunC.y - sunR, endY = sunC.y + sunR),
            radius = sunR, center = sunC
        )
        var band = 0
        var y = sunC.y
        while (y < sunC.y + sunR) {
            val bh = 2.dp.toPx() + band * 1.2f.dp.toPx()
            drawRect(Color(0xFF3B0A57), Offset(sunC.x - sunR, y), Size(sunR * 2, bh))
            y += bh + 6.dp.toPx()
            band++
        }
        // земля и сетка
        drawRect(Color(0xFF0D0221), Offset(0f, horizon), Size(w, h - horizon))
        val grid = Color(0xFFFF2E97).copy(alpha = 0.8f)
        for (i in 0 until 14) {
            val k = (i + phase) / 14f
            val yy = horizon + (h - horizon) * k * k
            drawLine(grid, Offset(0f, yy), Offset(w, yy), 2f)
        }
        val vp = Offset(w / 2, horizon)
        for (i in -12..12) {
            drawLine(grid, vp, Offset(w / 2 + i * w / 6f, h), 2f)
        }
        drawLine(Color(0xFF2DE2E6), Offset(0f, horizon), Offset(w, horizon), 3f)
    }
}

private const val RAIN_CHARS = "アイウエオカキクケコサシスセソタチツテトナニヌネノ0123456789ZXCVBNM<>*+=:"

/** «Падающий код» как в «Матрице». */
@Composable
fun CodeRain(modifier: Modifier, color: Color, alpha: Float) {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(70)
            tick++
        }
    }
    val pos = remember { FloatArray(160) { -Random.nextFloat() * 60f } }
    val speed = remember { FloatArray(160) { 0.4f + Random.nextFloat() * 0.8f } }
    val paint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
    Canvas(modifier) {
        if (tick < 0L) return@Canvas
        val cell = 14.dp.toPx()
        paint.textSize = cell * 0.9f
        paint.typeface = android.graphics.Typeface.MONOSPACE
        val cols = (size.width / cell).toInt().coerceAtMost(pos.size)
        val rows = (size.height / cell).toInt() + 1
        val nc = drawContext.canvas.nativeCanvas
        for (i in 0 until cols) {
            val head = pos[i]
            for (k in 0 until 14) {
                val row = (head - k).toInt()
                if (row < 0 || row > rows) continue
                val a = if (k == 0) 1f else (1f - k / 14f) * 0.8f
                paint.color = if (k == 0) Color.White.copy(alpha = alpha).toArgbInt() else color.copy(alpha = a * alpha).toArgbInt()
                val ch = RAIN_CHARS[(i * 31 + row * 17 + (tick / 6).toInt()) % RAIN_CHARS.length]
                nc.drawText(ch.toString(), i * cell, row * cell, paint)
            }
            pos[i] = head + speed[i]
            if (head - 14 > rows) pos[i] = -Random.nextFloat() * 30f
        }
    }
}

private fun Color.toArgbInt(): Int {
    val a = (alpha * 255).toInt() and 0xFF
    val r = (red * 255).toInt() and 0xFF
    val g = (green * 255).toInt() and 0xFF
    val b = (blue * 255).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

/** Вращающийся луч сканера внутри круга радара. */
@Composable
fun RadarSweep(color: Color, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "sweep")
    val angle by t.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "sweepAngle"
    )
    Canvas(modifier) {
        val r = size.minDimension / 2f
        rotate(angle) {
            drawCircle(
                Brush.sweepGradient(
                    0f to Color.Transparent, 0.82f to Color.Transparent, 1f to color.copy(alpha = 0.35f),
                    center = center
                ),
                radius = r
            )
            drawLine(color.copy(alpha = 0.7f), center, Offset(center.x + r, center.y), 2.dp.toPx())
        }
        for (k in 1..3) drawCircle(color.copy(alpha = 0.15f), r * k / 4f, center, style = Stroke(1.dp.toPx()))
    }
}
