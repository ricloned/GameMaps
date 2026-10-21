package com.radarmap.app.ui

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
import com.radarmap.app.AppViewModel
import com.radarmap.app.NavState
import com.radarmap.app.model.RouteResult
import com.radarmap.app.model.ViewMode
import com.radarmap.app.nav.Geo
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun BoxScope.Hud(vm: AppViewModel, onRequestPermission: () -> Unit) {
    val g = LocalGame.current
    val radar = vm.viewMode == ViewMode.RADAR
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
            if (nav != null) NavBanner(nav) else TopBar(vm)
            if (vm.routeLoading || vm.poiLoading) {
                GText("Загрузка…", size = 11.sp, color = Color(g.uiDim), shadow = true)
            }
            vm.message?.let { MessageBox(it) }
        }

        // ---- кнопки справа ----
        Column(
            Modifier.align(Alignment.CenterEnd).offset(y = (-30).dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HudButton(Glyph.MENU, { vm.openMenu() })
            HudButton(if (radar) Glyph.MAP else Glyph.RADAR, { vm.toggleMode() })
            HudButton(
                Glyph.LOCATE,
                { if (vm.hasPermission) vm.locate() else onRequestPermission() },
                active = vm.following && vm.location != null
            )
            val b = vm.cameraBearing
            val rotated = b > 1f && b < 359f
            val autoRotating = vm.following && vm.settings.headingUp && (radar || vm.nav != null)
            if (rotated && !autoRotating) HudButton(Glyph.NORTH, { vm.resetNorth() }, rotation = -b)
            HudButton(Glyph.ROUTE, { if (vm.editing) vm.closeEditor() else vm.startEditor() }, active = vm.editing)
        }

        // ---- низ ----
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZonePopup(vm)
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
                    "© OpenStreetMap · OpenFreeMap",
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
private fun TopBar(vm: AppViewModel) {
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
        if (vm.settings.showClock) {
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
        GText(vm.zoneName ?: "", size = 24.sp, color = Color(g.uiAccent), shadow = true)
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
        GText("Маршрут · ${route.profile.title}", size = 13.sp, color = Color(g.uiAccent))
        GText("${Geo.fmtDist(route.distance)} · ${Geo.fmtDur(route.duration)}", size = 22.sp, upper = false)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GButton("Поехали", { vm.startNavigation() }, Modifier.weight(1f), primary = true)
            GButton("Сброс", { vm.stopRoute() }, Modifier.weight(1f))
        }
    }
}
