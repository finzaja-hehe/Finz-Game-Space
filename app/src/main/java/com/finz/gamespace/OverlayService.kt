package com.finz.gamespace

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
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
    private var panel: View? = null
    private var panelLp: WindowManager.LayoutParams? = null
    private val vals = HashMap<String, TextView>()
    private var dndBtn: TextView? = null
    private var keepBtn: TextView? = null
    private var crossBtn: TextView? = null
    private var keepOn = false

    private val tick = object : Runnable {
        override fun run() {
            updateStats()
            handler.postDelayed(this, 1000)
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
        cross.destroy()
        try { panel?.let { wm.removeView(it) } } catch (e: Exception) {}
        try { bubble?.let { wm.removeView(it) } } catch (e: Exception) {}
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

    // ---------- bubble ----------
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

    // ---------- panel ----------
    private fun openPanel() {
        cross.hidePanel(false)
        bubble?.visibility = View.GONE
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = kit.box(0xEE12151C.toInt(), 22, kit.accent, 1)
            setPadding(kit.dp(14), kit.dp(10), kit.dp(14), kit.dp(10))
        }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val left = LinearLayout(this)
        dndBtn = circle(left, "🔕", "DND") { toggleDnd() }
        keepBtn = circle(left, "💡", "Layar On") { toggleKeep() }
        circle(left, "🧹", "Bersihkan") { act.cleanRam() }
        crossBtn = circle(left, "🎯", "Crosshair") {
            closePanel()
            cross.showPanel()
        }
        top.addView(left, LinearLayout.LayoutParams(kit.WRAP, kit.WRAP))

        val mid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val jam = kit.tx("--:--", 22f, Color.WHITE, true)
        vals["jam"] = jam
        mid.addView(jam)
        val row = LinearLayout(this)
        stat(row, "ram", "RAM")
        stat(row, "suhu", "SUHU")
        stat(row, "bat", "BATERAI")
        stat(row, "net", "INTERNET")
        mid.addView(row, LinearLayout.LayoutParams(-1, kit.WRAP))
        top.addView(mid, LinearLayout.LayoutParams(0, kit.WRAP, 1f))

        val right = LinearLayout(this)
        circle(right, "⚙", "Pengaturan") { act.openSettings() }
        circle(right, "↩", "Aplikasi") {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        circle(right, "✕", "Tutup") { closePanel() }
        circle(right, "⏻", "Matikan") { stopSelf() }
        top.addView(right, LinearLayout.LayoutParams(kit.WRAP, kit.WRAP))
        root.addView(top)

        val am = getSystemService(AudioManager::class.java)
        val bright = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        val sliders = LinearLayout(this)
        sliders.addView(
            kit.slider("☀", 5, 255, bright, 24) { act.setBrightness(it) },
            LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { rightMargin = kit.dp(10) }
        )
        sliders.addView(
            kit.slider(
                "🔊", 0, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                am.getStreamVolume(AudioManager.STREAM_MUSIC), 24
            ) { am.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) },
            LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { leftMargin = kit.dp(10) }
        )
        root.addView(sliders, LinearLayout.LayoutParams(-1, kit.WRAP).apply { topMargin = kit.dp(4) })

        dndBtn?.background = kit.circleBg(act.dndOn())
        keepBtn?.background = kit.circleBg(keepOn)
        crossBtn?.background = kit.circleBg(
            getSharedPreferences("ch", MODE_PRIVATE).getBoolean("on", false)
        )

        val w = minOf(resources.displayMetrics.widthPixels - kit.dp(24), kit.dp(760))
        val p = kit.lp(w, kit.WRAP, 0, kit.dp(6), Gravity.TOP or Gravity.CENTER_HORIZONTAL, keepOn)
        wm.addView(root, p)
        panel = root
        panelLp = p
        handler.post(tick)
    }

    private fun closePanel() {
        handler.removeCallbacks(tick)
        try { panel?.let { wm.removeView(it) } } catch (e: Exception) {}
        panel = null
        panelLp = null
        bubble?.visibility = View.VISIBLE
    }

    private fun circle(parent: LinearLayout, icon: String, label: String, action: () -> Unit):
        TextView {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setOnClickListener { action() }
        }
        val c = kit.tx(icon, 16f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            background = kit.circleBg(false)
        }
        col.addView(c, LinearLayout.LayoutParams(kit.dp(40), kit.dp(40)))
        col.addView(kit.tx(label, 9f, kit.muted).apply { gravity = Gravity.CENTER })
        parent.addView(col, LinearLayout.LayoutParams(kit.dp(58), kit.WRAP))
        return c
    }

    private fun stat(parent: LinearLayout, key: String, label: String) {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        col.addView(kit.tx(label, 8f, kit.muted, true))
        val v = kit.tx("-", 13f, Color.WHITE, true)
        vals[key] = v
        col.addView(v)
        parent.addView(col, LinearLayout.LayoutParams(0, kit.WRAP, 1f))
    }

    // ---------- aksi ----------
    private fun toggleDnd() {
        dndBtn?.background = kit.circleBg(act.toggleDnd())
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
        applyKeep(panelLp, panel)
        keepBtn?.background = kit.circleBg(keepOn)
        Toast.makeText(
            this, if (keepOn) "Layar tetap menyala" else "Layar normal", Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateStats() {
        vals["ram"]?.text = "${act.ramPct()}%"
        vals["suhu"]?.text = "%.1f°C".format(act.tempC())
        vals["bat"]?.text = "${act.batPct()}%"
        vals["net"]?.text = act.netText()
        vals["jam"]?.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
