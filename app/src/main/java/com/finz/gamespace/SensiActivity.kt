package com.finz.gamespace

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SensiActivity : AppCompatActivity() {

    private lateinit var kit: OverlayKit
    private lateinit var dev: DevInfo
    private lateinit var body: LinearLayout
    private lateinit var sv: ScrollView
    private lateinit var resultBox: LinearLayout
    private lateinit var etBrand: EditText
    private lateinit var etModel: EditText
    private lateinit var etRam: EditText
    private lateinit var etSto: EditText
    private val dark = 0xFF1F2430.toInt()
    private var style = 1
    private var role = 0
    private var level = 1
    private val comp = HashSet<Int>()
    private var shift = 0
    private var lastText = ""

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
        dev = SensiGen.detect(this)
        sv = ScrollView(this)
        body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(16), dp(20), dp(16), dp(28))
        sv.addView(body)
        setContentView(sv)
        build()
    }

    private fun field(value: String, hint: String, number: Boolean): EditText {
        val e = EditText(this)
        e.setText(value)
        e.hint = hint
        e.setTextColor(Color.WHITE)
        e.setHintTextColor(kit.muted)
        e.textSize = 14f
        e.background = kit.box(dark, 10, kit.accent, 1)
        e.setPadding(dp(10), dp(8), dp(10), dp(8))
        e.inputType = if (number) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
        return e
    }

    private fun labeled(label: String, e: EditText): View {
        val r = LinearLayout(this)
        r.gravity = Gravity.CENTER_VERTICAL
        r.addView(kit.tx(label, 12f, Color.WHITE), lp(dp(96), -2))
        r.addView(e, lp(0, -2, 1f))
        return r
    }

    private fun group(opts: Array<String>, get: (Int) -> Boolean, toggle: (Int) -> Unit): LinearLayout {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        val chips = ArrayList<TextView>()
        fun paint() {
            for (i in chips.indices) {
                chips[i].background = if (get(i)) kit.grad(12) else kit.box(dark, 12, kit.accent, 1)
            }
        }
        var i = 0
        while (i < opts.size) {
            val row = LinearLayout(this)
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
                    row.addView(View(this), m)
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
        body.addView(kit.tx("Generator Sensi Free Fire", 22f, Color.WHITE, true))
        body.addView(
            kit.tx("Isi data HP dan cara mainmu. Data HP terisi otomatis dan bisa diubah.", 12f, kit.muted),
            mp(4)
        )
        body.addView(kit.tx("HP KAMU", 11f, kit.muted, true), mp(14))
        etBrand = field(dev.brand, "Merek", false)
        etModel = field(dev.model, "Seri / model", false)
        etRam = field(dev.ramGb.toString(), "RAM (GB)", true)
        etSto = field(dev.storageGb.toString(), "Penyimpanan (GB)", true)
        body.addView(labeled("Merek", etBrand), mp(6))
        body.addView(labeled("Seri HP", etModel), mp(6))
        body.addView(labeled("RAM (GB)", etRam), mp(6))
        body.addView(labeled("Penyimpanan", etSto), mp(6))
        body.addView(
            kit.tx(
                "Layar ${dev.hz}Hz, %.1f inci, ruang kosong %.1fGB, Android SDK ${dev.sdk}"
                    .format(dev.inch, dev.freeGb),
                11f, kit.muted
            ), mp(6)
        )
        body.addView(kit.tx("CARA BERMAIN", 11f, kit.muted, true), mp(14))
        body.addView(group(SensiGen.STYLES, { it == style }, { style = it }), mp(4))
        body.addView(kit.tx("GAYA MAIN", 11f, kit.muted, true), mp(12))
        body.addView(group(SensiGen.ROLES, { it == role }, { role = it }), mp(4))
        body.addView(kit.tx("LEVEL", 11f, kit.muted, true), mp(12))
        body.addView(group(SensiGen.LEVELS, { it == level }, { level = it }), mp(4))
        body.addView(kit.tx("KELUHAN (boleh pilih lebih dari satu)", 11f, kit.muted, true), mp(12))
        body.addView(
            group(
                SensiGen.COMPLAINTS,
                { comp.contains(it) },
                { if (!comp.add(it)) comp.remove(it) }
            ), mp(4)
        )
        body.addView(btn("Buat Sensi", true) {
            shift = 0
            show()
        }, mp(16))
        resultBox = LinearLayout(this)
        resultBox.orientation = LinearLayout.VERTICAL
        body.addView(resultBox, mp(16))
    }

    private fun current(): DevInfo = DevInfo(
        etBrand.text.toString().trim().ifEmpty { dev.brand },
        etModel.text.toString().trim().ifEmpty { dev.model },
        etRam.text.toString().trim().toIntOrNull() ?: dev.ramGb,
        etSto.text.toString().trim().toIntOrNull() ?: dev.storageGb,
        dev.freeGb, dev.hz, dev.inch, dev.sdk
    )

    private fun barRow(label: String, v: Int): View {
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(kit.tx(label, 13f, Color.WHITE), lp(dp(110), -2))
        val track = LinearLayout(this)
        track.background = kit.box(0x33FFFFFF, 6)
        val fill = View(this)
        fill.background = kit.grad(6)
        track.addView(fill, LinearLayout.LayoutParams(0, dp(12), v.toFloat()))
        track.addView(View(this), LinearLayout.LayoutParams(0, dp(12), (100 - v).toFloat()))
        row.addView(track, lp(0, dp(12), 1f).apply { leftMargin = dp(6) })
        row.addView(kit.tx("$v", 14f, Color.WHITE, true).apply { gravity = Gravity.END }, lp(dp(40), -2))
        return row
    }

    private fun show() {
        val d = current()
        val res = SensiGen.generate(d, style, role, level, comp, shift)
        val v = res.first
        resultBox.removeAllViews()
        resultBox.addView(kit.tx("Hasil Sensi Free Fire", 18f, Color.WHITE, true))
        resultBox.addView(
            kit.tx("${d.brand} ${d.model}  •  ${d.ramGb}GB RAM  •  ${d.storageGb}GB  •  ${d.hz}Hz", 11f, kit.muted),
            mp(2)
        )
        for (i in v.indices) resultBox.addView(barRow(SensiGen.KEYS[i], v[i]), mp(8))
        resultBox.addView(kit.tx("Saran pengaturan", 14f, Color.WHITE, true), mp(14))
        for (t in res.second) resultBox.addView(kit.tx("•  $t", 12f, 0xFFD9D4FF.toInt()), mp(6))
        lastText = SensiGen.format(d, v)
        val r1 = LinearLayout(this)
        r1.addView(btn("Terlalu cepat (-4)", false) {
            shift = (shift - 4).coerceIn(-24, 24)
            show()
        }, lp(0, -2, 1f).apply { rightMargin = dp(4) })
        r1.addView(btn("Terlalu lambat (+4)", false) {
            shift = (shift + 4).coerceIn(-24, 24)
            show()
        }, lp(0, -2, 1f))
        resultBox.addView(r1, mp(14))
        val r2 = LinearLayout(this)
        r2.addView(btn("Reset", false) {
            shift = 0
            show()
        }, lp(0, -2, 1f).apply { rightMargin = dp(4) })
        r2.addView(btn("Salin hasil", true) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("sensi", lastText))
            Toast.makeText(this, "Disalin", Toast.LENGTH_SHORT).show()
        }, lp(0, -2, 1f))
        resultBox.addView(r2, mp(8))
        sv.post { sv.smoothScrollTo(0, resultBox.top) }
    }
}
