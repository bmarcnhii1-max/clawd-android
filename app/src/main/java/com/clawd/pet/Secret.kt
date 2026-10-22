package com.clawd.pet

/**
 * Тамагочи-ключи. Кодов нет в открытом виде — только SHA-256.
 * Каждой жизни — свой ключ:
 *   2-я — зеркальное отражение имени питомца;
 *   3-я — имя самого известного кота;
 *   4-я и дальше — имя его младшего брата.
 *
 * Подсказки дальше не раскрываются: остальное знает только админ.
 */
object Secret {
    const val LIFE2_HASH = "022fe68eb7c900244c3188dc1af2bd716335ed2239eee30f64a0a43787ccab79"
    const val LIFE3_HASH = "b248cfc4551e0b985db49b60d74dff66aba6f5efb1da79d073336d2c2a036639"
    const val LIFE4_HASH = "3b1c9b4d8e9c017bbaa168ca43a437e30c8b94f69f475193f830034c56acfb07"

    fun hashFor(lives: Int): String = when {
        lives >= 3 -> LIFE4_HASH
        lives == 2 -> LIFE3_HASH
        else -> LIFE2_HASH
    }
}
