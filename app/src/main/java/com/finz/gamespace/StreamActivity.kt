package com.finz.gamespace

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class StreamActivity : AppCompatActivity() {

    private lateinit var kit: OverlayKit
    private lateinit var list: MutableList<Layer>
    private lateinit var body: LinearLayout
    private lateinit var sv: ScrollView
    private val dark = 0xFF1F2430.toInt()
    private val colors = intArrayOf(
        0xFFFFFFFF.toInt(), 0xFFFFE600.toInt(), 0xFF22E58B.toInt(), 0xFF22E5FF.toInt(),
        0xFFFF4FD8.toInt(), 0xFFFF5B5B.toInt(), 0xFFFF7A1A.toInt(), 0xFFB45CFF.toInt()
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun lp(w: Int, h: Int, wt: Float = 0f): LinearLayout.LayoutParams =
        if (wt > 0f) LinearLayout.LayoutParams(w, h, wt) else LinearLayout.LayoutParams(w, h)

    private fun mp(top: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kit = OverlayKit(this)
        window.setBackgroundDrawable(ColorDrawable(Palette.BG))
        window.statusBarColor = Palette.BG
        window.navigationBarColor = Palette.BG
        list = StreamStore.load(this)
        sv = ScrollView(this)
        body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(16), dp(20), dp(16), dp(28))
        sv.addView(body)
        setContentView(sv)
        build()
    }

    override fun onPause() {
        persist()
        StreamLayers.sync(this)
        super.onPause()
    }

    private fun persist() {
        StreamStore.save(this, list)
    }

    private fun rebuildKeep() {
        val y = sv.scrollY
        build()
        sv.post { sv.scrollTo(0, y) }
    }

    private fun btn(label: String, on: Boolean, action: () -> Unit): TextView =
        kit.tx(label, 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = if (on) kit.grad(12) else kit.box(dark, 12, kit.accent, 1)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setOnClickListener { action() }
        }

    private fun field(hint: String, value: String, multi: Boolean, onText: (String) -> Unit): EditText {
        val e = EditText(this)
        e.setText(value)
        e.hint = hint
        e.setTextColor(Color.WHITE)
        e.setHintTextColor(kit.muted)
        e.textSize = 14f
        e.background = kit.box(dark, 10, kit.accent, 1)
        e.setPadding(dp(10), dp(8), dp(10), dp(8))
        e.inputType = InputType.TYPE_CLASS_TEXT or
            (if (multi) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0)
        e.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onText(s?.toString() ?: "")
            }
        })
        return e
    }

    private fun build() {
        body.removeAllViews()
        val master = StreamStore.master(this)
        body.addView(kit.tx("Stream Studio", 24f, Color.WHITE, true))
        body.addView(
            kit.tx(
                "Layer tampil di atas game dan TIDAK menghalangi sentuhan, jadi tombol tembak tetap " +
                    "bisa ditekan. Atur posisi dan ukuran di dalam game: dock > Stream > Atur.",
                12f, kit.muted
            ), mp(6)
        )
        if (!Settings.canDrawOverlays(this)) {
            body.addView(btn("Izinkan tampil di atas aplikasi lain", true) {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                )
            }, mp(10))
        }
        body.addView(
            btn(if (master) "Layer stream: ON" else "Layer stream: OFF", master) {
                persist()
                StreamLayers.setMaster(this, !master)
                rebuildKeep()
            },
            mp(12)
        )
        body.addView(kit.tx("TAMBAH LAYER", 11f, kit.muted, true), mp(16))
        for (rowTypes in listOf(intArrayOf(0, 1, 2), intArrayOf(3, 4))) {
            val r = LinearLayout(this)
            for (t in rowTypes) {
                r.addView(
                    btn("+ " + Layer.TYPE_NAMES[t], false) {
                        list.add(StreamStore.create(list, t))
                        persist()
                        rebuildKeep()
                    },
                    lp(0, -2, 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }
                )
            }
            body.addView(r)
        }
        for (l in list) body.addView(card(l), mp(12))
        body.addView(
            btn("Terapkan ke layar", true) {
                persist()
                StreamLayers.sync(this)
                Toast.makeText(this, "Diterapkan", Toast.LENGTH_SHORT).show()
            },
            mp(18)
        )
    }

    private fun card(l: Layer): View {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = kit.box(0xFF140C34.toInt(), 16, Palette.alpha(kit.accent, 120), 1)
        c.setPadding(dp(12), dp(10), dp(12), dp(12))
        val head = LinearLayout(this)
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(field("Nama layer", l.name, false) { l.name = it }, lp(0, -2, 1f))
        head.addView(
            btn(if (l.on) "ON" else "OFF", l.on) {
                l.on = !l.on
                persist()
                StreamLayers.sync(this)
                rebuildKeep()
            },
            lp(dp(56), -2).apply { leftMargin = dp(6) }
        )
        head.addView(
            btn("Hapus", false) {
                list.remove(l)
                persist()
                StreamLayers.sync(this)
                rebuildKeep()
            },
            lp(dp(64), -2).apply { leftMargin = dp(6) }
        )
        c.addView(head)
        c.addView(kit.tx("Jenis: " + Layer.TYPE_NAMES[l.type.coerceIn(0, 4)], 11f, kit.muted), mp(4))
        when (l.type) {
            Layer.WEB -> {
                c.addView(field("Tempel link widget (Saweria/Trakteer/dll)", l.text, false) { l.text = it }, mp(8))
                c.addView(
                    kit.tx(
                        "Salin link widget alert dari dashboard platform donasimu. " +
                            "Alert dan suaranya muncul di layar saat ada donasi.",
                        11f, kit.muted
                    ), mp(4)
                )
            }
            Layer.TEXT, Layer.MARQUEE ->
                c.addView(field("Isi teks / link donasi", l.text, true) { l.text = it }, mp(8))
            else -> {}
        }
        if (l.type == Layer.TEXT || l.type == Layer.MARQUEE || l.type == Layer.TIMER) {
            c.addView(kit.slider("Huruf", 10, 48, l.size, 56) { l.size = it }, mp(8))
            c.addView(colorRow(l), mp(6))
        }
        c.addView(kit.slider("Opasitas", 20, 100, l.alpha, 56) { l.alpha = it }, mp(6))
        c.addView(kit.tx("Posisi cepat", 11f, kit.muted), mp(8))
        c.addView(presets(l), mp(4))
        return c
    }

    private fun colorRow(l: Layer): View {
        val r = LinearLayout(this)
        for (col in colors) {
            val v = View(this)
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            g.setColor(col)
            if (l.color == col) g.setStroke(dp(3), Palette.A[0])
            else g.setStroke(dp(1), Color.WHITE)
            v.background = g
            v.setOnClickListener {
                l.color = col
                persist()
                StreamLayers.sync(this)
                rebuildKeep()
            }
            r.addView(v, lp(dp(26), dp(26)).apply { rightMargin = dp(8) })
        }
        return r
    }

    private fun presets(l: Layer): View {
        val r = LinearLayout(this)
        val names = arrayOf("Kiri atas", "Tengah atas", "Kanan atas", "Bawah")
        for (i in names.indices) {
            r.addView(
                btn(names[i], false) {
                    when (i) {
                        0 -> {
                            l.x = 0.02f
                            l.y = 0.03f
                        }
                        1 -> {
                            l.x = (1f - l.w) / 2f
                            l.y = 0.03f
                        }
                        2 -> {
                            l.x = 1f - l.w - 0.02f
                            l.y = 0.03f
                        }
                        else -> {
                            l.x = (1f - l.w) / 2f
                            l.y = 1f - l.h - 0.04f
                        }
                    }
                    persist()
                    StreamLayers.sync(this)
                },
                lp(0, -2, 1f).apply { rightMargin = dp(4) }
            )
        }
        return r
    }
}
