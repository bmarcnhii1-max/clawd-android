package com.clawd.pet

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextPaint
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class PetOverlay(private val context: Context, private val prefs: Prefs) {

    private companion object {
        const val STATE_IDLE = "idle"
        const val STATE_WALK = "walk"
        const val STATE_SLEEP = "sleep"
        const val STATE_FLY = "fly"
        const val STATE_DRAG = "drag"
        const val STATE_PET = "pet"
        const val STATE_FLEE = "flee"

        const val WALK_SPEED = 8f
        const val TOUCH_SLOP = 14f
        const val TICK_ACTIVE = 40L
        const val TICK_IDLE = 250L
        const val TICK_SLEEP = 1200L
        const val BUBBLE_MS = 2600L
        const val DRAG_MS = 1800L
        const val DREAM_MS = 5000L
        const val PET_HOLD_MS = 450L
        const val MENU_HOLD_MS = 1800L
        const val FLEE_HIDE_MS = 20000L
        const val TAP_WINDOW_MS = 2500L
        const val ANNOYED_TAPS = 4
        const val FLEE_TAPS = 8
        const val DISSOLVE_SPEED = 1.7f
        const val CONDENSE_SPEED = 2.4f
        const val DANCE_BEAT_MS = 560L
    }

    private val random = kotlin.random.Random.Default
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val dm = context.resources.displayMetrics
    private val density = dm.density
    private val handler = Handler(Looper.getMainLooper())

    private var unit = 6f * density
    private var bodyW = 0f
    private var bodyH = 0f
    private var margin = 0f
    private var bubbleH = 0f
    private var viewW = 0f
    private var viewH = 0f
    private var hopGravity = 0f
    private var flyGravity = 0f
    private var flyMin = 0f
    private var flyLimit = 0f
    private var throwSaySpeed = 0f
    private var landSpeed = 0f

    private var x = 0f
    private var y = 0f
    private var vx = 0f
    private var vy = 0f
    private var hopActive = false
    private var hopOff = 0f
    private var hopVy = 0f

    private var state = STATE_IDLE
    private var look = 0
    private var blinkUntil = 0L
    private var nextBlink = 0L
    private var nextAction = 0L
    private var nextZ = 0L
    private var nextDream = 0L
    private var expr = ""
    private var exprUntil = 0L
    private var squashUntil = 0L
    private var waveUntil = 0L
    private var heartNext = 0L
    private var petUntil = 0L
    private var walkTarget = 0f
    private var lastInteraction = 0L
    private var lastStep = uptime()
    private var lastTap = 0L

    private var dissolve = 0f
    private var dissolveTarget = 0f
    private var blockNext = 0L
    private var fleeRise = 0f

    private var dancing = false
    private var danceNext = 0L

    private var bubbleText: String? = null
    private var bubbleUntil = 0L

    var screenActive = true

    private var added = false
    private val particles = ArrayList<Particle>()
    private val tapTimes = ArrayList<Long>()
    private val touchHist = ArrayList<Sample>()

    private var downRawX = 0f
    private var downRawY = 0f
    private var downT = 0L
    private var prevRawX = 0f
    private var prevRawY = 0f
    private var downWinX = 0f
    private var downWinY = 0f
    private var moved = false
    private var petted = false
    private var dragSaid = false
    private var dragVx = 0f

    private lateinit var view: View
    private lateinit var lp: WindowManager.LayoutParams
    private lateinit var hitView: View
    private lateinit var hitLp: WindowManager.LayoutParams
    private var hitOx = 0f
    private var hitOy = 0f
    private var hitW = 0f
    private var hitH = 0f
    private var hitPadX = 0f
    private var hitPadY = 0f
    private var currentRot = 0f
    private class Particle(
        val type: String,
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val born: Long,
        val ttl: Long,
        val w: Float,
        val h: Float,
    )

    private class Sample(val t: Long, val x: Float, val y: Float)

    private val paintBody = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paintBubble = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xF21C1D22.toInt()
    }
    private val paintBubbleStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = density
        color = 0x33FFFFFF
    }
    private val paintParticle = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val paintText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * 1f
        color = 0xFFEDEDED.toInt()
        textAlign = Paint.Align.LEFT
    }

    private val heartRows = Sprite.particleRows("heart") ?: emptyArray()
    private var heartScale = 4
    private var heartW = 0f

    private val tickRunnable = object : Runnable {
        override fun run() {
            step()
            schedule()
        }
    }

    private val petRunnable = Runnable {
        petted = true
        state = STATE_PET
        lastInteraction = uptime()
        petUntil = uptime() + 12000L
        say("pet")
    }

    init {
        initMetrics()
        buildView()
        buildParams()
    }

    private fun uptime(): Long = SystemClock.elapsedRealtime()

    private fun initMetrics() {
        unit = Sprite.unitDp(prefs.size) * density
        bodyW = unit * Sprite.GRID_W
        bodyH = unit * Sprite.GRID_H
        margin = max(bodyW, bodyH) * 0.25f + 10f * density
        paintText.textSize = 13f * density
        val lineH = paintText.textSize * 1.25f
        bubbleH = 3f * lineH + 14f * density
        viewW = bodyW + margin * 2f
        viewH = bodyH + margin * 2f + bubbleH
        heartScale = max(2, (paintText.textSize / 6f).toInt())
        heartW = (heartRows.firstOrNull()?.length ?: 7) * heartScale.toFloat()
        hopGravity = 400f * unit
        flyGravity = 325f * unit
        flyMin = 87.5f * unit
        flyLimit = 750f * unit
        throwSaySpeed = 312f * unit
        landSpeed = 87.5f * unit
        paintBody.color = Sprite.bodyColor(prefs.color)
    }

    private fun buildParams() {
        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        hitLp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        lp.gravity = Gravity.TOP or Gravity.START
        hitLp.gravity = Gravity.TOP or Gravity.START
        lp.alpha = 0.8f
        if (Build.VERSION.SDK_INT >= 30) {
            lp.fitInsetsTypes = 0
            lp.fitInsetsSides = 0
            hitLp.fitInsetsTypes = 0
            hitLp.fitInsetsSides = 0
        }
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            hitLp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }

    @SuppressLint("ClickableViewAccessibility", "ViewConstructor")
    private fun buildView() {
        view = object : View(context) {
            override fun onMeasure(widthSpec: Int, heightSpec: Int) {
                setMeasuredDimension(viewW.roundToInt(), viewH.roundToInt())
            }

            override fun onDraw(canvas: Canvas) {
                drawPetEffects(canvas)
            }
        }
        hitView = object : View(context) {
            override fun onMeasure(widthSpec: Int, heightSpec: Int) {
                setMeasuredDimension(
                    hitW.roundToInt().coerceAtLeast(1),
                    hitH.roundToInt().coerceAtLeast(1),
                )
            }

            override fun onDraw(canvas: Canvas) {
                drawPetBody(canvas)
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                return handleTouch(event)
            }
        }
    }

    fun show(condense: Boolean = false) {
        if (added) return
        state = STATE_IDLE
        look = 0
        dissolve = if (condense) 1f else 0f
        dissolveTarget = 0f
        hopActive = false
        hopOff = 0f
        hopVy = 0f
        fleeRise = 0f
        vx = 0f
        vy = 0f
        tapTimes.clear()
        lastTap = 0L
        restorePos()
        try {
            wm.addView(view, lp)
            wm.addView(hitView, hitLp)
            added = true
        } catch (e: Exception) {
            try {
                wm.removeView(view)
            } catch (_: Exception) {
            }
            try {
                wm.removeView(hitView)
            } catch (_: Exception) {
            }
            return
        }
        pushWindow()
        val now = uptime()
        lastInteraction = now
        nextBlink = now + 2000L
        nextAction = now + 4000L
        schedule()
        handler.postDelayed({
            if (added) {
                hop(55f)
                waveUntil = uptime() + 1600L
                say("hello")
            }
        }, 700L)
    }

    fun hide() {
        handler.removeCallbacks(tickRunnable)
        handler.removeCallbacks(returnRunnable)
        handler.removeCallbacks(menuRunnable)
        hideMenu()
        if (added) {
            savePos()
            try {
                wm.removeView(hitView)
            } catch (_: Exception) {
            }
            try {
                wm.removeView(view)
            } catch (_: Exception) {
            }
            added = false
        }
        state = STATE_IDLE
        fleeRise = 0f
        dissolve = 0f
        dissolveTarget = 0f
        hopActive = false
        hopOff = 0f
        hopVy = 0f
        vx = 0f
        vy = 0f
        tapTimes.clear()
        lastTap = 0L
        dancing = false
        danceNext = 0L
    }

    private val returnRunnable = Runnable {
        if (!added && screenActive) {
            show(condense = true)
            say("back")
        }
    }

    fun resetPosition() {
        prefs.posX = Int.MIN_VALUE
        prefs.posY = Int.MIN_VALUE
        cancelFlee()
        state = STATE_IDLE
        hopActive = false
        hopOff = 0f
        hopVy = 0f
        vx = 0f
        vy = 0f
        restorePos()
        pushWindow()
        view.invalidate()
    }

    private fun cancelFlee() {
        if (state == STATE_FLEE) {
            state = STATE_IDLE
            fleeRise = 0f
            dissolve = 0f
            dissolveTarget = 0f
            handler.removeCallbacks(returnRunnable)
        }
    }

    fun applyConfig() {
        cancelFlee()
        val cx = x + viewW / 2f
        val cy = y + viewH / 2f
        initMetrics()
        x = cx - viewW / 2f
        y = cy - viewH / 2f
        clampXY()
        repeat(3) { spawnParticle("spark") }
        view.requestLayout()
        hitView.requestLayout()
        view.invalidate()
        if (added) {
            try {
                wm.updateViewLayout(view, lp)
            } catch (_: Exception) {
            }
            try {
                wm.updateViewLayout(hitView, hitLp)
            } catch (_: Exception) {
            }
        }
        pushWindow()
    }

    private fun schedule() {
        if (!added) return
        val interval = when {
            state == STATE_FLY || state == STATE_FLEE || state == STATE_WALK ||
                state == STATE_PET || hopActive || dancing ||
                dissolve != dissolveTarget -> TICK_ACTIVE
            state == STATE_SLEEP -> TICK_SLEEP
            else -> TICK_IDLE
        }
        handler.postDelayed(tickRunnable, interval)
    }

    private fun step() {
        val now = uptime()
        val dt = max(0f, min(0.15f, (now - lastStep) / 1000f))
        lastStep = now
        try {
            when (state) {
                STATE_FLY -> stepFly(dt)
                STATE_FLEE -> stepFlee(dt)
                else -> stepGround(now, dt)
            }
            stepHop(dt)
            stepDissolve(now, dt)
            stepParticles(now, dt)
            pushWindow()
        } catch (_: Exception) {
        }
        view.invalidate()
    }

    private fun stepGround(now: Long, dt: Float) {
        if (state == STATE_WALK) {
            val dir = if (walkTarget > x) 1 else -1
            look = dir
            x += dir * unit * WALK_SPEED * dt
            val reached = (dir > 0 && x >= walkTarget) || (dir < 0 && x <= walkTarget)
            val hitEdge = (dir < 0 && x <= 0f) || (dir > 0 && x >= maxX())
            if (reached || hitEdge) {
                x = x.coerceIn(0f, maxX())
                state = STATE_IDLE
                nextAction = now + 2500L + random.nextLong(3500)
                savePos()
            }
            return
        }

        if (state == STATE_SLEEP) {
            if (now >= nextZ) {
                nextZ = now + 1600L
                spawnParticle("zzz")
            }
            if (now >= nextDream) {
                nextDream = now + 24000L + random.nextLong(18000)
                say("dream", DREAM_MS)
            }
            return
        }

        if (now >= nextBlink) {
            blinkUntil = now + 130L
            nextBlink = now + if (random.nextFloat() < 0.15f) 350L else 2000L + random.nextLong(3500)
        }

        if (state == STATE_PET) {
            if (now >= petUntil) {
                state = STATE_IDLE
                lastInteraction = now
                setExpr("happy", 2200L)
                nextAction = now + 3000L + random.nextLong(3000)
            } else if (now >= heartNext) {
                heartNext = now + 450L
                spawnParticle("heart")
            }
            return
        }

        val timeout = prefs.sleepSeconds
        if (timeout > 0 && now - lastInteraction > timeout * 1000L && !hopActive && !dancing) {
            state = STATE_SLEEP
            look = 0
            nextZ = now + 600L
            nextDream = now + 10000L + random.nextLong(8000)
            bubbleUntil = 0L
            dissolveTarget = 1f
            say("sleep", 2000L)
            return
        }

        if (prefs.gravity && y < maxY() - 0.5f && !hopActive && !dancing) {
            startFall(0f)
            return
        }

        if (dancing) {
            if (!hopActive && now >= danceNext) {
                danceNext = now + DANCE_BEAT_MS
                hop(45f)
                if (random.nextFloat() < 0.2f) spawnParticle("spark")
            }
            return
        }

        if (now < nextAction || hopActive) return
        nextAction = now + 3500L + random.nextLong(5500)

        // экран выключен - питомец не шумит (его видно только в зеркале/scrcpy)
        if (!screenActive) return

        if (!prefs.wander) {
            look = random.nextInt(3) - 1
            return
        }

        when (random.nextFloat()) {
            in 0.0f..0.45f -> {
                val distance = (unit * 10f + random.nextFloat() * unit * 25f) * (if (random.nextBoolean()) 1 else -1)
                val target = (x + distance).coerceIn(0f, maxX())
                if (abs(target - x) > unit * 2f) {
                    walkTarget = target
                    state = STATE_WALK
                }
            }
            in 0.45f..0.65f -> look = random.nextInt(3) - 1
            in 0.65f..0.8f -> say("idle")
            in 0.8f..0.92f -> hop(50f)
            else -> {
                waveUntil = now + 1600L
                if (random.nextFloat() < 0.5f) say("wave", 2000L)
            }
        }
    }

    private fun stepFly(dt: Float) {
        x += vx * dt
        y += vy * dt
        if (prefs.gravity) {
            vy += flyGravity * dt
            vx *= exp(-0.4f * dt)
        } else {
            val friction = exp(-2.0f * dt)
            vx *= friction
            vy *= friction
        }
        if (x < minX()) {
            x = minX()
            vx = abs(vx) * 0.55f
            squashUntil = uptime() + 80L
        } else if (x > maxX()) {
            x = maxX()
            vx = -abs(vx) * 0.55f
            squashUntil = uptime() + 80L
        }
        if (y < minX()) {
            y = minX()
            vy = abs(vy) * 0.55f
        }
        val bottom = maxY()
        if (y > bottom) {
            y = bottom
            if (prefs.gravity && vy < landSpeed) {
                land()
            } else {
                vy = -abs(vy) * 0.45f
                vx *= 0.8f
                squashUntil = uptime() + 100L
            }
        }
        if (!prefs.gravity && hypot(vx, vy) < unit * 7.5f) {
            land()
        }
    }

    private fun land() {
        state = STATE_IDLE
        vx = 0f
        vy = 0f
        squashUntil = uptime() + 140L
        nextAction = uptime() + 2000L + random.nextLong(3000)
        lastInteraction = uptime()
        savePos()
        if (random.nextFloat() < 0.12f) {
            setExpr("dizzy", 1400L)
            say("dizzy")
        }
    }

    private fun stepFlee(dt: Float) {
        fleeRise += 2f * unit * dt
        x += sin(uptime() / 260f) * unit * 1.5f * dt
        x = x.coerceIn(0f, max(0f, maxX()))
        if (nowBlockAllowed()) spawnParticle("block")
        if (dissolve >= 1f && fleeRise > unit * 1.2f) {
            hideForFlee()
        }
    }

    private fun hideForFlee() {
        if (!added) return
        savePos()
        handler.removeCallbacks(tickRunnable)
        try {
            wm.removeView(hitView)
        } catch (_: Exception) {
        }
        try {
            wm.removeView(view)
        } catch (_: Exception) {
        }
        added = false
        handler.removeCallbacks(returnRunnable)
        handler.postDelayed(returnRunnable, FLEE_HIDE_MS)
    }

    private fun stepHop(dt: Float) {
        if (!hopActive) return
        hopOff += hopVy * dt
        hopVy += hopGravity * dt
        val maxRise = maxRise()
        if (hopOff < -maxRise) {
            hopOff = -maxRise
            if (hopVy < 0f) hopVy = 0f
        }
        if (hopOff >= 0f) {
            hopOff = 0f
            hopVy = 0f
            hopActive = false
            squashUntil = uptime() + 100L
        }
    }

    private fun stepDissolve(now: Long, dt: Float) {
        if (dissolve == dissolveTarget) return
        val prev = dissolve
        val speed = if (dissolveTarget > dissolve) DISSOLVE_SPEED else CONDENSE_SPEED
        dissolve = if (dissolve < dissolveTarget) {
            min(dissolveTarget, dissolve + speed * dt)
        } else {
            max(dissolveTarget, dissolve - speed * dt)
        }
        if (dissolve > prev && now >= blockNext) {
            blockNext = now + 40L
            spawnDissolveBlocks(prev, dissolve)
        }
    }

    private fun spawnDissolveBlocks(from: Float, to: Float) {
        val bodyColor = Sprite.bodyColor(prefs.color)
        for (cy in 1..12) {
            for (cx in 2..16) {
                val h = cellHash(cx, cy)
                if (h < from || h >= to) continue
                val px = margin + cx * unit
                val py = bodyTop() + bodyRise() + cy * unit
                particles.add(
                    Particle(
                        "block",
                        px,
                        py,
                        (random.nextFloat() * 0.6f - 0.3f) * unit,
                        -(random.nextFloat() * 30f + 25f) * unit / 8f,
                        uptime(),
                        900L,
                        unit * 0.8f,
                        unit * 0.8f,
                    ),
                )
            }
        }
        if (particles.size > 90) {
            while (particles.size > 90) particles.removeAt(0)
        }
        paintBody.color = bodyColor
    }

    private fun cellHash(cx: Int, cy: Int): Float {
        var h = cx * 374761393 + cy * 668265263
        h = (h xor (h shr 13)) * 1274126177
        h = h xor (h shr 16)
        return (h and 0x7FFFFFFF).toFloat() / 0x7FFFFFFF.toFloat()
    }

    private fun stepParticles(now: Long, dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy *= exp(-1.5f * dt)
            if (now - p.born > p.ttl) it.remove()
        }
    }

    private fun nowBlockAllowed(): Boolean = random.nextFloat() < 0.35f

    private fun spawnParticle(type: String) {
        if (type == "block") {
            val bodyColor = Sprite.bodyColor(prefs.color)
            particles.add(
                Particle(
                    "block",
                    margin + random.nextFloat() * bodyW,
                    bodyTop() + bodyRise() + bodyH * 0.6f,
                    (random.nextFloat() * 0.6f - 0.3f) * unit,
                    -(random.nextFloat() * 30f + 25f) * unit / 8f,
                    uptime(),
                    900L,
                    unit * 0.8f,
                    unit * 0.8f,
                ),
            )
            paintBody.color = bodyColor
            if (particles.size > 90) particles.removeAt(0)
            return
        }
        val rows = Sprite.particleRows(type) ?: return
        val scale = Sprite.particleScale(type, unit)
        val pw = rows[0].length * scale.toFloat()
        val ph = rows.size * scale.toFloat()
        val ttl = if (type == "zzz") 1600L else 1100L
        val drift: Float
        val sx: Float
        val sy: Float
        if (type == "zzz") {
            sx = margin + bodyW * 0.72f
            sy = bodyTop() + bodyRise() + unit * 2f
            drift = unit * 3f
        } else {
            sx = margin + bodyW / 2f - pw / 2f + random.nextFloat() * bodyW * 0.5f - bodyW * 0.25f
            sy = bodyTop() + bodyRise() + unit * 2f - ph
            drift = (random.nextFloat() * 4f - 2f) * unit
        }
        val vy = -(random.nextFloat() * 20f + 40f) * unit / 8f
        particles.add(Particle(type, sx, sy, drift * 1000f / ttl, vy, uptime(), ttl, pw, ph))
        if (particles.size > 18) particles.removeAt(0)
    }

    private fun hop(strength: Float) {
        if (state != STATE_IDLE && state != STATE_WALK) return
        val maxRise = maxRise()
        var v = unit * strength
        if (hopActive) {
            val allowed = maxRise + hopOff
            if (allowed <= 0f) return
            val cap = sqrt(2f * hopGravity * allowed)
            if (v > cap) v = cap
        }
        hopActive = true
        hopVy = -v
    }

    private fun maxRise(): Float = min(unit * 9f, margin + bubbleH + unit * 2f)

    private fun startFall(vx0: Float) {
        state = STATE_FLY
        vx = vx0
        vy = 0f
    }

    private fun wake() {
        state = STATE_IDLE
        look = 0
        bubbleUntil = 0L
        nextDream = uptime() + 20000L + random.nextLong(20000)
        setExpr("surprised", 900L)
        say("wake")
        lastInteraction = uptime()
        dissolveTarget = 0f
    }

    private fun setExpr(value: String, duration: Long) {
        expr = value
        exprUntil = uptime() + duration
    }

    private fun tap() {
        lastInteraction = uptime()
        val now = uptime()
        val doubleTap = now - lastTap < 320L
        lastTap = now
        tapTimes.removeAll { now - it > TAP_WINDOW_MS }
        tapTimes.add(now)
        if (tapTimes.size >= FLEE_TAPS) {
            tapTimes.clear()
            flee()
            return
        }
        if (tapTimes.size >= ANNOYED_TAPS) {
            say("annoyed")
            squashUntil = now + 150L
            setExpr("angry", 2500L)
            return
        }
        if (doubleTap) {
            hop(80f)
            setExpr("happy", 1300L)
            repeat(3) { spawnParticle("spark") }
            if (random.nextFloat() < 0.4f) say("jump")
            return
        }
        say("tap")
        squashUntil = now + 120L
        setExpr("surprised", 450L)
        hop(40f)
    }

    private fun flee() {
        say("flee")
        setExpr("angry", 2000L)
        state = STATE_FLEE
        hopActive = false
        hopOff = 0f
        hopVy = 0f
        vx = 0f
        vy = 0f
        fleeRise = 0f
        dissolve = 0f
        dissolveTarget = 1f
        tapTimes.clear()
        lastTap = 0L
    }

    private fun say(category: String, duration: Long = BUBBLE_MS) {
        if (!prefs.phrases) return
        val options = Phrases.get(prefs.lang)[category] ?: return
        if (options.isEmpty()) return
        bubbleText = options[random.nextInt(options.size)]
        bubbleUntil = uptime() + duration
    }

    /** Публичная «речь» для сервиса: напоминания, жалобы тамагочи. */
    fun speak(category: String, duration: Long = BUBBLE_MS) = say(category, duration)

    /** Реакция на встряску телефона. */
    fun scare() {
        if (state == STATE_FLEE) return
        if (state == STATE_SLEEP) wake()
        hop(85f)
        setExpr("surprised", 1200L)
        squashUntil = uptime() + 120L
        say("shake")
    }

    private var menuView: android.widget.LinearLayout? = null
    private val menuDismiss = Runnable { hideMenu() }
    var menuRequest: (() -> Unit)? = null
    private val menuRunnable = Runnable {
        if (added && state != STATE_FLEE) menuRequest?.invoke()
    }

    /** Небольшое всплывающее меню над питомцем: (подпись, ключ) → колбэк. */
    fun showMenu(items: List<Pair<String, String>>, onPick: (String) -> Unit) {
        hideMenu()
        val bg = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xF21C1D22.toInt())
            cornerRadius = 14f * density
            setStroke(1, 0x33FFFFFF)
        }
        val ll = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            background = bg
            setPadding((10f * density).toInt(), (6f * density).toInt(), (10f * density).toInt(), (6f * density).toInt())
        }
        for ((label, key) in items) {
            ll.addView(
                android.widget.TextView(context).apply {
                    text = label
                    textSize = 14f
                    setTextColor(0xFFEDEDED.toInt())
                    setPadding(
                        (12f * density).toInt(),
                        (12f * density).toInt(),
                        (12f * density).toInt(),
                        (12f * density).toInt(),
                    )
                    setOnClickListener {
                        hideMenu()
                        onPick(key)
                    }
                },
            )
        }
        val wSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        ll.measure(wSpec, hSpec)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = (x + viewW / 2f - ll.measuredWidth / 2f).roundToInt()
            .coerceIn(0, max(0, dm.widthPixels - ll.measuredWidth))
        lp.y = (y + bodyTop() - 10f * density - ll.measuredHeight).roundToInt()
            .coerceIn(0, max(0, dm.heightPixels - ll.measuredHeight))
        try {
            wm.addView(ll, lp)
            menuView = ll
            handler.postDelayed(menuDismiss, 7000L)
        } catch (_: Exception) {
        }
    }

    fun hideMenu() {
        handler.removeCallbacks(menuDismiss)
        menuView?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        menuView = null
    }

    fun setDance(active: Boolean) {
        val want = active && prefs.dance
        if (dancing == want) return
        dancing = want
        if (want) {
            if (state == STATE_SLEEP) state = STATE_IDLE
            if (dissolve != 0f || dissolveTarget != 0f) {
                dissolve = 0f
                dissolveTarget = 0f
            }
            lastInteraction = uptime()
            danceNext = uptime() + DANCE_BEAT_MS / 2
            say("dance")
        }
    }

    private fun bodyTop(): Float = margin + bubbleH

    private fun bodyRise(): Float = hopOff - fleeRise

    // The view is larger than the drawn body (margins + bubble space), so the window can
    // go "off-screen" by one margin: that is what lets the pet body itself touch the
    // screen edges, including the area right above the navigation bar.
    private fun minX(): Float = -margin

    private fun maxX(): Float = dm.widthPixels - viewW + margin

    private val navBarHeight: Int by lazy {
        val res = context.resources
        val id = res.getIdentifier("navigation_bar_height", "dimen", "android")
        if (id > 0) res.getDimensionPixelSize(id) else (48f * density).toInt()
    }

    private fun maxY(): Float =
        max(minX(), dm.heightPixels - navBarHeight - viewH + margin)

    private fun clampXY() {
        x = x.coerceIn(minX(), max(minX(), maxX()))
        y = y.coerceIn(minX(), max(minX(), maxY()))
    }

    private fun restorePos() {
        clampXY()
        if (prefs.positionSaved()) {
            x = prefs.posX.toFloat().coerceIn(minX(), max(minX(), maxX()))
            y = prefs.posY.toFloat().coerceIn(minX(), max(minX(), maxY()))
        } else {
            x = max(minX(), maxX() - unit * 3f)
            y = max(minX(), maxY())
        }
    }

    private fun savePos() {
        prefs.posX = x.roundToInt()
        prefs.posY = y.roundToInt()
    }

    private fun pushWindow() {
        val nx = x.roundToInt()
        val ny = y.roundToInt()
        if (lp.x != nx || lp.y != ny) {
            lp.x = nx
            lp.y = ny
            if (added) {
                try {
                    wm.updateViewLayout(view, lp)
                } catch (_: Exception) {
                }
            }
        }
        pushHitWindow()
    }

    /**
     * Геометрия интерактивного окна: оно должно точно совпадать с нарисованным питомцем,
     * иначе пустые зоны перехватывали бы тапы по приложениям под ним.
     *
     * Координаты — в клетках сетки 18x13 (см. sprite.py):
     *  - тело с лапами занимает клетки 1..17 по X и 2..13 по Y;
     *  - спящая кучка — клетки 4..14 по X и 8..11 по Y.
     */
    private fun updateHitGeometry() {
        val rot = rotationDeg()
        currentRot = rot
        val u = unit
        val (cx0, cy0, cw, ch) = if (dissolve >= 1f && state != STATE_FLEE) {
            floatArrayOf(4f, 8f, 10f, 3f)
        } else {
            floatArrayOf(1f, 2f, 16f, 11f)
        }
        val rad = Math.toRadians(rot.toDouble())
        val cos = abs(kotlin.math.cos(rad).toFloat())
        val sin = abs(kotlin.math.sin(rad).toFloat())
        val wCells = cw * cos + ch * sin
        val hCells = cw * sin + ch * cos
        // при наклоне габариты растут — добавляем поля, чтобы питомец не обрезался
        hitPadX = (wCells - cw) / 2f * u
        hitPadY = (hCells - ch) / 2f * u
        hitW = wCells * u
        hitH = hCells * u
        // смещение начала сетки относительно начала hit-окна
        hitOx = cx0 * u - hitPadX
        hitOy = cy0 * u - hitPadY
    }

    private fun pushHitWindow() {
        updateHitGeometry()
        val rise = hopOff - fleeRise
        val gx = x + margin
        val gy = y + bodyTop() + rise
        val hx = (gx + hitOx).roundToInt()
        val hy = (gy + hitOy).roundToInt()
        val newW = hitW.roundToInt().coerceAtLeast(1)
        val newH = hitH.roundToInt().coerceAtLeast(1)
        val sizeChanged = newW != hitLp.width || newH != hitLp.height
        if (sizeChanged) {
            hitLp.width = newW
            hitLp.height = newH
            hitView.requestLayout()
        }
        if (hitLp.x == hx && hitLp.y == hy && !sizeChanged) return
        hitLp.x = hx
        hitLp.y = hy
        if (added) {
            try {
                wm.updateViewLayout(hitView, hitLp)
            } catch (_: Exception) {
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleTouch(event: MotionEvent): Boolean {
        if (state == STATE_FLEE) return true
        val rawX = event.rawX
        val rawY = event.rawY
        val now = uptime()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downT = now
                prevRawX = rawX
                prevRawY = rawY
                downRawX = rawX
                downRawY = rawY
                downWinX = x
                downWinY = y
                moved = false
                petted = false
                dragSaid = false
                dragVx = 0f
                touchHist.clear()
                touchHist.add(Sample(now, rawX, rawY))
                lastInteraction = now
                if (state == STATE_SLEEP) wake()
                handler.postDelayed(petRunnable, PET_HOLD_MS)
                handler.postDelayed(menuRunnable, MENU_HOLD_MS)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = rawX - prevRawX
                val dy = rawY - prevRawY
                if (!moved && (abs(rawX - downRawX) > TOUCH_SLOP * density || abs(rawY - downRawY) > TOUCH_SLOP * density)) {
                    moved = true
                    handler.removeCallbacks(petRunnable)
                    handler.removeCallbacks(menuRunnable)
                    state = STATE_DRAG
                    if (!dragSaid) {
                        dragSaid = true
                        say("drag", DRAG_MS)
                    }
                }
                if (moved) {
                    x += dx
                    y += dy
                    clampXY()
                    pushWindow()
                    lastInteraction = now
                    touchHist.add(Sample(now, rawX, rawY))
                    while (touchHist.size > 2 && now - touchHist[0].t > 160L) {
                        touchHist.removeAt(0)
                    }
                    if (touchHist.size >= 2) {
                        val a = touchHist[touchHist.size - 2]
                        val b = touchHist[touchHist.size - 1]
                        val dt = (b.t - a.t) / 1000f
                        if (dt > 0.005f) dragVx = (b.x - a.x) / dt
                    }
                }
                prevRawX = rawX
                prevRawY = rawY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(petRunnable)
                handler.removeCallbacks(menuRunnable)
                val cancelled = event.actionMasked == MotionEvent.ACTION_CANCEL
                val dtSec = (now - downT) / 1000f
                if (cancelled) {
                    state = STATE_IDLE
                    lastInteraction = now
                    savePos()
                } else if (petted) {
                    state = STATE_IDLE
                    lastInteraction = now
                    setExpr("happy", 2200L)
                    hop(40f)
                } else if (!moved && dtSec < 0.35f) {
                    tap()
                } else if (moved) {
                    var rvx = 0f
                    var rvy = 0f
                    if (touchHist.size >= 2) {
                        val a = touchHist[0]
                        val b = touchHist[touchHist.size - 1]
                        val dt = (b.t - a.t) / 1000f
                        if (dt > 0.005f && now - b.t < 120L) {
                            rvx = (b.x - a.x) / dt
                            rvy = (b.y - a.y) / dt
                        }
                    }
                    rvx = rvx.coerceIn(-flyLimit, flyLimit)
                    rvy = rvy.coerceIn(-flyLimit, flyLimit)
                    val speed = hypot(rvx, rvy)
                    if (speed > flyMin) {
                        state = STATE_FLY
                        vx = rvx
                        vy = rvy
                        if (speed > throwSaySpeed) say("throw", DRAG_MS)
                    } else if (prefs.gravity && y < maxY() - 2f * density) {
                        state = STATE_FLY
                        vx = rvx
                        vy = rvy
                    } else {
                        state = STATE_IDLE
                        lastInteraction = now
                        savePos()
                    }
                }
            }
        }
        return true
    }

    private fun rotationDeg(): Float {
        val now = uptime()
        if (state == STATE_FLY) {
            return (vx / unit / 5.6f).coerceIn(-25f, 25f)
        }
        if (state == STATE_FLEE) return 0f
        if (expr == "dizzy" && now < exprUntil) {
            return sin(now / 1000f * 10f) * 7f
        }
        if (state == STATE_DRAG && dragVx != 0f) {
            return (dragVx / unit / 5.6f).coerceIn(-18f, 18f)
        }
        return 0f
    }

    private fun drawPetBody(canvas: Canvas) {
        val now = uptime()
        var eyes = "normal"
        var arms = "normal"
        var legs = "stand"
        var squash = 0
        var blush = false
        var mouth: String? = null

        when (state) {
            STATE_DRAG, STATE_FLY, STATE_FLEE -> {
                eyes = "surprised"
                arms = "up"
                legs = "dangle"
            }
            STATE_PET -> {
                eyes = "happy"
                blush = true
                squash = if ((now / 333L) % 2L == 0L) 1 else 0
                look = 0
            }
            STATE_SLEEP -> {
                eyes = "sleep"
                arms = "tuck"
                legs = "tuck"
                look = 0
            }
            STATE_WALK -> legs = if ((now / 143L) % 2L == 0L) "walk1" else "walk2"
            else -> {}
        }

        if ((state == STATE_IDLE || state == STATE_WALK) && now < waveUntil) {
            arms = if ((now / 200L) % 2L == 0L) "wave1" else "wave2"
        }

        if (dancing && (state == STATE_IDLE || state == STATE_WALK)) {
            eyes = "happy"
            blush = true
            look = 0
            val beat = (now / (DANCE_BEAT_MS / 2)) % 2L == 0L
            squash = if (beat) 1 else 0
            arms = if (beat) "wave1" else "wave2"
            legs = if (beat) "dangle" else "stand"
        }

        if (hopActive && (state == STATE_IDLE || state == STATE_WALK)) {
            legs = "dangle"
            if (hopVy < 0f) {
                squash = -1
                arms = "up"
            }
        }

        if (expr.isNotEmpty() && now < exprUntil && state != STATE_SLEEP) {
            eyes = expr
            if (expr == "happy") blush = true
        }
        if (now < squashUntil && squash == 0) squash = 1
        if (eyes == "normal" && (state == STATE_IDLE || state == STATE_WALK) && now < blinkUntil) {
            eyes = "blink"
        }
        if (legs == "tuck" && squash != 0) squash = 0
        if (state != STATE_SLEEP && bubbleUntil > now) mouth = "open"

        // hit-окно начинается с клетки hitOx/hitOy сетки 18x13,
        // поэтому сетку рисуем со сдвигом на минус эти смещения
        val ox = -hitOx
        val oy = -hitOy
        val rot = currentRot
        if (rot != 0f) {
            canvas.save()
            canvas.rotate(rot, hitW / 2f, hitH / 2f)
        }

        if (dissolve >= 1f && state != STATE_FLEE) {
            drawSleepPile(canvas, ox, oy)
        } else {
            drawSprite(canvas, ox, oy, eyes, arms, legs, squash, blush, mouth)
        }

        if (rot != 0f) canvas.restore()
    }

    private fun drawPetEffects(canvas: Canvas) {
        val now = uptime()
        for (p in particles) {
            val age = (now - p.born) / p.ttl.toFloat()
            paintParticle.alpha = ((1f - age) * 255f).roundToInt().coerceIn(0, 255)
            drawParticle(canvas, p)
        }
        paintParticle.alpha = 255

        drawBubble(canvas, now)
    }

    private fun drawSleepPile(canvas: Canvas, ox: Float, oy: Float) {
        val bodyColor = Sprite.bodyColor(prefs.color)
        val eyeColor = Sprite.eyeColor(prefs.color)
        val u = unit
        val baseY = oy + bodyH * 0.78f

        fun cell(cx: Int, cy: Int, color: Int) {
            paintBody.color = color
            canvas.drawRect(ox + cx * u, baseY + cy * u, ox + (cx + 1) * u, baseY + (cy + 1) * u, paintBody)
        }

        for (cx in 4..13) cell(cx, 0, bodyColor)
        for (cx in 5..12) cell(cx, -1, bodyColor)
        for (cx in 7..10) cell(cx, -2, bodyColor)
        cell(7, -1, eyeColor)
        cell(10, -1, eyeColor)
        cell(6, 0, eyeColor)
        cell(11, 0, eyeColor)
    }

    private fun drawParticle(canvas: Canvas, p: Particle) {
        if (p.type == "block") {
            paintParticle.color = Sprite.bodyColor(prefs.color)
            canvas.drawRect(p.x, p.y, p.x + p.w, p.y + p.h, paintParticle)
            return
        }
        val rows = Sprite.particleRows(p.type) ?: return
        val scale = Sprite.particleScale(p.type, unit)
        for (r in rows.indices) {
            val row = rows[r]
            for (c in row.indices) {
                val color = Sprite.particleColor(p.type, row[c]) ?: continue
                paintParticle.color = color
                canvas.drawRect(
                    p.x + c * scale,
                    p.y + r * scale,
                    p.x + (c + 1) * scale,
                    p.y + (r + 1) * scale,
                    paintParticle,
                )
            }
        }
    }

    private fun drawSprite(
        canvas: Canvas,
        ox: Float,
        oy: Float,
        eyes: String,
        arms: String,
        legs: String,
        squash: Int,
        blush: Boolean,
        mouth: String?,
    ) {
        val u = unit
        val bodyColor = Sprite.bodyColor(prefs.color)
        val eyeColor = Sprite.eyeColor(prefs.color)
        val blushColor = blend(bodyColor, 0xFFFF4F79.toInt(), 0.5f)

        fun px(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
            paintBody.color = color
            if (dissolve <= 0f) {
                canvas.drawRect(ox + x0 * u, oy + y0 * u, ox + x1 * u, oy + y1 * u, paintBody)
                return
            }
            for (cy in y0 until y1) {
                for (cx in x0 until x1) {
                    if (cellHash(cx, cy) < dissolve) continue
                    canvas.drawRect(
                        ox + cx * u,
                        oy + cy * u,
                        ox + (cx + 1) * u,
                        oy + (cy + 1) * u,
                        paintBody,
                    )
                }
            }
        }

        var left = 3
        var right = 15
        var top = 3
        var bottom = 10
        if (squash > 0) {
            left = 2
            right = 16
            top = 5
        } else if (squash < 0) {
            left = 4
            right = 14
            top = 2
        }
        if (legs == "tuck") {
            top += 1
            bottom += 1
        }

        val heights = when (legs) {
            "walk1" -> intArrayOf(1, 2, 1, 2)
            "walk2" -> intArrayOf(2, 1, 2, 1)
            "dangle" -> intArrayOf(3, 3, 3, 3)
            "tuck" -> intArrayOf(1, 1, 1, 1)
            else -> intArrayOf(2, 2, 2, 2)
        }
        val cols = intArrayOf(left + 1, left + 3, right - 4, right - 2)
        for (i in 0 until 4) {
            px(cols[i], bottom, cols[i] + 1, bottom + heights[i], bodyColor)
        }

        fun arm(side: Int, style: String) {
            when (style) {
                "up" -> if (side < 0) {
                    px(left - 2, top - 1, left - 1, top + 3, bodyColor)
                    px(left - 1, top + 2, left, top + 3, bodyColor)
                } else {
                    px(right + 1, top - 1, right + 2, top + 3, bodyColor)
                    px(right, top + 2, right + 1, top + 3, bodyColor)
                }
                "tuck" -> if (side < 0) {
                    px(left - 1, top + 3, left, top + 5, bodyColor)
                } else {
                    px(right, top + 3, right + 1, top + 5, bodyColor)
                }
                else -> if (side < 0) {
                    px(left - 2, top + 3, left, top + 5, bodyColor)
                } else {
                    px(right, top + 3, right + 2, top + 5, bodyColor)
                }
            }
        }

        var leftArm = arms
        var rightArm = arms
        if (arms == "wave1") {
            leftArm = "up"
            rightArm = "normal"
        } else if (arms == "wave2") {
            leftArm = "normal"
            rightArm = "up"
        }
        arm(-1, leftArm)
        arm(1, rightArm)

        px(left, top, right, bottom, bodyColor)

        val ey = top + 2
        for (side in intArrayOf(-1, 1)) {
            val cx = if (side < 0) left + 3 + look else right - 4 + look
            when (eyes) {
                "blink" -> px(cx, ey + 1, cx + 1, ey + 2, eyeColor)
                "happy" -> {
                    px(cx - 1, ey + 1, cx, ey + 2, eyeColor)
                    px(cx, ey, cx + 1, ey + 1, eyeColor)
                    px(cx + 1, ey + 1, cx + 2, ey + 2, eyeColor)
                }
                "surprised" -> px(cx, ey - 1, cx + 1, ey + 2, eyeColor)
                "dizzy" -> {
                    px(cx - 1, ey - 1, cx, ey, eyeColor)
                    px(cx + 1, ey - 1, cx + 2, ey, eyeColor)
                    px(cx, ey, cx + 1, ey + 1, eyeColor)
                    px(cx - 1, ey + 1, cx, ey + 2, eyeColor)
                    px(cx + 1, ey + 1, cx + 2, ey + 2, eyeColor)
                }
                "sleep" -> px(cx - 1, ey + 1, cx + 2, ey + 2, eyeColor)
                "angry" -> {
                    px(cx, ey, cx + 1, ey + 2, eyeColor)
                    val brow = if (side < 0) cx - 1 else cx + 1
                    px(brow, ey - 1, brow + 1, ey, eyeColor)
                }
                else -> px(cx, ey, cx + 1, ey + 2, eyeColor)
            }
            if (blush) {
                if (side < 0) {
                    px(cx - 2, ey + 2, cx, ey + 3, blushColor)
                } else {
                    px(cx + 1, ey + 2, cx + 3, ey + 3, blushColor)
                }
            }
        }

        if (mouth != null) {
            val mx = (left + right) / 2
            val my = ey + 2
            if (mouth == "open") {
                px(mx - 1, my, mx + 1, max(my + 1, min(my + 2, bottom)), eyeColor)
            } else {
                px(mx - 1, my, mx + 1, my + 1, eyeColor)
            }
        }
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        val r = (ar + (br - ar) * t).toInt()
        val g = (ag + (bg - ag) * t).toInt()
        val bl = (ab + (bb - ab) * t).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }

    private fun drawBubble(canvas: Canvas, now: Long) {
        val text = bubbleText ?: return
        if (bubbleUntil <= now) return
        val pad = 8f * density
        val maxW = viewW - pad * 2f - 8f * density
        val lineH = paintText.textSize * 1.25f
        val lines = wrapText(text, maxW, 3)
        val widths = lines.map { measureWithHearts(it) }
        val w = min(maxW, (widths.maxOrNull() ?: 0f) + pad * 2f)
        val h = lines.size * lineH + pad * 1.6f
        val left = (viewW - w) / 2f
        val petTop = bodyTop() + bodyRise()
        var bottomEdge = min(margin + bubbleH - 2f * density, petTop - 2f * density)
        if (bottomEdge - h < 0f) bottomEdge = h
        val top = bottomEdge - h
        val rect = RectF(left, top, left + w, bottomEdge)
        canvas.drawRoundRect(rect, unit * 2.2f, unit * 2.2f, paintBubble)
        canvas.drawRoundRect(rect, unit * 2.2f, unit * 2.2f, paintBubbleStroke)

        val tail = android.graphics.Path()
        val tailX = margin + bodyW / 2f
        tail.moveTo(tailX - unit * 1.2f, rect.bottom - 1f)
        tail.lineTo(tailX + unit * 1.2f, rect.bottom - 1f)
        tail.lineTo(tailX, rect.bottom + unit * 1.4f)
        tail.close()
        canvas.drawPath(tail, paintBubble)
        canvas.drawPath(tail, paintBubbleStroke)

        var ly = top + pad * 0.8f + lineH * 0.85f
        for (line in lines) {
            val lineW = measureWithHearts(line)
            var lx = left + (w - lineW) / 2f
            lx = drawTextWithHearts(canvas, line, lx, ly)
            ly += lineH
        }
    }

    private fun measureWithHearts(text: String): Float {
        var w = 0f
        var run = StringBuilder()
        for (ch in text) {
            if (ch == '\u2665') {
                w += paintText.measureText(run.toString())
                run = StringBuilder()
                w += heartW
            } else {
                run.append(ch)
            }
        }
        w += paintText.measureText(run.toString())
        return w
    }

    private fun drawTextWithHearts(canvas: Canvas, text: String, x: Float, baseline: Float): Float {
        var cursor = x
        var run = StringBuilder()
        fun flush() {
            if (run.isEmpty()) return
            val s = run.toString()
            canvas.drawText(s, cursor, baseline, paintText)
            cursor += paintText.measureText(s)
            run = StringBuilder()
        }
        for (ch in text) {
            if (ch == '\u2665') {
                flush()
                drawHeartGlyph(canvas, cursor, baseline)
                cursor += heartW
            } else {
                run.append(ch)
            }
        }
        flush()
        return cursor
    }

    private fun drawHeartGlyph(canvas: Canvas, x: Float, baseline: Float) {
        val s = heartScale.toFloat()
        val h = heartRows.size
        val top = baseline - paintText.textSize * 0.9f
        for (r in 0 until h) {
            val row = heartRows[r]
            for (c in row.indices) {
                if (row[c] != 'X') continue
                paintParticle.color = Sprite.HEART_COLOR
                paintParticle.alpha = 255
                canvas.drawRect(
                    x + c * s,
                    top + r * s,
                    x + (c + 1) * s,
                    top + (r + 1) * s,
                    paintParticle,
                )
            }
        }
    }

    private fun wrapText(text: String, maxW: Float, maxLines: Int): List<String> {
        val words = text.split(" ")
        val lines = ArrayList<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else current.toString() + " " + word
            if (current.isEmpty() || measureWithHearts(candidate) <= maxW) {
                current.clear()
                current.append(candidate)
            } else {
                lines.add(current.toString())
                current = StringBuilder(word)
                if (lines.size == maxLines) break
            }
        }
        if (current.isNotEmpty() && lines.size < maxLines) lines.add(current.toString())
        if (lines.size > maxLines) lines.subList(maxLines, lines.size).clear()
        if (lines.isNotEmpty()) {
            val lastIdx = lines.size - 1
            lines[lastIdx] = TextUtils.ellipsize(lines[lastIdx], paintText, maxW, TextUtils.TruncateAt.END).toString()
        }
        return lines
    }
}
