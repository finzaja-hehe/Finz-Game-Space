package com.finz.gamespace

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import android.widget.HorizontalScrollView
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
    private val open = HashSet<String>()
    private var armDel = false
    private var aName = "Finz"
    private var aAmt = "50000"
    private var aMsg = "Terima kasih sudah donasi!"

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

    private fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

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
        persist: Boolean = true, onText: (String) -> Unit
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
                if (persist) changed()
            }
        })
        return e
    }

    private fun fold(parent: LinearLayout, key: String, title: String, build: (LinearLayout) -> Unit) {
        val isOpen = open.contains(key)
        parent.addView(
            btn((if (isOpen) "-  " else "+  ") + title, isOpen) {
                if (!open.add(key)) open.remove(key)
                rebuildKeep()
            },
            mp(8)
        )
        if (isOpen) {
            val box = LinearLayout(ctx)
            box.orientation = LinearLayout.VERTICAL
            build(box)
            parent.addView(box, mp(4))
        }
    }

    private fun chips(opts: Array<String>, sel: Int, onSel: (Int) -> Unit): LinearLayout {
        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        val all = ArrayList<TextView>()
        fun paint(s: Int) {
            for (i in all.indices) {
                all[i].background = if (i == s) kit.grad(12) else kit.box(dark, 12, kit.accent, 1)
            }
        }
        var i = 0
        while (i < opts.size) {
            val row = LinearLayout(ctx)
            for (j in 0 until 3) {
                val k = i + j
                val m = lp(0, -2, 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }
                if (k < opts.size) {
                    val t = kit.tx(opts[k], 12f, Color.WHITE, true)
                    t.gravity = Gravity.CENTER
                    t.setPadding(dp(6), dp(9), dp(6), dp(9))
                    t.setOnClickListener {
                        paint(k)
                        onSel(k)
                    }
                    all.add(t)
                    row.addView(t, m)
                } else {
                    row.addView(View(ctx), m)
                }
            }
            col.addView(row)
            i += 3
        }
        paint(sel)
        return col
    }

    private fun flagChip(l: Layer, flag: Int, label: String, weight: Boolean = true): TextView {
        val t = kit.tx(label, 12f, Color.WHITE, true)
        t.gravity = Gravity.CENTER
        t.setPadding(dp(6), dp(9), dp(6), dp(9))
        fun paint() {
            t.background = if (l.fx and flag != 0) kit.grad(12) else kit.box(dark, 12, kit.accent, 1)
        }
        paint()
        t.setOnClickListener {
            l.fx = l.fx xor flag
            paint()
            changed()
            if (flag == TextStyle.GRAD) rebuildKeep()
        }
        return t
    }

    private fun fontRow(l: Layer): View {
        val hs = HorizontalScrollView(ctx)
        hs.isHorizontalScrollBarEnabled = false
        val row = LinearLayout(ctx)
        val all = ArrayList<TextView>()
        fun paint() {
            for (i in all.indices) {
                all[i].background = if (i == l.font) kit.grad(12) else kit.box(dark, 12, kit.accent, 1)
            }
        }
        for (i in TextStyle.FONTS.indices) {
            val t = kit.tx(TextStyle.FONTS[i], 15f, Color.WHITE, false)
            t.typeface = TextStyle.faceOf(i)
            t.setPadding(dp(12), dp(9), dp(12), dp(9))
            t.setOnClickListener {
                l.font = i
                paint()
                changed()
            }
            all.add(t)
            row.addView(t, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(6) })
        }
        paint()
        hs.addView(row)
        return hs
    }

    private fun mediaUsed(t: String, except: Layer): Boolean = list.any { it !== except && it.text == t }

    private fun dropMedia(l: Layer) {
        val t = l.text
        if ((t.startsWith("img:") || t.startsWith("vid:")) && !mediaUsed(t, l)) {
            try { File(t.substring(4)).delete() } catch (e: Exception) {}
        }
    }

    private fun add(type: Int) {
        val l = StreamStore.create(list, type)
        list.add(l)
        StreamStore.save(ctx, list)
        if (!StreamStore.master(ctx)) StreamLayers.setMaster(ctx, true)
        rebuildKeep()
        onEdit(l.id)
    }

    private fun duplicate(l: Layer) {
        val n = StreamStore.create(list, l.type)
        n.name = l.name + " 2"
        n.text = l.text
        n.size = l.size
        n.color = l.color
        n.color2 = l.color2
        n.thick = l.thick
        n.font = l.font
        n.fx = l.fx
        n.ext = l.ext
        n.anim = l.anim
        n.alpha = l.alpha
        n.w = l.w
        n.h = l.h
        n.x = (l.x + 0.03f).coerceIn(0f, 1f - l.w)
        n.y = (l.y + 0.04f).coerceIn(0f, 1f - l.h)
        list.add(n)
        changed()
        rebuildKeep()
    }

    private fun remove(l: Layer) {
        dropMedia(l)
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

    private fun template(n: Int) {
        list.addAll(StreamStore.template(list, n))
        StreamStore.save(ctx, list)
        if (!StreamStore.master(ctx)) StreamLayers.setMaster(ctx, true) else StreamLayers.sync(ctx)
        toast("Template dipasang")
        rebuildKeep()
    }

    private fun exportClip() {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("stream", StreamStore.export(list)))
        toast("Layout disalin (foto dan video tidak ikut)")
    }

    private fun importClip() {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        val t = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(ctx).toString() else ""
        val n = StreamStore.importAppend(ctx, t)
        if (n < 0) {
            toast("Tidak ada layout yang valid di clipboard")
        } else {
            list.clear()
            list.addAll(StreamStore.load(ctx))
            StreamLayers.sync(ctx)
            toast("$n layer ditambahkan")
            rebuildKeep()
        }
    }

    private fun deleteAll() {
        if (!armDel) {
            armDel = true
            toast("Ketuk sekali lagi untuk menghapus semua layer")
            rebuildKeep()
            return
        }
        armDel = false
        for (l in list.toList()) dropMedia(l)
        list.clear()
        StreamStore.save(ctx, list)
        StreamLayers.sync(ctx)
        rebuildKeep()
    }

    private fun build() {
        body.removeAllViews()
        val master = StreamStore.master(ctx)
        body.addView(
            kit.tx(
                "Layer tampil di atas game dan tidak menghalangi sentuhan. Setelah menambah layer, " +
                    "langsung geser dan atur ukurannya, lalu ketuk Simpan untuk mengunci.",
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
        fold(body, "tools", "Alat Stream (alert, template, cadangan)") { b -> toolsBody(b) }
        body.addView(kit.tx("TAMBAH LAYER", 11f, kit.muted, true), mp(12))
        var i = 0
        while (i < Layer.TYPE_NAMES.size) {
            val row = LinearLayout(ctx)
            for (j in 0 until 2) {
                val t = i + j
                val m = lp(0, -2, 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }
                if (t < Layer.TYPE_NAMES.size) {
                    row.addView(btn("+ " + Layer.TYPE_NAMES[t], false) { add(t) }, m)
                } else {
                    row.addView(View(ctx), m)
                }
            }
            body.addView(row)
            i += 2
        }
        for (l in list) body.addView(card(l), mp(10))
    }

    private fun toolsBody(b: LinearLayout) {
        b.addView(kit.tx("ALERT MANUAL (tes atau pemakaian sendiri)", 11f, kit.muted, true))
        b.addView(field("Nama", aName, false, persist = false) { aName = it }, mp(6))
        b.addView(field("Jumlah (angka)", aAmt, false, number = true, persist = false) { aAmt = it }, mp(6))
        b.addView(field("Pesan", aMsg, false, persist = false) { aMsg = it }, mp(6))
        b.addView(btn("Tampilkan alert", true) {
            StreamTools.alert(ctx, aName.ifBlank { "Anonim" }, aAmt, aMsg)
        }, mp(6))
        b.addView(
            kit.tx("Alert donasi asli muncul lewat Widget Web. Alert manual ini untuk tes posisi dan suara.", 10f, kit.muted),
            mp(4)
        )
        b.addView(kit.tx("TEMPLATE (membuat beberapa layer sekaligus)", 11f, kit.muted, true), mp(12))
        val names = arrayOf("Gaming", "Donasi", "Minimal", "Starting Soon")
        for (r in 0 until 2) {
            val row = LinearLayout(ctx)
            for (j in 0 until 2) {
                val k = r * 2 + j
                row.addView(
                    btn(names[k], false) { template(k) },
                    lp(0, -2, 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }
                )
            }
            b.addView(row)
        }
        b.addView(kit.tx("CADANGAN", 11f, kit.muted, true), mp(12))
        val cr = LinearLayout(ctx)
        cr.addView(btn("Salin layout", false) { exportClip() }, lp(0, -2, 1f).apply { rightMargin = dp(4) })
        cr.addView(btn("Tempel layout", false) { importClip() }, lp(0, -2, 1f))
        b.addView(cr, mp(4))
        b.addView(
            btn(if (armDel) "Ketuk lagi: HAPUS SEMUA" else "Hapus semua layer", armDel) { deleteAll() },
            mp(10)
        )
    }

    private fun textual(l: Layer): Boolean = when (l.type) {
        Layer.WEB, Layer.LOGO, Layer.FRAME -> false
        else -> true
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
        c.addView(
            kit.tx("Jenis: " + Layer.TYPE_NAMES[l.type.coerceIn(0, Layer.TYPE_NAMES.size - 1)], 11f, kit.muted),
            mp(4)
        )
        c.addView(btn("Atur posisi dan ukuran (geser langsung)", true) { onEdit(l.id) }, mp(8))
        val r2 = LinearLayout(ctx)
        r2.addView(btn("Duplikat", false) { duplicate(l) }, lp(0, -2, 1f).apply { rightMargin = dp(4) })
        r2.addView(btn("Ke depan", false) {
            StreamLayers.bringFront(ctx, l.id)
            toast("Layer dibawa ke depan")
        }, lp(0, -2, 1f))
        c.addView(r2, mp(6))
        typeBody(c, l)
        if (textual(l)) {
            fold(c, "s${l.id}", "Font dan gaya teks") { b -> styleBody(b, l) }
            fold(c, "k${l.id}", if (l.type == Layer.SHAPE) "Warna panel" else "Warna teks") { b -> colorBody(b, l) }
        }
        fold(c, "a${l.id}", "Ukuran, posisi, animasi") { b -> layoutBody(b, l) }
        return c
    }

    private fun typeBody(c: LinearLayout, l: Layer) {
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
                c.addView(kit.slider("Muat ulang (mnt)", 0, 30, l.ext, 96) { l.ext = it; changed() }, mp(6))
                c.addView(kit.tx("0 = tidak dimuat ulang otomatis", 10f, kit.muted), mp(2))
            }
            Layer.TEXT, Layer.MARQUEE ->
                c.addView(field("Isi teks / link donasi", l.text, true) { l.text = it }, mp(8))
            Layer.ROTATE -> {
                c.addView(field("Satu baris = satu halaman", l.text, true) { l.text = it }, mp(8))
                c.addView(kit.slider("Ganti tiap (dtk)", 1, 30, l.ext.coerceAtLeast(1), 96) { l.ext = it; changed() }, mp(6))
            }
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
                    toast("Hitung mundur dimulai")
                }, mp(6))
            }
            Layer.COUNTER -> {
                val sp = ctx.getSharedPreferences("stream", Context.MODE_PRIVATE)
                val key = "cnt_${l.id}"
                c.addView(field("Label (contoh: KILL)", l.text, false) { l.text = it }, mp(8))
                c.addView(kit.tx("Nilai sekarang: ${sp.getInt(key, 0)}", 13f, Color.WHITE, true), mp(8))
                val r = LinearLayout(ctx)
                r.addView(btn("+1", true) {
                    sp.edit().putInt(key, sp.getInt(key, 0) + 1).apply()
                    rebuildKeep()
                }, lp(0, -2, 1f).apply { rightMargin = dp(4) })
                r.addView(btn("-1", false) {
                    sp.edit().putInt(key, maxOf(0, sp.getInt(key, 0) - 1)).apply()
                    rebuildKeep()
                }, lp(0, -2, 1f).apply { rightMargin = dp(4) })
                r.addView(btn("Reset", false) {
                    sp.edit().putInt(key, 0).apply()
                    rebuildKeep()
                }, lp(0, -2, 1f))
                c.addView(r, mp(6))
            }
            Layer.SHAPE -> {
                c.addView(field("Label di panel (boleh kosong)", l.text, false) { l.text = it }, mp(8))
                c.addView(kit.tx("Arah gradasi", 11f, kit.muted), mp(8))
                c.addView(chips(arrayOf("Horizontal", "Vertikal", "Diagonal"), l.ext.coerceIn(0, 2)) {
                    l.ext = it
                    changed()
                }, mp(4))
            }
            Layer.SCENE -> {
                c.addView(kit.tx("Preset", 11f, kit.muted), mp(8))
                val names = arrayOf("BRB", "Starting Soon", "Offline", "Ngobrol")
                val texts = arrayOf(
                    "SEBENTAR LAGI KEMBALI\nBRB", "LIVE SEGERA DIMULAI\nStarting Soon",
                    "LIVE SELESAI\nTerima kasih sudah nonton", "LAGI NGOBROL\nIstirahat sebentar"
                )
                for (r in 0 until 2) {
                    val row = LinearLayout(ctx)
                    for (j in 0 until 2) {
                        val k = r * 2 + j
                        row.addView(
                            btn(names[k], false) {
                                l.text = texts[k]
                                l.name = names[k]
                                changed()
                                rebuildKeep()
                            },
                            lp(0, -2, 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) }
                        )
                    }
                    c.addView(row)
                }
                c.addView(field("Baris 1 = judul, baris 2 = keterangan", l.text, true) { l.text = it }, mp(6))
                c.addView(
                    kit.tx("Layar penuh menutupi game secara visual, tapi sentuhan tetap tembus. Matikan lewat ON/OFF.", 10f, kit.muted),
                    mp(4)
                )
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
                r.addView(btn("Logo Finz", false) {
                    dropMedia(l)
                    l.text = ""
                    changed()
                    rebuildKeep()
                }, lp(0, -2, 1f))
                c.addView(r, mp(6))
            }
            Layer.INFO -> c.addView(
                kit.tx("Menampilkan FPS, ping, suhu, RAM, dan baterai. FPS butuh ADB tersambung.", 11f, kit.muted), mp(6)
            )
            Layer.FRAME -> c.addView(
                kit.tx("Bingkai neon di sekeliling layar. Tidak menghalangi sentuhan.", 11f, kit.muted), mp(6)
            )
            Layer.CLOCK -> c.addView(
                kit.tx("Jam digital dan tanggal. Aktifkan Kotak latar di gaya teks agar lebih jelas.", 11f, kit.muted), mp(6)
            )
            else -> {}
        }
    }

    private fun styleBody(b: LinearLayout, l: Layer) {
        b.addView(kit.tx("FONT", 11f, kit.muted, true))
        b.addView(fontRow(l), mp(4))
        b.addView(kit.tx("EFEK", 11f, kit.muted, true), mp(10))
        val r1 = LinearLayout(ctx)
        val m = { lp(0, -2, 1f).apply { setMargins(dp(2), dp(3), dp(2), dp(3)) } }
        r1.addView(flagChip(l, TextStyle.NEON, "Neon"), m())
        r1.addView(flagChip(l, TextStyle.ITALIC, "Miring"), m())
        r1.addView(flagChip(l, TextStyle.BOX, "Kotak latar"), m())
        b.addView(r1)
        val r2 = LinearLayout(ctx)
        r2.addView(flagChip(l, TextStyle.GRAD, "Gradasi"), m())
        r2.addView(flagChip(l, TextStyle.UPPER, "HURUF BESAR"), m())
        r2.addView(View(ctx), m())
        b.addView(r2)
        b.addView(kit.slider("Huruf", 10, 48, l.size, 78) { l.size = it; changed() }, mp(8))
        if (l.type == Layer.SHAPE) {
            b.addView(kit.slider("Bingkai", 0, 12, l.thick, 78) { l.thick = it; changed() }, mp(6))
        } else {
            b.addView(kit.slider("Tebal huruf", 0, 8, l.thick, 78) { l.thick = it; changed() }, mp(6))
        }
    }

    private fun colorBody(b: LinearLayout, l: Layer) {
        if (l.type == Layer.SHAPE) {
            b.addView(kit.tx("WARNA 1", 11f, kit.muted, true))
            b.addView(ColorKit.picker(ctx, kit, l.color) { l.color = it; changed() }, mp(4))
            b.addView(kit.tx("WARNA 2", 11f, kit.muted, true), mp(10))
            b.addView(ColorKit.picker(ctx, kit, l.color2) { l.color2 = it; changed() }, mp(4))
        } else {
            b.addView(kit.tx("WARNA TEKS", 11f, kit.muted, true))
            b.addView(ColorKit.picker(ctx, kit, l.color) { l.color = it; changed() }, mp(4))
            if (l.fx and TextStyle.GRAD != 0) {
                b.addView(kit.tx("WARNA KEDUA (gradasi)", 11f, kit.muted, true), mp(10))
                b.addView(ColorKit.picker(ctx, kit, l.color2) { l.color2 = it; changed() }, mp(4))
            }
        }
    }

    private fun layoutBody(b: LinearLayout, l: Layer) {
        b.addView(kit.tx("ANIMASI", 11f, kit.muted, true))
        b.addView(chips(Layer.ANIM_NAMES, l.anim.coerceIn(0, Layer.ANIM_NAMES.size - 1)) {
            l.anim = it
            changed()
        }, mp(4))
        b.addView(kit.slider("Opasitas", 20, 100, l.alpha, 78) { l.alpha = it; changed() }, mp(8))
        if (l.type == Layer.WEB || l.type == Layer.LOGO || l.type == Layer.FRAME) {
            val lab = if (l.type == Layer.FRAME) "Tebal bingkai" else "Bingkai"
            val mn = if (l.type == Layer.FRAME) 1 else 0
            b.addView(kit.slider(lab, mn, 12, l.thick.coerceAtLeast(mn), 78) { l.thick = it; changed() }, mp(6))
        }
        b.addView(kit.tx("Ukuran dan posisi cepat", 11f, kit.muted), mp(8))
        val sz = LinearLayout(ctx)
        sz.addView(btn("Perkecil", false) { scale(l, 0.85f) }, lp(0, -2, 1f).apply { rightMargin = dp(4) })
        sz.addView(btn("Perbesar", false) { scale(l, 1.18f) }, lp(0, -2, 1f).apply { rightMargin = dp(4) })
        sz.addView(btn("Tengah", false) {
            l.x = (1f - l.w) / 2f
            l.y = (1f - l.h) / 2f
            changed()
        }, lp(0, -2, 1f))
        b.addView(sz, mp(4))
        val names = arrayOf("Kiri atas", "Tengah atas", "Kanan atas", "Bawah")
        val pr = LinearLayout(ctx)
        for (i in names.indices) {
            pr.addView(
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
                lp(0, -2, 1f).apply { rightMargin = dp(3) }
            )
        }
        b.addView(pr, mp(4))
    }
}
