package com.clawd.pet

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.roundToInt

class TamagotchiActivity : Activity() {

    private companion object {
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
    private lateinit var btnGame1: Button
    private lateinit var btnGame2: Button
    private lateinit var btnGame3: Button
    private lateinit var btnGame4: Button
    private lateinit var btnGame5: Button
    private lateinit var btnGame6: Button
    private lateinit var btnGame7: Button
    private lateinit var btnGame8: Button
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
        btnGame1 = findViewById(R.id.btnTgGame1)
        btnGame2 = findViewById(R.id.btnTgGame2)
        btnGame3 = findViewById(R.id.btnTgGame3)
        btnGame4 = findViewById(R.id.btnTgGame4)
        btnGame5 = findViewById(R.id.btnTgGame5)
        btnGame6 = findViewById(R.id.btnTgGame6)
        btnGame7 = findViewById(R.id.btnTgGame7)
        btnGame8 = findViewById(R.id.btnTgGame8)
        reviveBox = findViewById(R.id.tgReviveBox)
        tvHint = findViewById(R.id.tvTgHint)
        etCode = findViewById(R.id.etTgCode)
        btnRevive = findViewById(R.id.btnTgRevive)

        applyDecay()
        btnFeed.setOnClickListener { act("feed") }
        btnPlay.setOnClickListener { act("play") }
        btnSleep.setOnClickListener { act("sleep") }
        btnGame1.setOnClickListener { openGame(1) }
        btnGame2.setOnClickListener { openGame(2) }
        btnGame3.setOnClickListener { openGame(3) }
        btnGame4.setOnClickListener { openGame(4) }
        btnGame5.setOnClickListener { openGame(5) }
        btnGame6.setOnClickListener { openGame(6) }
        btnGame7.setOnClickListener { openGame(7) }
        btnGame8.setOnClickListener { openGame(8) }
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
    private fun applyDecay() = Tg.applyDecay(prefs)

    private fun act(kind: String) {
        if (!prefs.tgAlive) return
        applyDecay()
        if (!prefs.tgAlive) return
        prefs.tgUpdatedAt = System.currentTimeMillis()
        if (kind == "feed") {
            prefs.tgSleeping = false
            prefs.tgHunger = (prefs.tgHunger + 3).coerceAtMost(Tg.MAX)
            prefs.tgEnergy = (prefs.tgEnergy - 1).coerceAtLeast(0)
            petView.burst(PetPreviewView.BURST_FEED)
        } else if (kind == "play") {
            prefs.tgSleeping = false
            prefs.tgMood = (prefs.tgMood + 3).coerceAtMost(Tg.MAX)
            prefs.tgEnergy = (prefs.tgEnergy - 1).coerceAtLeast(0)
            prefs.tgHunger = (prefs.tgHunger - 1).coerceAtLeast(0)
            petView.burst(PetPreviewView.BURST_PLAY)
        } else {
            prefs.tgSleeping = !prefs.tgSleeping
        }
        draw()
    }

    private fun openGame(kind: Int) {
        if (!prefs.tgAlive) return
        applyDecay()
        if (!prefs.tgAlive) {
            draw()
            return
        }
        prefs.tgSleeping = false
        prefs.tgUpdatedAt = System.currentTimeMillis()
        when (kind) {
            1 -> gameCatch()
            2 -> gameCups()
            3 -> gameSimon()
            4 -> gameHold()
            5 -> gameAim()
            6 -> gameBeat()
            7 -> gameMoment()
            8 -> gameMirror()
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun accent(): Int = resources.getColor(R.color.accent, theme)

    private fun gameHead(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 16f
        setTypeface(null, Typeface.BOLD)
        gravity = Gravity.CENTER
        setTextColor(accent())
    }

    private fun styleDialog(dlg: AlertDialog) {
        dlg.setOnShowListener {
            dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private data class Shell(val root: LinearLayout, val status: TextView, val dlg: AlertDialog)

    /** Каркас игрового диалога: тёмная карточка, заголовок, правило, статус, «Закрыть». */
    private fun newGameShell(title: String, rule: String, close: String): Shell {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = resources.getDrawable(R.drawable.card_bg, theme)
        }
        root.addView(gameHead(title))
        root.addView(gameTitle(rule))
        val status = gameTitle("")
        root.addView(status)
        val dlg = AlertDialog.Builder(this).setView(root).setCancelable(true)
            .setNegativeButton(close, null).create()
        styleDialog(dlg)
        return Shell(root, status, dlg)
    }

    private fun gameTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(resources.getColor(R.color.muted, theme))
        gravity = Gravity.CENTER
    }

    /** Игра 1 «Догонялки»: тапнуть по мечущемуся питомцу 6 раз за 12 секунд. */
    private fun gameCatch() {
        val ti = I18n.t(lang)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = resources.getDrawable(R.drawable.card_bg, theme)
        }
        val head = gameHead(ti["tg_game_catch"] as String)
        val rule = gameTitle(ti["tg_catch_rule"] as String)
        val score = gameTitle((ti["tg_catch_score"] as String).format(0))
        val pet = PetPreviewView(this).apply {
            setPet(prefs.color, prefs.size)
            setMode(PetPreviewView.MODE_ALIVE)
            setRunning(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(190),
            ).apply { topMargin = dp(8) }
            isClickable = true
            isFocusable = true
        }
        root.addView(head)
        root.addView(rule)
        root.addView(score)
        root.addView(pet)
        val dlg = AlertDialog.Builder(this).setView(root).setCancelable(true)
            .setNegativeButton(ti["tg_close"] as String, null).create()
        styleDialog(dlg)
        var caught = 0
        var over = false
        val jobs = ArrayList<Runnable>()
        fun stop() {
            over = true
            jobs.forEach { handler.removeCallbacks(it) }
        }
        fun finish(win: Boolean) {
            if (over) return
            stop()
            prefs.tgEnergy = (prefs.tgEnergy - 1).coerceAtLeast(0)
            if (win) prefs.tgMood = (prefs.tgMood + 3).coerceAtMost(Tg.MAX)
            prefs.tgUpdatedAt = System.currentTimeMillis()
            Toast.makeText(
                this,
                if (win) ti["tg_catch_win"] as String else ti["tg_catch_lose"] as String,
                Toast.LENGTH_SHORT,
            ).show()
            dlg.dismiss()
            draw()
        }
        var side = 1f
        fun hop() {
            // Амплитуда больше полуширины спрайта: спам в центр экрана промахивается.
            side = -side
            pet.translationX = side * dp(90).toFloat()
        }
        // Засчитываем только попадание по спрайту, а не тап куда угодно:
        // вся вьюха широкая, питомец рисуется по центру (PetPreviewView.onDraw: ox).
        val dCatch = resources.displayMetrics.density
        val hitHw = Sprite.unitDp(prefs.size) * dCatch * Sprite.GRID_W / 2f + dp(12)
        val hitHh = Sprite.unitDp(prefs.size) * dCatch * Sprite.GRID_H / 2f + dp(12)
        pet.setOnTouchListener { _, e ->
            if (e.action != MotionEvent.ACTION_DOWN) return@setOnTouchListener false
            if (over) return@setOnTouchListener true
            if (abs(e.x - pet.width / 2f) <= hitHw &&
                abs(e.y - pet.height / 2f) <= hitHh
            ) {
                caught++
                score.text = (ti["tg_catch_score"] as String).format(caught)
                pet.burst(PetPreviewView.BURST_PLAY)
                if (caught >= 6) finish(true)
            } else {
                hop() // мимо: питомец тут же отпрыгивает в другую сторону
            }
            true
        }
        val hopTick = object : Runnable {
            override fun run() {
                if (over) return
                hop()
                handler.postDelayed(this, 380L)
            }
        }
        val timeout = Runnable { finish(false) }
        jobs.add(hopTick)
        jobs.add(timeout)
        handler.post(hopTick)
        handler.postDelayed(timeout, 12_000L)
        dlg.setOnDismissListener { stop() }
        dlg.show()
    }

    /** Игра 2 «Напёрстки»: угадать, под каким стаканом спрятался питомец. */
    private fun gameCups() {
        val ti = I18n.t(lang)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = resources.getDrawable(R.drawable.card_bg, theme)
        }
        val head = gameHead(ti["tg_game_cups"] as String)
        val rule = gameTitle(ti["tg_cups_rule"] as String)
        val result = gameTitle("")
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(head)
        root.addView(rule)
        root.addView(result)
        root.addView(row)
        val dlg = AlertDialog.Builder(this).setView(root).setCancelable(true)
            .setNegativeButton(ti["tg_close"] as String, null).create()
        styleDialog(dlg)
        val hide = kotlin.random.Random.nextInt(3)
        var done = false
        for (i in 0 until 3) {
            val b = Button(this).apply {
                text = "${i + 1}"
                textSize = 20f
                layoutParams = LinearLayout.LayoutParams(0, dp(64), 1f).apply {
                    if (i > 0) marginStart = dp(8)
                }
            }
            b.setOnClickListener {
                if (done) return@setOnClickListener
                done = true
                if (i == hide) {
                    prefs.tgHunger = (prefs.tgHunger + 3).coerceAtMost(Tg.MAX)
                    result.text = ti["tg_cups_win"] as String
                } else {
                    prefs.tgMood = (prefs.tgMood - 1).coerceAtLeast(0)
                    result.text = "${ti["tg_cups_lose"]} (${hide + 1})"
                }
                prefs.tgUpdatedAt = System.currentTimeMillis()
                draw()
                handler.postDelayed({ dlg.dismiss() }, 1_600L)
            }
            row.addView(b)
        }
        dlg.show()
    }

    /** Игра 3 «Повтори»: симон на трёх кнопках, ряды длиной 3 и 4. */
    private fun gameSimon() {
        val ti = I18n.t(lang)
        val accent = resources.getColor(R.color.accent, theme)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = resources.getDrawable(R.drawable.card_bg, theme)
        }
        val head = gameHead(ti["tg_game_simon"] as String)
        val rule = gameTitle(ti["tg_simon_rule"] as String)
        val status = gameTitle(ti["tg_simon_watch"] as String)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(head)
        root.addView(rule)
        root.addView(status)
        root.addView(row)
        val dlg = AlertDialog.Builder(this).setView(root).setCancelable(true)
            .setNegativeButton(ti["tg_close"] as String, null).create()
        styleDialog(dlg)
        val pads = ArrayList<Button>()
        for (i in 0 until 3) {
            val b = Button(this).apply {
                text = "${i + 1}"
                textSize = 20f
                layoutParams = LinearLayout.LayoutParams(0, dp(64), 1f).apply {
                    if (i > 0) marginStart = dp(8)
                }
                isEnabled = false
            }
            pads.add(b)
            row.addView(b)
        }
        val seq = ArrayList<Int>()
        var pos = 0
        var accepting = false
        var over = false
        val jobs = ArrayList<Runnable>()
        fun stop() {
            over = true
            jobs.forEach { handler.removeCallbacks(it) }
        }
        fun win() {
            if (over) return
            stop()
            prefs.tgMood = (prefs.tgMood + 2).coerceAtMost(Tg.MAX)
            prefs.tgEnergy = (prefs.tgEnergy + 1).coerceAtMost(Tg.MAX)
            prefs.tgUpdatedAt = System.currentTimeMillis()
            Toast.makeText(this, ti["tg_simon_win"] as String, Toast.LENGTH_SHORT).show()
            dlg.dismiss()
            draw()
        }
        fun lose() {
            if (over) return
            stop()
            prefs.tgUpdatedAt = System.currentTimeMillis()
            Toast.makeText(this, ti["tg_simon_lose"] as String, Toast.LENGTH_SHORT).show()
            dlg.dismiss()
            draw()
        }
        fun playAt(k: Int, done: Runnable) {
            val step = object : Runnable {
                var idx = 0
                override fun run() {
                    if (over) return
                    if (idx > k) {
                        handler.post(done)
                        return
                    }
                    val p = seq[idx++]
                    pads[p].backgroundTintList = ColorStateList.valueOf(accent)
                    val off = Runnable {
                        pads[p].backgroundTintList = null
                        handler.postDelayed(this, 200L)
                    }
                    jobs.add(off)
                    handler.postDelayed(off, 380L)
                }
            }
            jobs.add(step)
            handler.postDelayed(step, 600L)
        }
        fun nextRound() {
            if (over) return
            if (seq.size >= 4) {
                win()
                return
            }
            seq.add(kotlin.random.Random.nextInt(3))
            status.text = ti["tg_simon_watch"] as String
            playAt(seq.size - 1, Runnable {
                if (over) return@Runnable
                accepting = true
                pos = 0
                pads.forEach { it.isEnabled = true }
                status.text = ti["tg_simon_you"] as String
            })
        }
        pads.forEachIndexed { i, b ->
            b.setOnClickListener {
                if (over || !accepting) return@setOnClickListener
                if (i == seq[pos]) {
                    pos++
                    if (pos == seq.size) {
                        accepting = false
                        pads.forEach { it.isEnabled = false }
                        status.text = ti["tg_simon_watch"] as String
                        val go = Runnable { nextRound() }
                        jobs.add(go)
                        handler.postDelayed(go, 700L)
                    }
                } else {
                    lose()
                }
            }
        }
        dlg.setOnDismissListener { stop() }
        dlg.show()
        nextRound()
    }

    /** Игра 4 «Тихий час»: удерживай питомца 3 секунды, не отпускай. */
    private fun gameHold() {
        val ti = I18n.t(lang)
        val (root, status, dlg) = newGameShell(
            ti["tg_game_hold"] as String,
            ti["tg_hold_rule"] as String,
            ti["tg_close"] as String,
        )
        val pet = PetPreviewView(this).apply {
            setPet(prefs.color, prefs.size)
            setMode(PetPreviewView.MODE_SLEEP)
            setRunning(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(190),
            ).apply { topMargin = dp(8) }
            isClickable = true
            isFocusable = true
        }
        root.addView(pet, 2)
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 180
            progress = 0
            progressDrawable = resources.getDrawable(R.drawable.bar_track, theme)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(14),
            ).apply { topMargin = dp(10) }
        }
        root.addView(bar)
        var over = false
        var holding = false
        var ticks = 0
        val jobs = ArrayList<Runnable>()
        fun stop() {
            over = true
            jobs.forEach { handler.removeCallbacks(it) }
        }
        fun win() {
            if (over) return
            stop()
            prefs.tgEnergy = (prefs.tgEnergy + 3).coerceAtMost(Tg.MAX)
            prefs.tgUpdatedAt = System.currentTimeMillis()
            Toast.makeText(this, ti["tg_hold_win"] as String, Toast.LENGTH_SHORT).show()
            dlg.dismiss()
            draw()
        }
        val step = object : Runnable {
            override fun run() {
                if (over || !holding) return
                ticks++
                bar.progress = ticks
                status.text = (ti["tg_hold_progress"] as String).format(ticks * 100 / 180)
                if (ticks >= 180) {
                    win()
                    return
                }
                handler.postDelayed(this, 16L)
            }
        }
        jobs.add(step)
        pet.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    if (!over && !holding) {
                        holding = true
                        ticks = 0
                        bar.progress = 0
                        handler.post(step)
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    holding = false
                    ticks = 0
                    bar.progress = 0
                    status.text = ""
                    true
                }
                else -> true
            }
        }
        dlg.setOnDismissListener { stop() }
        dlg.show()
    }

    /** Игра 5 «Меткий»: попади по огоньку 5 раз за 12 секунд. */
    private fun gameAim() {
        val ti = I18n.t(lang)
        val (root, status, dlg) = newGameShell(
            ti["tg_game_aim"] as String,
            ti["tg_aim_rule"] as String,
            ti["tg_close"] as String,
        )
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(row)
        val slots = ArrayList<LinearLayout>()
        for (i in 0 until 5) {
            val s = LinearLayout(this).apply {
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(64))
            }
            slots.add(s)
            row.addView(s)
        }
        val target = View(this).apply {
            setBackgroundColor(accent())
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            isClickable = true
            isFocusable = true
        }
        var hits = 0
        var over = false
        val jobs = ArrayList<Runnable>()
        fun stop() {
            over = true
            jobs.forEach { handler.removeCallbacks(it) }
        }
        fun win() {
            if (over) return
            stop()
            prefs.tgMood = (prefs.tgMood + 2).coerceAtMost(Tg.MAX)
            prefs.tgUpdatedAt = System.currentTimeMillis()
            Toast.makeText(this, ti["tg_aim_win"] as String, Toast.LENGTH_SHORT).show()
            dlg.dismiss()
            draw()
        }
        target.setOnClickListener {
            if (over) return@setOnClickListener
            hits++
            status.text = (ti["tg_aim_score"] as String).format(hits)
            if (hits >= 5) win()
        }
        val move = object : Runnable {
            override fun run() {
                if (over) return
                (target.parent as? LinearLayout)?.removeView(target)
                slots[kotlin.random.Random.nextInt(slots.size)].addView(target)
                handler.postDelayed(this, 700L)
            }
        }
        val timeout = Runnable {
            if (over) return@Runnable
            stop()
            Toast.makeText(this, ti["tg_aim_lose"] as String, Toast.LENGTH_SHORT).show()
            dlg.dismiss()
            draw()
        }
        jobs.add(move)
        jobs.add(timeout)
        handler.post(move)
        handler.postDelayed(timeout, 12_000L)
        dlg.setOnDismissListener { stop() }
        dlg.show()
    }

    /** Игра 6 «В такт»: тапни, когда питомец самый большой. Нужно 4 раза. */
    private fun gameBeat() {
        val ti = I18n.t(lang)
        val (root, status, dlg) = newGameShell(
            ti["tg_game_beat"] as String,
            ti["tg_beat_rule"] as String,
            ti["tg_close"] as String,
        )
        val pet = PetPreviewView(this).apply {
            setPet(prefs.color, prefs.size)
            setMode(PetPreviewView.MODE_ALIVE)
            setRunning(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(190),
            ).apply { topMargin = dp(8) }
            isClickable = true
            isFocusable = true
        }
        root.addView(pet)
        var hits = 0
        var over = false
        var scale = 1f
        val jobs = ArrayList<Runnable>()
        val born = System.currentTimeMillis()
        fun stop() {
            over = true
            jobs.forEach { handler.removeCallbacks(it) }
        }
        fun win() {
            if (over) return
            stop()
            prefs.tgMood = (prefs.tgMood + 2).coerceAtMost(Tg.MAX)
            prefs.tgEnergy = (prefs.tgEnergy + 1).coerceAtMost(Tg.MAX)
            prefs.tgUpdatedAt = System.currentTimeMillis()
            Toast.makeText(this, ti["tg_beat_win"] as String, Toast.LENGTH_SHORT).show()
            dlg.dismiss()
            draw()
        }
        val pulse = object : Runnable {
            override fun run() {
                if (over) return
                val t = (System.currentTimeMillis() - born) % 1_200L
                scale = 1f + 0.4f * kotlin.math.abs(
                    kotlin.math.sin(t.toFloat() / 1_200f * Math.PI.toFloat()),
                )
                pet.scaleX = scale
                pet.scaleY = scale
                handler.postDelayed(this, 16L)
            }
        }
        val timeout = Runnable {
            if (over) return@Runnable
            stop()
            Toast.makeText(this, ti["tg_beat_lose"] as String, Toast.LENGTH_SHORT).show()
            dlg.dismiss()
            draw()
        }
        pet.setOnClickListener {
            if (over) return@setOnClickListener
            if (scale >= 1.32f) {
                hits++
                status.text = (ti["tg_beat_score"] as String).format(hits)
                pet.burst(PetPreviewView.BURST_PLAY)
                if (hits >= 4) win()
            } else {
                hits = 0
                status.text = ti["tg_beat_lose"] as String
            }
        }
        jobs.add(pulse)
        jobs.add(timeout)
        handler.post(pulse)
        handler.postDelayed(timeout, 15_000L)
        dlg.setOnDismissListener { stop() }
        dlg.show()
    }

    /** Игра 7 «Момент»: жми «Хватай», когда ползунок в зелёной зоне. */
    private fun gameMoment() {
        val ti = I18n.t(lang)
        val (root, status, dlg) = newGameShell(
            ti["tg_game_moment"] as String,
            ti["tg_moment_rule"] as String,
            ti["tg_close"] as String,
        )
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressDrawable = resources.getDrawable(R.drawable.bar_track, theme)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(16),
            ).apply { topMargin = dp(4) }
        }
        root.addView(zoneStrip())
        root.addView(bar)
        val grab = Button(this).apply {
            text = ti["tg_moment_btn"] as String
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56),
            ).apply { topMargin = dp(12) }
        }
        root.addView(grab)
        var pos = 0
        var posF = 0f
        var dir = 4
        var tries = 3
        var over = false
        val jobs = ArrayList<Runnable>()
        fun stop() {
            over = true
            jobs.forEach { handler.removeCallbacks(it) }
        }
        fun win() {
            if (over) return
            stop()
            prefs.tgHunger = (prefs.tgHunger + 3).coerceAtMost(Tg.MAX)
            prefs.tgUpdatedAt = System.currentTimeMillis()
            Toast.makeText(this, ti["tg_moment_win"] as String, Toast.LENGTH_SHORT).show()
            dlg.dismiss()
            draw()
        }
        grab.setOnClickListener {
            if (over) return@setOnClickListener
            if (pos in 70..90) {
                win()
            } else {
                tries--
                if (tries <= 0) {
                    stop()
                    Toast.makeText(this, ti["tg_moment_fail"] as String, Toast.LENGTH_SHORT).show()
                    dlg.dismiss()
                    draw()
                } else {
                    status.text = (ti["tg_moment_lose"] as String).format(tries)
                }
            }
        }
        val slide = object : Runnable {
            override fun run() {
                if (over) return
                posF += dir / 3f
                if (posF >= 100f) {
                    posF = 100f
                    dir = -dir
                } else if (posF <= 0f) {
                    posF = 0f
                    dir = -dir
                }
                pos = posF.roundToInt()
                bar.progress = pos
                handler.postDelayed(this, 20L)
            }
        }
        jobs.add(slide)
        handler.post(slide)
        dlg.setOnDismissListener { stop() }
        dlg.show()
    }

    /** Полоска-подсказка зелёной зоны 70–90 для «Момента». */
    private fun zoneStrip(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(8),
            ).apply { topMargin = dp(12) }
        }
        fun gap(w: Float) = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(8), w)
        }
        val mark = View(this).apply {
            setBackgroundColor(resources.getColor(R.color.stat_ok, theme))
            layoutParams = LinearLayout.LayoutParams(0, dp(8), 20f)
        }
        row.addView(gap(70f))
        row.addView(mark)
        row.addView(gap(10f))
        return row
    }

    /** Игра 8 «Зеркало»: куда он наклонился? Жми стрелку. Нужно 5 подряд. */
    private fun gameMirror() {
        val ti = I18n.t(lang)
        val (root, status, dlg) = newGameShell(
            ti["tg_game_mirror"] as String,
            ti["tg_mirror_rule"] as String,
            ti["tg_close"] as String,
        )
        val pet = PetPreviewView(this).apply {
            setPet(prefs.color, prefs.size)
            setMode(PetPreviewView.MODE_ALIVE)
            setRunning(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(170),
            ).apply { topMargin = dp(8) }
        }
        root.addView(pet)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(row)
        val left = Button(this).apply {
            text = "◀"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(0, dp(60), 1f)
            isEnabled = false
        }
        val right = Button(this).apply {
            text = "▶"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(0, dp(60), 1f).apply {
                marginStart = dp(12)
            }
            isEnabled = false
        }
        row.addView(left)
        row.addView(right)
        var combo = 0
        var side = -1f
        var accepting = false
        var over = false
        val jobs = ArrayList<Runnable>()
        fun stop() {
            over = true
            jobs.forEach { handler.removeCallbacks(it) }
        }
        fun win() {
            if (over) return
            stop()
            prefs.tgMood = (prefs.tgMood + 2).coerceAtMost(Tg.MAX)
            prefs.tgUpdatedAt = System.currentTimeMillis()
            Toast.makeText(this, ti["tg_mirror_win"] as String, Toast.LENGTH_SHORT).show()
            dlg.dismiss()
            draw()
        }
        fun ask() {
            if (over) return
            side = if (kotlin.random.Random.nextBoolean()) -1f else 1f
            pet.translationX = side * dp(80).toFloat()
            accepting = true
            left.isEnabled = true
            right.isEnabled = true
            status.text = ""
        }
        fun pick(rightSide: Boolean) {
            if (over || !accepting) return
            accepting = false
            left.isEnabled = false
            right.isEnabled = false
            val good = (rightSide && side > 0f) || (!rightSide && side < 0f)
            if (good) {
                combo++
                status.text = (ti["tg_mirror_combo"] as String).format(combo)
                if (combo >= 5) {
                    win()
                    return
                }
            } else {
                combo = 0
                status.text = (ti["tg_mirror_combo"] as String).format(combo)
            }
            pet.translationX = 0f
            val next = Runnable { ask() }
            jobs.add(next)
            handler.postDelayed(next, 600L)
        }
        left.setOnClickListener { pick(false) }
        right.setOnClickListener { pick(true) }
        val start = Runnable { ask() }
        jobs.add(start)
        handler.postDelayed(start, 700L)
        dlg.setOnDismissListener { stop() }
        dlg.show()
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
        btnGame1.text = ti["tg_game_catch"] as String
        btnGame2.text = ti["tg_game_cups"] as String
        btnGame3.text = ti["tg_game_simon"] as String
        btnGame4.text = ti["tg_game_hold"] as String
        btnGame5.text = ti["tg_game_aim"] as String
        btnGame6.text = ti["tg_game_beat"] as String
        btnGame7.text = ti["tg_game_moment"] as String
        btnGame8.text = ti["tg_game_mirror"] as String
        btnFeed.isEnabled = alive
        btnPlay.isEnabled = alive
        btnSleep.isEnabled = alive
        btnGame1.isEnabled = alive
        btnGame2.isEnabled = alive
        btnGame3.isEnabled = alive
        btnGame4.isEnabled = alive
        btnGame5.isEnabled = alive
        btnGame6.isEnabled = alive
        btnGame7.isEnabled = alive
        btnGame8.isEnabled = alive
        btnFeed.alpha = if (alive) 1f else 0.35f
        btnPlay.alpha = if (alive) 1f else 0.35f
        btnSleep.alpha = if (alive) 1f else 0.35f
        btnGame1.alpha = if (alive) 1f else 0.35f
        btnGame2.alpha = if (alive) 1f else 0.35f
        btnGame3.alpha = if (alive) 1f else 0.35f
        btnGame4.alpha = if (alive) 1f else 0.35f
        btnGame5.alpha = if (alive) 1f else 0.35f
        btnGame6.alpha = if (alive) 1f else 0.35f
        btnGame7.alpha = if (alive) 1f else 0.35f
        btnGame8.alpha = if (alive) 1f else 0.35f
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
