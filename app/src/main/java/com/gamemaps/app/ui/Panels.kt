package com.gamemaps.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gamemaps.app.AppViewModel
import com.gamemaps.app.model.GeoPoint
import com.gamemaps.app.model.Place
import com.gamemaps.app.model.PoiCategory
import com.gamemaps.app.model.Profile
import com.gamemaps.app.nav.Geo

// ===================== Поиск =====================

@Composable
fun SearchOverlay(vm: AppViewModel) {
    val g = LocalGame.current
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(g.uiBg).copy(alpha = 0.96f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HudButton(Glyph.BACK, { vm.closeSearch() }, size = 48.dp)
                Spacer(Modifier.width(8.dp))
                val shape = RoundedCornerShape(g.uiCorner.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(shape)
                        .background(Color(g.uiPanel))
                        .border(1.dp, Color(g.uiAccent), shape)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (vm.query.isEmpty()) {
                        GText("Магазин, адрес, место…", size = 16.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false)
                    }
                    BasicTextField(
                        value = vm.query,
                        onValueChange = { vm.onQuery(it) },
                        singleLine = true,
                        textStyle = gameTextStyle(17.sp, FontWeight.Normal).copy(color = Color(g.uiText)),
                        cursorBrush = SolidColor(Color(g.uiAccent)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    )
                }
                if (vm.query.isNotEmpty() || vm.searchTitle != null) {
                    Spacer(Modifier.width(8.dp))
                    HudButton(Glyph.CLOSE, { vm.clearSearch() }, size = 48.dp)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (vm.searching) GText("Ищу…", size = 12.sp, color = Color(g.uiAccent))
            if (vm.query.isBlank() && vm.searchTitle == null) CategoryGrid(vm) else ResultsList(vm)
        }
    }
}

@Composable
private fun CategoryGrid(vm: AppViewModel) {
    val g = LocalGame.current
    val shape = RoundedCornerShape(g.uiCorner.dp)
    Column {
        GText("Что рядом со мной", size = 13.sp, color = Color(g.uiAccent))
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PoiCategory.entries) { cat ->
                Row(
                    Modifier
                        .clip(shape)
                        .background(Color(g.uiPanel))
                        .border(1.dp, Color(g.uiAccent).copy(alpha = 0.3f), shape)
                        .clickable { vm.searchCategory(cat) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BlipImage(cat, 28.dp)
                    Spacer(Modifier.width(8.dp))
                    GText(cat.title, size = 13.sp, upper = false, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun ResultsList(vm: AppViewModel) {
    val g = LocalGame.current
    val me = vm.myPoint()
    Column {
        vm.searchTitle?.let { t ->
            GText("$t · ближайшие", size = 15.sp, color = Color(g.uiAccent))
            Spacer(Modifier.height(8.dp))
        }
        if (!vm.searching && vm.results.isEmpty()) {
            GText("Ничего не найдено", size = 14.sp, color = Color(g.uiDim), upper = false)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(vm.results) { p -> ResultRow(p, me) { vm.pickResult(p) } }
        }
    }
}

@Composable
private fun ResultRow(p: Place, me: GeoPoint?, onClick: () -> Unit) {
    val g = LocalGame.current
    val shape = RoundedCornerShape(g.uiCorner.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(g.uiPanel))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val cat = p.category
        if (cat != null) BlipImage(cat, 30.dp) else GlyphIcon(Glyph.LOCATE, Color(g.uiAccent), Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            GText(p.name, size = 15.sp, upper = false, maxLines = 1)
            val sub = listOf(p.kind, p.address).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotBlank()) GText(sub, size = 11.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false, maxLines = 2)
        }
        if (me != null) {
            Spacer(Modifier.width(8.dp))
            GText(Geo.fmtDist(Geo.dist(me, p.point)), size = 12.sp, color = Color(g.uiAccent), upper = false)
        }
    }
}

// ===================== Карточка места =====================

@Composable
fun PlaceCard(vm: AppViewModel, p: Place) {
    val g = LocalGame.current
    val me = vm.myPoint()
    GPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val cat = p.category
            if (cat != null) {
                BlipImage(cat, 36.dp)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                GText(p.name, size = 19.sp, upper = false, maxLines = 2)
                val sub = listOfNotNull(p.kind.ifBlank { null }, me?.let { Geo.fmtDist(Geo.dist(it, p.point)) }).joinToString(" · ")
                if (sub.isNotBlank()) GText(sub, size = 12.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false)
            }
            HudButton(Glyph.CLOSE, { vm.clearSelection() }, size = 38.dp)
        }
        if (p.address.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            GText(p.address, size = 13.sp, weight = FontWeight.Normal, upper = false)
        }
        if (p.hours.isNotBlank()) GText("Часы: ${p.hours}", size = 12.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false)
        if (p.phone.isNotBlank()) GText("Тел.: ${p.phone}", size = 12.sp, color = Color(g.uiDim), weight = FontWeight.Normal, upper = false)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GButton("Маршрут", { vm.routeToSelected() }, Modifier.weight(1f), primary = true)
            GButton("+ В свой", { vm.addSelectedToDraft() }, Modifier.weight(1f))
        }
    }
}

// ===================== Редактор своего маршрута =====================

@Composable
fun EditorPanel(vm: AppViewModel) {
    val g = LocalGame.current
    var showSave by remember { mutableStateOf(false) }
    GPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GText("Свой маршрут", size = 16.sp, color = Color(g.uiAccent), modifier = Modifier.weight(1f))
            GText("${vm.draft.size} точ.", size = 13.sp, color = Color(g.uiDim))
        }
        val r = vm.draftResult
        val status = when {
            vm.draft.isEmpty() -> "Тапай по карте, чтобы ставить точки"
            vm.draft.size == 1 -> "Добавь ещё точку"
            r != null -> "${Geo.fmtDist(r.distance)} · ${Geo.fmtDur(r.duration)}"
            else -> "Считаю маршрут…"
        }
        GText(status, size = 14.sp, weight = FontWeight.Normal, upper = false)
        Spacer(Modifier.height(8.dp))
        GSegment(Profile.entries.map { it.title }, vm.settings.profile.ordinal) { vm.setProfile(Profile.entries[it]) }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GButton("− Точка", { vm.undoDraft() }, Modifier.weight(1f), enabled = vm.draft.isNotEmpty())
            GButton("Очистить", { vm.clearDraft() }, Modifier.weight(1f), enabled = vm.draft.isNotEmpty())
            GButton("Сохр.", { showSave = true }, Modifier.weight(1f), enabled = vm.draft.size >= 2)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GButton("Закрыть", { vm.closeEditor() }, Modifier.weight(1f))
            GButton("Поехали", { vm.startDraft() }, Modifier.weight(1f), primary = true, enabled = vm.draft.isNotEmpty())
        }
    }
    if (showSave) {
        SaveDialog(onDismiss = { showSave = false }) { name ->
            vm.saveDraft(name)
            showSave = false
        }
    }
}

@Composable
private fun SaveDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val g = LocalGame.current
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(g.uiCorner.dp))
                .background(Color(g.uiBg))
                .border(2.dp, Color(g.uiAccent), RoundedCornerShape(g.uiCorner.dp))
                .padding(16.dp)
        ) {
            GText("Название маршрута", size = 16.sp, color = Color(g.uiAccent))
            Spacer(Modifier.height(10.dp))
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = gameTextStyle(17.sp, FontWeight.Normal).copy(color = Color(g.uiText)),
                cursorBrush = SolidColor(Color(g.uiAccent)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(g.uiAccent).copy(alpha = 0.6f))
                    .padding(12.dp)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GButton("Отмена", onDismiss, Modifier.weight(1f))
                GButton("Сохранить", { onSave(name) }, Modifier.weight(1f), primary = true)
            }
        }
    }
}
