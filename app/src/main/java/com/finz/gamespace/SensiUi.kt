package com.finz.gamespace

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SensiUi(private val ctx: Context, private val kit: OverlayKit) {

    private val dark = 0xFF1F2430.toInt()
    private val dev: DevInfo = SensiGen.detect(ctx)
    private val sv = ScrollView(ctx)
    private val body = LinearLayout(ctx)
    private var style = 1
    private var role = 0
    private var level = 1
    private val comp = HashSet<Int>()
    private var shift = 0
    private var lastText = ""
    private lateinit var etBrand: EditText
    private lateinit var etModel: EditText
    private lateinit var etRam: EditText
    private lateinit var etSto: EditText
    private lateinit var resultBox: LinearLayout

    val view: View get() = sv

    init {
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(4), dp(4), dp(4), dp(16))
        sv.isVerticalScrollBarEnabled = false
        sv.addView(body)
        build()
    }

    private fun dp(v: Int) = kit.dp(v)

    private fun lp(w: Int, h: Int, wt: Float = 0f): LinearLayout.LayoutParams =
        if (wt > 0f) LinearLayout.LayoutParams(w, h, wt) else LinearLayout.LayoutParams(w, h)

    private fun mp(top: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }

    private fun card(): LinearLayout {
        val c = LinearLayout(ctx)
        c.orientation = LinearLayout.VERTICAL
        c.background = kit.box(0xFF140C34.toInt(), 16, Palette.alpha(kit.accent, 120), 1)
        c.setPadding(dp(12), dp(10), dp(12), dp(12))
        return c
    }

    private fun field(value: String, hint: String, number: Boolean): EditText {
        val e = EditText(ctx)
        e.setText(value)
        e.hint = hint
        e.setTextColor(Color.WHITE)
        e.setHintTextColor(kit.muted)
        e.textSize = 14f
        e.background = kit.box(dark, 10, kit.accent, 1)
        e.setPadding(dp(10), dp(8), dp(10), dp(8))
        e.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
        e.inputType = if (number) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
        return e
    }

    private fun labeled(label: String, e: EditText): View {
        val r = LinearLayout(ctx)
        r.gravity = Gravity.CENTER_VERTICAL
        r.addView(kit.tx(label, 12f, Color.WHITE), lp(dp(92), -2))
        r.addView(e, lp(0, -2, 1f))
        return r
    }

    private fun group(opts: Array<String>, get: (Int) -> Boolean, toggle: (Int) -> Unit): LinearLayout {
        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        val chips = ArrayList<TextView>()
        fun paint() {
            for (i in chips.indices) {
                chips[i].background = if (get(i)) kit.grad(12) else kit.box(dark, 12, kit.accent, 1)
            }
        }
        var i = 0
        while (i < opts.size) {
            val row = LinearLayout(ctx)
            for (j in 0..1) {
                val k = i + j
                val m = lp(0, -2, 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
                if (k < opts.size) {
                    val t = kit.tx(opts[k], 12f, Color.WHITE, true)
                    t.gravity = Gravity.CENTER
                    t.setPadding(dp(8), dp(10), dp(8), dp(10))
                    t.setOnClickListener {
                        toggle(k)
                        paint()
                    }
                    chips.add(t)
                    row.addView(t, m)
                } else {
                    row.addView(View(ctx), m)
                }
            }
            col.addView(row)
            i += 2
        }
        paint()
        return col
    }

    private fun btn(label: String, main: Boolean, action: () -> Unit): TextView =
        kit.tx(label, 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = if (main) kit.grad(12) else kit.box(dark, 12, kit.accent, 1)
            setPadding(dp(8), dp(11), dp(8), dp(11))
            setOnClickListener { action() }
        }

    private fun build() {
        body.removeAllViews()
        body.addView(
            kit.tx("Data HP terisi otomatis dan bisa diubah. Pilih cara main, lalu buat sensi.", 11f, kit.muted)
        )
        val c1 = card()
        c1.addView(kit.tx("HP KAMU", 11f, kit.muted, true))
        etBrand = field(dev.brand, "Merek", false)
        etModel = field(dev.model, "Seri / model", false)
        etRam = field(dev.ramGb.toString(), "RAM (GB)", true)
        etSto = field(dev.storageGb.toString(), "Penyimpanan (GB)", true)
        c1.addView(labeled("Merek", etBrand), mp(6))
        c1.addView(labeled("Seri HP", etModel), mp(6))
        c1.addView(labeled("RAM (GB)", etRam), mp(6))
        c1.addView(labeled("Penyimpanan", etSto), mp(6))
        c1.addView(
            kit.tx(
                "Chipset: ${dev.soc}  •  ${dev.hz}Hz  •  %.1f inci  •  kosong %.1fGB".format(dev.inch, dev.freeGb),
                11f, kit.muted
            ), mp(6)
        )
        body.addView(c1, mp(10))
        val c2 = card()
        c2.addView(kit.tx("CARA BERMAIN", 11f, kit.muted, true))
        c2.addView(group(SensiGen.STYLES, { it == style }, { style = it }), mp(4))
        c2.addView(kit.tx("GAYA MAIN", 11f, kit.muted, true), mp(10))
        c2.addView(group(SensiGen.ROLES, { it == role }, { role = it }), mp(4))
        c2.addView(kit.tx("LEVEL", 11f, kit.muted, true), mp(10))
        c2.addView(group(SensiGen.LEVELS, { it == level }, { level = it }), mp(4))
        c2.addView(kit.tx("KELUHAN (boleh lebih dari satu)", 11f, kit.muted, true), mp(10))
        c2.addView(
            group(SensiGen.COMPLAINTS, { comp.contains(it) }, { if (!comp.add(it)) comp.remove(it) }),
            mp(4)
        )
        body.addView(c2, mp(10))
        body.addView(btn("Buat Sensi", true) {
            shift = 0
            show()
        }, mp(12))
        resultBox = LinearLayout(ctx)
        resultBox.orientation = LinearLayout.VERTICAL
        body.addView(resultBox, mp(14))
    }

    private fun current(): DevInfo = DevInfo(
        etBrand.text.toString().trim().ifEmpty { dev.brand },
        etModel.text.toString().trim().ifEmpty { dev.model },
        etRam.text.toString().trim().toIntOrNull() ?: dev.ramGb,
        etSto.text.toString().trim().toIntOrNull() ?: dev.storageGb,
        dev.freeGb, dev.hz, dev.inch, dev.sdk, dev.soc
    )

    private fun section(s: String): TextView = kit.tx(s, 14f, Color.WHITE, true)

    private fun bullet(s: String): TextView = kit.tx("•  $s", 12f, 0xFFD9D4FF.toInt())

    private fun kv(k: String, v: String): View {
        val r = LinearLayout(ctx)
        r.gravity = Gravity.CENTER_VERTICAL
        r.addView(kit.tx(k, 12f, kit.muted), lp(0, -2, 1f))
        r.addView(kit.tx(v, 12f, Color.WHITE, true).apply { gravity = Gravity.END }, lp(0, -2, 1.3f))
        return r
    }

    private fun barRow(label: String, v: Int): View {
        val row = LinearLayout(ctx)
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(kit.tx(label, 13f, Color.WHITE), lp(dp(104), -2))
        val track = LinearLayout(ctx)
        track.background = kit.box(0x33FFFFFF, 6)
        val fill = View(ctx)
        fill.background = kit.grad(6)
        track.addView(fill, LinearLayout.LayoutParams(0, dp(12), v.toFloat()))
        track.addView(View(ctx), LinearLayout.LayoutParams(0, dp(12), (100 - v).toFloat()))
        row.addView(track, lp(0, dp(12), 1f).apply { leftMargin = dp(6) })
        row.addView(kit.tx("$v", 14f, Color.WHITE, true).apply { gravity = Gravity.END }, lp(dp(38), -2))
        return row
    }

    private fun headerCard(d: DevInfo, r: SensiResult): View {
        val c = LinearLayout(ctx)
        c.gravity = Gravity.CENTER_VERTICAL
        c.background = kit.grad(16)
        c.setPadding(dp(12), dp(12), dp(12), dp(12))
        val ini = kit.tx(d.brand.take(1).uppercase(), 22f, Color.WHITE, true)
        ini.gravity = Gravity.CENTER
        ini.background = kit.circleBg(false)
        c.addView(ini, lp(dp(46), dp(46)))
        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        col.addView(kit.tx("${d.brand} ${d.model}", 15f, Color.WHITE, true))
        col.addView(kit.tx("${d.ramGb}GB RAM  •  ${d.storageGb}GB  •  ${d.hz}Hz", 11f, 0xFFEFEAFF.toInt()))
        col.addView(kit.tx("Chipset: ${d.soc}", 10f, 0xFFEFEAFF.toInt()))
        val badge = kit.tx("KELAS: " + r.tierName.uppercase(), 10f, Color.BLACK, true)
        badge.background = kit.box(
            when (r.tier) {
                0 -> 0xFFFFB454.toInt()
                1 -> 0xFF22E5FF.toInt()
                else -> 0xFF22E58B.toInt()
            }, 8
        )
        badge.setPadding(dp(8), dp(3), dp(8), dp(3))
        col.addView(badge, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
        c.addView(col, lp(0, -2, 1f).apply { leftMargin = dp(10) })
        return c
    }

    private fun show() {
        val d = current()
        val r = SensiGen.generate(d, style, role, level, comp, shift)
        resultBox.removeAllViews()
        resultBox.addView(headerCard(d, r))
        val c1 = card()
        c1.addView(section("Sensitivitas Free Fire"))
        for (i in r.v.indices) c1.addView(barRow(SensiGen.KEYS[i], r.v[i]), mp(8))
        resultBox.addView(c1, mp(10))
        val c2 = card()
        c2.addView(section("Pengaturan game yang disarankan"))
        for (s in r.settings) c2.addView(kv(s.first, s.second), mp(6))
        resultBox.addView(c2, mp(10))
        val c3 = card()
        c3.addView(section("Tips ${d.brand} (${r.mode})"))
        for (t in r.brandTips) c3.addView(bullet(t), mp(6))
        resultBox.addView(c3, mp(10))
        val c4 = card()
        c4.addView(section("Saran lain"))
        for (t in r.tips) c4.addView(bullet(t), mp(6))
        resultBox.addView(c4, mp(10))
        lastText = SensiGen.format(d, r)
        val r1 = LinearLayout(ctx)
        r1.addView(btn("Terlalu cepat (-4)", false) {
            shift = (shift - 4).coerceIn(-24, 24)
            show()
        }, lp(0, -2, 1f).apply { rightMargin = dp(4) })
        r1.addView(btn("Terlalu lambat (+4)", false) {
            shift = (shift + 4).coerceIn(-24, 24)
            show()
        }, lp(0, -2, 1f))
        resultBox.addView(r1, mp(12))
        val r2 = LinearLayout(ctx)
        r2.addView(btn("Reset", false) {
            shift = 0
            show()
        }, lp(0, -2, 1f).apply { rightMargin = dp(4) })
        r2.addView(btn("Salin hasil", true) {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("sensi", lastText))
            Toast.makeText(ctx, "Disalin", Toast.LENGTH_SHORT).show()
        }, lp(0, -2, 1f))
        resultBox.addView(r2, mp(8))
        sv.post { sv.smoothScrollTo(0, resultBox.top) }
    }
}
