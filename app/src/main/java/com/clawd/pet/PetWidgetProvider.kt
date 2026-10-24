package com.clawd.pet

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.RemoteViews

/**
 * Виджет домашнего экрана: состояние тамагочи + кнопка «Кормить».
 * Обновляется системным таймером (раз в 30 минут), по нажатию и при выходе
 * из экрана тамагочи — никакого фонового сервиса и опросов.
 */
class PetWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_FEED = "com.clawd.pet.WIDGET_FEED"

        /** Обновить все виджеты сразу, не дожидаясь системных 30 минут. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PetWidgetProvider::class.java))
            if (ids.isEmpty()) return
            push(context, manager, ids)
        }

        private fun push(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val prefs = Prefs(context)
            Tg.applyDecay(prefs)
            val t = I18n.t(prefs.lang)
            val views = RemoteViews(context.packageName, R.layout.widget_pet)
            views.setImageViewBitmap(R.id.ivWidgetPet, petBitmap(prefs.color))
            val status = when {
                !prefs.tgAlive -> t["tg_dead"] as String
                prefs.tgSleeping -> t["tg_sleep_note"] as String
                prefs.tgHealth <= 3 -> t["tg_sick"] as String
                else -> {
                    val h = prefs.tgHunger
                    val m = prefs.tgMood
                    val e = prefs.tgEnergy
                    val w = minOf(h, m, e)
                    when {
                        w > 6 -> t["tg_fine"] as String
                        w == h -> t["tg_hungry"] as String
                        w == e -> t["tg_tired"] as String
                        else -> t["tg_sad"] as String
                    }
                }
            }
            views.setTextViewText(R.id.tvWidgetStatus, status)
            views.setTextColor(R.id.tvWidgetStatus, 0xFF9AA0A6.toInt())
            views.setTextViewText(R.id.btnWidgetFeed, t["tg_feed"] as String)
            val feed = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, PetWidgetProvider::class.java).setAction(ACTION_FEED),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.btnWidgetFeed, feed)
            for (id in ids) manager.updateAppWidget(id, views)
        }

        /** Статичный пиксельный питомец как bitmap — RemoteViews умеет только ImageView. */
        private fun petBitmap(colorIndex: Int): Bitmap {
            val body = Sprite.bodyColor(colorIndex)
            val eye = Sprite.eyeColor(colorIndex)
            val cell = 6
            val bmp = Bitmap.createBitmap(Sprite.GRID_W * cell, Sprite.GRID_H * cell, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            fun px(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
                p.color = color
                c.drawRect(x0 * cell.toFloat(), y0 * cell.toFloat(), x1 * cell.toFloat(), y1 * cell.toFloat(), p)
            }
            val left = 3
            val right = 15
            val top = 3
            val bottom = 10
            for (i in 0 until 4) {
                val col = intArrayOf(left + 1, left + 3, right - 4, right - 2)[i]
                px(col, bottom, col + 1, bottom + 2, body)
            }
            px(left, top, right, bottom, body)
            val ey = top + 2
            for (side in intArrayOf(-1, 1)) {
                val cx = if (side < 0) left + 3 else right - 4
                px(cx, ey, cx + 1, ey + 2, eye)
            }
            px((left + right) / 2 - 2, ey + 2, (left + right) / 2 + 2, ey + 3, eye)
            return bmp
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        push(context, manager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_FEED) {
            val prefs = Prefs(context)
            Tg.applyDecay(prefs)
            if (prefs.tgAlive) {
                prefs.tgSleeping = false
                prefs.tgHunger = (prefs.tgHunger + 3).coerceAtMost(Tg.MAX)
                prefs.tgEnergy = (prefs.tgEnergy - 1).coerceAtLeast(0)
            }
            refresh(context)
        }
    }
}
