package com.finz.gamespace

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.min

class StreamPanel(
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

    private fun rebuild() {
        hide()
        show()
    }

    private fun mp(top: Int) =
        LinearLayout.LayoutParams(MATCH, kit.WRAP).apply { topMargin = kit.dp(top) }

    private fun btn(label: String, on: Boolean, action: () -> Unit): TextView =
        kit.tx(label, 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = if (on) kit.grad(12) else kit.box(dark, 12, kit.accent, 1)
            setPadding(kit.dp(8), kit.dp(9), kit.dp(8), kit.dp(9))
            setOnClickListener { action() }
        }

    fun show() {
        if (view != null) return
        val list = StreamStore.load(ctx)
        val master = StreamStore.master(ctx)
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL
        root.background = kit.box(0xF012151C.toInt(), 20, kit.accent, 1)
        root.setPadding(kit.dp(14), kit.dp(12), kit.dp(14), kit.dp(12))

        val head = LinearLayout(ctx)
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(kit.tx("STREAM STUDIO", 13f, Color.WHITE, true), LinearLayout.LayoutParams(0, kit.WRAP, 1f))
        head.addView(btn("✕", false) { hide() }, LinearLayout.LayoutParams(kit.dp(40), kit.WRAP))
        root.addView(head)
        root.addView(
            btn(if (master) "Layer stream: ON" else "Layer stream: OFF", master) {
                StreamLayers.setMaster(ctx, !master)
                rebuild()
            },
            mp(8)
        )

        val body = LinearLayout(ctx)
        body.orientation = LinearLayout.VERTICAL
        if (list.isEmpty()) {
            body.addView(kit.tx("Belum ada layer. Buka Stream Studio untuk membuat.", 11f, kit.muted), mp(8))
        }
        for (l in list) {
            val row = LinearLayout(ctx)
            row.gravity = Gravity.CENTER_VERTICAL
            row.addView(kit.tx(l.name, 12f, Color.WHITE, true), LinearLayout.LayoutParams(0, kit.WRAP, 1f))
            row.addView(
                btn(if (l.on) "ON" else "OFF", l.on) {
                    l.on = !l.on
                    StreamStore.save(ctx, list)
                    StreamLayers.sync(ctx)
                    rebuild()
                },
                LinearLayout.LayoutParams(kit.dp(54), kit.WRAP).apply { rightMargin = kit.dp(6) }
            )
            row.addView(
                btn("Atur", false) {
                    hide()
                    StreamLayers.startEdit(ctx, l.id)
                },
                LinearLayout.LayoutParams(kit.dp(60), kit.WRAP)
            )
            body.addView(row, mp(8))
        }
        val sv = ScrollView(ctx)
        sv.addView(body)
        root.addView(sv, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(
            btn("Buka Stream Studio", false) {
                hide()
                ctx.startActivity(
                    Intent(ctx, StreamActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            mp(8)
        )
        val r = KeyMapStore.real(ctx)
        val w = min(r[0] - kit.dp(16), kit.dp(340))
        val h = min(r[1] - kit.dp(24), kit.dp(380))
        wm.addView(root, kit.lp(w, h, 0, 0, Gravity.CENTER))
        view = root
    }
}
