package com.gamemaps.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamemaps.app.AppViewModel
import com.gamemaps.app.NavState
import com.gamemaps.app.model.RouteResult
import com.gamemaps.app.model.ViewMode
import com.gamemaps.app.style.HudKind
import com.gamemaps.app.nav.Geo
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun BoxScope.Hud(vm: AppViewModel, onRequestPermission: () -> Unit) {
    val g = LocalGame.current
    val radar = vm.viewMode == ViewMode.RADAR
    val hudOn = vm.settings.gameHud
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp)
    ) {
        // ---- верх ----
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val nav = vm.nav
            if (nav != null) NavBanner(nav) else TopBar(vm, showClock = vm.settings.showClock && !(hudOn && g.hud == HudKind.SA))
            if (hudOn && g.hud == HudKind.TACTICAL) {
                CompassTape(vm.cameraBearing, Color(g.uiText), Color(g.uiAccent))
            }
            if (hudOn) GameHudTop(vm, Modifier.align(Alignment.End))
            if (vm.routeLoading) {
                GText("Загрузка…", size = 11.sp, color = Color(g.uiDim), shadow = true)
            }
            vm.message?.let { MessageBox(it) }
            UpdateBanner(vm)
        }

        // ---- кнопки справа (в режиме карты) ----
        if (!radar) {
            Column(
                Modifier.align(Alignment.CenterEnd).offset(y = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HudButtons(vm, onRequestPermission, 48.dp)
            }
        }

        // ---- низ ----
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZonePopup(vm)
            if (hudOn) GameHudBottom(vm)
            if (radar) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HudButtons(vm, onRequestPermission, 42.dp)
                }
            }
            val sel = vm.selected
            val nav = vm.nav
            val route = vm.activeRoute
            when {
                vm.editing -> EditorPanel(vm)
                sel != null -> PlaceCard(vm, sel)
                nav != null -> NavBottom(vm, nav)
                route != null -> RoutePreview(vm, route)
            }
            if (!radar) {
                GText(
                    if (vm.settings.satellite) "© OpenStreetMap · OpenFreeMap · Esri, Maxar, Earthstar Geographics"
                    else "© OpenStreetMap · OpenFreeMap",
                    size = 9.sp,
                    color = Color(g.uiDim),
                    weight = FontWeight.Normal,
                    upper = false,
                    shadow = true,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
    }
}

@Composable
private fun HudButtons(vm: AppViewModel, onRequestPermission: () -> Unit, size: androidx.compose.ui.unit.Dp) {
    val radar = vm.viewMode == ViewMode.RADAR
    HudButton(Glyph.MENU, { vm.openMenu() }, size = size)
    HudButton(if (radar) Glyph.MAP else Glyph.RADAR, { vm.toggleMode() }, size = size)
    HudButton(Glyph.SATELLITE, { vm.toggleSatellite() }, active = vm.settings.satellite, size = size)
    if (!radar) HudButton(Glyph.CUBE, { vm.toggleTilt() }, active = vm.cameraTilt > 10f, size = size)
    HudButton(
        Glyph.LOCATE,
        { if (vm.hasPermission) vm.locate() else onRequestPermission() },
        active = vm.following && vm.location != null,
        size = size
    )
    val b = vm.cameraBearing
    val rotated = b > 1f && b < 359f
    val autoRotating = vm.following && vm.settings.headingUp
    if (rotated && !autoRotating) HudButton(Glyph.NORTH, { vm.resetNorth() }, rotation = -b, size = size)
    if (radar) HudButton(Glyph.SEARCH, { vm.openSearch() }, size = size)
    HudButton(Glyph.ROUTE, { if (vm.editing) vm.closeEditor() else vm.startEditor() }, active = vm.editing, size = size)
}

@Composable
private fun TopBar(vm: AppViewModel, showClock: Boolean) {
    val g = LocalGame.current
    val shape = RoundedCornerShape(g.uiCorner.dp)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .height(48.dp)
                .clip(shape)
                .background(Color(g.uiPanel))
                .border(1.dp, Color(g.uiAccent).copy(alpha = 0.5f), shape)
                .clickable { vm.openSearch() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlyphIcon(Glyph.SEARCH, Color(g.uiDim), Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            GText("Поиск: магазины, адреса", size = 14.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false, maxLines = 1)
        }
        if (showClock) {
            Spacer(Modifier.width(12.dp))
            Clock(vm)
        }
    }
}

@Composable
private fun Clock(vm: AppViewModel) {
    var time by remember { mutableStateOf(LocalTime.now().format(HHMM)) }
    LaunchedEffect(Unit) {
        while (true) {
            time = LocalTime.now().format(HHMM)
            delay(1000)
        }
    }
    val speed = ((vm.location?.speed ?: 0f) * 3.6f).roundToInt()
    Column(horizontalAlignment = Alignment.End) {
        GText(time, size = 26.sp, shadow = true)
        GText("$speed км/ч", size = 12.sp, shadow = true)
    }
}

@Composable
private fun MessageBox(text: String) {
    val g = LocalGame.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GPanel(padding = 10.dp) {
            GText(text, size = 13.sp, upper = false, color = Color(g.uiText))
        }
    }
}

/** Название улицы/района всплывает справа внизу, как в San Andreas. */
@Composable
private fun ColumnScope.ZonePopup(vm: AppViewModel) {
    val g = LocalGame.current
    var visible by remember { mutableStateOf(false) }
    val stamp = vm.zoneStamp
    LaunchedEffect(stamp) {
        if (stamp > 0 && vm.settings.showZone) {
            visible = true
            delay(4500)
            visible = false
        }
    }
    AnimatedVisibility(visible, modifier = Modifier.align(Alignment.End), enter = fadeIn(), exit = fadeOut()) {
        GText(vm.zoneName ?: "", size = if (g.hud == HudKind.SA) 30.sp else 24.sp, color = Color(g.zoneColor), shadow = true)
    }
}

@Composable
private fun NavBanner(nav: NavState) {
    val g = LocalGame.current
    val step = nav.route.steps.getOrNull(nav.stepIndex) ?: return
    GPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(58.dp).clip(RoundedCornerShape(g.uiCorner.dp)).background(Color(g.uiAccent)),
                contentAlignment = Alignment.Center
            ) {
                Text(step.arrow, fontSize = 34.sp, color = Color(g.uiOnAccent))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                GText(Geo.fmtDist(nav.distToStep), size = 26.sp, color = Color(g.uiAccent))
                GText(step.text, size = 15.sp, upper = false, maxLines = 2)
            }
        }
    }
}

@Composable
private fun NavBottom(vm: AppViewModel, nav: NavState) {
    val g = LocalGame.current
    val arrive = LocalTime.now().plusSeconds(nav.remainingTime.toLong()).format(HHMM)
    GPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                GText("${Geo.fmtDist(nav.remaining)} · ${Geo.fmtDur(nav.remainingTime)}", size = 20.sp, upper = false)
                GText("Прибытие в $arrive", size = 12.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false)
            }
            GButton("Стоп", { vm.stopRoute() })
        }
    }
}

@Composable
private fun RoutePreview(vm: AppViewModel, route: RouteResult) {
    val g = LocalGame.current
    GPanel(Modifier.fillMaxWidth()) {
        // способ передвижения: смена сразу пересчитывает варианты
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            com.gamemaps.app.model.Profile.entries.forEach { p ->
                GButton(p.title, { if (p != vm.settings.profile) vm.changeRouteMode(p) }, Modifier.weight(1f),
                    primary = p == vm.settings.profile, enabled = !vm.routeLoading)
            }
        }
        Spacer(Modifier.height(8.dp))
        val opts = vm.routeOptions.ifEmpty { listOf(route) }
        if (vm.routeLoading) {
            GText("Считаю варианты…", size = 13.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false)
            Spacer(Modifier.height(6.dp))
        }
        val fastest = opts.minOf { it.duration }
        val slowest = opts.maxOf { it.duration }
        opts.forEachIndexed { i, r ->
            val sel = i == vm.routeChoice
            val shape = RoundedCornerShape(g.uiCorner.dp)
            Column(
                Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(shape)
                    .background(if (sel) Color(g.uiAccent).copy(alpha = 0.16f) else Color(g.uiText).copy(alpha = 0.05f))
                    .border(if (sel) 2.dp else 1.dp, Color(g.uiAccent).copy(alpha = if (sel) 1f else 0.3f), shape)
                    .clickable { vm.chooseRoute(i) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GText("${Geo.fmtDur(r.duration)} · ${Geo.fmtDist(r.distance)}", size = 18.sp, upper = false, modifier = Modifier.weight(1f))
                    val diff = ((r.duration - fastest) / 60).roundToInt()
                    val (tag, col) = when {
                        opts.size > 1 && r.duration == fastest -> "Быстрый" to Color(0xFF4CD964)
                        opts.size > 2 && r.duration == slowest -> "Медленный · +$diff мин" to Color(0xFFFF6B5E)
                        diff > 0 -> "+$diff мин" to Color(0xFFFFC145)
                        else -> "" to Color(g.uiDim)
                    }
                    if (tag.isNotEmpty()) GText(tag, size = 12.sp, color = col)
                }
                if (r.segments.any { !it.walk }) {
                    // цепочка: пешком → линия → линия → пешком
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        r.segments.forEachIndexed { k, seg ->
                            if (k > 0) GText(" › ", size = 12.sp, color = Color(g.uiDim), upper = false)
                            if (seg.walk) GText("🚶${(seg.duration / 60).roundToInt().coerceAtLeast(1)}", size = 12.sp, color = Color(g.uiDim), upper = false)
                            else Box(
                                Modifier.clip(RoundedCornerShape(50)).background(Color(seg.color ?: g.route))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) { GText("M ${seg.stops}", size = 11.sp, color = Color.White, upper = false) }
                        }
                    }
                }
                if (r.summary.isNotBlank()) GText(r.summary, size = 12.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false, maxLines = 2)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GButton("Поехали", { vm.startNavigation() }, Modifier.weight(1f), primary = true)
            GButton("Сброс", { vm.stopRoute() }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun UpdateBanner(vm: AppViewModel) {
    val g = LocalGame.current
    val st = vm.updateState
    if (st != com.gamemaps.app.UpdateState.AVAILABLE && st != com.gamemaps.app.UpdateState.DOWNLOADING &&
        st != com.gamemaps.app.UpdateState.READY) return
    val r = vm.update ?: return
    GPanel(Modifier.fillMaxWidth()) {
        GText("Доступно обновление: ${r.name}", size = 14.sp, color = Color(g.uiAccent), upper = false)
        Spacer(Modifier.height(8.dp))
        when (st) {
            com.gamemaps.app.UpdateState.DOWNLOADING ->
                GText("Загрузка… ${(vm.updateProgress * 100).roundToInt()}%", size = 13.sp, upper = false)
            com.gamemaps.app.UpdateState.READY ->
                GButton("Установить", { vm.installUpdate() }, Modifier.fillMaxWidth(), primary = true)
            else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GButton("Позже", { vm.dismissUpdate() }, Modifier.weight(1f))
                GButton("Обновить", { vm.downloadUpdate() }, Modifier.weight(1f), primary = true)
            }
        }
    }
}
