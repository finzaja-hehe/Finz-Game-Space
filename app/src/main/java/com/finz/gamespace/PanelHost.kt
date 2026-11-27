package com.finz.gamespace

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import kotlin.math.min

class PanelHost(
    private val ctx: Context,
    private val wm: WindowManager,
    private val kit: OverlayKit,
    private val title: String,
    private val content: View,
    private val widthDp: Int,
    private val onClose: () -> Unit
) {
    private var view: View? = null

    fun hide() {
        view?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        view = null
    }

    fun show() {
        if (view != null) return
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL
        root.background = kit.box(0xF20E0A28.toInt(), 20, Palette.alpha(kit.accent, 200), 1)
        root.setPadding(kit.dp(12), kit.dp(10), kit.dp(12), kit.dp(10))
        val head = LinearLayout(ctx)
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(LogoView(ctx), LinearLayout.LayoutParams(kit.dp(30), kit.dp(30)))
        head.addView(
            kit.tx(title, 14f, Color.WHITE, true),
            LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { leftMargin = kit.dp(8) }
        )
        val x = kit.tx("✕", 14f, Color.WHITE, true)
        x.gravity = Gravity.CENTER
        x.background = kit.circleBg(false)
        x.setOnClickListener {
            hide()
            onClose()
        }
        head.addView(x, LinearLayout.LayoutParams(kit.dp(34), kit.dp(34)))
        root.addView(head)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = kit.dp(8) })
        val r = KeyMapStore.real(ctx)
        val w = min(r[0] - kit.dp(16), kit.dp(widthDp))
        val h = r[1] - kit.dp(16)
        val lp = WindowManager.LayoutParams(
            w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.CENTER
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        try {
            wm.addView(root, lp)
            view = root
        } catch (e: Exception) {
        }
    }
}
