package com.clawd.pet

/**
 * Общая логика тамагочи: затухание по реальному времени.
 * Используется и активностью, и оверлей-сервисом, чтобы питомец «чувствовал»
 * одно и то же состояние везде.
 */
object Tg {
    const val MAX = 10
    const val DECAY_MS = 20L * 60_000L
    const val SLEEP_REGEN_MS = 12L * 60_000L

    fun applyDecay(prefs: Prefs) {
        val now = System.currentTimeMillis()
        val stored = prefs.tgUpdatedAt
        if (stored == 0L) {
            prefs.tgUpdatedAt = now
            return
        }
        if (!prefs.tgAlive) return
        val elapsed = now - stored
        if (prefs.tgSleeping) {
            if (prefs.tgEnergy >= MAX) {
                // Спит с полной бодростью: нечего восполнять, но и будить нельзя —
                // иначе кнопка «спать» мгновенно отменяется на следующем тике.
                prefs.tgUpdatedAt = now
                return
            }
            val regen = (elapsed / SLEEP_REGEN_MS).toInt()
            if (regen > 0) {
                prefs.tgEnergy = prefs.tgEnergy + regen
                // Сон лечит, но вдвое медленнее бодрости.
                prefs.tgHealth = prefs.tgHealth + regen / 2
                if (prefs.tgEnergy >= MAX) prefs.tgSleeping = false
                // списываем только потраченное, остаток не сгорает
                prefs.tgUpdatedAt = stored + regen * SLEEP_REGEN_MS
            }
        } else {
            val lost = (elapsed / DECAY_MS).toInt()
            if (lost > 0) {
                prefs.tgHunger -= lost
                // Пока болеет — настроение тает вдвое быстрее.
                prefs.tgMood -= if (prefs.tgHealth <= 3) lost * 2 else lost
                prefs.tgEnergy -= lost
                // Голод бьёт по здоровью, сытая жизнь лечит.
                if (prefs.tgHunger <= 0) prefs.tgHealth -= lost
                else if (prefs.tgHunger >= 7 && prefs.tgEnergy >= 7) prefs.tgHealth += lost
                prefs.tgUpdatedAt = stored + lost * DECAY_MS
            }
        }
        if (prefs.tgAlive && prefs.tgHunger <= 0 && prefs.tgMood <= 0 && prefs.tgEnergy <= 0) {
            prefs.tgAlive = false
            prefs.tgSleeping = false
            prefs.tgUpdatedAt = now
        }
    }

    /** Худший показатель — им определяется «настроение» питомца в оверлее. */
    fun worst(prefs: Prefs): Int = minOf(prefs.tgHunger, prefs.tgMood, prefs.tgEnergy, prefs.tgHealth)
}
