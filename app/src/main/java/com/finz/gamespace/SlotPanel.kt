package com.finz.gamespace

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class SlotPanel(
    private val ctx: Context,
    private val wm: WindowManager,
    private val kit: OverlayKit
) {
    private var view: View? = null
    private val dark = 0xFF1F2430.toInt()
    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT

    fun hide() {
        view?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        view = null
    }

    private fun mp(top: Int) =
        LinearLayout.LayoutParams(MATCH, kit.WRAP).apply { topMargin = kit.dp(top) }

    private fun btn(label: String, on: Boolean, action: () -> Unit): TextView =
        kit.tx(label, 12f, if (on) Color.BLACK else Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = kit.box(if (on) kit.accent else dark, 14, kit.accent, 1)
            setPadding(kit.dp(8), kit.dp(10), kit.dp(8), kit.dp(10))
            setOnClickListener { action() }
        }

    private fun third(a: View, b: View, c: View): LinearLayout {
        val r = LinearLayout(ctx)
        val l1 = LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { rightMargin = kit.dp(5) }
        val l2 = LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { rightMargin = kit.dp(5) }
        val l3 = LinearLayout.LayoutParams(0, kit.WRAP, 1f)
        r.addView(a, l1)
        r.addView(b, l2)
        r.addView(c, l3)
        return r
    }

    fun show() {
        if (view != null) return
        KeyRouter.attach(ctx)
        val cur = KeyMapStore.slot(ctx)
        val rr = KeyMapStore.real(ctx)
        val orient = if (rr[0] > rr[1]) "Landscape" else "Portrait"
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = kit.box(0xF012151C.toInt(), 20, kit.accent, 1)
            setPadding(kit.dp(14), kit.dp(12), kit.dp(14), kit.dp(12))
        }
        root.addView(kit.tx("KONFIGURASI KEYMAP", 13f, Color.WHITE, true))
        root.addView(
            kit.tx("Profil terpisah untuk tiap orientasi ($orient).", 11f, kit.muted), mp(4)
        )
        for (n in 1..KeyMapStore.SLOTS) {
            val cnt = KeyMapStore.count(ctx, n)
            val label = "Profil $n  •  $cnt komponen" + (if (n == cur) "  ✓" else "")
            root.addView(btn(label, n == cur) {
                KeyMapStore.setSlot(ctx, n)
                KeyRouter.reload()
                hide()
                show()
            }, mp(6))
        }
        root.addView(
            third(
                btn("Salin", false) { KeyRouter.exportToClipboard(false) },
                btn("Kirim", false) { KeyRouter.exportToClipboard(true) },
                btn("Impor", false) {
                    hide()
                    ImportPanel(ctx, wm, kit).show()
                }
            ), mp(12)
        )
        root.addView(
            third(
                btn("Kosongkan", false) {
                    KeyRouter.clearProfile()
                    hide()
                    show()
                },
                btn("Tutup", false) { hide() },
                View(ctx)
            ), mp(6)
        )
        val w = minOf(ctx.resources.displayMetrics.widthPixels - kit.dp(16), kit.dp(380))
        wm.addView(root, kit.lp(w, kit.WRAP, 0, 0, Gravity.CENTER))
        view = root
    }
}

class ImportPanel(
    private val ctx: Context,
    private val wm: WindowManager,
    private val kit: OverlayKit
) {
    private var view: View? = null
    private val dark = 0xFF1F2430.toInt()
    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT

    fun hide() {
        view?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        view = null
    }

    private fun mp(top: Int) =
        LinearLayout.LayoutParams(MATCH, kit.WRAP).apply { topMargin = kit.dp(top) }

    private fun btn(label: String, action: () -> Unit): TextView =
        kit.tx(label, 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = kit.box(dark, 14, kit.accent, 1)
            setPadding(kit.dp(8), kit.dp(10), kit.dp(8), kit.dp(10))
            setOnClickListener { action() }
        }

    fun show() {
        if (view != null) return
        KeyRouter.attach(ctx)
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = kit.box(0xF012151C.toInt(), 20, kit.accent, 1)
            setPadding(kit.dp(14), kit.dp(12), kit.dp(14), kit.dp(12))
        }
        root.addView(kit.tx("IMPOR KEYMAP", 13f, Color.WHITE, true))
        root.addView(
            kit.tx("Tekan lama di kotak, pilih Tempel, lalu ketuk Impor.", 11f, kit.muted), mp(4)
        )
        val et = EditText(ctx).apply {
            hint = "FINZKEYMAP1|..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            maxLines = 5
            setTextColor(Color.WHITE)
            setHintTextColor(kit.muted)
            textSize = 12f
            background = kit.box(dark, 12, kit.accent, 1)
            setPadding(kit.dp(10), kit.dp(8), kit.dp(10), kit.dp(8))
        }
        root.addView(et, mp(8))
        val status = kit.tx("", 11f, Color.WHITE, true)
        root.addView(status, mp(6))
        val row = LinearLayout(ctx)
        val l1 = LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { rightMargin = kit.dp(6) }
        val l2 = LinearLayout.LayoutParams(0, kit.WRAP, 1f)
        row.addView(btn("Impor") {
            val err = KeyRouter.applyImport(et.text.toString())
            if (err != null) {
                status.text = err
            } else {
                Toast.makeText(ctx, "Berhasil diimpor", Toast.LENGTH_SHORT).show()
                hide()
            }
        }, l1)
        row.addView(btn("Batal") { hide() }, l2)
        root.addView(row, mp(8))

        val w = minOf(ctx.resources.displayMetrics.widthPixels - kit.dp(16), kit.dp(380))
        val lp = WindowManager.LayoutParams(
            w, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.y = kit.dp(24)
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        wm.addView(root, lp)
        view = root
    }
}
