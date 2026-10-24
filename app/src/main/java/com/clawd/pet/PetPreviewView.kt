package com.clawd.pet

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

class PetPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var unit = 0f
    private var colorIndex = 0
    private var sizeIndex = 1
    private var phase = 0f
    private var lastPuffStep = 0L
    private var running = true
    private var visible = true
    private var mode = MODE_ALIVE
    private var vigor = 1f
    private val puffs = ArrayList<Puff>()
    private val random = kotlin.random.Random.Default

    private class Puff(
        val x: Float,
        val y: Float,
        val vx: Float,
        val vy0: Float,
        val born: Long,
        val ttl: Long,
        val side: Boolean,
        val color: Int,
    ) {
        var px: Float = x
        var py: Float = y
        var vy: Float = vy0
    }

    companion object {
        const val MODE_ALIVE = 0
        const val MODE_SLEEP = 1
        const val MODE_DEAD = 2
        const val MODE_SAD = 3
        const val BURST_PLAY = 0
        const val BURST_FEED = 1
    }

    fun setPet(color: Int, size: Int) {
        colorIndex = color
        sizeIndex = size
        requestLayout()
        invalidate()
    }

    fun setRunning(value: Boolean) {
        running = value
        invalidate()
    }

    fun setMode(value: Int) {
        mode = value
        invalidate()
    }

    fun setVigor(value: Float) {
        vigor = value.coerceIn(0f, 1f)
    }

    fun burst(kind: Int) {
        val d = resources.displayMetrics.density
        val u = Sprite.unitDp(sizeIndex) * d
        val now = System.nanoTime() / 1_000_000L
        for (i in 0 until 7) {
            val fromTop = kind == BURST_PLAY
            puffs.add(
                Puff(
                    x = (width * (0.25f + random.nextFloat() * 0.5f)),
                    y = height * (if (fromTop) 0.30f else 0.62f) + u * (random.nextFloat() - 0.5f),
                    vx = (random.nextFloat() - 0.5f) * u * 3.2f,
                    vy0 = if (fromTop) -u * (1.6f + random.nextFloat() * 2f)
                    else -u * (0.6f + random.nextFloat() * 1.2f),
                    born = now,
                    ttl = 900L + random.nextLong(500L),
                    side = fromTop,
                    color = if (fromTop) 0xFFD97757.toInt() else 0xFF7BC996.toInt(),
                ),
            )
        }
        invalidate()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        visible = visibility == View.VISIBLE
        invalidate()
    }

    override fun onDetachedFromWindow() {
        visible = false
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val d = resources.displayMetrics.density
        val u = Sprite.unitDp(sizeIndex) * d
        val bodyW = u * Sprite.GRID_W
        val bodyH = u * Sprite.GRID_H
        val pad = max(bodyW, bodyH) * 0.12f
        setMeasuredDimension(
            resolveSize((bodyW + pad * 2f).roundToInt(), widthSpec),
            resolveSize((bodyH + pad * 2f).roundToInt(), heightSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val alive = mode == MODE_ALIVE || mode == MODE_SAD
        val sleeping = mode == MODE_SLEEP
        if ((alive || sleeping) && running && visible) {
            // 60 fps, но фаза идёт тем же темпом (цикл 3 с): шаг 1/3 за кадр.
            phase = (phase + 1f / 3f) % 60f
            postInvalidateDelayed(if (alive) 16L else 40L)
        }
        val d = resources.displayMetrics.density
        val u = Sprite.unitDp(sizeIndex) * d
        unit = u
        val dead = mode == MODE_DEAD
        var bodyColor = Sprite.bodyColor(colorIndex)
        var eyeColor = Sprite.eyeColor(colorIndex)
        val blushColor = blend(bodyColor, 0xFFFF4F79.toInt(), 0.5f)
        if (dead) {
            bodyColor = blend(bodyColor, 0xFF26282E.toInt(), 0.68f)
            eyeColor = blend(eyeColor, 0xFF26282E.toInt(), 0.68f)
        }
        val bodyW = u * Sprite.GRID_W
        val bodyH = u * Sprite.GRID_H
        val ox = (width - bodyW) / 2f
        val oy = (height - bodyH) / 2f
        val animating = alive && mode == MODE_ALIVE
        val bounce = if (animating) {
            -abs(sin(phase / 60.0 * Math.PI * 2.0)).toFloat() * u * (0.25f + 0.35f * vigor)
        } else {
            0f
        }
        val blink = animating && phase in 30f..33f
        val walk = animating && phase < 30f
        val zzz = mode == MODE_SLEEP


        fun px(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
            paint.color = color
            canvas.drawRect(
                ox + x0 * u,
                oy + y0 * u + bounce,
                ox + x1 * u,
                oy + y1 * u + bounce,
                paint,
            )
        }

        val left = 3
        val right = 15
        val top = 3
        val bottom = 10
        val heights = if (walk) {
            if (phase < 15) intArrayOf(1, 2, 1, 2) else intArrayOf(2, 1, 2, 1)
        } else {
            intArrayOf(2, 2, 2, 2)
        }
        val cols = intArrayOf(left + 1, left + 3, right - 4, right - 2)
        for (i in 0 until 4) {
            px(cols[i], bottom, cols[i] + 1, bottom + heights[i], bodyColor)
        }
        px(left - 2, top + 3, left, top + 5, bodyColor)
        px(right, top + 3, right + 2, top + 5, bodyColor)
        px(left, top, right, bottom, bodyColor)

        val ey = top + 2
        for (side in intArrayOf(-1, 1)) {
            val cx = if (side < 0) left + 3 else right - 4
            when {
                dead -> {
                    px(cx, ey, cx + 1, ey + 1, eyeColor)
                    px(cx + 1, ey + 1, cx + 2, ey + 2, eyeColor)
                    px(cx + 1, ey, cx + 2, ey + 1, eyeColor)
                    px(cx, ey + 1, cx + 1, ey + 2, eyeColor)
                }
                blink || mode == MODE_SLEEP -> px(cx, ey + 1, cx + 1, ey + 2, eyeColor)
                else -> px(cx, ey, cx + 1, ey + 2, eyeColor)
            }
            if (!dead) {
                if (side < 0) px(cx - 2, ey + 2, cx, ey + 3, blushColor)
                else px(cx + 1, ey + 2, cx + 3, ey + 3, blushColor)
            }
        }

        paint.color = if (dead) blend(eyeColor, 0xFF26282E.toInt(), 0.4f) else eyeColor
        when {
            dead -> px(8, top + 6, 10, top + 7, paint.color)
            mode == MODE_SAD -> {
                px(7, top + 6, 8, top + 7, paint.color)
                paint.color = eyeColor
                px(8, top + 7, 10, top + 8, paint.color)
            }
            else -> px(7, top + 6, 11, top + 7, paint.color)
        }


        if (zzz && running && visible) {
            paint.color = eyeColor
            paint.textSize = u * 3.2f
            paint.isFakeBoldText = true
            val bob = sin(phase / 60.0 * Math.PI * 2.0).toFloat()
            canvas.drawText("Z", right * u - u * 1.2f, top * u + u * 1.2f + bob * u * 0.8f, paint)
            canvas.drawText("z", right * u + u * 1.4f, top * u - u * 0.2f + bob * u * 0.8f, paint)
            paint.isFakeBoldText = false
        }

        if (puffs.isNotEmpty()) {
            val now = System.nanoTime() / 1_000_000L
            // Частицы привязаны к времени: при 60 fps шаг в 3 раза меньше.
            val dtF = if (lastPuffStep == 0L) 1f
            else ((now - lastPuffStep).coerceIn(1L, 100L)) / 50f
            lastPuffStep = now
            val it = puffs.iterator()
            while (it.hasNext()) {
                val p = it.next()
                val age = (now - p.born).toFloat()
                if (age > p.ttl) {
                    it.remove()
                    continue
                }
                val t = age / 20f
                p.px += (p.vx / 60f) * t * dtF
                p.py += (p.vy / 60f) * t * dtF
                p.vy += (if (p.side) 0.18f * u * t else -0.06f * u * t) * dtF
                paint.color = p.color
                paint.alpha = (255 * (1f - age / p.ttl)).toInt().coerceIn(0, 255)
                canvas.drawRect(p.px - u * 0.7f, p.py - u * 0.7f, p.px + u * 0.7f, p.py + u * 0.7f, paint)
                paint.alpha = 255
            }
            if (visible) postInvalidateDelayed(16L)
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
}
