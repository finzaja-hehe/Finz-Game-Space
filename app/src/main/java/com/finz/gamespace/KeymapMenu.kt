package com.finz.gamespace

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
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

    private fun mp(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int) =
        LinearLayout.LayoutParams(w, h).apply {
            setMargins(kit.dp(l), kit.dp(t), kit.dp(r), kit.dp(b))
        }

    private fun btn(label: String, on: Boolean, action: () -> Unit): TextView =
        kit.tx(label, 12f, if (on) Color.BLACK else Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = kit.box(if (on) kit.accent else dark, 14, kit.accent, 1)
            setPadding(kit.dp(8), kit.dp(10), kit.dp(8), kit.dp(10))
            setOnClickListener { action() }
        }

    private fun pickMode(m: Int) {
        if (m == InputBackend.ACC) {
            InputBackend.stop()
            toast("Mode dimatikan")
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
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = kit.box(0xF012151C.toInt(), 20, kit.accent, 1)
            setPadding(kit.dp(14), kit.dp(12), kit.dp(14), kit.dp(12))
        }
        root.addView(kit.tx("🎮 KEYMAP", 13f, Color.WHITE, true))

        val ready = InputBackend.ready
        val status = if (ready) "Mode aktif: ${InputBackend.name(InputBackend.mode)}"
        else "Belum terhubung. Ketuk ADB di bawah."
        root.addView(kit.tx(status, 11f, kit.muted), mp(kit.WRAP, kit.WRAP, 0, 4, 0, 0))
        if (ready) {
            val kc = KeyRouter.kbdCount
            val ks = when {
                kc < 0 -> "Mencari keyboard..."
                kc == 0 -> "Keyboard tidak terbaca " + InputBackend.kbdNote
                else -> "$kc perangkat keyboard terbaca. Tombol terakhir: " + KeyRouter.lastKey
            }
            root.addView(kit.tx(ks, 11f, kit.muted), mp(kit.WRAP, kit.WRAP, 0, 2, 0, 0))
        }
        if (InputBackend.lastMsg.isNotEmpty()) {
            root.addView(kit.tx("Terakhir: " + InputBackend.lastMsg, 10f, kit.muted),
                mp(kit.WRAP, kit.WRAP, 0, 2, 0, 0))
        }

        val row = LinearLayout(ctx)
        fun chip(label: String, m: Int) {
            val on = ready && InputBackend.mode == m
            val lp = mp(0, kit.WRAP, 0, 0, 5, 0)
            lp.weight = 1f
            row.addView(btn(label, on) { pickMode(m) }, lp)
        }
        chip("ROOT", InputBackend.ROOT)
        chip("SHIZUKU", InputBackend.SHIZUKU)
        chip("ADB", InputBackend.ADB)
        chip("STOP", InputBackend.ACC)
        root.addView(row, mp(MATCH, kit.WRAP, 0, 10, 0, 0))

        root.addView(btn("🔗  Pairing lewat notifikasi", false) {
            hide()
            ctx.startForegroundService(Intent(ctx, PairService::class.java).setAction("START"))
            toast("Tarik layar ke bawah, tulis kode lewat notifikasi")
        }, mp(MATCH, kit.WRAP, 0, 8, 0, 0))

        root.addView(btn("✎  Atur tombol (Editor)", false) {
            if (!InputBackend.ready) {
                toast("Ketuk ADB dulu sampai aktif")
            } else {
                hide()
                KeyRouter.openEditor()
            }
        }, mp(MATCH, kit.WRAP, 0, 8, 0, 0))

        val on = KeyRouter.enabled
        root.addView(
            btn(if (on) "Keymap: NYALA (ketuk untuk mati)" else "Keymap: MATI (ketuk untuk nyala)", on) {
                KeyRouter.enabled = !KeyRouter.enabled
                rebuild()
            },
            mp(MATCH, kit.WRAP, 0, 8, 0, 0)
        )
        root.addView(kit.tx("F10 = editor, F9 = nyala/mati, Esc = tutup editor", 10f, kit.muted),
            mp(kit.WRAP, kit.WRAP, 0, 6, 0, 0))
        root.addView(btn("↻  Segarkan status", false) { rebuild() },
            mp(MATCH, kit.WRAP, 0, 8, 0, 0))
        root.addView(btn("Tutup", false) { hide() }, mp(MATCH, kit.WRAP, 0, 8, 0, 0))

        val w = minOf(ctx.resources.displayMetrics.widthPixels - kit.dp(16), kit.dp(380))
        wm.addView(root, kit.lp(w, kit.WRAP, 0, 0, Gravity.CENTER))
        view = root
    }
}
