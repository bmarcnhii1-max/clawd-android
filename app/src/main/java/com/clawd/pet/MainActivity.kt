package com.clawd.pet

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private var lang = "ru"

    private lateinit var tvSubtitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLanguage: TextView
    private lateinit var tvSize: TextView
    private lateinit var tvSleep: TextView
    private lateinit var tvTips: TextView
    private lateinit var tvNoNet: TextView
    private lateinit var preview: PetPreviewView
    private lateinit var rgLang: RadioGroup
    private lateinit var rbRu: RadioButton
    private lateinit var rbEn: RadioButton
    private lateinit var rgSize: RadioGroup
    private lateinit var rbSize0: RadioButton
    private lateinit var rbSize1: RadioButton
    private lateinit var rbSize2: RadioButton
    private lateinit var rgSleep: RadioGroup
    private lateinit var rbSleep1: RadioButton
    private lateinit var rbSleep3: RadioButton
    private lateinit var rbSleep10: RadioButton
    private lateinit var rbSleepNever: RadioButton
    private lateinit var tvSecLook: TextView
    private lateinit var tvSecBehavior: TextView
    private lateinit var tvSecSystem: TextView
    private lateinit var secLookBody: View
    private lateinit var secBehaviorBody: View
    private lateinit var secSystemBody: View
    private lateinit var colorList: LinearLayout
    private var lookOpen = true
    private var behaviorOpen = false
    private var systemOpen = false
    private lateinit var swWander: Switch
    private lateinit var swPhrases: Switch
    private lateinit var swGravity: Switch
    private lateinit var swDance: Switch
    private lateinit var swSounds: Switch
    private lateinit var btnOverlay: Button
    private lateinit var btnBattery: Button
    private lateinit var btnTamagotchi: Button
    private lateinit var btnResetPos: Button
    private lateinit var btnHide: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnShare: Button
    private lateinit var btnWallpaper: Button
    private lateinit var tvHint: TextView

    private var suppressListeners = false

    private fun t() = I18n.t(lang)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        lang = prefs.lang
        bindViews()
        populate()
        applyTexts()
        listeners()
        // После переустановки/очистки лаунчера активная иконка может не совпадать
        // с сохранённым окрасом — синхронизируем при каждом запуске.
        applyPetIcon(prefs.color)
    }

    private fun bindViews() {
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvStatus = findViewById(R.id.tvStatus)
        tvLanguage = findViewById(R.id.tvLanguage)
        tvSize = findViewById(R.id.tvSize)
        tvSleep = findViewById(R.id.tvSleep)
        tvTips = findViewById(R.id.tvTips)
        tvNoNet = findViewById(R.id.tvNoNet)
        preview = findViewById(R.id.preview)
        rgLang = findViewById(R.id.rgLang)
        rbRu = findViewById(R.id.rbRu)
        rbEn = findViewById(R.id.rbEn)
        rgSize = findViewById(R.id.rgSize)
        rbSize0 = findViewById(R.id.rbSize0)
        rbSize1 = findViewById(R.id.rbSize1)
        rbSize2 = findViewById(R.id.rbSize2)
        rgSleep = findViewById(R.id.rgSleep)
        rbSleep1 = findViewById(R.id.rbSleep1)
        rbSleep3 = findViewById(R.id.rbSleep3)
        rbSleep10 = findViewById(R.id.rbSleep10)
        rbSleepNever = findViewById(R.id.rbSleepNever)
        tvSecLook = findViewById(R.id.tvSecLook)
        tvSecBehavior = findViewById(R.id.tvSecBehavior)
        tvSecSystem = findViewById(R.id.tvSecSystem)
        secLookBody = findViewById(R.id.secLookBody)
        secBehaviorBody = findViewById(R.id.secBehaviorBody)
        secSystemBody = findViewById(R.id.secSystemBody)
        colorList = findViewById(R.id.colorList)
        swWander = findViewById(R.id.swWander)
        swPhrases = findViewById(R.id.swPhrases)
        swGravity = findViewById(R.id.swGravity)
        swDance = findViewById(R.id.swDance)
        swSounds = findViewById(R.id.swSounds)
        btnOverlay = findViewById(R.id.btnOverlay)
        btnBattery = findViewById(R.id.btnBattery)
        btnTamagotchi = findViewById(R.id.btnTamagotchi)
        btnResetPos = findViewById(R.id.btnResetPos)
        btnHide = findViewById(R.id.btnHide)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnShare = findViewById(R.id.btnShare)
        btnWallpaper = findViewById(R.id.btnWallpaper)
        tvHint = findViewById(R.id.tvHint)
    }

    private fun populate() {
        suppressListeners = true
        if (lang == "en") rbEn.isChecked = true else rbRu.isChecked = true
        when (prefs.size) {
            0 -> rbSize0.isChecked = true
            2 -> rbSize2.isChecked = true
            else -> rbSize1.isChecked = true
        }
        when (prefs.sleepSeconds) {
            60 -> rbSleep1.isChecked = true
            600 -> rbSleep10.isChecked = true
            0 -> rbSleepNever.isChecked = true
            else -> rbSleep3.isChecked = true
        }
        swWander.isChecked = prefs.wander
        swPhrases.isChecked = prefs.phrases
        swGravity.isChecked = prefs.gravity
        swDance.isChecked = prefs.dance
        swSounds.isChecked = prefs.sounds
        suppressListeners = false
    }

    private fun applyTexts() {
        val ti = t()
        val prev = suppressListeners
        suppressListeners = true
        tvSubtitle.text = ti["subtitle"] as String
        tvLanguage.text = ti["language"] as String
        tvSize.text = ti["size"] as String
        tvSleep.text = ti["sleep"] as String
        tvTips.text = ti["tips"] as String
        tvNoNet.text = ti["no_internet"] as String
        rbRu.text = ti["language_ru"] as String
        rbEn.text = ti["language_en"] as String
        rbSleep1.text = ti["sleep_1m"] as String
        rbSleep3.text = ti["sleep_3m"] as String
        rbSleep10.text = ti["sleep_10m"] as String
        rbSleepNever.text = ti["sleep_never"] as String
        val sizes = ti["sizes"] as List<String>
        rbSize0.text = sizes[0]
        rbSize1.text = sizes[1]
        rbSize2.text = sizes[2]
        swWander.text = ti["wander"] as String
        swPhrases.text = ti["phrases"] as String
        swGravity.text = ti["gravity"] as String
        swDance.text = ti["dance"] as String
        swSounds.text = ti["sounds"] as String
        btnStart.text = ti["start"] as String
        btnStop.text = ti["stop"] as String
        btnShare.text = ti["share"] as String
        btnWallpaper.text = ti["wallpaper"] as String
        btnTamagotchi.text = ti["tamagotchi"] as String
        btnResetPos.text = ti["reset_pos"] as String
        btnHide.text = if (PetService.petHidden) ti["show"] as String else ti["hide"] as String
        tvHint.text = ti["hint"] as String
        val isRunning = PetService.running && !PetService.petHidden
        tvStatus.text = if (isRunning) ti["status_running"] as String else ti["status_stopped"] as String
        tvStatus.setTextColor(
            if (isRunning) 0xFF7FD1A8.toInt() else 0xFF9AA0A6.toInt(),
        )
        preview.setRunning(isRunning)
        preview.setPet(prefs.color, prefs.size)
        drawSections()
        renderColorList()
        suppressListeners = prev

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (Settings.canDrawOverlays(this)) {
            btnOverlay.text = ti["overlay_ok"] as String
        } else {
            btnOverlay.text = ti["overlay_needed"] as String
        }
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            btnBattery.text = ti["battery_ok"] as String
        } else {
            btnBattery.text = ti["battery_needed"] as String
        }
    }

    /** Заголовки секций со стрелками + видимость тел. */
    private fun drawSections() {
        val ti = t()
        tvSecLook.text = "${ti["sec_look"]}  ${if (lookOpen) "▾" else "▸"}"
        tvSecBehavior.text = "${ti["sec_behavior"]}  ${if (behaviorOpen) "▾" else "▸"}"
        tvSecSystem.text = "${ti["sec_system"]}  ${if (systemOpen) "▾" else "▸"}"
        secLookBody.visibility = if (lookOpen) View.VISIBLE else View.GONE
        secBehaviorBody.visibility = if (behaviorOpen) View.VISIBLE else View.GONE
        secSystemBody.visibility = if (systemOpen) View.VISIBLE else View.GONE
    }

    /** Инлайн-список окрасов вместо спиннера: точка + название + выбор. */
    private fun renderColorList() {
        val ti = t()
        @Suppress("UNCHECKED_CAST")
        val names = ti["colors"] as List<String>
        val selected = Math.floorMod(prefs.color, Sprite.count())
        colorList.removeAllViews()
        val d = resources.displayMetrics.density
        for (i in 0 until Sprite.count()) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt())
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            val dot = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Sprite.bodyColor(i))
                }
                layoutParams = LinearLayout.LayoutParams((18 * d).toInt(), (18 * d).toInt())
            }
            val label = TextView(this).apply {
                text = names.getOrElse(i) { Sprite.name(i) }
                textSize = 13f
                setTextColor(resources.getColor(R.color.text, theme))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (10 * d).toInt()
                }
            }
            val mark = TextView(this).apply {
                text = if (i == selected) "●" else ""
                textSize = 14f
                setTextColor(resources.getColor(R.color.accent, theme))
            }
            row.addView(dot)
            row.addView(label)
            row.addView(mark)
            row.setOnClickListener {
                prefs.color = i
                applyPetIcon(i)
                applyTexts()
                syncService()
            }
            colorList.addView(row)
        }
    }

    private fun listeners() {
        tvSecLook.setOnClickListener { lookOpen = !lookOpen; drawSections() }
        tvSecBehavior.setOnClickListener { behaviorOpen = !behaviorOpen; drawSections() }
        tvSecSystem.setOnClickListener { systemOpen = !systemOpen; drawSections() }
        rgLang.setOnCheckedChangeListener { _, checkedId ->
            if (suppressListeners) return@setOnCheckedChangeListener
            lang = if (checkedId == R.id.rbEn) "en" else "ru"
            prefs.lang = lang
            applyTexts()
            syncService()
        }
        rgSize.setOnCheckedChangeListener { _, checkedId ->
            if (suppressListeners) return@setOnCheckedChangeListener
            prefs.size = when (checkedId) {
                R.id.rbSize0 -> 0
                R.id.rbSize2 -> 2
                else -> 1
            }
            applyTexts()
            syncService()
        }
        rgSleep.setOnCheckedChangeListener { _, checkedId ->
            if (suppressListeners) return@setOnCheckedChangeListener
            prefs.sleepSeconds = when (checkedId) {
                R.id.rbSleep1 -> 60
                R.id.rbSleep10 -> 600
                R.id.rbSleepNever -> 0
                else -> 180
            }
            syncService()
        }
        swWander.setOnCheckedChangeListener { _, v -> if (!suppressListeners) { prefs.wander = v; syncService() } }
        swPhrases.setOnCheckedChangeListener { _, v -> if (!suppressListeners) { prefs.phrases = v; syncService() } }
        swGravity.setOnCheckedChangeListener { _, v -> if (!suppressListeners) { prefs.gravity = v; syncService() } }
        swDance.setOnCheckedChangeListener { _, v -> if (!suppressListeners) { prefs.dance = v; syncService() } }
        swSounds.setOnCheckedChangeListener { _, v -> if (!suppressListeners) { prefs.sounds = v; syncService() } }

        btnTamagotchi.setOnClickListener {
            startActivity(Intent(this, TamagotchiActivity::class.java))
        }
        btnOverlay.setOnClickListener {            if (!Settings.canDrawOverlays(this)) {
                try {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"),
                        ),
                    )
                } catch (_: Exception) {
                    Toast.makeText(this, "No overlay settings", Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnBattery.setOnClickListener { askBattery() }
        btnResetPos.setOnClickListener { resetPosition() }
        btnHide.setOnClickListener { toggleHide() }
        btnStart.setOnClickListener { startPet() }
        btnStop.setOnClickListener { stopPet() }
        btnShare.setOnClickListener { shareSettings() }
        btnWallpaper.setOnClickListener { matchWallpaper() }
    }

    private fun resetPosition() {
        prefs.posX = Int.MIN_VALUE
        prefs.posY = Int.MIN_VALUE
        sendToService(PetService.ACTION_RESET_POS)
        Toast.makeText(this, t()["reset_done"] as String, Toast.LENGTH_SHORT).show()
    }

    private fun toggleHide() {
        val action = if (PetService.petHidden) PetService.ACTION_SHOW else PetService.ACTION_HIDE
        sendToService(action)
        // Состояние придёт из сервиса через stateListener: статическое поле
        // обновится не в момент startService, а после обработки команды.
    }

    private fun sendToService(action: String) {
        if (!PetService.running) return
        val intent = Intent(this, PetService::class.java).setAction(action)
        try {
            startService(intent)
        } catch (_: Exception) {
            try {
                startForegroundService(intent)
            } catch (_: Exception) {
            }
        }
    }

    @SuppressLint("BatteryLife")
    private fun askBattery() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName")),
            )
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
            }
        }
    }

    private fun startPet() {
        if (!Settings.canDrawOverlays(this)) {
            applyTexts()
            btnOverlay.performClick()
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            return // дождёмся результата, иначе finish() оборвёт диалог
        }
        launchPetService()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Отказ в уведомлениях не мешает foreground-сервису: он просто
        // не покажет уведомление в шторке, поэтому запускаем в любом случае.
        if (requestCode == 1) launchPetService()
    }

    private fun launchPetService() {
        startForegroundService(Intent(this, PetService::class.java))
        sendToService(PetService.ACTION_SHOW)
        Toast.makeText(this, t()["started"] as String, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun stopPet() {
        if (!PetService.running) return
        sendToService(PetService.ACTION_STOP)
        Toast.makeText(this, t()["paused"] as String, Toast.LENGTH_SHORT).show()
    }

    /** Экспорт всех настроек и позиции одной строкой JSON через системный share. */
    private fun shareSettings() {        val pos = if (prefs.positionSaved()) ",\"posX\":${prefs.posX},\"posY\":${prefs.posY}" else ""
        val json = "{\"lang\":\"${prefs.lang}\",\"size\":${prefs.size},\"color\":${prefs.color}" +
            ",\"wander\":${prefs.wander},\"phrases\":${prefs.phrases},\"gravity\":${prefs.gravity}" +
            ",\"dance\":${prefs.dance},\"sounds\":${prefs.sounds},\"sleepSeconds\":${prefs.sleepSeconds}$pos}"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Clawd settings: $json")
        }
        try {
            startActivity(Intent.createChooser(send, null))
        } catch (_: Exception) {
        }
    }

    /** Разовый подбор окраса под доминирующий цвет обоев. */
    private fun matchWallpaper() {
        if (!btnWallpaper.isEnabled) return
        btnWallpaper.isEnabled = false
        // На API < 31 обои декодируются в битмап — это нельзя делать в UI-потоке.
        Thread {
            val idx = WallpaperPalette.matchColorIndex(this)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                btnWallpaper.isEnabled = true
                if (idx == null) {
                    Toast.makeText(this, t()["wallpaper_fail"] as String, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                prefs.color = idx
                applyPetIcon(idx)
                applyTexts()
                syncService()
                Toast.makeText(this, t()["wallpaper_done"] as String, Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun syncService() {
        sendToService(PetService.ACTION_APPLY)
    }

    /** Иконка лаунчера — питомец в выбранном окрасе. */
    private fun applyPetIcon(index: Int) {
        val want = Math.floorMod(index, Sprite.count())
        for (i in 0 until Sprite.count()) {
            val name = ComponentName(this, "com.clawd.pet.LauncherIcon$i")
            val state = if (i == want) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                if (packageManager.getComponentEnabledSetting(name) == state) continue
                packageManager.setComponentEnabledSetting(
                    name, state, PackageManager.DONT_KILL_APP,
                )
            } catch (_: Exception) {
            }
        }
    }
    override fun onStart() {
        super.onStart()
        // Сервис живёт в том же процессе: после действий из шторки
        // (скрыть/показать/остановить) экран обновляется сам.
        PetService.stateListener = { applyTexts() }
    }

    override fun onStop() {
        super.onStop()
        PetService.stateListener = null
    }

    override fun onResume() {
        super.onResume()
        applyTexts()
    }
}
