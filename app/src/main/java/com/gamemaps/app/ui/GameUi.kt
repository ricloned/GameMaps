package com.gamemaps.app.ui

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gamemaps.app.map.GameFonts
import com.gamemaps.app.map.MarkerIcons
import com.gamemaps.app.model.PoiCategory
import com.gamemaps.app.style.BlipShape
import com.gamemaps.app.style.GameStyle
import com.gamemaps.app.style.GameStyles
import com.gamemaps.app.style.UiFont

val LocalGame = staticCompositionLocalOf { GameStyles.V }

private val condensedFamily: FontFamily by lazy { FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL)) }
private val resFamilies = HashMap<Int, FontFamily>()

private fun resFamily(res: Int): FontFamily = resFamilies.getOrPut(res) { FontFamily(Font(res)) }

fun GameStyle.family(): FontFamily {
    val r = uiFontRes
    if (r != null) return resFamily(r)
    return when (uiFont) {
        UiFont.SANS -> FontFamily.SansSerif
        UiFont.SERIF -> FontFamily.Serif
        UiFont.MONO -> FontFamily.Monospace
        UiFont.CONDENSED -> condensedFamily
    }
}

/** Шрифт заголовков («MISSION PASSED», деньги и т.п.). */
fun GameStyle.titleFamily(): FontFamily = titleFontRes?.let { resFamily(it) } ?: family()

/** Текст заголовочным шрифтом стиля, с тенью. */
@Composable
fun TitleText(
    text: String,
    size: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
    outline: Color = Color.Black
) {
    val g = LocalGame.current
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = align,
        maxLines = 2,
        style = TextStyle(
            fontFamily = g.titleFamily(),
            fontSize = size,
            shadow = Shadow(outline, Offset(3f, 3f), 4f)
        )
    )
}

@Composable
fun gameTextStyle(size: TextUnit, weight: FontWeight = FontWeight.Bold): TextStyle {
    val g = LocalGame.current
    return TextStyle(
        fontFamily = g.family(),
        fontSize = size,
        fontWeight = weight,
        fontStyle = if (g.uiItalic) FontStyle.Italic else FontStyle.Normal,
        letterSpacing = g.uiSpacing.em
    )
}

@Composable
fun GText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 15.sp,
    color: Color = Color.Unspecified,
    weight: FontWeight = FontWeight.Bold,
    upper: Boolean = true,
    shadow: Boolean = false,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    val g = LocalGame.current
    val c = if (color == Color.Unspecified) Color(g.uiText) else color
    var st = gameTextStyle(size, weight)
    if (shadow) st = st.copy(shadow = Shadow(Color.Black, Offset(3f, 3f), 3f))
    Text(
        text = if (upper) text.uppercase() else text,
        modifier = modifier,
        color = c,
        style = st,
        textAlign = align,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun GPanel(modifier: Modifier = Modifier, padding: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    val g = LocalGame.current
    val shape = RoundedCornerShape(g.uiCorner.dp)
    Column(
        modifier
            .clip(shape)
            .background(Color(g.uiPanel))
            .border(1.dp, Color(g.uiAccent).copy(alpha = 0.35f), shape)
            .padding(padding),
        content = content
    )
}

@Composable
fun GButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true
) {
    val g = LocalGame.current
    val shape = RoundedCornerShape(g.uiCorner.dp)
    val bg = if (primary) Color(g.uiAccent) else Color(g.uiText).copy(alpha = 0.08f)
    val fg = if (primary) Color(g.uiOnAccent) else Color(g.uiText)
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(bg)
            .border(1.dp, Color(g.uiAccent).copy(alpha = if (primary) 1f else 0.5f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        GText(text, size = 13.sp, color = fg, maxLines = 1)
    }
}

enum class Glyph { MENU, SEARCH, LOCATE, RADAR, MAP, NORTH, ROUTE, CLOSE, BACK, SATELLITE, CUBE }

@Composable
fun GlyphIcon(glyph: Glyph, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.11f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(w * x1, h * y1), Offset(w * x2, h * y2), sw, StrokeCap.Round)
        when (glyph) {
            Glyph.MENU -> {
                line(0.12f, 0.22f, 0.88f, 0.22f); line(0.12f, 0.5f, 0.88f, 0.5f); line(0.12f, 0.78f, 0.88f, 0.78f)
            }
            Glyph.SEARCH -> {
                drawCircle(color, w * 0.28f, Offset(w * 0.42f, h * 0.42f), style = stroke)
                line(0.63f, 0.63f, 0.9f, 0.9f)
            }
            Glyph.LOCATE -> {
                drawCircle(color, w * 0.3f, center, style = stroke)
                drawCircle(color, w * 0.11f, center)
                line(0.5f, 0.02f, 0.5f, 0.18f); line(0.5f, 0.82f, 0.5f, 0.98f)
                line(0.02f, 0.5f, 0.18f, 0.5f); line(0.82f, 0.5f, 0.98f, 0.5f)
            }
            Glyph.RADAR -> {
                drawCircle(color, w * 0.42f, center, style = stroke)
                drawCircle(color, w * 0.18f, center, style = Stroke(width = sw * 0.7f))
                line(0.5f, 0.5f, 0.8f, 0.2f)
            }
            Glyph.MAP -> {
                val p = Path().apply {
                    moveTo(w * 0.08f, h * 0.2f); lineTo(w * 0.36f, h * 0.1f); lineTo(w * 0.64f, h * 0.22f)
                    lineTo(w * 0.92f, h * 0.12f); lineTo(w * 0.92f, h * 0.8f); lineTo(w * 0.64f, h * 0.9f)
                    lineTo(w * 0.36f, h * 0.78f); lineTo(w * 0.08f, h * 0.88f); close()
                }
                drawPath(p, color, style = stroke)
                line(0.36f, 0.1f, 0.36f, 0.78f); line(0.64f, 0.22f, 0.64f, 0.9f)
            }
            Glyph.NORTH -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.06f); lineTo(w * 0.78f, h * 0.88f)
                    lineTo(w * 0.5f, h * 0.7f); lineTo(w * 0.22f, h * 0.88f); close()
                }
                drawPath(p, color)
            }
            Glyph.ROUTE -> {
                val p = Path().apply {
                    moveTo(w * 0.2f, h * 0.84f)
                    cubicTo(w * 0.2f, h * 0.45f, w * 0.8f, h * 0.6f, w * 0.8f, h * 0.2f)
                }
                drawPath(p, color, style = stroke)
                drawCircle(color, w * 0.11f, Offset(w * 0.2f, h * 0.84f))
                drawCircle(color, w * 0.11f, Offset(w * 0.8f, h * 0.18f))
            }
            Glyph.CLOSE -> {
                line(0.2f, 0.2f, 0.8f, 0.8f); line(0.8f, 0.2f, 0.2f, 0.8f)
            }
            Glyph.SATELLITE -> {
                // спутник: корпус и две панели
                drawRect(color, Offset(w * 0.38f, h * 0.38f), androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.24f))
                drawRect(color, Offset(w * 0.04f, h * 0.42f), androidx.compose.ui.geometry.Size(w * 0.26f, h * 0.16f), style = stroke)
                drawRect(color, Offset(w * 0.70f, h * 0.42f), androidx.compose.ui.geometry.Size(w * 0.26f, h * 0.16f), style = stroke)
                line(0.30f, 0.5f, 0.38f, 0.5f); line(0.62f, 0.5f, 0.70f, 0.5f)
                line(0.5f, 0.62f, 0.5f, 0.78f)
                drawCircle(color, w * 0.06f, Offset(w * 0.5f, h * 0.84f))
            }
            Glyph.CUBE -> {
                val p = Path().apply {
                    moveTo(w * 0.5f, h * 0.1f); lineTo(w * 0.88f, h * 0.3f); lineTo(w * 0.88f, h * 0.72f)
                    lineTo(w * 0.5f, h * 0.92f); lineTo(w * 0.12f, h * 0.72f); lineTo(w * 0.12f, h * 0.3f); close()
                }
                drawPath(p, color, style = stroke)
                line(0.12f, 0.3f, 0.5f, 0.5f); line(0.88f, 0.3f, 0.5f, 0.5f); line(0.5f, 0.5f, 0.5f, 0.92f)
            }
            Glyph.BACK -> {
                line(0.62f, 0.15f, 0.28f, 0.5f); line(0.28f, 0.5f, 0.62f, 0.85f)
            }
        }
    }
}

@Composable
fun HudButton(
    icon: Glyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: Dp = 52.dp,
    rotation: Float = 0f
) {
    val g = LocalGame.current
    val shape = if (g.blipShape == BlipShape.CIRCLE) CircleShape else RoundedCornerShape(g.uiCorner.dp)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(if (active) Color(g.uiAccent) else Color(g.uiPanel))
            .border(2.dp, Color(g.uiAccent).copy(alpha = 0.7f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        GlyphIcon(
            icon,
            if (active) Color(g.uiOnAccent) else Color(g.uiText),
            Modifier.size(size * 0.48f).rotate(rotation)
        )
    }
}

@Composable
fun GToggle(label: String, value: Boolean, hint: String? = null, onChange: (Boolean) -> Unit) {
    val g = LocalGame.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!value) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            GText(label, size = 14.sp, upper = false)
            if (hint != null) GText(hint, size = 11.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false)
        }
        GText(
            if (value) "Вкл" else "Выкл",
            size = 13.sp,
            color = if (value) Color(g.uiAccent) else Color(g.uiDim)
        )
    }
}

@Composable
fun GSegment(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, t ->
            GButton(t, { onSelect(i) }, Modifier.weight(1f), primary = i == selected)
        }
    }
}

@Composable
fun GSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onDone: (Float) -> Unit) {
    val g = LocalGame.current
    var v by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        GText("$label: ${String.format("%.1f", v)}×", size = 14.sp, upper = false)
        Slider(
            value = v,
            onValueChange = { v = it },
            valueRange = range,
            onValueChangeFinished = { onDone(v) },
            colors = SliderDefaults.colors(
                thumbColor = Color(g.uiAccent),
                activeTrackColor = Color(g.uiAccent),
                inactiveTrackColor = Color(g.uiDim).copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    val g = LocalGame.current
    GText(text, size = 13.sp, color = Color(g.uiAccent), modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}

@Composable
fun BlipImage(cat: PoiCategory, size: Dp) {
    val g = LocalGame.current
    val d = LocalDensity.current.density
    val ctx = LocalContext.current
    val bmp = remember(g.id, cat, d) { MarkerIcons.blip(g, cat, d, GameFonts.ui(ctx, g)).asImageBitmap() }
    Image(bitmap = bmp, contentDescription = cat.title, modifier = Modifier.size(size))
}
