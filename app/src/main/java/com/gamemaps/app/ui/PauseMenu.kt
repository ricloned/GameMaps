package com.gamemaps.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamemaps.app.AppViewModel
import com.gamemaps.app.model.PoiCategory
import com.gamemaps.app.model.Profile
import com.gamemaps.app.nav.Geo
import com.gamemaps.app.style.GameStyle
import com.gamemaps.app.style.GameStyles
import com.gamemaps.app.style.StyleGroup
import com.gamemaps.app.model.Achievements
import java.util.Locale

private val TABS = listOf("Стиль", "Места", "Маршруты", "Статистика", "Настройки")

/** Меню «паузы» в духе игр: вкладки сверху, всё настраивается на лету. */
@Composable
fun PauseMenu(vm: AppViewModel) {
    val g = LocalGame.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(g.uiBg).copy(alpha = 0.95f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    GText("GameMaps", size = 32.sp, color = Color(g.uiAccent), shadow = true)
                    GText("${g.name} · ${g.tag}", size = 12.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false)
                }
                HudButton(Glyph.CLOSE, { vm.menuOpen = false }, size = 46.dp)
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TABS.forEachIndexed { i, t -> TabChip(t, vm.menuTab == i) { vm.menuTab = i } }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (vm.menuTab) {
                    0 -> StyleTab(vm)
                    1 -> PlacesTab(vm)
                    2 -> RoutesTab(vm)
                    3 -> StatsTab(vm)
                    else -> SettingsTab(vm)
                }
            }
        }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val g = LocalGame.current
    val shape = RoundedCornerShape(g.uiCorner.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) Color(g.uiAccent) else Color(g.uiPanel))
            .border(1.dp, Color(g.uiAccent).copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        GText(text, size = 15.sp, color = if (selected) Color(g.uiOnAccent) else Color(g.uiText))
    }
}

// ---------------- Стиль ----------------

@Composable
private fun StyleTab(vm: AppViewModel) {
    val s = vm.settings
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (group in StyleGroup.entries) {
            val list = GameStyles.all.filter { it.group == group && (!it.hidden || vm.stats.secret) }
            if (list.isEmpty()) continue
            item(key = "group_${group.name}") { SectionTitle(group.title) }
            items(list, key = { it.id }) { st ->
                StyleCard(st, st.id == s.styleId) { vm.selectStyle(st.id) }
            }
        }
        item { SectionTitle("Вид карты") }
        item {
            GPanel(Modifier.fillMaxWidth()) {
                GToggle("Подписи улиц и районов", s.labels) { v -> vm.updateSettings { it.copy(labels = v) } }
                GToggle("Здания", s.buildings) { v -> vm.updateSettings { it.copy(buildings = v) } }
                GToggle("3D-здания", s.buildings3d, "объёмные дома, красиво с наклоном") { v ->
                    vm.updateSettings { it.copy(buildings3d = v) }
                }
                Spacer(Modifier.height(6.dp))
                GText("Язык подписей", size = 13.sp, upper = false)
                Spacer(Modifier.height(6.dp))
                val langs = listOf("local", "ru", "en")
                GSegment(listOf("Местный", "Русский", "English"), langs.indexOf(s.labelLang).coerceAtLeast(0)) { i ->
                    vm.updateSettings { it.copy(labelLang = langs[i]) }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StyleCard(st: GameStyle, selected: Boolean, onClick: () -> Unit) {
    CompositionLocalProvider(LocalGame provides st) {
        val shape = RoundedCornerShape(st.uiCorner.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color(st.uiBg))
                .border(if (selected) 3.dp else 1.dp, Color(st.uiAccent).copy(alpha = if (selected) 1f else 0.45f), shape)
                .clickable(onClick = onClick)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StylePreview(st, Modifier.size(width = 112.dp, height = 84.dp).clip(shape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                TitleText(st.name, 22.sp, Color(st.uiAccent), outline = Color.Black.copy(alpha = 0.6f))
                GText(st.tag, size = 12.sp, color = Color(st.uiText), weight = FontWeight.Normal, upper = false)
                Spacer(Modifier.height(4.dp))
                GText(st.description, size = 11.sp, color = Color(st.uiDim), weight = FontWeight.Normal, upper = false)
                if (selected) {
                    Spacer(Modifier.height(4.dp))
                    GText("✓ Выбран", size = 12.sp, color = Color(st.uiAccent))
                }
            }
        }
    }
}

/** Мини-превью карты в цветах стиля. */
@Composable
private fun StylePreview(st: GameStyle, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Color(st.land))
        drawRect(Color(st.landUrban), Offset(w * 0.04f, h * 0.06f), Size(w * 0.44f, h * 0.38f))
        drawRect(Color(st.park), Offset(w * 0.6f, h * 0.08f), Size(w * 0.3f, h * 0.26f))
        for (i in 0..3) drawRect(Color(st.building), Offset(w * (0.08f + i * 0.1f), h * 0.12f), Size(w * 0.07f, h * 0.1f))
        val water = Path().apply {
            moveTo(0f, h * 0.74f)
            cubicTo(w * 0.3f, h * 0.62f, w * 0.6f, h * 0.92f, w, h * 0.72f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(water, Color(st.water))
        st.roadCasing?.let { drawLine(Color(it), Offset(0f, h * 0.56f), Offset(w, h * 0.38f), h * 0.12f) }
        drawLine(Color(st.roadHighway), Offset(0f, h * 0.56f), Offset(w, h * 0.38f), h * 0.08f)
        drawLine(Color(st.roadMajor), Offset(w * 0.52f, 0f), Offset(w * 0.47f, h * 0.76f), h * 0.05f)
        drawLine(Color(st.roadMinor), Offset(0f, h * 0.3f), Offset(w * 0.5f, h * 0.3f), h * 0.03f)
        drawLine(Color(st.route), Offset(w * 0.5f, h * 0.47f), Offset(w * 0.86f, h * 0.41f), h * 0.04f, StrokeCap.Round)
        val cx = w * 0.5f
        val cy = h * 0.47f
        val s = h * 0.13f
        val arrow = Path().apply {
            moveTo(cx, cy - s); lineTo(cx + s * 0.75f, cy + s); lineTo(cx, cy + s * 0.5f); lineTo(cx - s * 0.75f, cy + s); close()
        }
        drawPath(arrow, Color(st.player))
        drawPath(arrow, Color(st.playerLine), style = Stroke(width = 1.5f))
        drawCircle(Color(PoiCategory.FOOD.color), h * 0.07f, Offset(w * 0.8f, h * 0.2f))
        drawCircle(Color(st.waypoint), h * 0.06f, Offset(w * 0.86f, h * 0.41f))
    }
}

// ---------------- Места ----------------

@Composable
private fun PlacesTab(vm: AppViewModel) {
    val g = LocalGame.current
    val s = vm.settings
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            GPanel(Modifier.fillMaxWidth()) {
                GToggle("Показывать места на карте", s.poiEnabled) { v -> vm.updateSettings { it.copy(poiEnabled = v) } }
                GToggle("Названия у значков", s.poiNames, "видны при сильном приближении") { v ->
                    vm.updateSettings { it.copy(poiNames = v) }
                }
                GSlider("Размер значков", s.blipScale, 0.6f..1.8f) { v -> vm.updateSettings { it.copy(blipScale = v) } }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GButton("Показать все", { vm.updateSettings { it.copy(hiddenCats = emptySet()) } }, Modifier.weight(1f))
                GButton("Скрыть все", {
                    vm.updateSettings { st -> st.copy(hiddenCats = PoiCategory.entries.map { c -> c.name }.toSet()) }
                }, Modifier.weight(1f))
            }
        }
        item { SectionTitle("Легенда") }
        items(PoiCategory.entries) { cat ->
            val on = cat.name !in s.hiddenCats
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { vm.toggleCat(cat) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BlipImage(cat, 30.dp)
                Spacer(Modifier.width(12.dp))
                GText(cat.title, size = 15.sp, upper = false, modifier = Modifier.weight(1f))
                GText(if (on) "Вкл" else "Выкл", size = 13.sp, color = if (on) Color(g.uiAccent) else Color(g.uiDim))
            }
        }
        item {
            GText(
                "Магазины и заведения подгружаются, когда карта приближена (примерно уровень квартала). " +
                        "Данные — OpenStreetMap, без отзывов и рейтингов.",
                size = 11.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
        }
    }
}

// ---------------- Маршруты ----------------

@Composable
private fun RoutesTab(vm: AppViewModel) {
    val g = LocalGame.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { GButton("+ Новый маршрут", { vm.startEditor() }, Modifier.fillMaxWidth(), primary = true) }
        item {
            GText(
                "Нажми «Новый маршрут» и тапай по карте — каждая точка станет остановкой. " +
                        "Вне редактора долгое нажатие ставит метку.",
                size = 12.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false
            )
        }
        if (vm.savedRoutes.isEmpty()) {
            item { GText("Сохранённых маршрутов пока нет", size = 14.sp, upper = false, modifier = Modifier.padding(top = 12.dp)) }
        }
        items(vm.savedRoutes, key = { it.id }) { r ->
            GPanel(Modifier.fillMaxWidth()) {
                GText(r.name, size = 17.sp, upper = false)
                val dist = if (r.distance > 0) " · ${Geo.fmtDist(r.distance)}" else ""
                GText("${r.points.size} точек$dist · ${r.profile.title}", size = 12.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GButton("Показать", { vm.showSaved(r) }, Modifier.weight(1f))
                    GButton("Старт", { vm.startSaved(r) }, Modifier.weight(1f), primary = true)
                    GButton("Удалить", { vm.deleteSaved(r) }, Modifier.weight(1f))
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------- Настройки ----------------

@Composable
private fun SettingsTab(vm: AppViewModel) {
    val g = LocalGame.current
    val s = vm.settings
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SectionTitle("Маршруты") }
        item {
            GPanel(Modifier.fillMaxWidth()) {
                GText("Как передвигаюсь", size = 13.sp, upper = false)
                Spacer(Modifier.height(6.dp))
                GSegment(Profile.entries.map { it.title }, s.profile.ordinal) { vm.setProfile(Profile.entries[it]) }
            }
        }
        item { SectionTitle("Радар и навигация") }
        item {
            GPanel(Modifier.fillMaxWidth()) {
                GToggle("Курс вверх", s.headingUp, "карта крутится по ходу движения (и на полном экране, и в радаре)") { v ->
                    vm.updateSettings { it.copy(headingUp = v) }
                }
                GToggle("3D-наклон", s.tilt3d, "вид как с камеры за машиной") { v -> vm.updateSettings { it.copy(tilt3d = v) } }
                GToggle("Экран не гаснет", s.keepScreenOn) { v -> vm.updateSettings { it.copy(keepScreenOn = v) } }
            }
        }
        item { SectionTitle("HUD") }
        item {
            GPanel(Modifier.fillMaxWidth()) {
                GToggle("Часы и скорость", s.showClock) { v -> vm.updateSettings { it.copy(showClock = v) } }
                GToggle("Название улицы", s.showZone, "всплывает при въезде, как районы в San Andreas") { v ->
                    vm.updateSettings { it.copy(showZone = v) }
                }
                GToggle("Полосы под радаром", s.showBars, "скорость и точность GPS вместо здоровья и брони") { v ->
                    vm.updateSettings { it.copy(showBars = v) }
                }
                GToggle("Игровой HUD", s.gameHud, "деньги, звёзды розыска, опыт, спидометр — у каждого стиля свой") { v ->
                    vm.updateSettings { it.copy(gameHud = v) }
                }
                GToggle("Эффекты экрана", s.effects, "сканлайны, глитчи, виньетка, падающий код, день и ночь") { v ->
                    vm.updateSettings { it.copy(effects = v) }
                }
                GToggle("След за игроком", s.trail, "светящийся след, пунктир шагов — зависит от стиля") { v ->
                    vm.updateSettings { it.copy(trail = v) }
                }
                Spacer(Modifier.height(6.dp))
                GButton("Стереть след", { vm.clearTrail() }, Modifier.fillMaxWidth())
            }
        }
        item { SectionTitle("О приложении") }
        item {
            GPanel(Modifier.fillMaxWidth()) {
                GText(
                    "Карта © участники OpenStreetMap, тайлы OpenFreeMap. Поиск — Photon (komoot). " +
                            "Магазины — Overpass API. Маршруты — OSRM (FOSSGIS). Без аккаунтов, отзывов и рекламы. " +
                            "Шрифты — Google Fonts (SIL OFL / Apache). " +
                            "Стили — фанатские, вдохновлены играми и не связаны с их издателями.",
                    size = 12.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------- Статистика ----------------

@Composable
private fun StatsTab(vm: AppViewModel) {
    val g = LocalGame.current
    val st = vm.stats
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            GPanel(Modifier.fillMaxWidth()) {
                TitleText("${g.moneyPrefix}${String.format(Locale.US, "%,d", st.money)}", 30.sp, Color(g.moneyColor))
                Spacer(Modifier.height(6.dp))
                StatRow("Уровень", "${st.level}")
                StatRow("Пройдено всего", Geo.fmtDist(st.distance))
                StatRow("Из них пешком", Geo.fmtDist(st.walk))
                StatRow("Шагов", "${st.steps}")
                StatRow("Маршрутов завершено", "${st.routesDone}")
                StatRow("Встреч и взломов", "${st.encounters}")
                StatRow("Чит-кодов введено", "${st.cheats}")
            }
        }
        item { SectionTitle("Достижения ${st.achievements.size}/${Achievements.all.size}") }
        items(Achievements.all, key = { it.first }) { (id, text) ->
            val got = id in st.achievements
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                GText(if (got) "★" else "☆", size = 18.sp, color = if (got) Color(g.uiAccent) else Color(g.uiDim))
                Spacer(Modifier.width(10.dp))
                GText(
                    if (got || id != "secret") text else "??? — секретное",
                    size = 13.sp, upper = false,
                    color = if (got) Color(g.uiText) else Color(g.uiDim)
                )
            }
        }
        item {
            GText(
                "Деньги начисляются за пройденные метры и доезды по маршруту. Звёзды розыска появляются рядом с полицией. " +
                        "А ещё попробуй ввести в поиск коды из старых игр…",
                size = 11.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val g = LocalGame.current
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        GText(label, size = 13.sp, color = Color(g.uiDim), upper = false, weight = FontWeight.Normal, modifier = Modifier.weight(1f))
        GText(value, size = 13.sp, upper = false)
    }
}
