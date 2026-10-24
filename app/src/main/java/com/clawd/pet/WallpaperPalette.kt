package com.clawd.pet

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
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
            val dominant = systemPrimaryColor(wm) ?: bitmapDominantColor(wm) ?: return null
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

    /** API 31+: системный цвет обоев напрямую — работает и с живыми обоями. */
    private fun systemPrimaryColor(wm: WallpaperManager): Int? {
        if (Build.VERSION.SDK_INT < 31) return null
        return try {
            wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Фолбэк: анализ битмапа обоев (API < 31 или цвета недоступны).
     * getDrawable требует READ_EXTERNAL_STORAGE на старых API, но у нас
     * minSdk 26 и разрешение не запрашивается — при отказе ловим SecurityException
     * и возвращаем null, поэтому доступ объявлен безопасным.
     */
    @SuppressLint("MissingPermission")
    private fun bitmapDominantColor(wm: WallpaperManager): Int? {
        val bmp = try {
            (wm.drawable as? BitmapDrawable)?.bitmap
        } catch (_: Exception) {
            null
        } ?: return null
        return dominantColor(bmp)
    }

    private class Acc {
        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0L
    }

    /** Доминирующий цвет: даунскейл до 24x24 и подсчёт по квантованным бакетам. */
    private fun dominantColor(src: Bitmap): Int? {
        val small = Bitmap.createScaledBitmap(src, 24, 24, true)
        val buckets = HashMap<Int, Acc>()
        for (y in 0 until small.height) {
            for (x in 0 until small.width) {
                val c = small.getPixel(x, y)
                if ((c ushr 24) < 128) continue
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val key = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
                val acc = buckets.getOrPut(key) { Acc() }
                acc.r += r
                acc.g += g
                acc.b += b
                acc.n += 1
            }
        }
        if (buckets.isEmpty()) return null
        // крупнейший бакет → средний цвет
        val best = buckets.values.maxByOrNull { it.n } ?: return null
        if (best.n == 0L) return null
        val r = (best.r / best.n).toInt()
        val g = (best.g / best.n).toInt()
        val b = (best.b / best.n).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
