package com.clawd.pet

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.security.MessageDigest

class TamagotchiActivity : Activity() {

    private companion object {
        const val MAX = 10
        const val DECAY_MS = 20L * 60_000L
        const val SLEEP_REGEN_MS = 12_000L
        const val TICK_MS = 5_000L
    }

    private lateinit var prefs: Prefs
    private var lang = "ru"
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var petView: PetPreviewView
    private lateinit var tvMood: TextView
    private lateinit var tvHunger: TextView
    private lateinit var tvEnergy: TextView
    private lateinit var pbMood: ProgressBar
    private lateinit var pbHunger: ProgressBar
    private lateinit var pbEnergy: ProgressBar
    private lateinit var tvMoodVal: TextView
    private lateinit var tvHungerVal: TextView
    private lateinit var tvEnergyVal: TextView
    private lateinit var vdDot: View
    private lateinit var btnFeed: Button
    private lateinit var btnPlay: Button
    private lateinit var btnSleep: Button
    private lateinit var reviveBox: View
    private lateinit var tvHint: TextView
    private lateinit var etCode: EditText
    private lateinit var btnRevive: Button

    private val tick = object : Runnable {
        override fun run() {
            applyDecay()
            draw()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tamagotchi)
        prefs = Prefs(this)
        lang = prefs.lang
        tvTitle = findViewById(R.id.tvTgTitle)
        tvStatus = findViewById(R.id.tvTgStatus)
        petView = findViewById(R.id.tgPreview)
        tvMood = findViewById(R.id.tvTgMood)
        tvHunger = findViewById(R.id.tvTgHunger)
        tvEnergy = findViewById(R.id.tvTgEnergy)
        pbMood = findViewById(R.id.pbTgMood)
        pbHunger = findViewById(R.id.pbTgHunger)
        pbEnergy = findViewById(R.id.pbTgEnergy)
        tvMoodVal = findViewById(R.id.tvTgMoodVal)
        tvHungerVal = findViewById(R.id.tvTgHungerVal)
        tvEnergyVal = findViewById(R.id.tvTgEnergyVal)
        vdDot = findViewById(R.id.vdTgDot)
        btnFeed = findViewById(R.id.btnTgFeed)
        btnPlay = findViewById(R.id.btnTgPlay)
        btnSleep = findViewById(R.id.btnTgSleep)
        reviveBox = findViewById(R.id.tgReviveBox)
        tvHint = findViewById(R.id.tvTgHint)
        etCode = findViewById(R.id.etTgCode)
        btnRevive = findViewById(R.id.btnTgRevive)

        applyDecay()
        btnFeed.setOnClickListener { act("feed") }
        btnPlay.setOnClickListener { act("play") }
        btnSleep.setOnClickListener { act("sleep") }
        btnRevive.setOnClickListener { revive() }
        draw()
        handler.postDelayed(tick, TICK_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    /**
     * Жизнь одна: переодически затухает даже когда игра закрыта (по реальному времени).
     * Пока спит — голод/настроение не тикают, бодрость восполняется.
     */
    private fun applyDecay() {
        val now = System.currentTimeMillis()
        val stored = prefs.tgUpdatedAt
        if (stored == 0L) {
            prefs.tgUpdatedAt = now
            return
        }
        if (prefs.tgAlive) {
            val elapsed = now - stored
            if (prefs.tgSleeping) {
                if (prefs.tgEnergy >= MAX) {
                    // спит с полной бодростью — время сна не конвертируется в голод
                    prefs.tgSleeping = false
                    prefs.tgUpdatedAt = now
                    return
                }
                val regen = (elapsed / SLEEP_REGEN_MS).toInt()
                if (regen > 0) {
                    prefs.tgEnergy = prefs.tgEnergy + regen
                    if (prefs.tgEnergy >= MAX) prefs.tgSleeping = false
                    // списываем только потраченное, остаток не сгорает
                    prefs.tgUpdatedAt = stored + regen * SLEEP_REGEN_MS
                }
            } else {
                val lost = (elapsed / DECAY_MS).toInt()
                if (lost > 0) {
                    prefs.tgHunger -= lost
                    prefs.tgMood -= lost
                    prefs.tgEnergy -= lost
                    prefs.tgUpdatedAt = stored + lost * DECAY_MS
                }
            }
            if (prefs.tgAlive && prefs.tgHunger <= 0 && prefs.tgMood <= 0 && prefs.tgEnergy <= 0) {
                prefs.tgAlive = false
                prefs.tgSleeping = false
                prefs.tgUpdatedAt = now
            }
        }
    }

    private fun act(kind: String) {
        if (!prefs.tgAlive) return
        applyDecay()
        if (!prefs.tgAlive) return
        prefs.tgUpdatedAt = System.currentTimeMillis()
        if (kind == "feed") {
            prefs.tgSleeping = false
            prefs.tgHunger = (prefs.tgHunger + 3).coerceAtMost(MAX)
            prefs.tgEnergy = (prefs.tgEnergy - 1).coerceAtLeast(0)
            petView.burst(PetPreviewView.BURST_FEED)
        } else if (kind == "play") {
            prefs.tgSleeping = false
            prefs.tgMood = (prefs.tgMood + 3).coerceAtMost(MAX)
            prefs.tgEnergy = (prefs.tgEnergy - 1).coerceAtLeast(0)
            prefs.tgHunger = (prefs.tgHunger - 1).coerceAtLeast(0)
            petView.burst(PetPreviewView.BURST_PLAY)
        } else {
            prefs.tgSleeping = !prefs.tgSleeping
        }
        draw()
    }



    private fun revive() {
        val code = etCode.text?.toString()?.trim().orEmpty()
        if (code.isEmpty()) return
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(code.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        if (hex != Secret.hashFor(prefs.tgLives)) {
            Toast.makeText(this, I18n.str(lang, "tg_wrong"), Toast.LENGTH_SHORT).show()
            return
        }
        doRevive()
    }

    private fun doRevive() {
        prefs.tgAlive = true
        prefs.tgLives += 1
        prefs.tgHunger = 9
        prefs.tgMood = 9
        prefs.tgEnergy = 9
        prefs.tgSleeping = false
        prefs.tgUpdatedAt = System.currentTimeMillis()
        etCode.setText("")
        val ti = I18n.t(lang)
        Toast.makeText(
            this,
            (ti["tg_revived"] as String).format(prefs.tgLives),
            Toast.LENGTH_SHORT,
        ).show()
        draw()
    }

    private fun draw() {
        val ti = I18n.t(lang)
        val alive = prefs.tgAlive
        val sleeping = prefs.tgSleeping
        petView.setPet(prefs.color, prefs.size)
        petView.setRunning(alive)
        petView.setMode(
            when {
                !alive -> PetPreviewView.MODE_DEAD
                sleeping -> PetPreviewView.MODE_SLEEP
                minOf(prefs.tgMood, prefs.tgHunger, prefs.tgEnergy) <= 2 && alive -> PetPreviewView.MODE_SAD
                else -> PetPreviewView.MODE_ALIVE
            },
        )
        petView.setVigor(minOf(prefs.tgMood, prefs.tgHunger, prefs.tgEnergy) / 10f)
        stat(pbMood, tvMoodVal, prefs.tgMood)
        stat(pbHunger, tvHungerVal, prefs.tgHunger)
        stat(pbEnergy, tvEnergyVal, prefs.tgEnergy)
        tvTitle.text = "${ti["tamagotchi"]}  ·  ${ti["tg_life"]} ${prefs.tgLives}"
        var dotColor = resources.getColor(R.color.stat_bad, theme)
        tvStatus.text = when {
            !alive -> ti["tg_dead"] as String
            sleeping -> ti["tg_sleep_note"] as String
            else -> {
                val candidates = listOf(
                    prefs.tgMood to "tg_sad",
                    prefs.tgHunger to "tg_hungry",
                    prefs.tgEnergy to "tg_tired",
                )
                val lowest = candidates.minByOrNull { it.first } ?: (0 to "tg_sad")
                val word = when {
                    lowest.first <= 3 -> {
                        dotColor = resources.getColor(R.color.stat_bad, theme)
                        ti[lowest.second] as String
                    }
                    lowest.first <= 6 -> {
                        dotColor = resources.getColor(R.color.stat_warn, theme)
                        ti[lowest.second] as String
                    }
                    else -> {
                        dotColor = resources.getColor(R.color.stat_ok, theme)
                        ti["tg_fine"] as String
                    }
                }
                word
            }
        }
        if (sleeping) {
            dotColor = resources.getColor(R.color.accent, theme)
        }
        vdDot.backgroundTintList = ColorStateList.valueOf(dotColor)
        btnSleep.text = if (sleeping) ti["tg_wake"] as String else ti["tg_sleep"] as String
        btnFeed.text = ti["tg_feed"] as String
        btnPlay.text = ti["tg_play"] as String
        btnFeed.isEnabled = alive
        btnPlay.isEnabled = alive
        btnSleep.isEnabled = alive
        btnFeed.alpha = if (alive) 1f else 0.35f
        btnPlay.alpha = if (alive) 1f else 0.35f
        btnSleep.alpha = if (alive) 1f else 0.35f
        reviveBox.visibility = if (alive) View.GONE else View.VISIBLE
        tvHint.text = ti["tg_hint"] as String
        etCode.hint = ti["tg_code"] as String
        btnRevive.text = ti["tg_revive"] as String
        tvMood.text = ti["tg_mood"] as String
        tvHunger.text = ti["tg_hunger"] as String
        tvEnergy.text = ti["tg_energy"] as String
    }

    private fun stat(bar: ProgressBar, valView: TextView, value: Int) {
        bar.progress = value
        valView.text = "$value/10"
        val color = when {
            value <= 3 -> R.color.stat_bad
            value <= 6 -> R.color.stat_warn
            else -> R.color.stat_ok
        }
        bar.progressTintList = ColorStateList.valueOf(
            resources.getColor(color, theme),
        )
        valView.setTextColor(resources.getColor(color, theme))
    }
}
