package com.gamemaps.app.style

import com.gamemaps.app.R

enum class UiFont { SANS, SERIF, MONO, CONDENSED }
enum class BlipShape { CIRCLE, SQUARE, DIAMOND, HEX, PIXEL }
enum class PlayerShape { ARROW, TRIANGLE, CHEVRON, PIXEL }
enum class DestShape { PIN, X, FLAG, DIAMOND }

/** Набор текстур карты (см. map/Textures.kt). */
enum class TexPack { NONE, BLOCKS, GRAIN, POCKET, PAPER, BLUEPRINT, PIP, CYBER, CHROME, SYNTH, TACTICAL, MATRIX, VICE, CARBON, VAPOR }

enum class StyleGroup(val title: String) {
    GTA("GTA"), GAMES("Игры"), THEMES("Тематические"), SECRET("Секретные")
}

/** Визуальные эффекты поверх карты. */
enum class Fx { SCANLINES, FLICKER, VIGNETTE, PAPER, GRID_PAPER, GLITCH, SWEEP, CODE_RAIN, SYNTH, DAY_NIGHT, LCD, TINT }

/** Какой игровой HUD показывать. */
enum class HudKind { SA, V, VICE, WEST, HACKER, CRED, PIP, BLOCK, POCKET, RACER, TACTICAL, MATRIX, TREASURE, BLUEPRINT, SYNTH }

/**
 * Полное описание стиля: цвета карты, маркеров, интерфейса, шрифты, HUD и пасхалки.
 * Все цвета — ARGB Int.
 */
data class GameStyle(
    val id: String,
    val name: String,
    val tag: String,
    val description: String,
    val group: StyleGroup,
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
    val landmark: Int,
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
    val barB: Int,
    // игровые штуки
    val uiFontRes: Int? = null,
    val titleFontRes: Int? = null,
    val hud: HudKind = HudKind.V,
    val fx: Set<Fx> = emptySet(),
    val zoneColor: Int = 0xFFFFFFFF.toInt(),
    val moneyColor: Int = 0xFF6EC86E.toInt(),
    val moneyPrefix: String = "$",
    val passTitle: String = "MISSION PASSED",
    val passSub: String = "RESPECT +",
    val passColor: Int = 0xFFF0C850.toInt(),
    val wastedTitle: String = "WASTED",
    val wastedColor: Int = 0xFFC0392B.toInt(),
    val trailColor: Int? = null,
    val trailDashed: Boolean = false,
    val destShape: DestShape = DestShape.PIN,
    val playerShape: PlayerShape = PlayerShape.ARROW,
    val radarTilt: Boolean = true,
    val hidden: Boolean = false,
    val texture: TexPack = TexPack.NONE,
    val blockBuildings: Boolean = false
)

private fun c(v: Long): Int = v.toInt()

object GameStyles {

    // ======================= GTA =======================

    val SA = GameStyle(
        id = "sa", name = "SAN ANDREAS", group = StyleGroup.GTA,
        tag = "Ретро 2004 · в духе GTA San Andreas",
        description = "Плоский радар с толстым чёрным кольцом, квадратные блипы, зелёные деньги, звёзды розыска и «Mission passed!».",
        land = c(0xFF6B7356), landUrban = c(0xFF77796A), park = c(0xFF4F6B3A), wood = c(0xFF3F5A30),
        water = c(0xFF4C6E96), waterLine = c(0xFF4C6E96),
        building = c(0xFF5E6252), buildingLine = c(0xFF4C4F42),
        roadMinor = c(0xFF9A9A90), roadMajor = c(0xFFB5B3A6), roadHighway = c(0xFFCFCBB8),
        roadCasing = c(0xFF3C3C34), roadScale = 1.0f,
        rail = c(0xFF3A3A33), path = c(0xFF84846F),
        label = c(0xFFFFFFFF), labelHalo = c(0xFF000000), labelFont = "Noto Sans Bold", labelUpper = true,
        landmark = c(0xFFE1B84C),
        player = c(0xFFFFFFFF), playerLine = c(0xFF000000),
        waypoint = c(0xFFD23C3C), route = c(0xFFE04B4B), routeCasing = c(0xFF4A0F0F),
        blipShape = BlipShape.SQUARE, blipColored = true, blipBg = c(0xFF000000), blipLine = c(0xFF000000),
        uiBg = c(0xFF000000), uiPanel = c(0xE0000000), uiText = c(0xFFD9D9D9), uiDim = c(0xFF8C8C8C),
        uiAccent = c(0xFFE1B84C), uiOnAccent = c(0xFF000000),
        uiFont = UiFont.CONDENSED, uiItalic = false, uiCorner = 0, uiSpacing = 0.03f,
        ring = c(0xFF000000), ringWidth = 12f, barA = c(0xFFB4191D), barB = c(0xFFE1E1E1),
        uiFontRes = R.font.oswald, titleFontRes = R.font.bangers, hud = HudKind.SA, texture = TexPack.GRAIN,
        zoneColor = c(0xFF9FB8E0), moneyColor = c(0xFF36682C),
        passTitle = "MISSION PASSED!", passSub = "RESPECT +", passColor = c(0xFFE1A33A),
        wastedColor = c(0xFFB4191D), playerShape = PlayerShape.TRIANGLE, radarTilt = false
    )

    val V = GameStyle(
        id = "v", name = "LOS SANTOS HD", group = StyleGroup.GTA,
        tag = "Тёмный мегаполис · в духе GTA V",
        description = "Графитовые кварталы, приглушённая вода, фиолетовый маршрут, круглые блипы и 3D-радар.",
        land = c(0xFF2D3035), landUrban = c(0xFF34373D), park = c(0xFF34503A), wood = c(0xFF2F4834),
        water = c(0xFF395B7A), waterLine = c(0xFF395B7A),
        building = c(0xFF3C4047), buildingLine = c(0xFF4A4E56),
        roadMinor = c(0xFF5F646C), roadMajor = c(0xFF7E848D), roadHighway = c(0xFFA9AEB5),
        roadCasing = null, roadScale = 1.1f,
        rail = c(0xFF4B4F56), path = c(0xFF4F545B),
        label = c(0xFFF2F2F2), labelHalo = c(0xFF15171A), labelFont = "Noto Sans Bold", labelUpper = false,
        landmark = c(0xFFF0C850),
        player = c(0xFFFFFFFF), playerLine = c(0xFF1A1A1A),
        waypoint = c(0xFFB46AE0), route = c(0xFFA35BD2), routeCasing = c(0xFF3A1F4F),
        blipShape = BlipShape.CIRCLE, blipColored = true, blipBg = c(0xFF1A1A1A), blipLine = c(0xFF111111),
        uiBg = c(0xFF0D0E10), uiPanel = c(0xCC0D0E10), uiText = c(0xFFFFFFFF), uiDim = c(0xFF9AA0A6),
        uiAccent = c(0xFF3E8DD8), uiOnAccent = c(0xFFFFFFFF),
        uiFont = UiFont.SANS, uiItalic = false, uiCorner = 0, uiSpacing = 0.04f,
        ring = c(0xCC000000), ringWidth = 6f, barA = c(0xFF56A85A), barB = c(0xFF4C9BD6),
        titleFontRes = R.font.bangers, hud = HudKind.V, texture = TexPack.GRAIN,
        moneyColor = c(0xFF72C472), passTitle = "MISSION PASSED", passSub = "", passColor = c(0xFFF0C850)
    )

    val VICE = GameStyle(
        id = "vice", name = "NEON VICE", group = StyleGroup.GTA,
        tag = "Неон 80-х · в духе GTA Vice City",
        description = "Фиолетовая ночь, розовые проспекты, бирюзовые трассы, золотой маршрут и неоновые сканлайны.",
        land = c(0xFF1B1030), landUrban = c(0xFF22143B), park = c(0xFF15302A), wood = c(0xFF122822),
        water = c(0xFF0B3D5C), waterLine = c(0xFF1BA8D0),
        building = c(0xFF2C1A4A), buildingLine = c(0xFFFF4FA3),
        roadMinor = c(0xFF6A2C7A), roadMajor = c(0xFFFF4FA3), roadHighway = c(0xFF39E8FF),
        roadCasing = c(0xFF3A0F4E), roadScale = 1.0f,
        rail = c(0xFF7A5AA0), path = c(0xFF4A2A60),
        label = c(0xFFFFE3F5), labelHalo = c(0xFF5A0A6E), labelFont = "Noto Sans Italic", labelUpper = true,
        landmark = c(0xFF39E8FF),
        player = c(0xFF39E8FF), playerLine = c(0xFF120020),
        waypoint = c(0xFFFF4FA3), route = c(0xFFFFE14F), routeCasing = c(0xFF5A3A00),
        blipShape = BlipShape.CIRCLE, blipColored = false, blipBg = c(0xFF1B1030), blipLine = c(0xFFFF4FA3),
        uiBg = c(0xFF12081F), uiPanel = c(0xD912081F), uiText = c(0xFFFFE3F5), uiDim = c(0xFFB07CC0),
        uiAccent = c(0xFFFF4FA3), uiOnAccent = c(0xFF12081F),
        uiFont = UiFont.SANS, uiItalic = true, uiCorner = 6, uiSpacing = 0.06f,
        ring = c(0xFF39E8FF), ringWidth = 5f, barA = c(0xFFFF4FA3), barB = c(0xFF39E8FF),
        uiFontRes = R.font.exo2_italic, titleFontRes = R.font.pacifico, hud = HudKind.VICE, texture = TexPack.VICE,
        fx = setOf(Fx.SCANLINES), zoneColor = c(0xFFFF4FA3), moneyColor = c(0xFF39E8FF),
        passTitle = "Mission Passed", passSub = "RESPECT +", passColor = c(0xFFFF4FA3),
        trailColor = c(0x88FF4FA3)
    )

    // ======================= Игры =======================

    val WEST = GameStyle(
        id = "west", name = "FRONTIER", group = StyleGroup.GAMES,
        tag = "Карта шерифа · в духе Red Dead Redemption",
        description = "Пергамент, коричневые тропы, красные чернила. Розыск превращается в награду за твою голову.",
        land = c(0xFFD9C9A3), landUrban = c(0xFFCFBC92), park = c(0xFFB7B784), wood = c(0xFF9FA36F),
        water = c(0xFF8DA8A6), waterLine = c(0xFF6F8E8C),
        building = c(0xFFBBA57A), buildingLine = c(0xFF8C744C),
        roadMinor = c(0xFF8A6A45), roadMajor = c(0xFF6B4A2A), roadHighway = c(0xFF4E3219),
        roadCasing = null, roadScale = 0.8f,
        rail = c(0xFF3E2A18), path = c(0xFFA88B62),
        label = c(0xFF3A2614), labelHalo = c(0xFFE9DDBF), labelFont = "Noto Sans Italic", labelUpper = false,
        landmark = c(0xFF8B1E1E),
        player = c(0xFF8B1E1E), playerLine = c(0xFFF0E4C6),
        waypoint = c(0xFF8B1E1E), route = c(0xFFA3271F), routeCasing = c(0xFFF0E4C6),
        blipShape = BlipShape.DIAMOND, blipColored = false, blipBg = c(0xFFEFE3C2), blipLine = c(0xFF3A2614),
        uiBg = c(0xFFE6D8B4), uiPanel = c(0xEEE9DDBF), uiText = c(0xFF2E1E10), uiDim = c(0xFF7A6248),
        uiAccent = c(0xFF8B1E1E), uiOnAccent = c(0xFFF3E9D2),
        uiFont = UiFont.SERIF, uiItalic = false, uiCorner = 2, uiSpacing = 0.02f,
        ring = c(0xFF3A2614), ringWidth = 8f, barA = c(0xFF8B1E1E), barB = c(0xFF3A2614),
        uiFontRes = R.font.kelly_slab, titleFontRes = R.font.rye, hud = HudKind.WEST, texture = TexPack.PAPER,
        fx = setOf(Fx.PAPER, Fx.VIGNETTE), zoneColor = c(0xFF3A2614), moneyColor = c(0xFF3A2614),
        passTitle = "MISSION COMPLETE", passSub = "HONOR +", passColor = c(0xFF8B1E1E),
        wastedTitle = "DEAD", wastedColor = c(0xFF8B1E1E),
        trailColor = c(0x998B1E1E), trailDashed = true, destShape = DestShape.X,
        playerShape = PlayerShape.TRIANGLE, radarTilt = false
    )

    val CYBER = GameStyle(
        id = "cyber", name = "CYBER GRID", group = StyleGroup.GAMES,
        tag = "Хакерская сеть · в духе Watch Dogs",
        description = "Чёрный город и бирюзовые линии. Сканер радара, глитчи и профайлер: рядом с магазинами находит «уязвимости».",
        land = c(0xFF05080D), landUrban = c(0xFF080D14), park = c(0xFF06140F), wood = c(0xFF05110C),
        water = c(0xFF071A26), waterLine = c(0xFF0F4D66),
        building = c(0xFF0B141E), buildingLine = c(0xFF1FD1FF),
        roadMinor = c(0xFF0F3D4D), roadMajor = c(0xFF1A8FB3), roadHighway = c(0xFF7FF9FF),
        roadCasing = null, roadScale = 0.9f,
        rail = c(0xFF124050), path = c(0xFF0B2A36),
        label = c(0xFFB8F4FF), labelHalo = c(0xFF000000), labelFont = "Noto Sans Regular", labelUpper = true,
        landmark = c(0xFFFFC21F),
        player = c(0xFFFFFFFF), playerLine = c(0xFF1FD1FF),
        waypoint = c(0xFFFFC21F), route = c(0xFFFFC21F), routeCasing = c(0xFF2A1C00),
        blipShape = BlipShape.HEX, blipColored = false, blipBg = c(0xFF05080D), blipLine = c(0xFF1FD1FF),
        uiBg = c(0xFF020406), uiPanel = c(0xE6020406), uiText = c(0xFFB8F4FF), uiDim = c(0xFF3F7F90),
        uiAccent = c(0xFF1FD1FF), uiOnAccent = c(0xFF000000),
        uiFont = UiFont.MONO, uiItalic = false, uiCorner = 0, uiSpacing = 0.06f,
        ring = c(0xFF1FD1FF), ringWidth = 3f, barA = c(0xFF1FD1FF), barB = c(0xFFFFC21F),
        uiFontRes = R.font.jura, titleFontRes = R.font.rubik_glitch, hud = HudKind.HACKER, texture = TexPack.CYBER,
        fx = setOf(Fx.GLITCH, Fx.SWEEP), zoneColor = c(0xFF1FD1FF), moneyColor = c(0xFF1FD1FF),
        passTitle = "HACK COMPLETE", passSub = "ДОСТУП ПОЛУЧЕН", passColor = c(0xFF1FD1FF),
        wastedTitle = "SIGNAL LOST", destShape = DestShape.DIAMOND
    )

    val CHROME = GameStyle(
        id = "chrome", name = "CHROME 2077", group = StyleGroup.GAMES,
        tag = "Киберпанк-мегаполис · в духе Cyberpunk 2077",
        description = "Красный неон и жёлтые трассы. За километры растёт Street Cred, деньги — в эдди, а экран иногда глючит.",
        land = c(0xFF0A0A12), landUrban = c(0xFF10101C), park = c(0xFF0B1A14), wood = c(0xFF0A1712),
        water = c(0xFF04232E), waterLine = c(0xFF00F0FF),
        building = c(0xFF1A0F14), buildingLine = c(0xFFFF003C),
        roadMinor = c(0xFF3A1420), roadMajor = c(0xFFFF003C), roadHighway = c(0xFFFCEE0A),
        roadCasing = null, roadScale = 1.0f,
        rail = c(0xFF5A2030), path = c(0xFF2A1018),
        label = c(0xFFFCEE0A), labelHalo = c(0xFF000000), labelFont = "Noto Sans Bold", labelUpper = true,
        landmark = c(0xFF00F0FF),
        player = c(0xFF00F0FF), playerLine = c(0xFF000000),
        waypoint = c(0xFFFCEE0A), route = c(0xFFFCEE0A), routeCasing = c(0xFF3A3400),
        blipShape = BlipShape.HEX, blipColored = false, blipBg = c(0xFF0A0A12), blipLine = c(0xFFFF003C),
        uiBg = c(0xFF050508), uiPanel = c(0xE6080810), uiText = c(0xFF5EF6FF), uiDim = c(0xFF9A4050),
        uiAccent = c(0xFFFCEE0A), uiOnAccent = c(0xFF000000),
        uiFont = UiFont.SANS, uiItalic = false, uiCorner = 0, uiSpacing = 0.05f,
        ring = c(0xFFFCEE0A), ringWidth = 3f, barA = c(0xFFFF003C), barB = c(0xFF00F0FF),
        uiFontRes = R.font.exo2, titleFontRes = R.font.tektur, hud = HudKind.CRED, texture = TexPack.CHROME,
        fx = setOf(Fx.GLITCH, Fx.SCANLINES), zoneColor = c(0xFFFCEE0A), moneyColor = c(0xFFFCEE0A), moneyPrefix = "€$ ",
        passTitle = "GIG COMPLETE", passSub = "STREET CRED +", passColor = c(0xFFFCEE0A),
        wastedTitle = "FLATLINED", wastedColor = c(0xFFFF003C), destShape = DestShape.DIAMOND
    )

    val WASTELAND = GameStyle(
        id = "pip", name = "WASTELAND", group = StyleGroup.GAMES,
        tag = "Зелёный терминал · в духе Fallout (Pip-Boy)",
        description = "Монохромный ЭЛТ-экран с мерцанием. Шаги, уровень, HP — заряд батареи, AP — GPS, и счётчик радиации.",
        land = c(0xFF0B1A0B), landUrban = c(0xFF0F230F), park = c(0xFF12301A), wood = c(0xFF0F2A15),
        water = c(0xFF062006), waterLine = c(0xFF19C25A),
        building = c(0xFF133013), buildingLine = c(0xFF2BFF6E),
        roadMinor = c(0xFF1C6B34), roadMajor = c(0xFF2BC25A), roadHighway = c(0xFF66FF99),
        roadCasing = null, roadScale = 0.9f,
        rail = c(0xFF1C5A2C), path = c(0xFF164A26),
        label = c(0xFF66FF99), labelHalo = c(0xFF031003), labelFont = "Noto Sans Regular", labelUpper = true,
        landmark = c(0xFFB6FFCC),
        player = c(0xFFB6FFCC), playerLine = c(0xFF031003),
        waypoint = c(0xFFCCFFDD), route = c(0xFF66FF99), routeCasing = c(0xFF063010),
        blipShape = BlipShape.CIRCLE, blipColored = false, blipBg = c(0xFF031003), blipLine = c(0xFF66FF99),
        uiBg = c(0xFF020A03), uiPanel = c(0xE6031005), uiText = c(0xFF66FF99), uiDim = c(0xFF2B8A4A),
        uiAccent = c(0xFF66FF99), uiOnAccent = c(0xFF021004),
        uiFont = UiFont.MONO, uiItalic = false, uiCorner = 0, uiSpacing = 0.05f,
        ring = c(0xFF66FF99), ringWidth = 3f, barA = c(0xFF66FF99), barB = c(0xFF66FF99),
        uiFontRes = R.font.jura, titleFontRes = R.font.vt323, hud = HudKind.PIP, texture = TexPack.PIP,
        fx = setOf(Fx.SCANLINES, Fx.FLICKER, Fx.TINT), zoneColor = c(0xFF66FF99), moneyColor = c(0xFF66FF99), moneyPrefix = "CAPS ",
        passTitle = "QUEST COMPLETED", passSub = "XP +", passColor = c(0xFF66FF99),
        wastedTitle = "YOU DIED", wastedColor = c(0xFF66FF99), trailColor = c(0x8866FF99)
    )

    val BLOCKS = GameStyle(
        id = "blocks", name = "BLOCKCRAFT", group = StyleGroup.GAMES,
        tag = "Кубический мир · в духе Minecraft",
        description = "Пиксельные значки, сердечки (заряд), полоска опыта, смена дня и ночи по реальному времени и достижения.",
        land = c(0xFF7FB238), landUrban = c(0xFF8DA35A), park = c(0xFF5C9A2A), wood = c(0xFF2F6E1A),
        water = c(0xFF3F55E0), waterLine = c(0xFF3F55E0),
        building = c(0xFF8F7748), buildingLine = c(0xFF5E4A2A),
        roadMinor = c(0xFF8F8F8F), roadMajor = c(0xFF707070), roadHighway = c(0xFFB0B0B0),
        roadCasing = c(0xFF4A4A4A), roadScale = 1.0f,
        rail = c(0xFF6B5A3A), path = c(0xFFD8C690),
        label = c(0xFFFFFFFF), labelHalo = c(0xFF3F3F3F), labelFont = "Noto Sans Bold", labelUpper = false,
        landmark = c(0xFFFFFF55),
        player = c(0xFFFFFFFF), playerLine = c(0xFF2B2B2B),
        waypoint = c(0xFFFF3B3B), route = c(0xFFFFFF55), routeCasing = c(0xFF3F3F00),
        blipShape = BlipShape.PIXEL, blipColored = true, blipBg = c(0xFF2B2B2B), blipLine = c(0xFF1E1E1E),
        uiBg = c(0xFF2B2B2B), uiPanel = c(0xEE3C3C3C), uiText = c(0xFFFFFFFF), uiDim = c(0xFFA0A0A0),
        uiAccent = c(0xFF55FF55), uiOnAccent = c(0xFF1E1E1E),
        uiFont = UiFont.MONO, uiItalic = false, uiCorner = 0, uiSpacing = 0.02f,
        ring = c(0xFF3C3C3C), ringWidth = 10f, barA = c(0xFFFF3B3B), barB = c(0xFF80FF20),
        uiFontRes = R.font.pixelify, titleFontRes = R.font.press_start, hud = HudKind.BLOCK, texture = TexPack.BLOCKS, blockBuildings = true,
        fx = setOf(Fx.DAY_NIGHT), zoneColor = c(0xFFFFFF55), moneyColor = c(0xFF55FF55), moneyPrefix = "◆ ",
        passTitle = "ADVANCEMENT MADE!", passSub = "XP +", passColor = c(0xFFFFFF55),
        wastedTitle = "YOU DIED!", wastedColor = c(0xFFFF3B3B),
        playerShape = PlayerShape.PIXEL, radarTilt = true
    )

    val POCKET = GameStyle(
        id = "pocket", name = "POCKET 1989", group = StyleGroup.GAMES,
        tag = "Карманная консоль · в духе Pokémon",
        description = "Четыре оттенка зелёного и пиксельный шрифт. Проходишь мимо магазина — «появляется дикий магазин»!",
        land = c(0xFF9BBC0F), landUrban = c(0xFF9BBC0F), park = c(0xFF8BAC0F), wood = c(0xFF8BAC0F),
        water = c(0xFF306230), waterLine = c(0xFF306230),
        building = c(0xFF8BAC0F), buildingLine = c(0xFF306230),
        roadMinor = c(0xFF306230), roadMajor = c(0xFF0F380F), roadHighway = c(0xFF0F380F),
        roadCasing = null, roadScale = 0.8f,
        rail = c(0xFF306230), path = c(0xFF306230),
        label = c(0xFF0F380F), labelHalo = c(0xFF9BBC0F), labelFont = "Noto Sans Bold", labelUpper = true,
        landmark = c(0xFF0F380F),
        player = c(0xFF0F380F), playerLine = c(0xFF9BBC0F),
        waypoint = c(0xFF0F380F), route = c(0xFF0F380F), routeCasing = c(0xFF9BBC0F),
        blipShape = BlipShape.PIXEL, blipColored = false, blipBg = c(0xFF9BBC0F), blipLine = c(0xFF0F380F),
        uiBg = c(0xFF9BBC0F), uiPanel = c(0xF09BBC0F), uiText = c(0xFF0F380F), uiDim = c(0xFF306230),
        uiAccent = c(0xFF0F380F), uiOnAccent = c(0xFF9BBC0F),
        uiFont = UiFont.MONO, uiItalic = false, uiCorner = 0, uiSpacing = 0.0f,
        ring = c(0xFF0F380F), ringWidth = 6f, barA = c(0xFF0F380F), barB = c(0xFF306230),
        uiFontRes = R.font.press_start, titleFontRes = R.font.press_start, hud = HudKind.POCKET, texture = TexPack.POCKET,
        fx = setOf(Fx.LCD), zoneColor = c(0xFF0F380F), moneyColor = c(0xFF0F380F), moneyPrefix = "₽",
        passTitle = "QUEST CLEAR!", passSub = "", passColor = c(0xFF0F380F),
        wastedTitle = "GAME OVER", wastedColor = c(0xFF0F380F),
        destShape = DestShape.FLAG, playerShape = PlayerShape.PIXEL, radarTilt = false
    )

    val RACER = GameStyle(
        id = "racer", name = "STREET RACER", group = StyleGroup.GAMES,
        tag = "Ночные гонки · в духе Need for Speed: Most Wanted",
        description = "Огромный спидометр, «HEAT» рядом с полицией, секундомер заезда и «RACE WON» на финише.",
        land = c(0xFF1A1F24), landUrban = c(0xFF20262C), park = c(0xFF1E2A22), wood = c(0xFF1B261E),
        water = c(0xFF0E2233), waterLine = c(0xFF1B4566),
        building = c(0xFF262D35), buildingLine = c(0xFF2F3842),
        roadMinor = c(0xFF4A535C), roadMajor = c(0xFF8A949E), roadHighway = c(0xFFE6EAEE),
        roadCasing = null, roadScale = 1.1f,
        rail = c(0xFF333B44), path = c(0xFF2A3138),
        label = c(0xFFE6EAEE), labelHalo = c(0xFF0B0E11), labelFont = "Noto Sans Italic", labelUpper = true,
        landmark = c(0xFFF2C230),
        player = c(0xFFFFFFFF), playerLine = c(0xFF000000),
        waypoint = c(0xFFF2C230), route = c(0xFF9FE15A), routeCasing = c(0xFF20330F),
        blipShape = BlipShape.CIRCLE, blipColored = true, blipBg = c(0xFF0B0E11), blipLine = c(0xFF000000),
        uiBg = c(0xFF0B0E11), uiPanel = c(0xE00B0E11), uiText = c(0xFFEDEDED), uiDim = c(0xFF8A949E),
        uiAccent = c(0xFFF2C230), uiOnAccent = c(0xFF000000),
        uiFont = UiFont.SANS, uiItalic = true, uiCorner = 4, uiSpacing = 0.03f,
        ring = c(0xFFF2C230), ringWidth = 4f, barA = c(0xFFE53935), barB = c(0xFFF2C230),
        uiFontRes = R.font.russo_one, titleFontRes = R.font.russo_one, hud = HudKind.RACER, texture = TexPack.CARBON,
        fx = setOf(Fx.VIGNETTE), zoneColor = c(0xFFF2C230), moneyColor = c(0xFFF2C230),
        passTitle = "RACE WON", passSub = "BOUNTY +", passColor = c(0xFFF2C230),
        wastedTitle = "BUSTED", wastedColor = c(0xFFE53935),
        trailColor = c(0x889FE15A), destShape = DestShape.FLAG
    )

    val TACTICAL = GameStyle(
        id = "tactical", name = "TACTICAL OPS", group = StyleGroup.GAMES,
        tag = "Миникарта спецназа · в духе Call of Duty",
        description = "Хаки, компасная лента сверху, сканер БПЛА на радаре, полиция — красные отметки.",
        land = c(0xFF2B2F27), landUrban = c(0xFF33372E), park = c(0xFF2E3A28), wood = c(0xFF28331F),
        water = c(0xFF1E2D33), waterLine = c(0xFF2F4A55),
        building = c(0xFF3F4436), buildingLine = c(0xFF565C4A),
        roadMinor = c(0xFF5A5F50), roadMajor = c(0xFF7C8270), roadHighway = c(0xFFA3A890),
        roadCasing = null, roadScale = 1.0f,
        rail = c(0xFF44483B), path = c(0xFF4A4E41),
        label = c(0xFFD8DCC8), labelHalo = c(0xFF15170F), labelFont = "Noto Sans Bold", labelUpper = true,
        landmark = c(0xFFF2E14C),
        player = c(0xFFF2E14C), playerLine = c(0xFF000000),
        waypoint = c(0xFFF2E14C), route = c(0xFFF2E14C), routeCasing = c(0xFF3A3610),
        blipShape = BlipShape.SQUARE, blipColored = false, blipBg = c(0xFF15170F), blipLine = c(0xFFD8DCC8),
        uiBg = c(0xFF0D0F0A), uiPanel = c(0xE00D0F0A), uiText = c(0xFFD8DCC8), uiDim = c(0xFF7C8270),
        uiAccent = c(0xFFF2E14C), uiOnAccent = c(0xFF000000),
        uiFont = UiFont.CONDENSED, uiItalic = false, uiCorner = 0, uiSpacing = 0.08f,
        ring = c(0xFF15170F), ringWidth = 6f, barA = c(0xFFE53935), barB = c(0xFFF2E14C),
        uiFontRes = R.font.russo_one, titleFontRes = R.font.black_ops, hud = HudKind.TACTICAL, texture = TexPack.TACTICAL,
        fx = setOf(Fx.SWEEP, Fx.VIGNETTE), zoneColor = c(0xFFF2E14C), moneyColor = c(0xFFF2E14C),
        passTitle = "MISSION COMPLETE", passSub = "+XP", passColor = c(0xFFF2E14C),
        wastedTitle = "KIA", wastedColor = c(0xFFE53935),
        destShape = DestShape.DIAMOND, playerShape = PlayerShape.CHEVRON, radarTilt = false
    )

    // ======================= Тематические =======================

    val SYNTH = GameStyle(
        id = "synth", name = "GRID RUNNER", group = StyleGroup.THEMES,
        tag = "Синтвейв 80-х · в духе Tron",
        description = "Неоновая сетка до горизонта, закатное солнце вокруг радара и светящийся след «светоцикла» за тобой.",
        land = c(0xFF0D0221), landUrban = c(0xFF140433), park = c(0xFF0F0628), wood = c(0xFF0F0628),
        water = c(0xFF050015), waterLine = c(0xFF2DE2E6),
        building = c(0xFF1A0640), buildingLine = c(0xFFFF2E97),
        roadMinor = c(0xFF4A1F7A), roadMajor = c(0xFFFF2E97), roadHighway = c(0xFF2DE2E6),
        roadCasing = null, roadScale = 1.0f,
        rail = c(0xFF3A1760), path = c(0xFF2A1050),
        label = c(0xFFF9C80E), labelHalo = c(0xFF0D0221), labelFont = "Noto Sans Bold", labelUpper = true,
        landmark = c(0xFF2DE2E6),
        player = c(0xFF2DE2E6), playerLine = c(0xFF0D0221),
        waypoint = c(0xFFF9C80E), route = c(0xFFF9C80E), routeCasing = c(0xFF3A2A00),
        blipShape = BlipShape.CIRCLE, blipColored = false, blipBg = c(0xFF0D0221), blipLine = c(0xFFFF2E97),
        uiBg = c(0xFF0D0221), uiPanel = c(0xE00D0221), uiText = c(0xFFF6E8FF), uiDim = c(0xFF8C5AB5),
        uiAccent = c(0xFFFF2E97), uiOnAccent = c(0xFF0D0221),
        uiFont = UiFont.SANS, uiItalic = true, uiCorner = 0, uiSpacing = 0.08f,
        ring = c(0xFFFF2E97), ringWidth = 4f, barA = c(0xFFFF2E97), barB = c(0xFF2DE2E6),
        uiFontRes = R.font.exo2_italic, titleFontRes = R.font.orbitron, hud = HudKind.SYNTH, texture = TexPack.SYNTH,
        fx = setOf(Fx.SYNTH, Fx.SCANLINES), zoneColor = c(0xFF2DE2E6), moneyColor = c(0xFFF9C80E), moneyPrefix = "SCORE ",
        passTitle = "LEVEL COMPLETE", passSub = "BONUS +", passColor = c(0xFF2DE2E6),
        wastedTitle = "GAME OVER", wastedColor = c(0xFFFF2E97),
        trailColor = c(0xFF2DE2E6), destShape = DestShape.DIAMOND
    )

    val MATRIX = GameStyle(
        id = "matrix", name = "THE CODE", group = StyleGroup.THEMES,
        tag = "Зелёный код · в духе «Матрицы»",
        description = "Падающие символы вокруг радара, всё зелёное и цифровое. Маршрут — белый кролик.",
        land = c(0xFF000500), landUrban = c(0xFF000A02), park = c(0xFF001205), wood = c(0xFF001005),
        water = c(0xFF000300), waterLine = c(0xFF00661A),
        building = c(0xFF001A06), buildingLine = c(0xFF00FF41),
        roadMinor = c(0xFF004D14), roadMajor = c(0xFF00B32D), roadHighway = c(0xFF00FF41),
        roadCasing = null, roadScale = 0.9f,
        rail = c(0xFF003310), path = c(0xFF00330D),
        label = c(0xFF00FF41), labelHalo = c(0xFF000000), labelFont = "Noto Sans Regular", labelUpper = false,
        landmark = c(0xFFCCFFD6),
        player = c(0xFFCCFFD6), playerLine = c(0xFF00FF41),
        waypoint = c(0xFFFFFFFF), route = c(0xFFE8FFE8), routeCasing = c(0xFF003300),
        blipShape = BlipShape.HEX, blipColored = false, blipBg = c(0xFF000000), blipLine = c(0xFF00FF41),
        uiBg = c(0xFF000000), uiPanel = c(0xE6000000), uiText = c(0xFF00FF41), uiDim = c(0xFF00802A),
        uiAccent = c(0xFF00FF41), uiOnAccent = c(0xFF000000),
        uiFont = UiFont.MONO, uiItalic = false, uiCorner = 0, uiSpacing = 0.04f,
        ring = c(0xFF00FF41), ringWidth = 2f, barA = c(0xFF00FF41), barB = c(0xFF00FF41),
        titleFontRes = R.font.rubik_glitch, hud = HudKind.MATRIX, texture = TexPack.MATRIX,
        fx = setOf(Fx.CODE_RAIN, Fx.GLITCH), zoneColor = c(0xFF00FF41), moneyColor = c(0xFF00FF41),
        passTitle = "SYSTEM OVERRIDE", passSub = "", passColor = c(0xFF00FF41),
        wastedTitle = "CONNECTION LOST", wastedColor = c(0xFF00FF41), destShape = DestShape.DIAMOND
    )

    val TREASURE = GameStyle(
        id = "treasure", name = "TREASURE MAP", group = StyleGroup.THEMES,
        tag = "Пиратская карта сокровищ",
        description = "Старая бумага, роза ветров, следы пунктиром за тобой и крестик «X» на месте назначения.",
        land = c(0xFFE8D3A2), landUrban = c(0xFFE0C68F), park = c(0xFFC9C08A), wood = c(0xFFB5AE73),
        water = c(0xFF7FA8A0), waterLine = c(0xFF4F7F78),
        building = c(0xFFD3B984), buildingLine = c(0xFFA4864F),
        roadMinor = c(0xFFA0763F), roadMajor = c(0xFF7A5228), roadHighway = c(0xFF5A3A1A),
        roadCasing = null, roadScale = 0.7f,
        rail = c(0xFF5A3A1A), path = c(0xFFA0763F),
        label = c(0xFF4A2E14), labelHalo = c(0xFFF0E0B8), labelFont = "Noto Sans Italic", labelUpper = false,
        landmark = c(0xFFB22222),
        player = c(0xFF2E1A0A), playerLine = c(0xFFF0E0B8),
        waypoint = c(0xFFB22222), route = c(0xFFB22222), routeCasing = c(0xFFF0E0B8),
        blipShape = BlipShape.DIAMOND, blipColored = false, blipBg = c(0xFFF0E0B8), blipLine = c(0xFF4A2E14),
        uiBg = c(0xFFE8D3A2), uiPanel = c(0xF0F0E0B8), uiText = c(0xFF3A2410), uiDim = c(0xFF7A5A38),
        uiAccent = c(0xFFB22222), uiOnAccent = c(0xFFF0E0B8),
        uiFont = UiFont.SERIF, uiItalic = false, uiCorner = 2, uiSpacing = 0.02f,
        ring = c(0xFF4A2E14), ringWidth = 8f, barA = c(0xFFB22222), barB = c(0xFF4A2E14),
        uiFontRes = R.font.underdog, titleFontRes = R.font.pirata_one, hud = HudKind.TREASURE, texture = TexPack.PAPER,
        fx = setOf(Fx.PAPER, Fx.VIGNETTE), zoneColor = c(0xFF4A2E14), moneyColor = c(0xFF8A6A10), moneyPrefix = "☠ ",
        passTitle = "TREASURE FOUND!", passSub = "ДОБЫЧА +", passColor = c(0xFFB22222),
        wastedTitle = "SHIPWRECKED", wastedColor = c(0xFFB22222),
        trailColor = c(0xFF7A5228), trailDashed = true, destShape = DestShape.X,
        playerShape = PlayerShape.TRIANGLE, radarTilt = false
    )

    val BLUEPRINT = GameStyle(
        id = "blueprint", name = "BLUEPRINT", group = StyleGroup.THEMES,
        tag = "Инженерный чертёж",
        description = "Синяя калька с сеткой, белые линии, рукописные подписи и масштабная линейка.",
        land = c(0xFF0F3B7A), landUrban = c(0xFF12438A), park = c(0xFF154A92), wood = c(0xFF154A92),
        water = c(0xFF0B2E60), waterLine = c(0xFFCFE3FF),
        building = c(0xFF12438A), buildingLine = c(0xFFCFE3FF),
        roadMinor = c(0xFF8FB6E8), roadMajor = c(0xFFCFE3FF), roadHighway = c(0xFFFFFFFF),
        roadCasing = null, roadScale = 0.7f,
        rail = c(0xFF8FB6E8), path = c(0xFF6F9AD6),
        label = c(0xFFFFFFFF), labelHalo = c(0xFF0F3B7A), labelFont = "Noto Sans Italic", labelUpper = true,
        landmark = c(0xFFFFDD55),
        player = c(0xFFFFFFFF), playerLine = c(0xFF0F3B7A),
        waypoint = c(0xFFFFDD55), route = c(0xFFFFDD55), routeCasing = c(0xFF0F3B7A),
        blipShape = BlipShape.CIRCLE, blipColored = false, blipBg = c(0xFF0F3B7A), blipLine = c(0xFFFFFFFF),
        uiBg = c(0xFF0F3B7A), uiPanel = c(0xE60F3B7A), uiText = c(0xFFFFFFFF), uiDim = c(0xFF8FB6E8),
        uiAccent = c(0xFFFFDD55), uiOnAccent = c(0xFF0F3B7A),
        uiFont = UiFont.SANS, uiItalic = false, uiCorner = 0, uiSpacing = 0.02f,
        ring = c(0xFFFFFFFF), ringWidth = 2f, barA = c(0xFFFFDD55), barB = c(0xFFFFFFFF),
        uiFontRes = R.font.caveat, titleFontRes = R.font.caveat, hud = HudKind.BLUEPRINT, texture = TexPack.BLUEPRINT,
        fx = setOf(Fx.GRID_PAPER), zoneColor = c(0xFFFFFFFF), moneyColor = c(0xFFFFDD55),
        passTitle = "ПРОЕКТ СДАН!", passSub = "Премия +", passColor = c(0xFFFFDD55),
        wastedTitle = "ОШИБКА В ЧЕРТЕЖЕ", wastedColor = c(0xFFFFDD55),
        trailColor = c(0xAAFFFFFF), trailDashed = true, destShape = DestShape.X, radarTilt = false
    )

    // ======================= Секрет =======================

    val VAPOR = GameStyle(
        id = "vapor", name = "V A P O R", group = StyleGroup.SECRET,
        tag = "Секретный стиль · вейпорвейв",
        description = "Пастельные небеса, розовые трассы и эстетика 90-х. Открывается секретным кодом.",
        land = c(0xFFF6C6EA), landUrban = c(0xFFEBB4E0), park = c(0xFFA0E7E5), wood = c(0xFF8ED8D6),
        water = c(0xFF7EC8E3), waterLine = c(0xFF4FA3C7),
        building = c(0xFFE3A6D8), buildingLine = c(0xFFB07CC6),
        roadMinor = c(0xFFFFFFFF), roadMajor = c(0xFFFFF59D), roadHighway = c(0xFFFF71CE),
        roadCasing = null, roadScale = 1.0f,
        rail = c(0xFFB07CC6), path = c(0xFFFFFFFF),
        label = c(0xFF6A3D9A), labelHalo = c(0xFFFFFFFF), labelFont = "Noto Sans Italic", labelUpper = true,
        landmark = c(0xFF01CDFE),
        player = c(0xFF01CDFE), playerLine = c(0xFF6A3D9A),
        waypoint = c(0xFFFF71CE), route = c(0xFFB967FF), routeCasing = c(0xFFFFFFFF),
        blipShape = BlipShape.CIRCLE, blipColored = true, blipBg = c(0xFFFFFFFF), blipLine = c(0xFFFFFFFF),
        uiBg = c(0xFFFBE3F5), uiPanel = c(0xF0FFFFFF), uiText = c(0xFF6A3D9A), uiDim = c(0xFFB07CC6),
        uiAccent = c(0xFFFF71CE), uiOnAccent = c(0xFFFFFFFF),
        uiFont = UiFont.SANS, uiItalic = true, uiCorner = 10, uiSpacing = 0.12f,
        ring = c(0xFFFF71CE), ringWidth = 5f, barA = c(0xFFFF71CE), barB = c(0xFF01CDFE),
        uiFontRes = R.font.exo2_italic, titleFontRes = R.font.pacifico, hud = HudKind.SYNTH, texture = TexPack.VAPOR,
        fx = setOf(Fx.SCANLINES), zoneColor = c(0xFFB967FF), moneyColor = c(0xFFB967FF), moneyPrefix = "¥ ",
        passTitle = "Aesthetic", passSub = "VIBES +", passColor = c(0xFFFF71CE),
        trailColor = c(0xFFFF71CE), hidden = true
    )

    val all = listOf(SA, V, VICE, WEST, CYBER, CHROME, WASTELAND, BLOCKS, POCKET, RACER, TACTICAL, SYNTH, MATRIX, TREASURE, BLUEPRINT, VAPOR)

    fun byId(id: String): GameStyle = all.firstOrNull { it.id == id } ?: V
}
