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
import android.media.AudioManager
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
    private var musicActive = false
    private val audioManager: AudioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    /**
     * Реакция на музыку: лёгкий опрос isMusicActive (1 р/с, только пока включён танец).
     * Танцует под любой активный MEDIA-стрим (Spotify, YouTube Music и пр.),
     * системные звуки и звонки игнорируются.
     */
    private val musicCheck: Runnable = Runnable {
        if (!prefs.dance || userHidden) {
            return@Runnable
        }
        val active = try {
            audioManager.isMusicActive
        } catch (_: Exception) {
            false
        }
        if (active != musicActive) {
            musicActive = active
            overlay?.setDance(active)
        }
        handler.postDelayed(musicCheck, 1000L)
    }

    private fun startMusicCheck() {
        handler.removeCallbacks(musicCheck)
        if (prefs.dance) {
            handler.post(musicCheck)
        } else {
            musicActive = false
            overlay?.setDance(false)
        }
    }

    /** Проверка состояния тамагочи: питомец в оверлее жалуется, если всё плохо. */
    private val tgCheck: Runnable = Runnable {
        Tg.applyDecay(prefs)
        if (prefs.tgAlive && !userHidden && prefs.phrases &&
            Tg.worst(prefs) <= 2 && kotlin.random.Random.nextFloat() < 0.35f
        ) {
            overlay?.speak("care", 3200L)
        }
        handler.postDelayed(tgCheck, 60_000L)
    }

    /** Раз в полчаса — заботливое напоминание (попить воды, разомнуться). */
    private val remindCheck: Runnable = Runnable {
        if (prefs.phrases && !userHidden && screenOn) {
            overlay?.speak("remind", 4000L)
        }
        handler.postDelayed(remindCheck, 30L * 60_000L)
    }

    private var sensorManager: android.hardware.SensorManager? = null

    private val shakeListener = object : android.hardware.SensorEventListener {
        private var last = 0L

        override fun onSensorChanged(event: android.hardware.SensorEvent) {
            val v = event.values
            val mag = kotlin.math.sqrt((v[0] * v[0] + v[1] * v[1] + v[2] * v[2]).toDouble()).toFloat()
            if (mag > 28f) {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - last > 2500L) {
                    last = now
                    overlay?.scare()
                }
            }
        }

        override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
    }

    private fun showQuickMenu() {
        val t = I18n.t(prefs.lang)
        overlay?.showMenu(
            listOf(
                t["tamagotchi"] as String to "tamagotchi",
                t["hide"] as String to "hide",
                t["settings"] as String to "settings",
            ),
        ) { key ->
            when (key) {
                "hide" -> {
                    userHidden = true
                    petHidden = true
                    overlay?.hide()
                    refreshNotification()
                }
                "tamagotchi" -> startActivity(
                    Intent(this, TamagotchiActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                "settings" -> startActivity(
                    Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

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

    /** -1, если уровень недоступен: getIntProperty возвращает 0/минус при ошибке. */
    private fun readBatteryPercent(): Int {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (pct in 0..100) pct else {
            val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) level * 100 / scale else -1
        }
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
        batteryLow = readBatteryPercent() in 0..15
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
        overlay?.menuRequest = { showQuickMenu() }
        maybeShow()
        startMusicCheck()
        handler.postDelayed(screenCheck, 1500L)
        handler.post(tgCheck)
        handler.postDelayed(remindCheck, 30L * 60_000L)
        sensorManager = getSystemService(SENSOR_SERVICE) as android.hardware.SensorManager?
        sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager?.registerListener(shakeListener, it, android.hardware.SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun maybeShow() {
        if (screenOn && !batteryLow && !userHidden) {
            overlay?.show()
            overlay?.setDance(musicActive && prefs.dance)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                overlay?.hide()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_APPLY -> {
                overlay?.applyConfig()
                startMusicCheck()
            }
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
                startMusicCheck()
                refreshNotification()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(screenCheck)
        handler.removeCallbacks(musicCheck)
        handler.removeCallbacks(tgCheck)
        handler.removeCallbacks(remindCheck)
        sensorManager?.unregisterListener(shakeListener)
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
        // FLAG_UPDATE_CURRENT обязателен: requestCode один, а action меняется —
        // без него система возвращает закэшированный первый PendingIntent
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, PetService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val toggle = if (userHidden) {
            PendingIntent.getService(
                this, 2,
                Intent(this, PetService::class.java).setAction(ACTION_SHOW),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        } else {
            PendingIntent.getService(
                this, 2,
                Intent(this, PetService::class.java).setAction(ACTION_HIDE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
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
