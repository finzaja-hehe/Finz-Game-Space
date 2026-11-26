package com.finz.gamespace

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class OverlayKit(private val ctx: Context) {

    val accent: Int get() = Palette.a(ctx)
    val accent2: Int get() = Palette.b(ctx)
    val muted = Palette.MUTED
    val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

    fun dp(v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    fun lp(w: Int, h: Int, x: Int, y: Int, g: Int, keep: Boolean = false):
        WindowManager.LayoutParams {
        var f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (keep) f = f or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        return WindowManager.LayoutParams(
            w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, f, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = g
            this.x = x
            this.y = y
        }
    }

    fun tx(s: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(ctx).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    fun box(color: Int, radius: Int, strokeColor: Int = 0, strokeDp: Int = 0) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
        }

    fun grad(radius: Int): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(accent, accent2))
            .apply { cornerRadius = dp(radius).toFloat() }

    fun circleBg(on: Boolean): GradientDrawable {
        val g = if (on) {
            GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(accent, accent2))
        } else {
            GradientDrawable().apply { setColor(0xFF140F2E.toInt()) }
        }
        g.shape = GradientDrawable.OVAL
        g.setStroke(dp(1), accent)
        return g
    }

    fun slider(label: String, min: Int, max: Int, now: Int, labelDp: Int,
               onChange: (Int) -> Unit): LinearLayout {
        val r = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        r.addView(tx(label, 11f, Color.WHITE), LinearLayout.LayoutParams(dp(labelDp), WRAP))
        val value = tx("$now", 11f, muted, true).apply { gravity = Gravity.END }
        val sb = SeekBar(ctx).apply {
            this.min = min
            this.max = max
            progress = now
            progressTintList = ColorStateList.valueOf(accent)
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    value.text = "$p"
                    if (fromUser) onChange(p)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        r.addView(sb, LinearLayout.LayoutParams(0, WRAP, 1f))
        r.addView(value, LinearLayout.LayoutParams(dp(34), WRAP))
        return r
    }
}
