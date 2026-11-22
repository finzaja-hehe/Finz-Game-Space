package com.finz.gamespace

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class CrosshairActivity : BaseUi() {

    private lateinit var sp: SharedPreferences
    private lateinit var preview: CrosshairView
    private lateinit var toggle: TextView
    private val chips = ArrayList<TextView>()
    private val shapes = arrayOf("Titik", "Plus", "Silang", "Lingkaran", "Lingk.+Titik", "Plus Penuh")
    private val colors = intArrayOf(
        0xFF00FF66.toInt(), 0xFFFF3B47.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFE600.toInt(),
        0xFF00E5FF.toInt(), 0xFF2D9CFF.toInt(), 0xFFFF7A1A.toInt(), 0xFFFF4FD8.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        themeIndex = getSharedPreferences("gs", Context.MODE_PRIVATE)
            .getInt("theme", 0).coerceIn(0, 3)
        sp = getSharedPreferences("ch", Context.MODE_PRIVATE)
        build()
    }

    override fun onResume() {
        super.onResume()
        showToggle()
    }

    private fun head(s: String) = tv(s, 11f, muted, true).apply { letterSpacing = 0.15f }

    private fun build() {
        chips.clear()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(36), dp(18), dp(24))
        }
        root.addView(tv("FINZ CROSSHAIR", 10f, accent, true).apply { letterSpacing = 0.25f })
        root.addView(tv("Atur Crosshair", 24f, Color.WHITE, true))

        val pv = FrameLayout(this).apply { background = box(0xFF20252E.toInt(), 16) }
        preview = CrosshairView(this)
        preview.load(sp)
        pv.addView(preview, FrameLayout.LayoutParams(dp(200), dp(200), Gravity.CENTER))
        root.addView(pv, lp(MATCH, dp(200), t = 14))

        root.addView(head("BENTUK"), lp(WRAP, WRAP, t = 16))
        val sc = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val sr = LinearLayout(this)
        shapes.forEachIndexed { i, n ->
            val c = tv(n, 13f, Color.WHITE, true).apply {
                setPadding(dp(14), dp(8), dp(14), dp(8))
                setOnClickListener { put("shape", i); paintChips() }
            }
            chips.add(c)
            sr.addView(c, lp(WRAP, WRAP, r = 8))
        }
        sc.addView(sr)
        root.addView(sc, lp(MATCH, WRAP, t = 6))
        paintChips()

        root.addView(head("WARNA"), lp(WRAP, WRAP, t = 16))
        val cr = LinearLayout(this)
        colors.forEach { col ->
            val dot = View(this).apply {
                background = box(col, 14, Color.WHITE, 1)
                setOnClickListener { put("color", col) }
            }
            cr.addView(dot, lp(dp(28), dp(28), r = 10))
        }
        root.addView(cr, lp(WRAP, WRAP, t = 8))
        root.addView(seek("Warna bebas", 0, 360, 120) {
            put("color", Color.HSVToColor(floatArrayOf(it.toFloat(), 1f, 1f)))
        }, lp(MATCH, WRAP, t = 10))

        root.addView(seek("Ukuran", 2, 60, sp.getInt("size", 12)) { put("size", it) },
            lp(MATCH, WRAP, t = 12))
        root.addView(seek("Ketebalan", 1, 10, sp.getInt("thick", 2)) { put("thick", it) },
            lp(MATCH, WRAP, t = 8))
        root.addView(seek("Opacity (%)", 10, 100, sp.getInt("alpha", 100)) { put("alpha", it) },
            lp(MATCH, WRAP, t = 8))
        root.addView(seek("Celah tengah", 0, 20, sp.getInt("gap", 3)) { put("gap", it) },
            lp(MATCH, WRAP, t = 8))
        root.addView(seek("Geser kiri / kanan", -100, 100, sp.getInt("ox", 0)) { put("ox", it) },
            lp(MATCH, WRAP, t = 8))
        root.addView(seek("Geser atas / bawah", -100, 100, sp.getInt("oy", 0)) { put("oy", it) },
            lp(MATCH, WRAP, t = 8))

        val ol = tv("", 13f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        fun paintOl() {
            val on = sp.getBoolean("outline", true)
            ol.text = if (on) "Garis tepi hitam: ON" else "Garis tepi hitam: OFF"
            ol.background = box(if (on) accent else panel, 16)
        }
        ol.setOnClickListener {
            sp.edit().putBoolean("outline", !sp.getBoolean("outline", true)).apply()
            preview.load(sp)
            refreshService()
            paintOl()
        }
        paintOl()
        root.addView(ol, lp(MATCH, WRAP, t = 14))

        toggle = tv("", 15f, Color.BLACK, true).apply {
            gravity = Gravity.CENTER
            background = box(accent, 16)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener { switchOverlay() }
        }
        root.addView(toggle, lp(MATCH, WRAP, t = 18))
        showToggle()

        val reset = tv("Reset pengaturan", 13f, muted, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener {
                val on = sp.getBoolean("on", false)
                sp.edit().clear().putBoolean("on", on).apply()
                refreshService()
                build()
            }
        }
        root.addView(reset, lp(MATCH, WRAP, t = 4))

        val sv = ScrollView(this)
        sv.setBackgroundColor(bg)
        sv.addView(root)
        setContentView(sv)
    }

    private fun paintChips() {
        chips.forEachIndexed { i, c ->
            c.background = box(if (sp.getInt("shape", 1) == i) accent else panel, 16)
        }
    }

    private fun put(k: String, v: Int) {
        sp.edit().putInt(k, v).apply()
        preview.load(sp)
        refreshService()
    }

    private fun refreshService() {
        if (sp.getBoolean("on", false)) {
            try { startService(Intent(this, CrosshairService::class.java)) } catch (e: Exception) {}
        }
    }

    private fun showToggle() {
        toggle.text = if (sp.getBoolean("on", false)) "■  MATIKAN CROSSHAIR" else "▶  NYALAKAN CROSSHAIR"
    }

    private fun switchOverlay() {
        if (sp.getBoolean("on", false)) {
            startService(Intent(this, CrosshairService::class.java).setAction("STOP"))
            sp.edit().putBoolean("on", false).apply()
        } else {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Izinkan 'Tampil di atas aplikasi lain', lalu coba lagi",
                    Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                return
            }
            startForegroundService(Intent(this, CrosshairService::class.java))
            sp.edit().putBoolean("on", true).apply()
        }
        showToggle()
    }

    private fun seek(title: String, min: Int, max: Int, now: Int, onChange: (Int) -> Unit): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val top = LinearLayout(this)
        top.addView(tv(title, 12f, muted, true), LinearLayout.LayoutParams(0, WRAP, 1f))
        val value = tv("$now", 12f, Color.WHITE, true)
        top.addView(value)
        col.addView(top)
        val sb = SeekBar(this).apply {
            this.min = min
            this.max = max
            progress = now
            progressTintList = ColorStateList.valueOf(accent)
            thumbTintList = ColorStateList.valueOf(accent)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    value.text = "$p"
                    if (fromUser) onChange(p)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        col.addView(sb)
        return col
    }
}
