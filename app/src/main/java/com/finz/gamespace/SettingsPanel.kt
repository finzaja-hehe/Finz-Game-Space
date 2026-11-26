package com.finz.gamespace

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class SettingsPanel(
    private val ctx: Context,
    private val wm: WindowManager,
    private val kit: OverlayKit
) {
    private var view: View? = null
    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    private val soft = 0xFFD9D4FF.toInt()

    fun hide() {
        view?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        view = null
    }

    private fun rebuild() {
        hide()
        show()
    }

    private fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    private fun fmt(v: Int): String = String.format("%.1f", v.toFloat())

    private fun section(s: String): TextView = kit.tx(s, 13f, Color.WHITE, true).apply {
        background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Palette.alpha(kit.accent, 200), Palette.alpha(kit.accent2, 200))
        ).apply { cornerRadius = kit.dp(8).toFloat() }
        setPadding(kit.dp(10), kit.dp(5), kit.dp(10), kit.dp(5))
    }

    private fun rowBox(): LinearLayout = LinearLayout(ctx).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(kit.dp(14), kit.dp(8), kit.dp(14), kit.dp(8))
    }

    private fun addDivider(body: LinearLayout) {
        val v = View(ctx)
        v.setBackgroundColor(0x22FFFFFF)
        body.addView(v, LinearLayout.LayoutParams(MATCH, 1))
    }

    private fun note(s: String): TextView = kit.tx(s, 11f, soft).apply {
        setPadding(kit.dp(14), kit.dp(6), kit.dp(14), kit.dp(6))
    }

    private fun toggleRow(label: String, on0: Boolean, onChange: (Boolean) -> Unit): View {
        val r = rowBox()
        r.addView(kit.tx(label, 14f, Color.WHITE), LinearLayout.LayoutParams(0, kit.WRAP, 1f))
        val t = GgToggle(ctx)
        t.on = on0
        t.setOnClickListener {
            t.on = !t.on
            onChange(t.on)
        }
        r.addView(t)
        return r
    }

    private fun stepBtn(s: String, action: () -> Unit): TextView =
        kit.tx(s, 16f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x22FFFFFF)
                setStroke(kit.dp(1), kit.accent)
            }
            setOnClickListener { action() }
        }

    private fun stepRow(
        label: String, min: Int, max: Int, now: Int, step: Int, onChange: (Int) -> Unit
    ): View {
        val r = rowBox()
        r.addView(kit.tx(label, 14f, Color.WHITE), LinearLayout.LayoutParams(0, kit.WRAP, 1f))
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val value = kit.tx(fmt(now), 11f, Color.WHITE).apply { gravity = Gravity.CENTER }
        col.addView(value, LinearLayout.LayoutParams(MATCH, kit.WRAP))
        val line = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        val sb = SeekBar(ctx)
        sb.min = min
        sb.max = max
        sb.progress = now
        sb.progressTintList = ColorStateList.valueOf(kit.accent)
        sb.thumbTintList = ColorStateList.valueOf(Color.WHITE)
        fun setVal(v: Int) {
            val x = v.coerceIn(min, max)
            sb.progress = x
            value.text = fmt(x)
            onChange(x)
        }
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                value.text = fmt(p)
                if (fromUser) onChange(p)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        line.addView(stepBtn("-") { setVal(sb.progress - step) },
            LinearLayout.LayoutParams(kit.dp(30), kit.dp(30)))
        line.addView(sb, LinearLayout.LayoutParams(kit.dp(150), kit.WRAP))
        line.addView(stepBtn("+") { setVal(sb.progress + step) },
            LinearLayout.LayoutParams(kit.dp(30), kit.dp(30)))
        col.addView(line)
        r.addView(col)
        return r
    }

    private fun seg(items: List<String>, sel: Int, onSel: (Int) -> Unit): LinearLayout {
        val r = LinearLayout(ctx)
        r.background = kit.box(0x33FFFFFF, 10)
        r.setPadding(kit.dp(2), kit.dp(2), kit.dp(2), kit.dp(2))
        fun paint(s: Int) {
            for (j in 0 until r.childCount) {
                val t = r.getChildAt(j) as TextView
                val on = j == s
                t.setTextColor(if (on) Color.WHITE else soft)
                t.setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
                t.background = if (on) kit.grad(8) else null
            }
        }
        for (i in items.indices) {
            val t = kit.tx(items[i], 12f, Color.WHITE).apply {
                gravity = Gravity.CENTER
                setPadding(kit.dp(10), kit.dp(7), kit.dp(10), kit.dp(7))
                setOnClickListener {
                    paint(i)
                    onSel(i)
                }
            }
            r.addView(t)
        }
        paint(sel)
        return r
    }

    private fun segRow(
        title: String, desc: String, items: List<String>, sel: Int, onSel: (Int) -> Unit
    ): View {
        val r = rowBox()
        val left = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        left.addView(kit.tx(title, 14f, Color.WHITE))
        if (desc.isNotEmpty()) left.addView(kit.tx(desc, 10f, soft))
        r.addView(left, LinearLayout.LayoutParams(0, kit.WRAP, 1f))
        r.addView(seg(items, sel, onSel))
        return r
    }

    private fun connect(m: Int) {
        if (m == InputBackend.ACC) {
            InputBackend.stop()
            toast("Terputus")
            rebuild()
            return
        }
        toast("Menyiapkan ${InputBackend.name(m)}...")
        InputBackend.start(ctx, m) { _, msg ->
            toast(msg)
            rebuild()
        }
    }

    fun show() {
        if (view != null) return
        KeyRouter.attach(ctx)
        val dm = ctx.resources.displayMetrics
        val rad = kit.dp(20).toFloat()
        val bgd = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xF2200F5A.toInt(), 0xF2070419.toInt())
        )
        bgd.cornerRadii = floatArrayOf(rad, rad, 0f, 0f, 0f, 0f, rad, rad)
        bgd.setStroke(kit.dp(1), Palette.alpha(kit.accent, 160))
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.background = bgd
        root.addView(
            kit.tx("Pengaturan Dasar", 18f, Color.WHITE, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, kit.dp(14), 0, kit.dp(10))
            },
            LinearLayout.LayoutParams(MATCH, kit.WRAP)
        )

        val body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        body.addView(section("Pengaturan Dasar"))
        body.addView(toggleRow("Tampilkan titik sentuh", KeyMapStore.getBool(ctx, "dots", false)) {
            KeyMapStore.putBool(ctx, "dots", it)
            DotsOverlay.refresh()
        })
        addDivider(body)
        body.addView(stepRow("Ukuran titik sentuh", 8, 40, KeyMapStore.getInt(ctx, "dotsize", 18), 1) {
            KeyMapStore.putInt(ctx, "dotsize", it)
            DotsOverlay.refresh()
        })
        addDivider(body)
        body.addView(stepRow("Transparansi titik", 10, 100, KeyMapStore.getInt(ctx, "dotalpha", 80), 5) {
            KeyMapStore.putInt(ctx, "dotalpha", it)
            DotsOverlay.refresh()
        })
        addDivider(body)
        body.addView(stepRow("Transparansi Logo", 20, 100, KeyMapStore.getInt(ctx, "ballalpha", 90), 5) {
            KeyMapStore.putInt(ctx, "ballalpha", it)
            KeyRouter.syncBalls()
        })
        addDivider(body)

        body.addView(section("Koneksi"))
        val ready = InputBackend.ready
        body.addView(note(if (ready) "Terhubung: ${InputBackend.name(InputBackend.mode)}" else "Terputus"))
        if (ready) {
            val kc = KeyRouter.kbdCount
            val ks = when {
                kc < 0 -> "Mencari keyboard..."
                kc == 0 -> "Keyboard tidak terbaca " + InputBackend.kbdNote
                else -> "Keyboard terbaca ($kc). Tombol terakhir: " + KeyRouter.lastKey
            }
            body.addView(note(ks))
            val mc = InputBackend.mouseCount
            val ms = when {
                mc < 0 -> "Mencari mouse..."
                mc == 0 -> "Mouse tidak terdeteksi"
                else -> "Mouse terdeteksi ($mc), digenggam: " +
                    (if (InputBackend.grabCount > 0) "ya" else "tidak")
            }
            body.addView(note(ms))
        }
        if (InputBackend.lastMsg.isNotEmpty()) {
            body.addView(note("Terakhir: " + InputBackend.lastMsg))
        }
        val modes = intArrayOf(
            InputBackend.ADB, InputBackend.ROOT, InputBackend.SHIZUKU, InputBackend.ACC
        )
        val cur = if (ready) modes.indexOf(InputBackend.mode).coerceAtLeast(0) else 3
        body.addView(
            segRow("Mode koneksi", "", listOf("ADB", "ROOT", "SHIZUKU", "STOP"), cur) {
                connect(modes[it])
            }
        )
        addDivider(body)

        body.addView(section("Pengaturan Mouse"))
        body.addView(stepRow("Sensitivitas Penunjuk Mouse", 1, 20, KeyMapStore.getInt(ctx, "msens", 5), 1) {
            KeyMapStore.putInt(ctx, "msens", it)
        })
        addDivider(body)
        body.addView(
            segRow(
                "Tingkat Polling Maks Mouse",
                "Atur lebih rendah jika ponsel Anda berperforma rendah",
                listOf("125hz", "250hz", "500hz", "Tanpa Batas"),
                KeyMapStore.getInt(ctx, "mpoll", 1)
            ) {
                KeyMapStore.putInt(ctx, "mpoll", it)
                InputBackend.setPoll()
            }
        )
        addDivider(body)
        body.addView(stepRow("Ukuran Penunjuk Mouse", 5, 60, KeyMapStore.getInt(ctx, "msize", 20), 1) {
            KeyMapStore.putInt(ctx, "msize", it)
        })
        addDivider(body)
        body.addView(
            segRow(
                "Gaya Penunjuk yang Disesuaikan", "",
                listOf("Gaya 1", "Gaya 2", "Gaya 3", "Gaya 4", "Disesuaikan"),
                KeyMapStore.getInt(ctx, "mstyle", 0)
            ) { KeyMapStore.putInt(ctx, "mstyle", it) }
        )
        addDivider(body)

        val sv = ScrollView(ctx)
        sv.addView(body)
        root.addView(sv, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val close = kit.tx("Tutup", 14f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = kit.grad(12)
            setPadding(kit.dp(30), kit.dp(10), kit.dp(30), kit.dp(10))
            setOnClickListener { hide() }
        }
        val cl = LinearLayout.LayoutParams(kit.WRAP, kit.WRAP).apply {
            gravity = Gravity.END
            topMargin = kit.dp(8)
            bottomMargin = kit.dp(10)
            rightMargin = kit.dp(14)
        }
        root.addView(close, cl)

        val w = minOf(dm.widthPixels - kit.dp(8), kit.dp(470))
        wm.addView(root, kit.lp(w, MATCH, 0, 0, Gravity.END or Gravity.TOP))
        view = root
    }
}
