package com.clawd.pet

import android.content.Context
import android.content.SharedPreferences

class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("clawd", Context.MODE_PRIVATE)

    var lang: String
        get() = if (sp.getString("lang", "ru") == "en") "en" else "ru"
        set(value) = sp.edit().putString("lang", if (value == "en") "en" else "ru").apply()

    var wander: Boolean
        get() = sp.getBoolean("wander", false)
        set(value) = sp.edit().putBoolean("wander", value).apply()

    var phrases: Boolean
        get() = sp.getBoolean("phrases", true)
        set(value) = sp.edit().putBoolean("phrases", value).apply()

    var gravity: Boolean
        get() = sp.getBoolean("gravity", false)
        set(value) = sp.edit().putBoolean("gravity", value).apply()

    var dance: Boolean
        get() = sp.getBoolean("dance", true)
        set(value) = sp.edit().putBoolean("dance", value).apply()

    var sounds: Boolean
        get() = sp.getBoolean("sounds", true)
        set(value) = sp.edit().putBoolean("sounds", value).apply()

    var tgAlive: Boolean
        get() = sp.getBoolean("tg_alive", true)
        set(value) = sp.edit().putBoolean("tg_alive", value).apply()

    var tgLives: Int
        get() = sp.getInt("tg_lives", 1)
        set(value) = sp.edit().putInt("tg_lives", value).apply()

    var tgHunger: Int
        get() = sp.getInt("tg_hunger", 10)
        set(value) = sp.edit().putInt("tg_hunger", value.coerceIn(0, 10)).apply()

    var tgMood: Int
        get() = sp.getInt("tg_mood", 10)
        set(value) = sp.edit().putInt("tg_mood", value.coerceIn(0, 10)).apply()

    var tgEnergy: Int
        get() = sp.getInt("tg_energy", 10)
        set(value) = sp.edit().putInt("tg_energy", value.coerceIn(0, 10)).apply()

    var tgHealth: Int
        get() = sp.getInt("tg_health", 10)
        set(value) = sp.edit().putInt("tg_health", value.coerceIn(0, 10)).apply()

    var tgSleeping: Boolean
        get() = sp.getBoolean("tg_sleeping", false)
        set(value) = sp.edit().putBoolean("tg_sleeping", value).apply()

    var tgUpdatedAt: Long
        get() = sp.getLong("tg_updated_at", 0L)
        set(value) = sp.edit().putLong("tg_updated_at", value).apply()

    var size: Int
        get() = sp.getInt("size", 1)
        set(value) = sp.edit().putInt("size", Math.floorMod(value, Sprite.sizes())).apply()

    var color: Int
        get() = sp.getInt("color", 0)
        set(value) = sp.edit().putInt("color", Math.floorMod(value, Sprite.count())).apply()

    var sleepSeconds: Int
        get() = sp.getInt("sleep_seconds", 180)
        set(value) = sp.edit().putInt("sleep_seconds", value).apply()

    var posX: Int
        get() = sp.getInt("pos_x", Int.MIN_VALUE)
        set(value) = sp.edit().putInt("pos_x", value).apply()

    var posY: Int
        get() = sp.getInt("pos_y", Int.MIN_VALUE)
        set(value) = sp.edit().putInt("pos_y", value).apply()

    /**
     * Питомец не прячется при выключенном экране: он остаётся виден в зеркале
     * (scrcpy --turn-screen-off) и при этом не тратит батарею на анимацию.
     */
    var alwaysVisible: Boolean
        get() = sp.getBoolean("always_visible", true)
        set(value) = sp.edit().putBoolean("always_visible", value).apply()

    fun positionSaved(): Boolean = posX != Int.MIN_VALUE && posY != Int.MIN_VALUE
}
