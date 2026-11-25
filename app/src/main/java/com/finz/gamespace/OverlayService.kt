package com.finz.gamespace

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var kit: OverlayKit
    private lateinit var act: OverlayActions
    private lateinit var cross: CrosshairPanel
    private val handler = Handler(Looper.getMainLooper())
    private var bubble: View? = null
    private var bubbleLp: WindowManager.LayoutParams? = null
    private var hud: HudPanel? = null
    private var panelLp: WindowManager.LayoutParams? = null
    private var mon: TextView? = null
    private var keepOn = false
    private var running = false

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
        startFg()
        showBubble()
        cross.restore()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        running = false
        Ping.stop()
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

    private fun showBubble() {
        val size = kit.dp(46)
        val v = kit.tx("F", 18f, Color.BLACK, true).apply {
            gravity = Gravity.CENTER
            alpha = 0.9f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(kit.accent)
            }
        }
        val p = kit.lp(size, size, kit.dp(8), kit.dp(80), Gravity.TOP or Gravity.START, keepOn)
        var sx = 0
        var sy = 0
        var tx0 = 0f
        var ty0 = 0f
        var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = p.x; sy = p.y; tx0 = e.rawX; ty0 = e.rawY; moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx0).toInt()
                    val dy = (e.rawY - ty0).toInt()
                    if (abs(dx) > kit.dp(5) || abs(dy) > kit.dp(5)) moved = true
                    p.x = sx + dx
                    p.y = sy + dy
                    wm.updateViewLayout(v, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) openPanel()
                    true
                }
                else -> false
            }
        }
        wm.addView(v, p)
        bubble = v
        bubbleLp = p
    }

    private fun openPanel() {
        cross.hidePanel(false)
        bubble?.visibility = View.GONE
        val h = HudPanel(this, kit, act) { onAction(it) }
        h.active("dnd", act.dndOn())
        h.active("keep", keepOn)
        h.active("cross", getSharedPreferences("ch", MODE_PRIVATE).getBoolean("on", false))
        h.active("mon", mon != null)
        h.active("rec", InputBackend.recording)
        h.active(
            "rot",
            Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1) == 1
        )
        val w = minOf(resources.displayMetrics.widthPixels - kit.dp(16), kit.dp(400))
        val p = kit.lp(w, kit.WRAP, 0, kit.dp(4), Gravity.TOP or Gravity.CENTER_HORIZONTAL, keepOn)
        wm.addView(h.root, p)
        hud = h
        panelLp = p
        updateStats()
        startTick()
    }

    private fun closePanel() {
        hud?.let { try { wm.removeView(it.root) } catch (e: Exception) {} }
        hud = null
        panelLp = null
        bubble?.visibility = View.VISIBLE
    }

    private fun needAdb(): Boolean {
        if (InputBackend.ready) return false
        Toast.makeText(this, "Sambungkan ADB dulu: Keymap > Sambung ADB", Toast.LENGTH_LONG).show()
        return true
    }

    private fun doShot() {
        if (needAdb()) return
        closePanel()
        handler.postDelayed({ InputBackend.shot() }, 500)
    }

    private fun doRec() {
        if (needAdb()) return
        val starting = !InputBackend.recording
        InputBackend.recToggle()
        if (starting) {
            closePanel()
        } else {
            hud?.active("rec", false)
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
            "shot" -> doShot()
            "rec" -> doRec()
            "app" -> startActivity(
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            "off" -> stopSelf()
            "turbo" -> act.cleanRam()
            "mon" -> toggleMonitor()
            "rot" -> toggleRotate()
            "close" -> closePanel()
        }
    }

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
        wm.updateViewLayout(v, p)
    }

    private fun toggleKeep() {
        keepOn = !keepOn
        applyKeep(bubbleLp, bubble)
        applyKeep(panelLp, hud?.root)
        hud?.active("keep", keepOn)
        Toast.makeText(
            this, if (keepOn) "Layar tetap menyala" else "Layar normal", Toast.LENGTH_SHORT
        ).show()
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
            gravity = Gravity.TOP or Gravity.START
            x = kit.dp(70)
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
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        hud?.update(ram, temp, net, time)
        hud?.active("rec", InputBackend.recording)
        mon?.text = String.format(
            "RAM %d%%  |  %.1f°C  |  BAT %d%%  |  %s  |  CPU %s  |  %s",
            ram, temp, act.batPct(), net, SysInfo.cpuGhz(), Ping.text()
        )
    }
}
