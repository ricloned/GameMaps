package com.gamemaps.app.nav

/** Текст подсказок навигации по манёврам OSRM. */
object Instructions {

    private fun dir(mod: String) = when (mod) {
        "left" -> "налево"
        "right" -> "направо"
        "slight left" -> "плавно налево"
        "slight right" -> "плавно направо"
        "sharp left" -> "резко налево"
        "sharp right" -> "резко направо"
        "uturn" -> "на разворот"
        "straight" -> "прямо"
        else -> ""
    }

    fun text(type: String, mod: String, name: String, exit: Int, lastLeg: Boolean): String {
        val on = if (name.isNotBlank()) " — $name" else ""
        return when (type) {
            "depart" -> "Начните движение$on"
            "arrive" -> if (lastLeg) "Вы на месте" else "Промежуточная точка"
            "roundabout", "rotary", "roundabout turn" ->
                if (exit > 0) "Кольцо: $exit-й съезд$on" else "Въезд на кольцо$on"
            "exit roundabout", "exit rotary" -> "Съезд с кольца$on"
            "merge" -> "Перестройтесь ${dir(mod)}$on"
            "on ramp" -> "Заезд на трассу ${dir(mod)}$on"
            "off ramp" -> "Съезд ${dir(mod)}$on"
            "fork" -> when {
                mod.contains("left") -> "Держитесь левее$on"
                mod.contains("right") -> "Держитесь правее$on"
                else -> "Прямо$on"
            }
            "new name", "continue" ->
                if (mod == "straight" || mod.isBlank()) "Прямо$on" else "Двигайтесь ${dir(mod)}$on"
            else -> when (mod) {
                "uturn" -> "Развернитесь$on"
                "straight" -> "Прямо$on"
                "" -> "Продолжайте$on"
                else -> "Поверните ${dir(mod)}$on"
            }
        }
    }

    fun arrow(type: String, mod: String): String = when {
        type == "arrive" -> "◉"
        type.startsWith("roundabout") || type.startsWith("rotary") -> "↻"
        mod == "uturn" -> "↶"
        mod == "slight left" -> "↖"
        mod == "slight right" -> "↗"
        mod.contains("left") -> "←"
        mod.contains("right") -> "→"
        else -> "↑"
    }
}
