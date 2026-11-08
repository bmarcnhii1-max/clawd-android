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
    /** Сутки без ухода — всё в ноль, даже во сне. Спать вечно больше нельзя. */
    const val NEGLECT_MS = 24L * 60_000L * 60L

    fun applyDecay(prefs: Prefs) {
        val now = System.currentTimeMillis()
        val stored = prefs.tgUpdatedAt
        if (stored == 0L) {
            prefs.tgUpdatedAt = now
            return
        }
        if (!prefs.tgAlive) return
        var lastCare = prefs.tgLastCare
        if (lastCare == 0L) {
            // Миграция старых сохранений: отсчёт с этого момента, без казни.
            lastCare = now
        }
        var hunger = prefs.tgHunger
        var mood = prefs.tgMood
        var energy = prefs.tgEnergy
        var health = prefs.tgHealth
        var sleeping = prefs.tgSleeping
        var alive = true
        var updatedAt = prefs.tgUpdatedAt

        if (now - lastCare > NEGLECT_MS) {
            hunger = 0
            mood = 0
            energy = 0
            sleeping = false
            updatedAt = now
        }
        var elapsed = now - stored
        if (elapsed < 0L) {
            // Часы перевели назад — не «перематываем» decay и не
            // оставляем stored в будущем, а начинаем отсчёт заново.
            updatedAt = now
            elapsed = 0L
        }
        if (sleeping) {
            if (energy >= MAX) {
                // Спит с полной бодростью: нечего восполнять, но и будить нельзя —
                // иначе кнопка «спать» мгновенно отменяется на следующем тике.
                updatedAt = now
            } else {
                val regen = (elapsed / SLEEP_REGEN_MS).toInt()
                if (regen > 0) {
                    energy = (energy + regen).coerceAtMost(MAX)
                    // Сон лечит, но вдвое медленнее бодрости; округление
                    // вверх, чтобы реген 1 не терялся целочисленным делением.
                    health = (health + (regen + 1) / 2).coerceAtMost(MAX)
                    if (energy >= MAX) sleeping = false
                    // списываем только потраченное, остаток не сгорает
                    updatedAt = stored + regen * SLEEP_REGEN_MS
                }
            }
        } else {
            val lost = (elapsed / DECAY_MS).toInt()
            if (lost > 0) {
                hunger -= lost
                // Пока болеет — настроение тает вдвое быстрее.
                mood -= if (health <= 3) lost * 2 else lost
                energy -= lost
                // Голод бьёт по здоровью, сытая жизнь лечит.
                if (hunger <= 0) health -= lost
                else if (hunger >= 7 && energy >= 7) health += lost
                updatedAt = stored + lost * DECAY_MS
            }
        }
        hunger = hunger.coerceIn(0, MAX)
        mood = mood.coerceIn(0, MAX)
        energy = energy.coerceIn(0, MAX)
        health = health.coerceIn(0, MAX)
        // Смерть — от полного запустения (все три главных нуля) или от
        // голодной смерти при нулевом здоровье. Раньше health в условии
        // смерти не участвовал и питомец при health=0, но с едой был бессмертен.
        if (alive && ((hunger <= 0 && mood <= 0 && energy <= 0) || (hunger <= 0 && health <= 0))) {
            alive = false
            sleeping = false
            updatedAt = now
        }
        prefs.batchTg(hunger, mood, energy, health, sleeping, alive, updatedAt, lastCare)
    }

    /** Худший показатель — им определяется «настроение» питомца в оверлее. */
    fun worst(prefs: Prefs): Int = minOf(prefs.tgHunger, prefs.tgMood, prefs.tgEnergy, prefs.tgHealth)
}
