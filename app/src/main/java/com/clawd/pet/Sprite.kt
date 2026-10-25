package com.clawd.pet

object Sprite {
    const val GRID_W = 18
    const val GRID_H = 13
    val SIZE_UNITS = intArrayOf(5, 7, 9)

    val PALETTES = listOf(
        "Claude" to intArrayOf(0xFFD97757.toInt(), 0xFF1A1A1A.toInt()),
        "Мятный" to intArrayOf(0xFF4FBF98.toInt(), 0xFF12241D.toInt()),
        "Лавандовый" to intArrayOf(0xFF9B8AEE.toInt(), 0xFF1B1631.toInt()),
        "Небесный" to intArrayOf(0xFF4FA8E8.toInt(), 0xFF0E1C29.toInt()),
        "Розовый" to intArrayOf(0xFFEE85AE.toInt(), 0xFF2A1220.toInt()),
        "Графит" to intArrayOf(0xFF5E6670.toInt(), 0xFFF2F2F2.toInt()),
        "Терракот" to intArrayOf(0xFFC96F4A.toInt(), 0xFF2B160D.toInt()),
        "Морская волна" to intArrayOf(0xFF26A69A.toInt(), 0xFF0A2422.toInt()),
        "Магма" to intArrayOf(0xFFFF6D00.toInt(), 0xFF2B1200.toInt()),
    )

    const val HEART_COLOR = 0xFFFF5C7A.toInt()
    const val ZZZ_COLOR = 0xFF8FA3BF.toInt()
    const val SPARK_COLOR = 0xFFFFC94D.toInt()
    const val NOTE_COLOR = 0xFF6FB8FF.toInt()

    private val POOP_COLORS = mapOf(
        'X' to 0xFF8A5A33.toInt(),
        'o' to 0xFFB27D4E.toInt(),
        'w' to 0xFFFFFFFF.toInt(),
        'k' to 0xFF1A1A1A.toInt(),
    )

    private val PARTICLE_COLORS = mapOf(
        "heart" to HEART_COLOR,
        "zzz" to ZZZ_COLOR,
        "spark" to SPARK_COLOR,
        "note" to NOTE_COLOR,
    )

    val PATTERNS: Map<String, Array<String>> = mapOf(
        "heart" to arrayOf(
            ".XX.XX.",
            "XXXXXXX",
            "XXXXXXX",
            ".XXXXX.",
            "..XXX..",
            "...X...",
        ),
        "zzz" to arrayOf(
            "XXXXX",
            "...X.",
            "..X..",
            ".X...",
            "XXXXX",
        ),
        "spark" to arrayOf(
            "..X..",
            "..X..",
            "XX.XX",
            "..X..",
            "..X..",
        ),
        "note" to arrayOf(
            "..XXXXX",
            "..X...X",
            "..X...X",
            "..X...X",
            "XXX.XXX",
            "XXX.XXX",
        ),
        "poop" to arrayOf(
            "....X....",
            "...XXo...",
            "..XXXXX..",
            ".XXXXXXo.",
            ".XwkXwkX.",
            "XXXXXXXXo",
            "XXXXXXXXX",
        ),
    )

    fun count() = PALETTES.size

    fun sizes() = SIZE_UNITS.size

    fun name(index: Int) = PALETTES[Math.floorMod(index, PALETTES.size)].first

    fun bodyColor(index: Int) = PALETTES[Math.floorMod(index, PALETTES.size)].second[0]

    fun eyeColor(index: Int) = PALETTES[Math.floorMod(index, PALETTES.size)].second[1]

    fun unitDp(sizeIndex: Int) = SIZE_UNITS[Math.floorMod(sizeIndex, SIZE_UNITS.size)]

    fun particleRows(kind: String): Array<String>? = PATTERNS[kind]

    fun particleScale(kind: String, unit: Float): Int {
        val raw = if (kind == "poop") unit * 0.9f else unit * 0.75f
        return maxOf(2, raw.toInt())
    }

    fun particleColor(kind: String, char: Char): Int? =
        if (kind == "poop") POOP_COLORS[char]
        else if (char == 'X') PARTICLE_COLORS[kind]
        else null
}
