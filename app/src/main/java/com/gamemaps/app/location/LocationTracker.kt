package com.gamemaps.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.Surface
import android.view.WindowManager

/** GPS через системный LocationManager (без Google Play Services) + компас. */
class LocationTracker(
    ctx: Context,
    private val onLocation: (Location) -> Unit,
    private val onHeading: (Float) -> Unit
) : SensorEventListener {

    private val app = ctx.applicationContext
    private val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sm = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var running = false
    private var lastBest: Location? = null
    private var declination = 0f
    private val rot = FloatArray(9)
    private val remap = FloatArray(9)
    private val orient = FloatArray(3)
    private var smoothed = Float.NaN

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = deliver(location)
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    private fun deliver(l: Location) {
        val prev = lastBest
        // Если есть свежий GPS-фикс, сетевой (менее точный) игнорируем
        if (prev != null && l.provider != LocationManager.GPS_PROVIDER &&
            prev.provider == LocationManager.GPS_PROVIDER && l.time - prev.time < 10_000
        ) return
        lastBest = l
        declination = try {
            GeomagneticField(l.latitude.toFloat(), l.longitude.toFloat(), l.altitude.toFloat(), l.time).declination
        } catch (e: Exception) { 0f }
        onLocation(l)
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        running = true
        try {
            val providers = lm.allProviders
            val last = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { providers.contains(it) }
                .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
            if (last != null) deliver(last)
            if (providers.contains(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
            }
            if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 0f, listener, Looper.getMainLooper())
            }
        } catch (e: SecurityException) {
            running = false
            return
        }
        sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        if (!running) return
        running = false
        try { lm.removeUpdates(listener) } catch (e: Exception) { }
        sm.unregisterListener(this)
    }

    override fun onSensorChanged(e: SensorEvent) {
        val v = if (e.values.size > 4) e.values.copyOf(4) else e.values
        SensorManager.getRotationMatrixFromVector(rot, v)
        @Suppress("DEPRECATION")
        val r = wm.defaultDisplay.rotation
        val (ax, ay) = when (r) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(rot, ax, ay, remap)
        SensorManager.getOrientation(remap, orient)
        val deg = ((Math.toDegrees(orient[0].toDouble()).toFloat() + declination) + 360f) % 360f
        smoothed = if (smoothed.isNaN()) deg else {
            var d = deg - smoothed
            if (d > 180f) d -= 360f
            if (d < -180f) d += 360f
            (smoothed + d * 0.15f + 360f) % 360f
        }
        onHeading(smoothed)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
