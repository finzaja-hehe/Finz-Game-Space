package com.finz.gamespace

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class CrosshairPanel(
    private val ctx: Context,
    private val wm: WindowManager,
    private val kit: OverlayKit,
    private val onClosed: () -> Unit
) {
    private val sp = ctx.getSharedPreferences("ch", Context.MODE_PRIVATE)
    private var cross: CrosshairView? = null
    private var panel: View? = null
    private var sv: ScrollView? = null
    private val chips = ArrayList<TextView>()
    private val dark = 0xFF1F2430.toInt()
    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    private val shapes = arrayOf(
        "Titik", "Plus", "Silang", "Lingkaran", "Lingk+Titik", "Plus Penuh", "Kotak", "Plus+Titik", "Caret"
    )
    private val presets = arrayOf(
        "Cyan" to 0xFF22E5FF.toInt(), "Pink" to 0xFFFF4FD8.toInt(), "Hijau" to 0xFF22E58B.toInt(),
        "Kuning" to 0xFFFFE600.toInt(), "Ungu" to 0xFFB45CFF.toInt(), "Merah" to 0xFFFF3B47.toInt(),
        "Biru" to 0xFF2D9CFF.toInt(), "Oranye" to 0xFFFF7A1A.toInt()
    )

    fun restore() {
        if (sp.getBoolean("on", false)) showCross()
    }

    fun destroy() {
        hidePanel(false)
        removeCross()
    }

    private fun mp(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int) =
        LinearLayout.LayoutParams(w, h).apply {
            setMargins(kit.dp(l), kit.dp(t), kit.dp(r), kit.dp(b))
        }

    private fun label(s: String) = kit.tx(s, 9f, kit.muted, true)

    private fun crossParams(): WindowManager.LayoutParams {
        val f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        return WindowManager.LayoutParams(
            kit.dp(240), kit.dp(240), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            f, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
    }

    private fun showCross() {
        if (cross != null) return
        val v = CrosshairView(ctx)
        cross = v
        wm.addView(v, crossParams())
        refresh()
    }

    private fun removeCross() {
        try { cross?.let { wm.removeView(it) } } catch (e: Exception) {}
        cross = null
    }

    private fun refresh() {
        val v = cross ?: return
        v.load(sp)
        val p = crossParams()
        p.x = kit.dp(sp.getInt("ox", 0))
        p.y = kit.dp(sp.getInt("oy", 0))
        try { wm.updateViewLayout(v, p) } catch (e: Exception) {}
    }

    private fun put(k: String, v: Int) {
        sp.edit().putInt(k, v).apply()
        refresh()
    }

    private fun setOn(on: Boolean) {
        sp.edit().putBoolean("on", on).apply()
        if (on) showCross() else removeCross()
    }

    private fun paintChips() {
        chips.forEachIndexed { i, c ->
            c.background = kit.box(if (sp.getInt("shape", 1) == i) kit.accent else dark, 14)
        }
    }

    fun hidePanel(notify: Boolean) {
        val v = panel ?: return
        try { wm.removeView(v) } catch (e: Exception) {}
        panel = null
        sv = null
        if (notify) onClosed()
    }

    private fun rebuild() {
        val y = sv?.scrollY ?: 0
        hidePanel(false)
        showPanel()
        sv?.post { sv?.scrollTo(0, y) }
    }

    private fun toggle(title: String, key: String, def: Boolean): View {
        val t = kit.tx("", 12f, Color.WHITE, true)
        t.gravity = Gravity.CENTER
        t.setPadding(kit.dp(10), kit.dp(9), kit.dp(10), kit.dp(9))
        fun paint() {
            val on = sp.getBoolean(key, def)
            t.text = title + (if (on) ": ON" else ": OFF")
            t.background = if (on) kit.grad(14) else kit.box(dark, 14, kit.accent, 1)
        }
        paint()
        t.setOnClickListener {
            sp.edit().putBoolean(key, !sp.getBoolean(key, def)).apply()
            refresh()
            paint()
        }
        return t
    }

    private fun neonPreset(color: Int) {
        sp.edit().putInt("color", color).putBoolean("neon", true).putInt("neonint", 6)
            .putInt("neoncolor", 0).putBoolean("rainbow", false).apply()
        refresh()
        rebuild()
    }

    fun showPanel() {
        if (panel != null) return
        val dm = ctx.resources.displayMetrics
        val land = dm.widthPixels > dm.heightPixels
        val acc = kit.accent
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = kit.box(0xF012151C.toInt(), 20, acc, 1)
            setPadding(kit.dp(12), kit.dp(10), kit.dp(12), kit.dp(10))
        }

        val head = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(kit.tx("CROSSHAIR", 12f, Color.WHITE, true), LinearLayout.LayoutParams(0, kit.WRAP, 1f))
        val onBtn = kit.tx("", 11f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(kit.dp(14), kit.dp(6), kit.dp(14), kit.dp(6))
        }
        fun paintOn() {
            val on = sp.getBoolean("on", false)
            onBtn.text = if (on) "ON" else "OFF"
            onBtn.setTextColor(if (on) Color.BLACK else Color.WHITE)
            onBtn.background = kit.box(if (on) acc else dark, 14, acc, 1)
        }
        onBtn.setOnClickListener {
            setOn(!sp.getBoolean("on", false))
            paintOn()
        }
        paintOn()
        head.addView(onBtn, mp(kit.WRAP, kit.WRAP, 0, 0, 8, 0))
        val close = kit.tx("✕", 14f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = kit.box(dark, 17, acc, 1)
            setOnClickListener { hidePanel(true) }
        }
        head.addView(close, mp(kit.dp(34), kit.dp(34), 0, 0, 0, 0))
        root.addView(head)

        val body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        body.addView(label("BENTUK"), mp(kit.WRAP, kit.WRAP, 0, 8, 0, 4))
        val sc = HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false }
        val sr = LinearLayout(ctx)
        chips.clear()
        shapes.forEachIndexed { i, n ->
            val c = kit.tx(n, 12f, Color.WHITE, true).apply {
                setPadding(kit.dp(12), kit.dp(7), kit.dp(12), kit.dp(7))
                setOnClickListener {
                    put("shape", i)
                    paintChips()
                }
            }
            chips.add(c)
            sr.addView(c, mp(kit.WRAP, kit.WRAP, 0, 0, 6, 0))
        }
        sc.addView(sr)
        body.addView(sc)
        paintChips()

        body.addView(label("WARNA (32 pilihan + warna bebas)"), mp(kit.WRAP, kit.WRAP, 0, 10, 0, 2))
        body.addView(ColorKit.picker(ctx, kit, sp.getInt("color", 0xFF00FF66.toInt())) { put("color", it) })

        body.addView(label("NEON"), mp(kit.WRAP, kit.WRAP, 0, 12, 0, 4))
        body.addView(toggle("Neon", "neon", false), mp(MATCH, kit.WRAP, 0, 0, 0, 0))
        body.addView(
            kit.slider("Intensitas", 1, 10, sp.getInt("neonint", 5), 76) { put("neonint", it) },
            mp(MATCH, kit.WRAP, 0, 6, 0, 0)
        )
        body.addView(toggle("Denyut", "pulse", false), mp(MATCH, kit.WRAP, 0, 6, 0, 0))
        body.addView(toggle("Pelangi (warna berganti)", "rainbow", false), mp(MATCH, kit.WRAP, 0, 6, 0, 0))
        body.addView(label("PRESET NEON (satu ketukan)"), mp(kit.WRAP, kit.WRAP, 0, 10, 0, 4))
        val ps = HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false }
        val pr = LinearLayout(ctx)
        for (pe in presets) {
            val t = kit.tx(pe.first, 11f, Color.BLACK, true).apply {
                gravity = Gravity.CENTER
                background = kit.box(pe.second, 12)
                setPadding(kit.dp(12), kit.dp(8), kit.dp(12), kit.dp(8))
                setOnClickListener { neonPreset(pe.second) }
            }
            pr.addView(t, mp(kit.WRAP, kit.WRAP, 0, 0, 6, 0))
        }
        ps.addView(pr)
        body.addView(ps)
        body.addView(label("WARNA CAHAYA NEON (opsional)"), mp(kit.WRAP, kit.WRAP, 0, 10, 0, 2))
        body.addView(
            kit.tx("Samakan dengan warna utama", 12f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                background = kit.box(dark, 12, acc, 1)
                setPadding(kit.dp(10), kit.dp(8), kit.dp(10), kit.dp(8))
                setOnClickListener {
                    put("neoncolor", 0)
                    rebuild()
                }
            },
            mp(MATCH, kit.WRAP, 0, 2, 0, 0)
        )
        body.addView(
            ColorKit.picker(ctx, kit, sp.getInt("neoncolor", 0), false) { put("neoncolor", it) }
        )

        body.addView(label("UKURAN DAN POSISI"), mp(kit.WRAP, kit.WRAP, 0, 12, 0, 2))
        fun row(t: String, mn: Int, mx: Int, key: String, def: Int) {
            body.addView(
                kit.slider(t, mn, mx, sp.getInt(key, def), 76) { put(key, it) },
                mp(MATCH, kit.WRAP, 0, 4, 0, 0)
            )
        }
        row("Ukuran", 2, 60, "size", 12)
        row("Tebal", 1, 10, "thick", 2)
        row("Opacity", 10, 100, "alpha", 100)
        row("Celah", 0, 20, "gap", 3)
        row("Kiri/Kanan", -100, 100, "ox", 0)
        row("Atas/Bawah", -100, 100, "oy", 0)
        body.addView(toggle("Garis tepi hitam (tanpa neon)", "outline", true), mp(MATCH, kit.WRAP, 0, 10, 0, 0))

        val rs = kit.tx("Reset pengaturan", 12f, kit.muted, true).apply {
            gravity = Gravity.CENTER
            setPadding(kit.dp(12), kit.dp(10), kit.dp(12), kit.dp(10))
            setOnClickListener {
                val on = sp.getBoolean("on", false)
                sp.edit().clear().putBoolean("on", on).apply()
                refresh()
                rebuild()
            }
        }
        body.addView(rs, mp(MATCH, kit.WRAP, 0, 4, 0, 0))

        val scroll = ScrollView(ctx)
        sv = scroll
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val p = if (land) {
            kit.lp(
                kit.dp(320), minOf(dm.heightPixels - kit.dp(16), kit.dp(460)),
                kit.dp(8), 0, Gravity.END or Gravity.CENTER_VERTICAL
            )
        } else {
            kit.lp(
                dm.widthPixels - kit.dp(16), (dm.heightPixels * 0.5f).toInt(),
                0, kit.dp(8), Gravity.TOP or Gravity.CENTER_HORIZONTAL
            )
        }
        wm.addView(root, p)
        panel = root
    }
}
