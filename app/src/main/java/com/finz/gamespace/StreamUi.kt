package com.finz.gamespace

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

class StreamUi(
    private val ctx: Context,
    private val kit: OverlayKit,
    private val onEdit: (Int) -> Unit,
    private val onPick: (Int) -> Unit
) {
    private val dark = 0xFF1F2430.toInt()
    private val list: MutableList<Layer> = StreamStore.load(ctx)
    private val body = LinearLayout(ctx)
    private val sv = ScrollView(ctx)
    private val ui = Handler(Looper.getMainLooper())
    private val applyRun = Runnable { StreamLayers.sync(ctx) }
    private val colors = intArrayOf(
        0xFFFFFFFF.toInt(), 0xFFFFE600.toInt(), 0xFF22E58B.toInt(), 0xFF22E5FF.toInt(),
        0xFFFF4FD8.toInt(), 0xFFFF5B5B.toInt(), 0xFFFF7A1A.toInt(), 0xFFB45CFF.toInt()
    )

    val view: View get() = sv

    init {
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(4), dp(4), dp(4), dp(16))
        sv.isVerticalScrollBarEnabled = false
        sv.addView(body)
        build()
    }

    private fun dp(v: Int) = kit.dp(v)

    private fun mp(top: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top) }

    private fun lp(w: Int, h: Int, wt: Float = 0f): LinearLayout.LayoutParams =
        if (wt > 0f) LinearLayout.LayoutParams(w, h, wt) else LinearLayout.LayoutParams(w, h)

    private fun changed() {
        StreamStore.save(ctx, list)
        ui.removeCallbacks(applyRun)
        ui.postDelayed(applyRun, 450)
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

    private fun field(
        hint: String, value: String, multi: Boolean, number: Boolean = false,
        onText: (String) -> Unit
    ): EditText {
        val e = EditText(ctx)
        e.setText(value)
        e.hint = hint
        e.setTextColor(Color.WHITE)
        e.setHintTextColor(kit.muted)
        e.textSize = 14f
        e.background = kit.box(dark, 10, kit.accent, 1)
        e.setPadding(dp(10), dp(8), dp(10), dp(8))
        e.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
        e.inputType = if (number) InputType.TYPE_CLASS_NUMBER
        else InputType.TYPE_CLASS_TEXT or (if (multi) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0)
        e.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onText(s?.toString() ?: "")
                changed()
            }
        })
        return e
    }

    private fun add(type: Int) {
        val l = StreamStore.create(list, type)
        list.add(l)
        StreamStore.save(ctx, list)
        if (!StreamStore.master(ctx)) StreamLayers.setMaster(ctx, true)
        rebuildKeep()
        onEdit(l.id)
    }

    private fun clearMedia(l: Layer) {
        val t = l.text
        if (t.startsWith("img:") || t.startsWith("vid:")) {
            try { File(t.substring(4)).delete() } catch (e: Exception) {}
        }
        l.text = ""
        changed()
        rebuildKeep()
    }

    private fun remove(l: Layer) {
        val t = l.text
        if (t.startsWith("img:") || t.startsWith("vid:")) {
            try { File(t.substring(4)).delete() } catch (e: Exception) {}
        }
        list.remove(l)
        changed()
        rebuildKeep()
    }

    private fun scale(l: Layer, f: Float) {
        val cx = l.x + l.w / 2f
        val cy = l.y + l.h / 2f
        l.w = (l.w * f).coerceIn(0.05f, 1f)
        l.h = (l.h * f).coerceIn(0.04f, 1f)
        l.x = (cx - l.w / 2f).coerceIn(0f, 1f - l.w)
        l.y = (cy - l.h / 2f).coerceIn(0f, 1f - l.h)
        changed()
    }

    private fun build() {
        body.removeAllViews()
        val master = StreamStore.master(ctx)
        body.addView(
            kit.tx(
                "Layer tampil di atas game dan tidak menghalangi sentuhan, jadi tombol tembak tetap " +
                    "bisa ditekan. Setelah menambah layer, langsung geser dan atur ukurannya, " +
                    "lalu ketuk Simpan untuk mengunci.",
                11f, kit.muted
            )
        )
        if (!Settings.canDrawOverlays(ctx)) {
            body.addView(btn("Izinkan tampil di atas aplikasi lain", true) {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }, mp(8))
        }
        body.addView(
            btn(if (master) "Layer stream: ON" else "Layer stream: OFF", master) {
                StreamLayers.setMaster(ctx, !master)
                rebuildKeep()
            },
            mp(8)
        )
        body.addView(kit.tx("TAMBAH LAYER", 11f, kit.muted, true), mp(12))
        var i = 0
        while (i < Layer.TYPE_NAMES.size) {
            val row = LinearLayout(ctx)
            for (j in 0 until 3) {
                val t = i + j
                val m = lp(0, -2, 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }
                if (t < Layer.TYPE_NAMES.size) {
                    row.addView(btn("+ " + Layer.TYPE_NAMES[t], false) { add(t) }, m)
                } else {
                    row.addView(View(ctx), m)
                }
            }
            body.addView(row)
            i += 3
        }
        for (l in list) body.addView(card(l), mp(10))
    }

    private fun card(l: Layer): View {
        val c = LinearLayout(ctx)
        c.orientation = LinearLayout.VERTICAL
        c.background = kit.box(0xFF140C34.toInt(), 16, Palette.alpha(kit.accent, 120), 1)
        c.setPadding(dp(10), dp(8), dp(10), dp(10))
        val head = LinearLayout(ctx)
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(field("Nama layer", l.name, false) { l.name = it }, lp(0, -2, 1f))
        head.addView(
            btn(if (l.on) "ON" else "OFF", l.on) {
                l.on = !l.on
                changed()
                rebuildKeep()
            },
            lp(dp(54), -2).apply { leftMargin = dp(6) }
        )
        head.addView(btn("Hapus", false) { remove(l) }, lp(dp(60), -2).apply { leftMargin = dp(6) })
        c.addView(head)
        c.addView(kit.tx("Jenis: " + Layer.TYPE_NAMES[l.type.coerceIn(0, 8)], 11f, kit.muted), mp(4))
        c.addView(btn("Atur posisi dan ukuran (geser langsung)", true) { onEdit(l.id) }, mp(8))

        when (l.type) {
            Layer.WEB -> {
                c.addView(field("Tempel link widget (Saweria, Trakteer, dll)", l.text, false) { l.text = it }, mp(8))
                c.addView(
                    kit.tx(
                        "Salin link widget alert dari dashboard platform donasimu. Alert dan suaranya " +
                            "muncul di layar saat ada donasi. Suara lewat volume media.",
                        11f, kit.muted
                    ), mp(4)
                )
            }
            Layer.TEXT, Layer.MARQUEE ->
                c.addView(field("Isi teks / link donasi", l.text, true) { l.text = it }, mp(8))
            Layer.GOAL -> {
                val p = (l.text + "||").split("|")
                val s = arrayOf(p.getOrElse(0) { "" }, p.getOrElse(1) { "0" }, p.getOrElse(2) { "1" })
                fun upd() {
                    l.text = s[0] + "|" + s[1] + "|" + s[2]
                }
                c.addView(field("Judul goal", s[0], false) { s[0] = it; upd() }, mp(8))
                c.addView(field("Terkumpul (angka)", s[1], false, true) { s[1] = it; upd() }, mp(6))
                c.addView(field("Target (angka)", s[2], false, true) { s[2] = it; upd() }, mp(6))
            }
            Layer.COUNT -> {
                c.addView(field("Menit hitung mundur", l.text, false, true) { l.text = it }, mp(8))
                c.addView(btn("Mulai / reset hitung mundur", false) {
                    val m = l.text.trim().toIntOrNull() ?: 10
                    ctx.getSharedPreferences("stream", Context.MODE_PRIVATE).edit()
                        .putLong("end_${l.id}", System.currentTimeMillis() + m * 60000L).apply()
                    Toast.makeText(ctx, "Hitung mundur dimulai", Toast.LENGTH_SHORT).show()
                }, mp(6))
            }
            Layer.LOGO -> {
                val st = when {
                    l.text.startsWith("img:") -> "Foto terpasang"
                    l.text.startsWith("vid:") -> "Video terpasang"
                    else -> "Logo Finz (bawaan)"
                }
                c.addView(kit.tx(st, 12f, Color.WHITE, true), mp(8))
                val r = LinearLayout(ctx)
                r.addView(btn("Pilih foto / video", true) { onPick(l.id) },
                    lp(0, -2, 1f).apply { rightMargin = dp(4) })
                r.addView(btn("Logo Finz", false) { clearMedia(l) }, lp(0, -2, 1f))
                c.addView(r, mp(6))
            }
            Layer.INFO -> c.addView(
                kit.tx("Menampilkan FPS, ping, suhu, RAM, dan baterai. FPS butuh ADB tersambung.", 11f, kit.muted),
                mp(6)
            )
            Layer.FRAME -> c.addView(
                kit.tx("Bingkai neon di sekeliling layar. Tidak menghalangi sentuhan.", 11f, kit.muted),
                mp(6)
            )
            else -> {}
        }

        val textual = l.type == Layer.TEXT || l.type == Layer.MARQUEE || l.type == Layer.TIMER ||
            l.type == Layer.INFO || l.type == Layer.GOAL || l.type == Layer.COUNT
        if (textual) {
            c.addView(kit.slider("Huruf", 10, 48, l.size, 62) { l.size = it; changed() }, mp(8))
            c.addView(kit.slider("Tebal huruf", 0, 8, l.thick, 62) { l.thick = it; changed() }, mp(6))
            c.addView(colorRow(l), mp(6))
        } else {
            val lab = if (l.type == Layer.FRAME) "Tebal bingkai" else "Bingkai"
            val mn = if (l.type == Layer.FRAME) 1 else 0
            c.addView(kit.slider(lab, mn, 12, l.thick.coerceAtLeast(mn), 62) { l.thick = it; changed() }, mp(8))
        }
        c.addView(kit.slider("Opasitas", 20, 100, l.alpha, 62) { l.alpha = it; changed() }, mp(6))

        c.addView(kit.tx("Ukuran dan posisi cepat", 11f, kit.muted), mp(8))
        val sz = LinearLayout(ctx)
        sz.addView(btn("Perkecil", false) { scale(l, 0.85f) }, lp(0, -2, 1f).apply { rightMargin = dp(4) })
        sz.addView(btn("Perbesar", false) { scale(l, 1.18f) }, lp(0, -2, 1f).apply { rightMargin = dp(4) })
        sz.addView(btn("Tengah", false) {
            l.x = (1f - l.w) / 2f
            l.y = (1f - l.h) / 2f
            changed()
        }, lp(0, -2, 1f))
        c.addView(sz, mp(4))
        c.addView(presets(l), mp(4))
        return c
    }

    private fun colorRow(l: Layer): View {
        val r = LinearLayout(ctx)
        for (col in colors) {
            val v = View(ctx)
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            g.setColor(col)
            if (l.color == col) g.setStroke(dp(3), Palette.A[0]) else g.setStroke(dp(1), Color.WHITE)
            v.background = g
            v.setOnClickListener {
                l.color = col
                changed()
                rebuildKeep()
            }
            r.addView(v, lp(dp(26), dp(26)).apply { rightMargin = dp(8) })
        }
        return r
    }

    private fun presets(l: Layer): View {
        val r = LinearLayout(ctx)
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
                    changed()
                },
                lp(0, -2, 1f).apply { rightMargin = dp(4) }
            )
        }
        return r
    }
}
