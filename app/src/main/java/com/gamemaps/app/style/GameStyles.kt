package com.gamemaps.app.style

enum class UiFont { SANS, SERIF, MONO, CONDENSED }
enum class BlipShape { CIRCLE, SQUARE, DIAMOND, HEX }

/**
 * Полное описание «игрового» стиля: цвета карты, маркеров и интерфейса.
 * Все цвета — ARGB Int.
 */
data class GameStyle(
    val id: String,
    val name: String,
    val tag: String,
    val description: String,
    // карта
    val land: Int,
    val landUrban: Int,
    val park: Int,
    val wood: Int,
    val water: Int,
    val waterLine: Int,
    val building: Int,
    val buildingLine: Int,
    val roadMinor: Int,
    val roadMajor: Int,
    val roadHighway: Int,
    val roadCasing: Int?,
    val roadScale: Float,
    val rail: Int,
    val path: Int,
    val label: Int,
    val labelHalo: Int,
    val labelFont: String,
    val labelUpper: Boolean,
    // маркеры
    val player: Int,
    val playerLine: Int,
    val waypoint: Int,
    val route: Int,
    val routeCasing: Int,
    val blipShape: BlipShape,
    val blipColored: Boolean,
    val blipBg: Int,
    val blipLine: Int,
    // интерфейс
    val uiBg: Int,
    val uiPanel: Int,
    val uiText: Int,
    val uiDim: Int,
    val uiAccent: Int,
    val uiOnAccent: Int,
    val uiFont: UiFont,
    val uiItalic: Boolean,
    val uiCorner: Int,
    val uiSpacing: Float,
    val ring: Int,
    val ringWidth: Float,
    val barA: Int,
    val barB: Int
)

private fun c(v: Long): Int = v.toInt()

object GameStyles {

    val SA = GameStyle(
        id = "sa",
        name = "SAN ANDREAS",
        tag = "Ретро 2004 · в духе GTA San Andreas",
        description = "Оливковая земля, светлые дороги, квадратные блипы и толстое чёрное кольцо радара.",
        land = c(0xFF7C8069), landUrban = c(0xFF8A8B7A), park = c(0xFF5E7A45), wood = c(0xFF4E6A3A),
        water = c(0xFF6A8CAF), waterLine = c(0xFF6A8CAF),
        building = c(0xFF707362), buildingLine = c(0xFF5C5E50),
        roadMinor = c(0xFFB9B8AC), roadMajor = c(0xFFD0CFC2), roadHighway = c(0xFFE6E2CF),
        roadCasing = null, roadScale = 1.0f,
        rail = c(0xFF4D4D45), path = c(0xFF9C9C8D),
        label = c(0xFFFFFFFF), labelHalo = c(0xFF000000), labelFont = "Noto Sans Bold", labelUpper = true,
        player = c(0xFFFFFFFF), playerLine = c(0xFF000000),
        waypoint = c(0xFFD4246E), route = c(0xFFD94FD9), routeCasing = c(0xFF3B0F3B),
        blipShape = BlipShape.SQUARE, blipColored = true, blipBg = c(0xFF000000), blipLine = c(0xFF000000),
        uiBg = c(0xFF000000), uiPanel = c(0xD9000000), uiText = c(0xFFE8E8E8), uiDim = c(0xFF8C8C8C),
        uiAccent = c(0xFFE1B84C), uiOnAccent = c(0xFF000000),
        uiFont = UiFont.CONDENSED, uiItalic = true, uiCorner = 0, uiSpacing = 0.06f,
        ring = c(0xFF000000), ringWidth = 10f, barA = c(0xFFB4191D), barB = c(0xFFE1E1E1)
    )

    val V = GameStyle(
        id = "v",
        name = "MODERN HD",
        tag = "Тёмный мегаполис · в духе GTA V",
        description = "Графитовые кварталы, приглушённая вода, фиолетовый маршрут и круглые цветные блипы.",
        land = c(0xFF2D3035), landUrban = c(0xFF34373D), park = c(0xFF34503A), wood = c(0xFF2F4834),
        water = c(0xFF395B7A), waterLine = c(0xFF395B7A),
        building = c(0xFF3C4047), buildingLine = c(0xFF4A4E56),
        roadMinor = c(0xFF5F646C), roadMajor = c(0xFF7E848D), roadHighway = c(0xFFA9AEB5),
        roadCasing = null, roadScale = 1.1f,
        rail = c(0xFF4B4F56), path = c(0xFF4F545B),
        label = c(0xFFF2F2F2), labelHalo = c(0xFF15171A), labelFont = "Noto Sans Bold", labelUpper = false,
        player = c(0xFFFFFFFF), playerLine = c(0xFF1A1A1A),
        waypoint = c(0xFFB46AE0), route = c(0xFFA35BD2), routeCasing = c(0xFF3A1F4F),
        blipShape = BlipShape.CIRCLE, blipColored = true, blipBg = c(0xFF1A1A1A), blipLine = c(0xFF111111),
        uiBg = c(0xFF0D0E10), uiPanel = c(0xCC0D0E10), uiText = c(0xFFFFFFFF), uiDim = c(0xFF9AA0A6),
        uiAccent = c(0xFF3E8DD8), uiOnAccent = c(0xFFFFFFFF),
        uiFont = UiFont.SANS, uiItalic = false, uiCorner = 0, uiSpacing = 0.04f,
        ring = c(0xCC000000), ringWidth = 6f, barA = c(0xFF56A85A), barB = c(0xFF4C9BD6)
    )

    val VICE = GameStyle(
        id = "vice",
        name = "NEON VICE",
        tag = "Неон 80-х · в духе GTA Vice City",
        description = "Фиолетовая ночь, розовые проспекты, бирюзовые трассы и золотой маршрут.",
        land = c(0xFF1B1030), landUrban = c(0xFF22143B), park = c(0xFF15302A), wood = c(0xFF122822),
        water = c(0xFF0B3D5C), waterLine = c(0xFF1BA8D0),
        building = c(0xFF2C1A4A), buildingLine = c(0xFFFF4FA3),
        roadMinor = c(0xFF6A2C7A), roadMajor = c(0xFFFF4FA3), roadHighway = c(0xFF39E8FF),
        roadCasing = c(0xFF3A0F4E), roadScale = 1.0f,
        rail = c(0xFF7A5AA0), path = c(0xFF4A2A60),
        label = c(0xFFFFE3F5), labelHalo = c(0xFF5A0A6E), labelFont = "Noto Sans Italic", labelUpper = true,
        player = c(0xFF39E8FF), playerLine = c(0xFF120020),
        waypoint = c(0xFFFF4FA3), route = c(0xFFFFE14F), routeCasing = c(0xFF5A3A00),
        blipShape = BlipShape.CIRCLE, blipColored = false, blipBg = c(0xFF1B1030), blipLine = c(0xFFFF4FA3),
        uiBg = c(0xFF12081F), uiPanel = c(0xD912081F), uiText = c(0xFFFFE3F5), uiDim = c(0xFFB07CC0),
        uiAccent = c(0xFFFF4FA3), uiOnAccent = c(0xFF12081F),
        uiFont = UiFont.SANS, uiItalic = true, uiCorner = 6, uiSpacing = 0.08f,
        ring = c(0xFF39E8FF), ringWidth = 5f, barA = c(0xFFFF4FA3), barB = c(0xFF39E8FF)
    )

    val WEST = GameStyle(
        id = "west",
        name = "FRONTIER",
        tag = "Старая карта · в духе Red Dead Redemption",
        description = "Пергамент, коричневые тропы и красные чернила. Как карта шерифа.",
        land = c(0xFFD9C9A3), landUrban = c(0xFFCFBC92), park = c(0xFFB7B784), wood = c(0xFF9FA36F),
        water = c(0xFF8DA8A6), waterLine = c(0xFF6F8E8C),
        building = c(0xFFBBA57A), buildingLine = c(0xFF8C744C),
        roadMinor = c(0xFF8A6A45), roadMajor = c(0xFF6B4A2A), roadHighway = c(0xFF4E3219),
        roadCasing = null, roadScale = 0.8f,
        rail = c(0xFF3E2A18), path = c(0xFFA88B62),
        label = c(0xFF3A2614), labelHalo = c(0xFFE9DDBF), labelFont = "Noto Sans Italic", labelUpper = false,
        player = c(0xFF8B1E1E), playerLine = c(0xFFF0E4C6),
        waypoint = c(0xFF8B1E1E), route = c(0xFFA3271F), routeCasing = c(0xFFF0E4C6),
        blipShape = BlipShape.DIAMOND, blipColored = false, blipBg = c(0xFFEFE3C2), blipLine = c(0xFF3A2614),
        uiBg = c(0xFFE6D8B4), uiPanel = c(0xEEE9DDBF), uiText = c(0xFF2E1E10), uiDim = c(0xFF7A6248),
        uiAccent = c(0xFF8B1E1E), uiOnAccent = c(0xFFF3E9D2),
        uiFont = UiFont.SERIF, uiItalic = false, uiCorner = 2, uiSpacing = 0.03f,
        ring = c(0xFF3A2614), ringWidth = 7f, barA = c(0xFF8B1E1E), barB = c(0xFF3A2614)
    )

    val CYBER = GameStyle(
        id = "cyber",
        name = "CYBER GRID",
        tag = "Хакерский интерфейс · в духе Watch Dogs",
        description = "Чёрный город, светящиеся бирюзовые линии, жёлтый маршрут и шестиугольные блипы.",
        land = c(0xFF05080D), landUrban = c(0xFF080D14), park = c(0xFF06140F), wood = c(0xFF05110C),
        water = c(0xFF071A26), waterLine = c(0xFF0F4D66),
        building = c(0xFF0B141E), buildingLine = c(0xFF1FD1FF),
        roadMinor = c(0xFF0F3D4D), roadMajor = c(0xFF1A8FB3), roadHighway = c(0xFF7FF9FF),
        roadCasing = null, roadScale = 0.9f,
        rail = c(0xFF124050), path = c(0xFF0B2A36),
        label = c(0xFFB8F4FF), labelHalo = c(0xFF000000), labelFont = "Noto Sans Regular", labelUpper = true,
        player = c(0xFFFFFFFF), playerLine = c(0xFF1FD1FF),
        waypoint = c(0xFFFFC21F), route = c(0xFFFFC21F), routeCasing = c(0xFF2A1C00),
        blipShape = BlipShape.HEX, blipColored = false, blipBg = c(0xFF05080D), blipLine = c(0xFF1FD1FF),
        uiBg = c(0xFF020406), uiPanel = c(0xE6020406), uiText = c(0xFFB8F4FF), uiDim = c(0xFF3F7F90),
        uiAccent = c(0xFF1FD1FF), uiOnAccent = c(0xFF000000),
        uiFont = UiFont.MONO, uiItalic = false, uiCorner = 0, uiSpacing = 0.1f,
        ring = c(0xFF1FD1FF), ringWidth = 3f, barA = c(0xFF1FD1FF), barB = c(0xFFFFC21F)
    )

    val all = listOf(SA, V, VICE, WEST, CYBER)

    fun byId(id: String): GameStyle = all.firstOrNull { it.id == id } ?: V
}
