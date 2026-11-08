package com.clawd.pet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * После перезагрузки устройства питомец возвращается на экран сам —
 * но только если пользователь не нажимал «стоп» (Prefs.petWanted).
 * Без него после каждого рестарта питомец исчезал до ручного запуска.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = Prefs(context)
        if (!prefs.petWanted) return
        if (!Settings.canDrawOverlays(context)) return
        try {
            val serviceIntent = Intent(context, PetService::class.java)
                .setAction(PetService.ACTION_SHOW)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) {
        }
    }
}
