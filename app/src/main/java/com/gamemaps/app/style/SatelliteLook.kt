package com.gamemaps.app.style

/**
 * Как подкрашивать спутниковые снимки под каждый игровой стиль.
 * saturation/contrast: -1..1, brightness: 0..1, hue: градусы, tint — цвет поверх снимка.
 */
data class SatelliteLook(
    val saturation: Double = 0.0,
    val contrast: Double = 0.0,
    val brightnessMin: Double = 0.0,
    val brightnessMax: Double = 1.0,
    val hue: Double = 0.0,
    val tint: Int? = null,
    val tintOpacity: Double = 0.0
) {
    companion object {
        private fun c(v: Long) = v.toInt()

        fun of(id: String): SatelliteLook = when (id) {
            "sa" -> SatelliteLook(saturation = -0.3, contrast = 0.12, brightnessMax = 0.92, tint = c(0xFF6B7356), tintOpacity = 0.2)
            "v" -> SatelliteLook(saturation = -0.25, contrast = 0.1, brightnessMax = 0.85, tint = c(0xFF2D3035), tintOpacity = 0.15)
            "vice" -> SatelliteLook(saturation = 0.2, contrast = 0.15, brightnessMax = 0.7, tint = c(0xFFFF4FA3), tintOpacity = 0.18)
            "west" -> SatelliteLook(saturation = -0.75, contrast = 0.05, brightnessMin = 0.15, tint = c(0xFFD9C9A3), tintOpacity = 0.38)
            "cyber" -> SatelliteLook(saturation = -1.0, contrast = 0.2, brightnessMax = 0.5, tint = c(0xFF1FD1FF), tintOpacity = 0.14)
            "chrome" -> SatelliteLook(saturation = -0.6, contrast = 0.25, brightnessMax = 0.6, tint = c(0xFFFF003C), tintOpacity = 0.15)
            "pip" -> SatelliteLook(saturation = -1.0, contrast = 0.2, brightnessMax = 0.7, tint = c(0xFF66FF99), tintOpacity = 0.3)
            "blocks" -> SatelliteLook(saturation = 0.45, contrast = 0.2)
            "pocket" -> SatelliteLook(saturation = -1.0, contrast = 0.35, tint = c(0xFF9BBC0F), tintOpacity = 0.55)
            "racer" -> SatelliteLook(saturation = -0.3, contrast = 0.2, brightnessMax = 0.72)
            "tactical" -> SatelliteLook(saturation = -0.6, contrast = 0.1, brightnessMax = 0.85, tint = c(0xFF3F4436), tintOpacity = 0.3)
            "synth" -> SatelliteLook(saturation = -0.8, contrast = 0.2, brightnessMax = 0.55, tint = c(0xFFFF2E97), tintOpacity = 0.25)
            "matrix" -> SatelliteLook(saturation = -1.0, contrast = 0.3, brightnessMax = 0.55, tint = c(0xFF00FF41), tintOpacity = 0.22)
            "treasure" -> SatelliteLook(saturation = -0.8, brightnessMin = 0.12, tint = c(0xFFE8D3A2), tintOpacity = 0.45)
            "blueprint" -> SatelliteLook(saturation = -1.0, contrast = 0.15, brightnessMax = 0.6, tint = c(0xFF0F3B7A), tintOpacity = 0.55)
            "vapor" -> SatelliteLook(saturation = 0.3, brightnessMin = 0.2, tint = c(0xFFFF71CE), tintOpacity = 0.2)
            else -> SatelliteLook()
        }
    }
}
