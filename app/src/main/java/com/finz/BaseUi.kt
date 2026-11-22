package com.finz.gamespace

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

abstract class BaseUi : AppCompatActivity() {

    protected val themes = intArrayOf(
        0xFFFF7A1A.toInt(), 0xFF2D9CFF.toInt(),
        0xFFFF3B47.toInt(), 0xFF22E58B.toInt()
    )
    protected var themeIndex = 0
    protected val bg = 0xFF0B0D12.toInt()
    protected val panel = 0xFF161A22.toInt()
    protected val muted = 0xFF8B93A1.toInt()
    protected val accent: Int get() = themes[themeIndex]
    protected val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    protected val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

    protected fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    protected fun mix(c: Int, r: Float): Int = Color.rgb(
        (Color.red(bg) + (Color.red(c) - Color.red(bg)) * r).toInt(),
        (Color.green(bg) + (Color.green(c) - Color.green(bg)) * r).toInt(),
        (Color.blue(bg) + (Color.blue(c) - Color.blue(bg)) * r).toInt()
    )

    protected fun box(color: Int, radius: Int, strokeColor: Int = 0, strokeDp: Int = 0) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
        }

    protected fun lp(w: Int, h: Int, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }

    protected fun tv(s: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    protected fun stat(label: String, value: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        addView(tv(label, 9f, muted, true).apply { letterSpacing = 0.15f })
        addView(tv(value, 16f, Color.WHITE, true).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
    }
}
