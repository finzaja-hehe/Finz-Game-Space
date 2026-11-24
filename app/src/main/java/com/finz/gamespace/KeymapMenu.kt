package com.finz.gamespace

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class KeymapMenu(
    private val ctx: Context,
    private val wm: WindowManager,
    private val kit: OverlayKit
) {
    private var view: View? = null
    private val dark = 0xFF1F2430.toInt()
    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT

    companion object {
        private var current: KeymapMenu? = null

        fun open(ctx: Context, wm: WindowManager, kit: OverlayKit) {
            current?.hide()
            val m = KeymapMenu(ctx, wm, kit)
            current = m
            m.show()
        }
    }

    fun hide() {
        view?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        view = null
    }

    private fun rebuild() {
        hide()
        show()
    }

    private fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    private fun mp(top: Int) =
        LinearLayout.LayoutParams(MATCH, kit.WRAP).apply { topMargin = kit.dp(top) }

    private fun small(label: String, on: Boolean, action: () -> Unit): TextView =
        kit.tx(label, 11f, if (on) Color.BLACK else Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = kit.box(if (on) kit.accent else dark, 12, kit.accent, 1)
            setPadding(kit.dp(6), kit.dp(8), kit.dp(6), kit.dp(8))
            setOnClickListener { action() }
        }

    private fun tile(icon: String, label: String, on: Boolean, action: () -> Unit): View {
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setOnClickListener { action() }
        }
        val h = HexButton(ctx, icon, "")
        h.accent = kit.accent
        h.active = on
        col.addView(h)
        col.addView(
            kit.tx(label, 10f, Color.WHITE).apply {
                gravity = Gravity.CENTER
                maxLines = 2
            },
            LinearLayout.LayoutParams(kit.dp(100), kit.WRAP)
        )
        return col
    }

    private fun row2(a: View, b: View): LinearLayout {
        val r = LinearLayout(ctx)
        r.addView(a, LinearLayout.LayoutParams(0, kit.WRAP, 1f))
        r.addView(b, LinearLayout.LayoutParams(0, kit.WRAP, 1f))
        return r
    }

    private fun pickMode(m: Int) {
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
        val rad = kit.dp(18).toFloat()
        val bg = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(0xF01A2150.toInt(), 0xF012151C.toInt())
        )
        bg.cornerRadii = floatArrayOf(0f, 0f, rad, rad, rad, rad, 0f, 0f)
        bg.setStroke(kit.dp(1), kit.accent)
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = bg
            setPadding(kit.dp(14), kit.dp(12), kit.dp(14), kit.dp(12))
        }

        val head = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(kit.tx("🎮 KEYMAP", 14f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, kit.WRAP, 1f))
        head.addView(small("✕", false) { hide() }, LinearLayout.LayoutParams(kit.dp(40), kit.WRAP))
        root.addView(head)

        val body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val ready = InputBackend.ready
        val st = if (ready) "Terhubung: ${InputBackend.name(InputBackend.mode)}" else "Terputus"
        body.addView(kit.tx(st, 11f, kit.muted), mp(6))
        if (ready) {
            val kc = KeyRouter.kbdCount
            val ks = when {
                kc < 0 -> "Mencari keyboard..."
                kc == 0 -> "Keyboard tidak terbaca " + InputBackend.kbdNote
                else -> "Keyboard terbaca ($kc). Tombol terakhir: " + KeyRouter.lastKey
            }
            body.addView(kit.tx(ks, 11f, kit.muted), mp(2))
        }
        if (InputBackend.lastMsg.isNotEmpty()) {
            body.addView(kit.tx("Terakhir: " + InputBackend.lastMsg, 10f, kit.muted), mp(2))
        }

        val chips = LinearLayout(ctx)
        fun chip(label: String, m: Int) {
            val on = ready && InputBackend.mode == m
            val lp = LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { rightMargin = kit.dp(4) }
            chips.addView(small(label, on) { pickMode(m) }, lp)
        }
        chip("ADB", InputBackend.ADB)
        chip("ROOT", InputBackend.ROOT)
        chip("SHZK", InputBackend.SHIZUKU)
        chip("STOP", InputBackend.ACC)
        body.addView(chips, mp(10))

        body.addView(
            row2(
                tile("📁", "Konfigurasi Keymap", false) {
                    hide()
                    SlotPanel(ctx, wm, kit).show()
                },
                tile("✏", "Edit keymap", false) {
                    if (!InputBackend.ready) {
                        toast("Ketuk ADB dulu sampai terhubung")
                    } else {
                        hide()
                        KeyRouter.openEditor()
                    }
                }
            ), mp(12)
        )
        body.addView(
            row2(
                tile("⚙", "Pengaturan Keymap", false) {
                    hide()
                    SettingsPanel(ctx, wm, kit).show()
                },
                tile("🔤", "Mengetik dengan Keyboard", !KeyRouter.enabled) {
                    KeyRouter.switchEnabled(!KeyRouter.enabled)
                    rebuild()
                }
            ), mp(10)
        )
        body.addView(
            row2(
                tile("🔗", "Pairing lewat notifikasi", false) {
                    hide()
                    ctx.startForegroundService(
                        Intent(ctx, PairService::class.java).setAction("START")
                    )
                    toast("Tarik layar ke bawah, tulis kode lewat notifikasi")
                },
                tile("🔄", "Segarkan status", false) { rebuild() }
            ), mp(10)
        )
        body.addView(
            kit.tx("F10 = editor, F9 = nyala/mati, Esc = keluar editor", 10f, kit.muted), mp(10)
        )

        val sv = ScrollView(ctx)
        sv.addView(body)
        root.addView(sv, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val w = minOf(dm.widthPixels - kit.dp(16), kit.dp(290))
        wm.addView(root, kit.lp(w, MATCH, 0, 0, Gravity.START or Gravity.TOP))
        view = root
    }
}
