package com.finz.gamespace

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
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
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var bubble: View? = null
    private var panel: View? = null
    private var ramTv: TextView? = null
    private var tempTv: TextView? = null
    private var batTv: TextView? = null
    private var timeTv: TextView? = null
    private var dndTv: TextView? = null

    private val themes = intArrayOf(
        0xFFFF7A1A.toInt(), 0xFF2D9CFF.toInt(),
        0xFFFF3B47.toInt(), 0xFF22E58B.toInt()
    )
    private val accent: Int
        get() = themes[getSharedPreferences("gs", MODE_PRIVATE).getInt("theme", 0).coerceIn(0, 3)]
    private val muted = 0xFF8B93A1.toInt()
    private val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

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
        startFg()
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(tick)
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

    // ---------- helper ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun lp(w: Int, h: Int, x: Int, y: Int, g: Int) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = g
        this.x = x
        this.y = y
    }

    private fun text(s: String, size: Float, color: Int, bold: Boolean) =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    // ---------- bubble ----------
    private fun showBubble() {
        val size = dp(46)
        val v = text("F", 18f, Color.BLACK, true).apply {
            gravity = Gravity.CENTER
            alpha = 0.9f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accent)
            }
        }
        val p = lp(size, size, dp(8), dp(80), Gravity.TOP or Gravity.START)
        var sx = 0
        var sy = 0
        var tx = 0f
        var ty = 0f
        var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = p.x; sy = p.y; tx = e.rawX; ty = e.rawY; moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - tx).toInt()
                    val dy = (e.rawY - ty).toInt()
                    if (abs(dx) > dp(5) || abs(dy) > dp(5)) moved = true
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
    }

    // ---------- panel ----------
    private fun openPanel() {
        bubble?.visibility = View.GONE
        val acc = accent
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(0xEE12151C.toInt())
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), acc)
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val stats = LinearLayout(this)
        ramTv = stat(stats, "RAM")
        tempTv = stat(stats, "SUHU")
        batTv = stat(stats, "BATERAI")
        timeTv = stat(stats, "JAM")
        root.addView(stats)

        // kecerahan
        val bright = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        root.addView(slider("☀", 255, bright) { setBrightness(it) },
            LinearLayout.LayoutParams(-1, WRAP).apply { topMargin = dp(10) })

        // volume
        val am = getSystemService(AudioManager::class.java)
        root.addView(
            slider(
                "🔊", am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                am.getStreamVolume(AudioManager.STREAM_MUSIC)
            ) { am.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) },
            LinearLayout.LayoutParams(-1, WRAP).apply { topMargin = dp(4) }
        )

        // tombol
        val nm = getSystemService(NotificationManager::class.java)
        val dndOn = nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        val buttons = LinearLayout(this)
        dndTv = pill(buttons, if (dndOn) "DND: ON" else "DND: OFF") { toggleDnd() }
        pill(buttons, "Ke Aplikasi") {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        pill(buttons, "Tutup") { closePanel() }
        pill(buttons, "Matikan") { stopSelf() }
        root.addView(buttons, LinearLayout.LayoutParams(-1, WRAP).apply { topMargin = dp(10) })

        val w = minOf(resources.displayMetrics.widthPixels - dp(32), dp(560))
        wm.addView(root, lp(w, WRAP, 0, dp(8), Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        panel = root
        handler.post(tick)
    }

    private fun closePanel() {
        handler.removeCallbacks(tick)
        try { panel?.let { wm.removeView(it) } } catch (e: Exception) {}
        panel = null
        bubble?.visibility = View.VISIBLE
    }

    private fun stat(parent: LinearLayout, label: String): TextView {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        col.addView(text(label, 9f, muted, true))
        val v = text("-", 16f, Color.WHITE, true)
        col.addView(v)
        parent.addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
        return v
    }

    private fun slider(icon: String, max: Int, now: Int, onChange: (Int) -> Unit): LinearLayout {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(text(icon, 16f, Color.WHITE, false))
        val sb = SeekBar(this).apply {
            this.max = max
            progress = now
            progressTintList = ColorStateList.valueOf(accent)
            thumbTintList = ColorStateList.valueOf(accent)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) onChange(p)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        row.addView(sb, LinearLayout.LayoutParams(0, WRAP, 1f).apply { leftMargin = dp(10) })
        return row
    }

    private fun pill(parent: LinearLayout, label: String, action: () -> Unit): TextView {
        val t = text(label, 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(0xFF1F2430.toInt())
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), accent)
            }
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setOnClickListener { action() }
        }
        parent.addView(t, LinearLayout.LayoutParams(0, WRAP, 1f).apply {
            leftMargin = dp(3)
            rightMargin = dp(3)
        })
        return t
    }

    // ---------- aksi ----------
    private fun setBrightness(v: Int) {
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "Izinkan ubah pengaturan sistem dulu", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        Settings.System.putInt(
            contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(
            contentResolver, Settings.System.SCREEN_BRIGHTNESS, v.coerceIn(5, 255)
        )
    }

    private fun toggleDnd() {
        val nm = getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Izinkan akses Jangan Ganggu dulu", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val on = nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        nm.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_ALL
            else NotificationManager.INTERRUPTION_FILTER_NONE
        )
        dndTv?.text = if (on) "DND: OFF" else "DND: ON"
    }

    private fun updateStats() {
        val am = getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val total = mi.totalMem / 1048576
        val used = total - mi.availMem / 1048576
        ramTv?.text = "${used * 100 / total}%"

        val bi = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = (bi?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val level = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = bi?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        tempTv?.text = "%.1f°C".format(temp)
        batTv?.text = "${level * 100 / scale}%"
        timeTv?.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}
