package com.gamemaps.app.ui

import android.content.Context
import android.os.BatteryManager
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamemaps.app.AppViewModel
import com.gamemaps.app.BannerKind
import com.gamemaps.app.map.GameFonts
import com.gamemaps.app.style.HudKind
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val HH_MM = DateTimeFormatter.ofPattern("HH:mm")

// ---------------- общие кусочки ----------------

@Composable
private fun rememberBattery(): Int {
    val ctx = LocalContext.current
    var level by remember { mutableIntStateOf(100) }
    LaunchedEffect(Unit) {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        while (true) {
            level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 } ?: 100
            delay(30_000)
        }
    }
    return level
}

@Composable
private fun rememberClock(): String {
    var t by remember { androidx.compose.runtime.mutableStateOf(LocalTime.now().format(HH_MM)) }
    LaunchedEffect(Unit) {
        while (true) {
            t = LocalTime.now().format(HH_MM)
            delay(1000)
        }
    }
    return t
}

private fun gpsQuality(vm: AppViewModel): Float {
    val acc = vm.location?.accuracy ?: return 0f
    return (1f - (acc - 5f) / 45f).coerceIn(0.05f, 1f)
}

private fun speedKmh(vm: AppViewModel): Int = ((vm.location?.speed ?: 0f) * 3.6f).roundToInt()

private fun starPath(cx: Float, cy: Float, ro: Float, ri: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) ro else ri
        val a = Math.toRadians(-90.0 + i * 36.0)
        val x = cx + r * cos(a).toFloat()
        val y = cy + r * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
fun WantedStars(count: Int, max: Int, active: Color, inactive: Color, size: Dp = 22.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 0 until max) {
            // звёзды заполняются справа налево, как в GTA
            val on = i >= max - count
            Canvas(Modifier.size(size)) {
                val p = starPath(this.size.width / 2, this.size.height / 2, this.size.width / 2 - 1.5f, this.size.width / 4.6f)
                drawPath(p, if (on) active else inactive)
                drawPath(p, Color.Black, style = Stroke(1.6.dp.toPx()))
            }
        }
    }
}

@Composable
private fun HudBar(fraction: Float, color: Color, width: Dp, height: Dp = 11.dp, back: Color = Color.Black.copy(alpha = 0.55f)) {
    Box(
        Modifier
            .width(width)
            .height(height)
            .background(back)
            .border(2.dp, Color.Black)
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(color))
    }
}

private fun groupMoney(v: Long): String = String.format(Locale("ru"), "%,d", v).replace(' ', ' ')

// ---------------- верхний HUD (справа сверху) ----------------

@Composable
fun GameHudTop(vm: AppViewModel, modifier: Modifier = Modifier) {
    val g = LocalGame.current
    val money = vm.stats.money
    val battery = rememberBattery()
    when (g.hud) {
        HudKind.SA -> Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TitleText(rememberClock(), 30.sp, Color(0xFFE8E8E8))
            HudBar(battery / 100f, Color(g.barA), 120.dp)
            HudBar(gpsQuality(vm), Color(g.barB), 120.dp)
            TitleText("\$" + String.format(Locale.US, "%08d", money.coerceAtMost(99_999_999)), 30.sp, Color(g.moneyColor))
            WantedStars(vm.wanted, 6, Color(0xFFE1B84C), Color.Black.copy(alpha = 0.35f), 22.dp)
        }
        HudKind.V -> Column(modifier, horizontalAlignment = Alignment.End) {
            TitleText("\$" + groupMoney(money), 28.sp, Color(g.moneyColor))
            WantedStars(vm.wanted.coerceAtMost(5), 5, Color.White, Color.White.copy(alpha = 0.15f), 20.dp)
        }
        HudKind.VICE -> Column(modifier, horizontalAlignment = Alignment.End) {
            TitleText("\$" + groupMoney(money), 30.sp, Color(g.moneyColor), outline = Color(0xFF5A0A6E))
            WantedStars(vm.wanted, 6, Color(g.uiAccent), Color.Black.copy(alpha = 0.3f), 20.dp)
        }
        HudKind.WEST -> Column(modifier, horizontalAlignment = Alignment.End) {
            TitleText("\$" + String.format(Locale.US, "%.2f", money / 100.0), 26.sp, Color(g.moneyColor), outline = Color(0xFFE9DDBF))
            if (vm.wanted > 0) {
                GText("Розыск · награда \$${vm.wanted * 25}", size = 13.sp, color = Color(g.uiAccent), upper = false)
            }
        }
        HudKind.HACKER -> GPanel(modifier, padding = 8.dp) {
            GText("NET // ONLINE", size = 11.sp, color = Color(g.uiAccent))
            GText("Устройств рядом: ${vm.nearbyCount}", size = 11.sp, upper = false)
            GText("Взломов: ${vm.stats.encounters}", size = 11.sp, upper = false)
        }
        HudKind.CRED -> Column(modifier, horizontalAlignment = Alignment.End) {
            TitleText(g.moneyPrefix + groupMoney(money), 24.sp, Color(g.moneyColor))
            GText("Street Cred  ${vm.stats.level}", size = 12.sp, color = Color(g.uiText))
            HudBar(vm.stats.levelProgress, Color(g.uiText), 120.dp, 6.dp)
        }
        HudKind.PIP -> GPanel(modifier, padding = 8.dp) {
            TitleText("LVL ${vm.stats.level}", 22.sp, Color(g.uiAccent))
            GText("Шаги: ${vm.stats.steps}", size = 11.sp, upper = false)
            GText(g.moneyPrefix + money, size = 11.sp, upper = false)
        }
        HudKind.POCKET -> GPanel(modifier, padding = 8.dp) {
            GText("Шаги ${vm.stats.steps}", size = 9.sp, upper = false)
            GText("Встреч ${vm.stats.encounters}", size = 9.sp, upper = false)
            GText(g.moneyPrefix + money, size = 9.sp, upper = false)
        }
        HudKind.RACER -> Column(modifier, horizontalAlignment = Alignment.End) {
            val nav = vm.nav
            if (nav != null) RaceTimer(nav.startedAt)
            TitleText("\$" + groupMoney(money), 20.sp, Color(g.moneyColor))
        }
        HudKind.MATRIX -> Column(modifier, horizontalAlignment = Alignment.End) {
            BlinkCursorText("> wake up_", Color(g.uiAccent))
        }
        HudKind.TREASURE -> Column(modifier, horizontalAlignment = Alignment.End) {
            CompassRose(vm.cameraBearing, Color(g.uiText), Color(g.uiAccent), 76.dp)
            TitleText(g.moneyPrefix + groupMoney(money), 22.sp, Color(g.moneyColor), outline = Color(0xFFF0E0B8))
        }
        HudKind.SYNTH -> Column(modifier, horizontalAlignment = Alignment.End) {
            TitleText(g.moneyPrefix + String.format(Locale.US, "%06d", money.coerceAtMost(999_999)), 20.sp, Color(g.moneyColor))
            GText("LVL ${vm.stats.level}", size = 12.sp, color = Color(g.uiAccent))
        }
        HudKind.BLUEPRINT -> Column(modifier, horizontalAlignment = Alignment.End) {
            GText("Лист 1 из 1 · масштаб 1:${(vm.metersPerDp * 160).roundToInt()}", size = 14.sp, upper = false)
        }
        HudKind.BLOCK, HudKind.TACTICAL -> {}
    }
}

// ---------------- нижний HUD ----------------

@Composable
fun GameHudBottom(vm: AppViewModel, modifier: Modifier = Modifier) {
    val g = LocalGame.current
    val battery = rememberBattery()
    when (g.hud) {
        HudKind.PIP -> Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GText("HP", size = 12.sp)
            HudBar(battery / 100f, Color(g.uiAccent), 70.dp, 8.dp, Color(g.uiBg))
            Spacer(Modifier.weight(1f))
            val rad = ((vm.myPoint()?.let { (it.lat * 1000 + it.lon * 1000) } ?: 0.0) % 7.0).let { kotlin.math.abs(it) }
            GText(String.format(Locale.US, "RAD %.1f", rad), size = 12.sp)
            Spacer(Modifier.weight(1f))
            GText("AP", size = 12.sp)
            HudBar(gpsQuality(vm), Color(g.uiAccent), 70.dp, 8.dp, Color(g.uiBg))
        }
        HudKind.BLOCK -> Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Hearts(battery)
            Spacer(Modifier.height(4.dp))
            Box(contentAlignment = Alignment.Center) {
                HudBar(vm.stats.levelProgress, Color(0xFF80FF20), 220.dp, 8.dp, Color(0xFF1E1E1E))
                TitleText("${vm.stats.level}", 12.sp, Color(0xFF80FF20), Modifier.padding(bottom = 18.dp))
            }
        }
        HudKind.RACER -> Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column {
                GText("Heat", size = 12.sp, color = Color(g.uiDim))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (i in 0 until 5) {
                        Box(
                            Modifier.size(width = 18.dp, height = 10.dp)
                                .background(if (i < vm.wanted) Color(g.barA) else Color.White.copy(alpha = 0.15f))
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                TitleText("${speedKmh(vm)}", 56.sp, Color(g.uiText))
                Spacer(Modifier.width(4.dp))
                GText("км/ч", size = 14.sp, color = Color(g.uiAccent), modifier = Modifier.padding(bottom = 12.dp))
            }
        }
        HudKind.BLUEPRINT -> ScaleRuler(vm.metersPerDp, Color(g.uiText), modifier)
        else -> {}
    }
}

@Composable
private fun Hearts(battery: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        val full = (battery + 5) / 10
        for (i in 0 until 10) {
            Canvas(Modifier.size(16.dp)) {
                val u = this.size.width / 9f
                // пиксельное сердечко 9x8
                val rows = listOf("011000110", "111101111", "111111111", "111111111", "011111110", "001111100", "000111000", "000010000")
                val col = if (i < full) Color(0xFFE02020) else Color(0xFF3A3A3A)
                rows.forEachIndexed { y, line ->
                    line.forEachIndexed { x, ch ->
                        if (ch == '1') drawRect(col, Offset(x * u, y * u + u * 0.5f), androidx.compose.ui.geometry.Size(u + 0.5f, u + 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RaceTimer(startedAt: Long) {
    val g = LocalGame.current
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(startedAt) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(100)
        }
    }
    val ms = (now - startedAt).coerceAtLeast(0)
    val txt = String.format(Locale.US, "%02d:%02d.%d", ms / 60000, (ms / 1000) % 60, (ms / 100) % 10)
    TitleText(txt, 26.sp, Color(g.uiAccent))
}

@Composable
private fun BlinkCursorText(text: String, color: Color) {
    var on by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(530)
            on = 1 - on
        }
    }
    GText(if (on == 1) text else text.dropLast(1), size = 13.sp, color = color, upper = false)
}

/** Компасная лента сверху (как в шутерах). */
@Composable
fun CompassTape(heading: Float, color: Color, accent: Color, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val g = LocalGame.current
    val tf = remember(g.id) { GameFonts.ui(ctx, g) }
    Canvas(modifier.fillMaxWidth().height(34.dp)) {
        val w = size.width
        val span = 120f
        val pxPerDeg = w / span
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = colorToInt(color); textSize = 12.dp.toPx(); typeface = tf
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val labels = mapOf(0 to "N", 45 to "NE", 90 to "E", 135 to "SE", 180 to "S", 225 to "SW", 270 to "W", 315 to "NW")
        val start = (heading - span / 2).toInt() - 1
        for (deg in start..(start + span.toInt() + 2)) {
            if (deg % 5 != 0) continue
            val x = w / 2 + (deg - heading) * pxPerDeg
            val norm = ((deg % 360) + 360) % 360
            val big = norm % 15 == 0
            drawLine(color.copy(alpha = 0.8f), Offset(x, 0f), Offset(x, if (big) 10.dp.toPx() else 5.dp.toPx()), 1.5f)
            val label = labels[norm] ?: if (norm % 15 == 0) norm.toString() else null
            if (label != null) {
                paint.color = colorToInt(if (labels.containsKey(norm)) accent else color)
                drawContext.canvas.nativeCanvas.drawText(label, x, 26.dp.toPx(), paint)
            }
        }
        val tri = Path().apply {
            moveTo(w / 2, 12.dp.toPx()); lineTo(w / 2 - 6.dp.toPx(), 0f); lineTo(w / 2 + 6.dp.toPx(), 0f); close()
        }
        drawPath(tri, accent)
    }
}

private fun colorToInt(c: Color): Int {
    val a = (c.alpha * 255).toInt() and 0xFF
    val r = (c.red * 255).toInt() and 0xFF
    val g = (c.green * 255).toInt() and 0xFF
    val b = (c.blue * 255).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

/** Роза ветров для пиратской карты. */
@Composable
private fun CompassRose(bearing: Float, color: Color, accent: Color, size: Dp) {
    Canvas(Modifier.size(size).rotate(-bearing)) {
        val c = center
        val r = this.size.minDimension / 2f
        drawCircle(color.copy(alpha = 0.6f), r - 2f, c, style = Stroke(2.dp.toPx()))
        for (k in 0 until 8) {
            val long = k % 2 == 0
            val a = Math.toRadians(k * 45.0 - 90.0)
            val len = if (long) r * 0.95f else r * 0.55f
            val tip = Offset(c.x + len * cos(a).toFloat(), c.y + len * sin(a).toFloat())
            val side = Math.toRadians(k * 45.0)
            val wdt = r * 0.12f
            val p = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(c.x + wdt * cos(side).toFloat(), c.y + wdt * sin(side).toFloat())
                lineTo(c.x - wdt * cos(side).toFloat(), c.y - wdt * sin(side).toFloat())
                close()
            }
            drawPath(p, if (k == 0) accent else color)
        }
    }
}

/** Масштабная линейка для стиля «чертёж». */
@Composable
private fun ScaleRuler(metersPerDp: Double, color: Color, modifier: Modifier) {
    val target = metersPerDp * 120
    val nice = listOf(5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0, 20000.0, 50000.0)
        .lastOrNull { it <= target } ?: 5.0
    val widthDp = (nice / metersPerDp).toFloat().coerceIn(20f, 200f)
    Column(modifier) {
        Canvas(Modifier.width(widthDp.dp).height(12.dp)) {
            drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), 3f)
            for (k in 0..4) {
                val x = size.width * k / 4
                drawLine(color, Offset(x, size.height), Offset(x, if (k % 2 == 0) 0f else size.height / 2), 3f)
            }
        }
        GText(if (nice >= 1000) "${(nice / 1000).toInt()} км" else "${nice.toInt()} м", size = 14.sp, upper = false)
    }
}

// ---------------- баннеры событий ----------------

@Composable
fun BoxScope.BannerOverlay(vm: AppViewModel) {
    val b = vm.banner ?: return
    val g = LocalGame.current
    val anim = remember(b.id) { Animatable(0f) }
    LaunchedEffect(b.id) {
        anim.animateTo(1f, tween(450))
        delay(if (b.kind == BannerKind.WASTED) 4200 else 3800)
        if (vm.banner?.id == b.id) vm.banner = null
    }
    val dismiss = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.banner = null }
    val a = anim.value
    when (b.kind) {
        BannerKind.PASS -> Box(
            Modifier.fillMaxSize().then(dismiss).graphicsLayer(alpha = a),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.graphicsLayer(scaleX = 0.6f + 0.4f * a, scaleY = 0.6f + 0.4f * a),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TitleText(b.title, 46.sp, Color(g.passColor), align = TextAlign.Center)
                if (b.sub.isNotBlank()) TitleText(b.sub, 24.sp, Color.White, align = TextAlign.Center)
            }
        }
        BannerKind.WASTED -> Box(
            Modifier.fillMaxSize().then(dismiss).background(Color.Black.copy(alpha = 0.5f * a)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer(alpha = a)) {
                TitleText(b.title, 64.sp, Color(g.wastedColor), align = TextAlign.Center)
                GText(b.sub, size = 16.sp, color = Color.White, shadow = true, upper = false)
            }
        }
        BannerKind.ACHIEVEMENT -> Box(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 70.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val shape = RoundedCornerShape(g.uiCorner.dp)
            Row(
                Modifier
                    .graphicsLayer(alpha = a, translationY = (1f - a) * -80f)
                    .clip(shape)
                    .background(if (g.hud == HudKind.BLOCK) Color(0xFF212121) else Color(g.uiPanel))
                    .border(2.dp, if (g.hud == HudKind.BLOCK) Color(0xFF8B8B8B) else Color(g.uiAccent), shape)
                    .then(dismiss)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(Modifier.size(26.dp)) {
                    drawPath(starPath(size.width / 2, size.height / 2, size.width / 2, size.width / 4.6f), Color(0xFFFFD84A))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    GText(b.title, size = 12.sp, color = if (g.hud == HudKind.BLOCK) Color(0xFFFFFF55) else Color(g.uiAccent))
                    GText(b.sub, size = 14.sp, color = if (g.hud == HudKind.BLOCK) Color.White else Color(g.uiText), upper = false)
                }
            }
        }
        BannerKind.ENCOUNTER -> Box(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp).padding(bottom = 90.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer(alpha = a)
                    .background(Color(g.uiBg))
                    .border(4.dp, Color(g.uiText))
                    .then(dismiss)
                    .padding(14.dp)
            ) {
                GText(b.title, size = 14.sp, upper = false, weight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                GText(b.sub, size = 11.sp, color = Color(g.uiDim), upper = false)
            }
        }
    }
}
