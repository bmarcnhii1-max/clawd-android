package com.clawd.pet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager

class PetService : Service() {

    companion object {
        const val ACTION_STOP = "com.clawd.pet.STOP"
        const val ACTION_APPLY = "com.clawd.pet.APPLY"
        const val ACTION_RESET_POS = "com.clawd.pet.RESET_POS"
        const val ACTION_HIDE = "com.clawd.pet.HIDE"
        const val ACTION_SHOW = "com.clawd.pet.SHOW"
        private const val CHANNEL_ID = "pet"
        private const val NOTIF_ID = 1

        @Volatile
        var running = false
            private set

        @Volatile
        var petHidden = false
            private set
    }

    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: PetOverlay? = null
    private var screenOn = true
    private var batteryLow = false
    private var userHidden = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    overlay?.screenActive = false
                    if (prefs.alwaysVisible) return
                    if (isScreenInteractive()) {
                        return
                    }
                    screenOn = false
                    overlay?.screenActive = false
                    overlay?.hide()
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                    overlay?.screenActive = true
                    maybeShow()
                }
                Intent.ACTION_BATTERY_LOW -> {
                    batteryLow = true
                    overlay?.hide()
                }
                Intent.ACTION_BATTERY_OKAY -> {
                    batteryLow = false
                    maybeShow()
                }
            }
        }
    }

    private fun isScreenInteractive(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= 20) pm.isInteractive else pm.isScreenOn
    }

    private val screenCheck: Runnable = Runnable {
        val interactive = isScreenInteractive()
        overlay?.screenActive = interactive
        val visible = prefs.alwaysVisible || interactive
        if (!visible && screenOn) {
            screenOn = false
            overlay?.screenActive = false
            overlay?.hide()
        } else if (visible && !screenOn) {
            screenOn = true
            overlay?.screenActive = true
            maybeShow()
        }
        handler.postDelayed(screenCheck, 1500L)
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        prefs = Prefs(this)
        screenOn = prefs.alwaysVisible || isScreenInteractive()
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        batteryLow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) <= 15
        startForeground(NOTIF_ID, buildNotification())
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        overlay = PetOverlay(this, prefs)
        overlay?.screenActive = isScreenInteractive()
        maybeShow()
        handler.postDelayed(screenCheck, 1500L)
    }

    private fun maybeShow() {
        if (screenOn && !batteryLow && !userHidden) overlay?.show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                overlay?.hide()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_APPLY -> overlay?.applyConfig()
            ACTION_RESET_POS -> overlay?.resetPosition()
            ACTION_HIDE -> {
                userHidden = true
                petHidden = true
                overlay?.hide()
                refreshNotification()
            }
            ACTION_SHOW -> {
                userHidden = false
                petHidden = false
                maybeShow()
                refreshNotification()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(screenCheck)
        overlay?.hide()
        try {
            unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
        running = false
        petHidden = false
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Clawd", NotificationManager.IMPORTANCE_MIN),
        )
        val t = I18n.t(prefs.lang)
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, PetService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val toggle = if (userHidden) {
            PendingIntent.getService(
                this, 2,
                Intent(this, PetService::class.java).setAction(ACTION_SHOW),
                PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            PendingIntent.getService(
                this, 2,
                Intent(this, PetService::class.java).setAction(ACTION_HIDE),
                PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val toggleLabel = if (userHidden) t["show"] as String else t["hide"] as String
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pet_notif)
            .setContentTitle("Clawd")
            .setContentText(if (userHidden) t["paused"] as String else t["running"] as String)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, toggleLabel, toggle)
            .addAction(0, t["stop"] as String, stop)
        if (userHidden) {
            builder.setOngoing(false)
        }
        return builder.build()
    }

    private fun refreshNotification() {
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotification())
        } catch (_: Exception) {
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
