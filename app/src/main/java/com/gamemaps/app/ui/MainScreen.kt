package com.gamemaps.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gamemaps.app.AppViewModel
import com.gamemaps.app.map.GameFonts
import com.gamemaps.app.model.ViewMode
import com.gamemaps.app.style.Fx
import com.gamemaps.app.style.HudKind
import kotlinx.coroutines.delay
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameMapsApp(vm: AppViewModel) {
    val g = vm.style
    val context = LocalContext.current
    val perms = remember {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        vm.onPermission(res.values.any { it })
    }
    LaunchedEffect(Unit) {
        val ok = perms.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (ok) vm.onPermission(true) else launcher.launch(perms)
    }

    val view = LocalView.current
    val keep = vm.settings.keepScreenOn
    DisposableEffect(keep) {
        view.keepScreenOn = keep
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = vm.canGoBack) { vm.back() }

    val msg = vm.message
    LaunchedEffect(msg) {
        if (msg != null) {
            delay(2800)
            if (vm.message == msg) vm.message = null
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(g.uiAccent))) {
        CompositionLocalProvider(LocalGame provides g) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Color(g.uiBg))) {
                val radar = vm.viewMode == ViewMode.RADAR
                val diameter = minOf(maxWidth * 0.86f, maxHeight * 0.5f)
                val shiftY = -(maxHeight * 0.05f)
                val hudOn = vm.settings.gameHud

                if (radar) RadarBackdrop(g, Modifier.fillMaxSize())

                MapHost(
                    vm,
                    if (radar) Modifier.align(Alignment.Center).offset(y = shiftY).size(diameter).clip(CircleShape)
                    else Modifier.fillMaxSize()
                )

                if (radar) {
                    if (Fx.SWEEP in g.fx && vm.settings.effects) {
                        RadarSweep(Color(g.uiAccent), Modifier.align(Alignment.Center).offset(y = shiftY).size(diameter).clip(CircleShape))
                    }
                    RadarRing(vm.cameraBearing, Modifier.align(Alignment.Center).offset(y = shiftY).size(diameter))
                    val ownBars = hudOn && g.hud in setOf(HudKind.SA, HudKind.PIP, HudKind.BLOCK, HudKind.RACER)
                    if (vm.settings.showBars && !ownBars) {
                        RadarBars(
                            vm,
                            Modifier
                                .align(Alignment.Center)
                                .offset(y = shiftY + diameter / 2 + 24.dp)
                                .width(diameter * 0.78f)
                        )
                    }
                }

                if (vm.settings.effects) MapOverlays(g, vm.forceNight)

                Hud(vm, onRequestPermission = { launcher.launch(perms) })

                BannerOverlay(vm)

                if (vm.searchOpen) SearchOverlay(vm)
                if (vm.menuOpen) PauseMenu(vm)
            }
        }
    }
}

/** Карта MapLibre внутри Compose. TextureView-режим нужен, чтобы карту можно было обрезать кругом. */
@Composable
fun MapHost(vm: AppViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context, MapLibreMapOptions.createFromAttributes(context, null).textureMode(true)).also { mv ->
            mv.onCreate(null)
            mv.getMapAsync { m -> vm.attachMap(m) }
        }
    }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    AndroidView(factory = { mapView }, modifier = modifier)
}

/** Кольцо радара с маркером севера, который вращается вместе с картой. */
@Composable
fun RadarRing(bearing: Float, modifier: Modifier) {
    val g = LocalGame.current
    val ctx = LocalContext.current
    val tf = remember(g.id) { GameFonts.title(ctx, g) }
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val ringW = g.ringWidth.dp.toPx()
        drawCircle(Color(g.ring), radius = r - ringW / 2f, style = Stroke(width = ringW))
        drawCircle(Color(g.uiAccent).copy(alpha = 0.6f), radius = r - ringW, style = Stroke(width = 1.5.dp.toPx()))
        val ang = Math.toRadians(-bearing.toDouble())
        val rr = r - ringW / 2f
        val p = Offset(center.x + rr * sin(ang).toFloat(), center.y - rr * cos(ang).toFloat())
        val nr = 13.dp.toPx()
        drawCircle(Color(g.uiBg), nr, p)
        drawCircle(Color(g.uiAccent), nr, p, style = Stroke(width = 2.dp.toPx()))
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = g.uiText
            textSize = 14.dp.toPx()
            typeface = tf
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("N", p.x, p.y - (paint.descent() + paint.ascent()) / 2f, paint)
    }
}

/** Полоски под радаром как «здоровье/броня»: скорость и точность GPS. */
@Composable
fun RadarBars(vm: AppViewModel, modifier: Modifier) {
    val g = LocalGame.current
    val loc = vm.location
    val speed = (loc?.speed ?: 0f) * 3.6f
    val acc = loc?.accuracy ?: 99f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Bar("${speed.roundToInt()} км/ч", (speed / 120f).coerceIn(0f, 1f), Color(g.barA))
        Bar(if (loc == null) "GPS…" else "GPS ±${acc.roundToInt()} м", if (loc == null) 0f else (1f - (acc - 5f) / 45f).coerceIn(0.05f, 1f), Color(g.barB))
    }
}

@Composable
private fun Bar(label: String, fraction: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .background(Color.Black.copy(alpha = 0.6f))
                .border(1.dp, Color.Black)
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(color))
        }
        Spacer(Modifier.width(8.dp))
        GText(label, size = 11.sp, shadow = true, modifier = Modifier.width(92.dp), maxLines = 1)
    }
}
