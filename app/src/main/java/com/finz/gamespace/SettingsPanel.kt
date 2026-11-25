package com.finz.gamespace

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsPanel(
    private val ctx: Context,
    private val wm: WindowManager,
    private val kit: OverlayKit
) {
    private var view: View? = null
    private val blue = 0xFF5B67F0.toInt()
    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT

    fun hide() {
        view?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        view = null
    }

    private fun mp(top: Int) =
        LinearLayout.LayoutParams(MATCH, kit.WRAP).apply { topMargin = kit.dp(top) }

    private fun section(s: String): TextView = kit.tx(s, 12f, blue, true).apply {
        background = kit.box(Color.WHITE, 6)
        setPadding(kit.dp(10), kit.dp(5), kit.dp(10), kit.dp(5))
    }

    fun show() {
        if (view != null) return
        KeyRouter.attach(ctx)
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = kit.box(blue, 18)
            setPadding(kit.dp(16), kit.dp(12), kit.dp(16), kit.dp(12))
        }
        root.addView(kit.tx("Pengaturan Dasar", 16f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
        })

        val body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        body.addView(section("Pengaturan Dasar"), mp(10))

        val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(kit.tx("Tampilkan titik sentuh", 13f, Color.WHITE),
            LinearLayout.LayoutParams(0, kit.WRAP, 1f))
        val tgl = kit.tx("", 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(kit.dp(16), kit.dp(6), kit.dp(16), kit.dp(6))
        }
        fun paint() {
            val on = KeyMapStore.getBool(ctx, "dots", false)
            tgl.text = if (on) "ON" else "OFF"
            tgl.setTextColor(if (on) Color.BLACK else Color.WHITE)
            tgl.background = kit.box(if (on) 0xFF22E58B.toInt() else 0x44000000, 14)
        }
        tgl.setOnClickListener {
            KeyMapStore.putBool(ctx, "dots", !KeyMapStore.getBool(ctx, "dots", false))
            DotsOverlay.refresh()
            paint()
        }
        paint()
        row.addView(tgl)
        body.addView(row, mp(10))

        body.addView(
            kit.slider("Ukuran titik", 8, 40, KeyMapStore.getInt(ctx, "dotsize", 18), 90) {
                KeyMapStore.putInt(ctx, "dotsize", it)
                DotsOverlay.refresh()
            }, mp(8)
        )
        body.addView(
            kit.slider("Transparansi", 10, 100, KeyMapStore.getInt(ctx, "dotalpha", 80), 90) {
                KeyMapStore.putInt(ctx, "dotalpha", it)
                DotsOverlay.refresh()
            }, mp(4)
        )
        body.addView(
            kit.slider("Transp. bola", 20, 100, KeyMapStore.getInt(ctx, "ballalpha", 90), 90) {
                KeyMapStore.putInt(ctx, "ballalpha", it)
                KeyRouter.syncBalls()
            }, mp(4)
        )

        body.addView(section("Pengaturan Mouse"), mp(14))
        body.addView(
            kit.tx(
                "Sensitivitas penunjuk, polling mouse, ukuran dan gaya penunjuk " +
                    "tersedia setelah dukungan mouse ditambahkan (tahap berikutnya).",
                11f, 0xFFDDE1FF.toInt()
            ), mp(8)
        )

        val sv = ScrollView(ctx)
        sv.addView(body)
        root.addView(sv, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val close = kit.tx("Tutup", 13f, blue, true).apply {
            gravity = Gravity.CENTER
            background = kit.box(Color.WHITE, 12)
            setPadding(kit.dp(28), kit.dp(10), kit.dp(28), kit.dp(10))
            setOnClickListener { hide() }
        }
        val cl = LinearLayout.LayoutParams(kit.WRAP, kit.WRAP).apply {
            gravity = Gravity.END
            topMargin = kit.dp(8)
        }
        root.addView(close, cl)

        val w = minOf(ctx.resources.displayMetrics.widthPixels - kit.dp(16), kit.dp(420))
        wm.addView(root, kit.lp(w, MATCH, kit.dp(8), 0, Gravity.END or Gravity.TOP))
        view = root
    }
}
