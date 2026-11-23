package com.finz.gamespace

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.provider.Settings
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

    private fun needAccessibility() {
        toast("Aktifkan Finz Keymap di Aksesibilitas dulu")
        ctx.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun pickMode(m: Int) {
        if (m == InputBackend.ACC) {
            InputBackend.stop()
            toast("Mode dasar (Aksesibilitas)")
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
        val svc = TapService.instance
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = kit.box(0xF012151C.toInt(), 20, kit.accent, 1)
            setPadding(kit.dp(14), kit.dp(12), kit.dp(14), kit.dp(12))
        }
        root.addView(kit.tx("🎮 KEYMAP", 13f, Color.WHITE, true))

        val st = if (svc == null) "Finz Keymap (Aksesibilitas) belum aktif"
        else "Keymap: " + (if (svc.isEnabled()) "nyala" else "mati")
        root.addView(kit.tx(st, 11f, kit.muted), mp(kit.WRAP, kit.WRAP, 0, 4, 0, 0))
        root.addView(kit.tx(InputBackend.statusText(), 11f, kit.muted),
            mp(kit.WRAP, kit.WRAP, 0, 2, 0, 8))

        val row = LinearLayout(ctx)
        fun chip(label: String, m: Int) {
            val on = if (m == InputBackend.ACC) !InputBackend.ready
            else (InputBackend.ready && InputBackend.mode == m)
            val lp = mp(0, kit.WRAP, 0, 0, 5, 0)
            lp.weight = 1f
            row.addView(btn(label, on) { pickMode(m) }, lp)
        }
        chip("ROOT", InputBackend.ROOT)
        chip("SHIZUKU", InputBackend.SHIZUKU)
        chip("ADB", InputBackend.ADB)
        chip("DASAR", InputBackend.ACC)
        root.addView(row, mp(MATCH, kit.WRAP, 0, 0, 0, 0))

        root.addView(btn("🔗  Pairing debugging nirkabel", false) {
            hide()
            PairOverlay(ctx, wm, kit).show()
        }, mp(MATCH, kit.WRAP, 0, 10, 0, 0))

        root.addView(btn("✎  Atur tombol (Editor)", false) {
            val s = TapService.instance
            if (s == null) {
                needAccessibility()
            } else {
                hide()
                s.openEditorExternal()
            }
        }, mp(MATCH, kit.WRAP, 0, 8, 0, 0))

        val on = svc?.isEnabled() ?: false
        root.addView(
            btn(if (on) "Keymap: NYALA (ketuk untuk mati)" else "Keymap: MATI (ketuk untuk nyala)", on) {
                val t = TapService.instance
                if (t == null) {
                    needAccessibility()
                } else {
                    t.toggleEnabled()
                    rebuild()
                }
            },
            mp(MATCH, kit.WRAP, 0, 8, 0, 0)
        )

        if (svc == null) {
            root.addView(btn("Buka pengaturan Aksesibilitas", false) { needAccessibility() },
                mp(MATCH, kit.WRAP, 0, 8, 0, 0))
        }
        root.addView(btn("Tutup", false) { hide() }, mp(MATCH, kit.WRAP, 0, 8, 0, 0))

        val w = minOf(ctx.resources.displayMetrics.widthPixels - kit.dp(16), kit.dp(380))
        wm.addView(root, kit.lp(w, kit.WRAP, 0, 0, Gravity.CENTER))
        view = root
    }
}
