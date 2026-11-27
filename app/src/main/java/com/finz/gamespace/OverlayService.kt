package com.finz.gamespace

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.min

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var kit: OverlayKit
    private lateinit var act: OverlayActions
    private lateinit var cross: CrosshairPanel
    private val handler = Handler(Looper.getMainLooper())
    private var bubble: View? = null
    private var bubbleLp: WindowManager.LayoutParams? = null
    private var hud: DockPanel? = null
    private var panelLp: WindowManager.LayoutParams? = null
    private var mon: TextView? = null
    private var keepOn = false
    private var running = false
    private val sessionStart = SystemClock.elapsedRealtime()
    private var restMin = 0
    private var nextRest = 0L
    private var heatLimit = 0
    private var lastHeat = 0L
    private var lastLow = 0L
    private var muteOn = false
    private var oldVol = 0
    private var saveOn = false
    private var oldBright = 128
    private var boostOn = false

    private val tick = object : Runnable {
        override fun run() {
            updateStats()
            if (hud != null || mon != null) {
                Ping.start()
                handler.postDelayed(this, 1000)
            } else {
                Ping.stop()
                running = false
            }
        }
    }

    private val bg = object : Runnable {
        override fun run() {
            checkAlerts()
            handler.postDelayed(this, 5000)
        }
    }

    private fun startTick() {
        if (!running) {
            running = true
            handler.post(tick)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        kit = OverlayKit(this)
        act = OverlayActions(this, handler)
        cross = CrosshairPanel(this, wm, kit) { bubble?.visibility = View.VISIBLE }
        val sp = getSharedPreferences("gs", MODE_PRIVATE)
        restMin = sp.getInt("restmin", 0)
        heatLimit = sp.getInt("heatlim", 0)
        if (restMin > 0) nextRest = SystemClock.elapsedRealtime() + restMin * 60000L
        startFg()
        showBubble()
        cross.restore()
        handler.postDelayed(bg, 5000)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        handler.removeCallbacks(bg)
        running = false
        Ping.stop()
        if (muteOn) {
            try {
                getSystemService(AudioManager::class.java)
                    .setStreamVolume(AudioManager.STREAM_MUSIC, oldVol.coerceAtLeast(1), 0)
            } catch (e: Exception) {
            }
        }
        if (saveOn && Settings.System.canWrite(this)) act.setBrightness(oldBright)
        cross.destroy()
        hud?.let { try { wm.removeView(it.root) } catch (e: Exception) {} }
        mon?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        bubble?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        super.onDestroy()
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("gs", "Game Space", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, "gs")
            .setContentTitle("Finz Game Space aktif")
            .setContentText("Overlay berjalan")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun say(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    // ---------- peringatan ----------
    @Suppress("DEPRECATION")
    private fun vibrate() {
        try {
            val v = getSystemService(Vibrator::class.java)
            v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 120, 200), -1))
        } catch (e: Exception) {
        }
    }

    private fun alert(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        vibrate()
    }

    private fun charging(): Boolean {
        val st = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return st == BatteryManager.BATTERY_STATUS_CHARGING ||
            st == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun checkAlerts() {
        val now = SystemClock.elapsedRealtime()
        if (restMin > 0 && now >= nextRest) {
            nextRest = now + restMin * 60000L
            alert("Sudah ${(now - sessionStart) / 60000} menit bermain. Waktunya istirahat sejenak.")
        }
        if (heatLimit > 0) {
            val t = act.tempC()
            if (t >= heatLimit && now - lastHeat > 120000L) {
                lastHeat = now
                alert("Suhu HP %.1f°C. Istirahatkan HP supaya tidak terlalu panas.".format(t))
            }
        }
        val b = act.batPct()
        if (b <= 15 && !charging() && now - lastLow > 300000L) {
            lastLow = now
            alert("Baterai tinggal $b%")
        }
    }

    private fun sessionText(): String {
        val s = (SystemClock.elapsedRealtime() - sessionStart) / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }

    // ---------- bubble ----------
    private fun snap(v: View, p: WindowManager.LayoutParams, size: Int) {
        val r = KeyMapStore.real(this)
        val m = kit.dp(6)
        val target = if (p.x + size / 2 < r[0] / 2) m else r[0] - size - m
        val from = p.x
        p.y = p.y.coerceIn(m, r[1] - size - m)
        Glide.run(v, 0, 200) { u ->
            p.x = (from + (target - from) * u).toInt()
            try { wm.updateViewLayout(v, p) } catch (e: Exception) {}
        }
    }

    private fun showBubble() {
        val size = kit.dp(54)
        val m = kit.dp(4)
        val fl = FrameLayout(this)
        fl.background = kit.circleBg(false)
        fl.addView(
            LogoView(this),
            FrameLayout.LayoutParams(size - 2 * m, size - 2 * m).apply { gravity = Gravity.CENTER }
        )
        fl.alpha = 0.78f
        val p = kit.lp(size, size, kit.dp(6), kit.dp(80), Gravity.TOP or Gravity.START, keepOn)
        var sx = 0
        var sy = 0
        var tx0 = 0f
        var ty0 = 0f
        var moved = false
        fl.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = p.x; sy = p.y; tx0 = e.rawX; ty0 = e.rawY; moved = false
                    fl.alpha = 1f
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx0).toInt()
                    val dy = (e.rawY - ty0).toInt()
                    if (abs(dx) > kit.dp(5) || abs(dy) > kit.dp(5)) moved = true
                    p.x = sx + dx
                    p.y = sy + dy
                    wm.updateViewLayout(fl, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    fl.alpha = 0.78f
                    if (!moved) openPanel() else snap(fl, p, size)
                    true
                }
                else -> false
            }
        }
        wm.addView(fl, p)
        bubble = fl
        bubbleLp = p
    }

    // ---------- dock ----------
    private fun restLabel(): String = if (restMin > 0) "Istirahat ${restMin}m" else "Istirahat"

    private fun heatLabel(): String = if (heatLimit > 0) "Panas ${heatLimit}°" else "Alarm Panas"

    private fun openPanel() {
        if (hud != null) return
        cross.hidePanel(false)
        bubble?.visibility = View.GONE
        val left = KeyMapStore.getInt(this, "dockside", 0) == 1
        val h = DockPanel(this, kit, act, left) { onAction(it) }
        h.active("dnd", act.dndOn())
        h.active("keep", keepOn)
        h.active("cross", getSharedPreferences("ch", MODE_PRIVATE).getBoolean("on", false))
        h.active("mon", mon != null)
        h.active("rec", InputBackend.recording)
        h.active("mute", muteOn)
        h.active("save", saveOn)
        h.active("boost", boostOn)
        h.active("rest", restMin > 0)
        h.active("heat", heatLimit > 0)
        h.setLabel("rest", restLabel())
        h.setLabel("heat", heatLabel())
        h.active(
            "rot",
            Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        )
        val dm = KeyMapStore.real(this)
        val w = min(dm[0] - kit.dp(8), kit.dp(280))
        val ph = dm[1] - kit.dp(12)
        var f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        if (keepOn) f = f or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        val p = WindowManager.LayoutParams(
            w, ph, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, f, PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or (if (left) Gravity.START else Gravity.END)
        p.x = kit.dp(4)
        p.y = kit.dp(6)
        h.root.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_OUTSIDE) {
                closePanel()
                true
            } else {
                false
            }
        }
        wm.addView(h.root, p)
        hud = h
        panelLp = p
        val from = (if (left) -1f else 1f) * w
        h.root.translationX = from
        h.root.alpha = 0f
        Glide.run(h.root, 0, 260) { u ->
            h.root.translationX = from * (1f - u)
            h.root.alpha = u * h.targetAlpha
        }
        updateStats()
        startTick()
    }

    private fun closePanel() {
        hud?.let { try { wm.removeView(it.root) } catch (e: Exception) {} }
        hud = null
        panelLp = null
        bubble?.visibility = View.VISIBLE
    }

    private fun withAdb(action: () -> Unit) {
        if (InputBackend.ready) {
            action()
            return
        }
        hud?.setStatus("Menyambung ADB...")
        say("Menyambung ADB...")
        InputBackend.start(this, InputBackend.ADB) { ok, msg ->
            if (ok) {
                hud?.setStatus("")
                action()
            } else {
                hud?.setStatus(msg)
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun doShot() {
        withAdb {
            say("Mengambil foto layar...")
            closePanel()
            handler.postDelayed({ InputBackend.shot() }, 600)
        }
    }

    private fun doRec() {
        if (InputBackend.recording) {
            InputBackend.recToggle()
            hud?.active("rec", false)
            hud?.setStatus("Menghentikan rekaman...")
            return
        }
        withAdb {
            InputBackend.recToggle()
            say("Mulai merekam layar...")
            closePanel()
        }
    }

    private fun openSys(action: String) {
        closePanel()
        try {
            startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            say("Tidak bisa membuka pengaturan itu")
        }
    }

    private fun onAction(key: String) {
        when (key) {
            "dnd" -> hud?.active("dnd", act.toggleDnd())
            "keep" -> toggleKeep()
            "cross" -> {
                closePanel()
                cross.showPanel()
            }
            "keymap" -> {
                closePanel()
                KeymapMenu.open(this, wm, kit)
            }
            "shot" -> doShot()
            "rec" -> doRec()
            "turbo" -> act.cleanRam()
            "mon" -> toggleMonitor()
            "rot" -> toggleRotate()
            "mute" -> toggleMute()
            "save" -> toggleSave()
            "rest" -> cycleRest()
            "heat" -> cycleHeat()
            "boost" -> toggleBoost()
            "net" -> openSys(
                if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                else Settings.ACTION_WIRELESS_SETTINGS
            )
            "disp" -> openSys(Settings.ACTION_DISPLAY_SETTINGS)
            "side" -> {
                val cur = KeyMapStore.getInt(this, "dockside", 0)
                KeyMapStore.putInt(this, "dockside", 1 - cur)
                closePanel()
                openPanel()
            }
            "app" -> {
                closePanel()
                startActivity(
                    Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            "off" -> stopSelf()
            "close" -> closePanel()
        }
    }

    // ---------- fitur ----------
    private fun needWrite(): Boolean {
        if (Settings.System.canWrite(this)) return false
        Toast.makeText(this, "Izinkan ubah pengaturan sistem dulu", Toast.LENGTH_LONG).show()
        startActivity(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }

    private fun toggleRotate() {
        if (needWrite()) return
        val cur = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
        Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1 - cur)
        hud?.active("rot", cur == 0)
    }

    private fun applyKeep(p: WindowManager.LayoutParams?, v: View?) {
        if (p == null || v == null) return
        val f = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        p.flags = if (keepOn) p.flags or f else p.flags and f.inv()
        try { wm.updateViewLayout(v, p) } catch (e: Exception) {}
    }

    private fun toggleKeep() {
        keepOn = !keepOn
        applyKeep(bubbleLp, bubble)
        applyKeep(panelLp, hud?.root)
        hud?.active("keep", keepOn)
        say(if (keepOn) "Layar tetap menyala" else "Layar normal")
    }

    private fun toggleMute() {
        val am = getSystemService(AudioManager::class.java)
        if (!muteOn) {
            oldVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            muteOn = true
            say("Suara media dibisukan")
        } else {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, oldVol.coerceAtLeast(1), 0)
            muteOn = false
            say("Suara media kembali")
        }
        hud?.active("mute", muteOn)
    }

    private fun toggleSave() {
        if (!saveOn) {
            if (needWrite()) return
            oldBright = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            act.setBrightness(15)
            saveOn = true
            say("Hemat layar aktif")
        } else {
            act.setBrightness(oldBright)
            saveOn = false
            say("Kecerahan dikembalikan")
        }
        hud?.active("save", saveOn)
    }

    private fun cycleRest() {
        restMin = when (restMin) {
            0 -> 30
            30 -> 45
            45 -> 60
            else -> 0
        }
        getSharedPreferences("gs", MODE_PRIVATE).edit().putInt("restmin", restMin).apply()
        nextRest = SystemClock.elapsedRealtime() + restMin * 60000L
        hud?.setLabel("rest", restLabel())
        hud?.active("rest", restMin > 0)
        say(if (restMin > 0) "Pengingat istirahat tiap $restMin menit" else "Pengingat istirahat mati")
    }

    private fun cycleHeat() {
        heatLimit = when (heatLimit) {
            0 -> 40
            40 -> 42
            42 -> 45
            else -> 0
        }
        getSharedPreferences("gs", MODE_PRIVATE).edit().putInt("heatlim", heatLimit).apply()
        lastHeat = 0L
        hud?.setLabel("heat", heatLabel())
        hud?.active("heat", heatLimit > 0)
        say(if (heatLimit > 0) "Alarm panas di $heatLimit°C" else "Alarm panas mati")
    }

    private fun toggleBoost() {
        if (!boostOn) {
            boostOn = true
            if (!act.dndOn()) hud?.active("dnd", act.toggleDnd())
            if (!keepOn) toggleKeep()
            act.cleanRam()
            say("Mode Game aktif: DND, layar menyala, RAM dibersihkan")
        } else {
            boostOn = false
            if (act.dndOn()) hud?.active("dnd", act.toggleDnd())
            if (keepOn) toggleKeep()
            say("Mode Game dimatikan")
        }
        hud?.active("boost", boostOn)
    }

    private fun toggleMonitor() {
        if (mon != null) {
            mon?.let { try { wm.removeView(it) } catch (e: Exception) {} }
            mon = null
            hud?.active("mon", false)
            return
        }
        val t = kit.tx("", 11f, Color.WHITE, true).apply {
            background = kit.box(0xAA0B0D12.toInt(), 8)
            setPadding(kit.dp(8), kit.dp(4), kit.dp(8), kit.dp(4))
        }
        val f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val p = WindowManager.LayoutParams(
            kit.WRAP, kit.WRAP, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            f, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = kit.dp(4)
        }
        wm.addView(t, p)
        mon = t
        hud?.active("mon", true)
        updateStats()
        startTick()
    }

    private fun updateStats() {
        val ram = act.ramPct()
        val temp = act.tempC()
        val net = act.netText()
        val bat = act.batPct()
        val ping = Ping.text()
        val ses = sessionText()
        hud?.update(ram, temp, bat, ping, net, ses)
        hud?.active("rec", InputBackend.recording)
        mon?.text = String.format(
            "%s  |  RAM %d%%  |  %.1f°C  |  BAT %d%%  |  %s  |  %s",
            ses, ram, temp, bat, ping, net
        )
    }
}
