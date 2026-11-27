package com.finz.gamespace

import android.content.Context
import android.view.WindowManager

class SensiPanel(
    private val ctx: Context,
    private val wm: WindowManager,
    private val kit: OverlayKit
) {
    private var host: PanelHost? = null

    fun hide() {
        host?.hide()
        host = null
    }

    fun show() {
        if (host != null) return
        val h = PanelHost(ctx, wm, kit, "GENERATOR SENSI", SensiUi(ctx, kit).view, 440) { host = null }
        host = h
        h.show()
    }
}
