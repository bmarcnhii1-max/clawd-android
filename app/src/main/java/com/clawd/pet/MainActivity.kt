package com.clawd.pet

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
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
    private lateinit var tvColor: TextView
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
    private lateinit var spColor: Spinner
    private lateinit var swWander: Switch
    private lateinit var swPhrases: Switch
    private lateinit var swGravity: Switch
    private lateinit var swDance: Switch
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
    private var colorAdapterLang = ""

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
    }

    private fun bindViews() {
        tvSubtitle = findViewById(R.id.tvSubtitle)
        tvStatus = findViewById(R.id.tvStatus)
        tvLanguage = findViewById(R.id.tvLanguage)
        tvSize = findViewById(R.id.tvSize)
        tvColor = findViewById(R.id.tvColor)
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
        spColor = findViewById(R.id.spColor)
        swWander = findViewById(R.id.swWander)
        swPhrases = findViewById(R.id.swPhrases)
        swGravity = findViewById(R.id.swGravity)
        swDance = findViewById(R.id.swDance)
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
        suppressListeners = false
    }

    private fun applyTexts() {
        val ti = t()
        val prev = suppressListeners
        suppressListeners = true
        tvSubtitle.text = ti["subtitle"] as String
        tvLanguage.text = ti["language"] as String
        tvSize.text = ti["size"] as String
        tvColor.text = ti["color"] as String
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

        @Suppress("UNCHECKED_CAST")
        val colors = ti["colors"] as List<String>
        if (spColor.adapter == null || colorAdapterLang != lang) {
            colorAdapterLang = lang
            suppressListeners = true
            spColor.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colors)
        }
        if (spColor.selectedItemPosition != Math.floorMod(prefs.color, Sprite.count())) {
            suppressListeners = true
            spColor.setSelection(Math.floorMod(prefs.color, Sprite.count()))
        }
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

    private fun listeners() {
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
        spColor.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (suppressListeners) return
                prefs.color = position
                applyTexts()
                syncService()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        swWander.setOnCheckedChangeListener { _, v -> if (!suppressListeners) { prefs.wander = v; syncService() } }
        swPhrases.setOnCheckedChangeListener { _, v -> if (!suppressListeners) { prefs.phrases = v; syncService() } }
        swGravity.setOnCheckedChangeListener { _, v -> if (!suppressListeners) { prefs.gravity = v; syncService() } }
        swDance.setOnCheckedChangeListener { _, v -> if (!suppressListeners) { prefs.dance = v; syncService() } }

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
        applyTexts()
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
            ",\"dance\":${prefs.dance},\"sleepSeconds\":${prefs.sleepSeconds}$pos}"
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
        val idx = WallpaperPalette.matchColorIndex(this)
        if (idx == null) {
            Toast.makeText(this, t()["wallpaper_fail"] as String, Toast.LENGTH_SHORT).show()
            return
        }
        prefs.color = idx
        applyTexts()
        syncService()
        Toast.makeText(this, t()["wallpaper_done"] as String, Toast.LENGTH_SHORT).show()
    }

    private fun syncService() {
        sendToService(PetService.ACTION_APPLY)
    }
    override fun onResume() {
        super.onResume()
        applyTexts()
    }
}
