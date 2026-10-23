package com.clawd.pet

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import kotlin.math.abs

/**
 * Разовый подбор окраса питомца под доминирующий цвет обоев.
 * Вызывается только по нажатию кнопки — ни таймеров, ни фоновой работы.
 */
object WallpaperPalette {

    /** Индекс ближайшей палитры Sprite к доминирующему цвету обоев, или null. */
    fun matchColorIndex(context: Context): Int? {
        return try {
            val wm = WallpaperManager.getInstance(context)
            val bmp = (wm.drawable as? BitmapDrawable)?.bitmap ?: return null
            val dominant = dominantColor(bmp) ?: return null
            var best = 0
            var bestDist = Int.MAX_VALUE
            for (i in 0 until Sprite.count()) {
                val c = Sprite.bodyColor(i)
                val d = abs(((c shr 16) and 0xFF) - ((dominant shr 16) and 0xFF)) +
                    abs(((c shr 8) and 0xFF) - ((dominant shr 8) and 0xFF)) +
                    abs((c and 0xFF) - (dominant and 0xFF))
                if (d < bestDist) {
                    bestDist = d
                    best = i
                }
            }
            best
        } catch (_: Exception) {
            null
        }
    }

    /** Доминирующий цвет: даунскейл до 24x24 и подсчёт по квантованным бакетам. */
    private fun dominantColor(src: Bitmap): Int? {
        val small = Bitmap.createScaledBitmap(src, 24, 24, true)
        val buckets = HashMap<Int, Long>() // key: quantized rgb, value: packed sums+count
        for (y in 0 until small.height) {
            for (x in 0 until small.width) {
                val c = small.getPixel(x, y)
                if ((c ushr 24) < 128) continue
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val key = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
                val prev = buckets[key] ?: 0L
                buckets[key] = prev + ((r.toLong() shl 40) or (g.toLong() shl 24) or (b.toLong() shl 8) or 1L)
            }
        }
        if (buckets.isEmpty()) return null
        // крупнейший бакет → средний цвет
        val best = buckets.maxByOrNull { it.value and 0xFFL }?.value ?: return null
        val count = best and 0xFFL
        if (count == 0L) return null
        val r = ((best shr 40) and 0xFFL).toInt() / count.toInt()
        val g = ((best shr 24) and 0xFFL).toInt() / count.toInt()
        val b = ((best shr 8) and 0xFFL).toInt() / count.toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
