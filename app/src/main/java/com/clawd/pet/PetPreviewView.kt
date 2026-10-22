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
    private var phase = 0L
    private var running = true
    private var visible = true

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
        if (running && visible) {
            phase = (phase + 1) % 60
            postInvalidateDelayed(50L)
        }
        val d = resources.displayMetrics.density
        val u = Sprite.unitDp(sizeIndex) * d
        unit = u
        val bodyColor = Sprite.bodyColor(colorIndex)
        val eyeColor = Sprite.eyeColor(colorIndex)
        val blushColor = blend(bodyColor, 0xFFFF4F79.toInt(), 0.5f)
        val bodyW = u * Sprite.GRID_W
        val bodyH = u * Sprite.GRID_H
        val ox = (width - bodyW) / 2f
        val oy = (height - bodyH) / 2f
        val bounce = if (running) -abs(sin(phase / 60.0 * Math.PI * 2.0)).toFloat() * u * 0.6f else 0f
        val blink = running && phase in 30..32
        val walk = running && phase < 30

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
            if (blink) {
                px(cx, ey + 1, cx + 1, ey + 2, eyeColor)
            } else {
                px(cx, ey, cx + 1, ey + 2, eyeColor)
            }
            if (side < 0) px(cx - 2, ey + 2, cx, ey + 3, blushColor)
            else px(cx + 1, ey + 2, cx + 3, ey + 3, blushColor)
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
