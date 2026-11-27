package com.finz.gamespace

import android.content.Context
import android.content.Intent
import android.view.WindowManager

class StreamPanel(
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
        val ui = StreamUi(ctx, kit, { id -> edit(id) }, { id -> pick(id) })
        val h = PanelHost(ctx, wm, kit, "STREAM STUDIO", ui.view, 420) { host = null }
        host = h
        h.show()
    }

    private fun edit(id: Int) {
        hide()
        StreamLayers.afterEdit = { show() }
        StreamLayers.startEdit(ctx, id)
    }

    private fun pick(id: Int) {
        hide()
        StreamLayers.afterPick = { show() }
        ctx.startActivity(
            Intent(ctx, MediaPickActivity::class.java).putExtra("id", id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
