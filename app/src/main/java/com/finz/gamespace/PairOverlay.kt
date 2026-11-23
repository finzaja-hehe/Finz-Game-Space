package com.finz.gamespace

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class PairOverlay(
    private val ctx: Context,
    private val wm: WindowManager,
    private val kit: OverlayKit
) {
    private var view: View? = null
    private val ui = Handler(Looper.getMainLooper())
    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT

    fun hide() {
        view?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        view = null
    }

    private fun field(h: String): EditText = EditText(ctx).apply {
        hint = h
        inputType = InputType.TYPE_CLASS_NUMBER
        setTextColor(Color.WHITE)
        setHintTextColor(kit.muted)
        textSize = 14f
        background = kit.box(0xFF1F2430.toInt(), 12, kit.accent, 1)
        setPadding(kit.dp(12), kit.dp(10), kit.dp(12), kit.dp(10))
    }

    private fun button(label: String, action: () -> Unit): TextView =
        kit.tx(label, 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = kit.box(0xFF1F2430.toInt(), 14, kit.accent, 1)
            setPadding(kit.dp(10), kit.dp(10), kit.dp(10), kit.dp(10))
            setOnClickListener { action() }
        }

    private fun mp(top: Int) =
        LinearLayout.LayoutParams(MATCH, kit.WRAP).apply { topMargin = kit.dp(top) }

    fun show() {
        if (view != null) return
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = kit.box(0xF012151C.toInt(), 20, kit.accent, 1)
            setPadding(kit.dp(14), kit.dp(12), kit.dp(14), kit.dp(12))
        }
        root.addView(kit.tx("🔗 PAIRING DEBUGGING NIRKABEL", 13f, Color.WHITE, true))
        root.addView(
            kit.tx(
                "1. Buka Pengaturan > Opsi pengembang > Debugging nirkabel (pakai Wi-Fi)\n" +
                    "2. Ketuk \"Pasangkan perangkat dengan kode\"\n" +
                    "3. Isi PORT dan KODE yang tampil di sana, lalu ketuk Pasangkan",
                11f, kit.muted
            ), mp(6)
        )
        val port = field("Port (contoh 41823)")
        val code = field("Kode 6 digit")
        val status = kit.tx("", 11f, Color.WHITE, true)
        root.addView(port, mp(10))
        root.addView(code, mp(8))
        root.addView(status, mp(8))

        val row = LinearLayout(ctx)
        val lp1 = LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { rightMargin = kit.dp(6) }
        val lp2 = LinearLayout.LayoutParams(0, kit.WRAP, 1f)
        row.addView(button("Pasangkan") {
            val p = port.text.toString().trim().toIntOrNull()
            val c = code.text.toString().trim()
            if (p == null || c.length != 6) {
                status.text = "Isi port dan kode 6 digit"
            } else {
                status.text = "Memasangkan..."
                Thread {
                    val msg = try {
                        AdbLink.pair(ctx, p, c)
                        "Berhasil! Sekarang pilih ADB di menu Keymap"
                    } catch (e: Throwable) {
                        "Gagal: " + (e.message ?: e.toString())
                    }
                    ui.post { status.text = msg }
                }.start()
            }
        }, lp1)
        row.addView(button("Tutup") { hide() }, lp2)
        root.addView(row, mp(10))

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
