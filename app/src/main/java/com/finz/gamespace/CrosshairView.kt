package com.finz.gamespace

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class CrosshairView(ctx: Context) : View(ctx) {

    private var shape = 1
    private var color = Color.GREEN
    private var size = 12
    private var thick = 2
    private var alphaPct = 100
    private var gap = 3
    private var outline = true
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    fun load(s: SharedPreferences) {
        shape = s.getInt("shape", 1)
        color = s.getInt("color", 0xFF00FF66.toInt())
        size = s.getInt("size", 12)
        thick = s.getInt("thick", 2)
        alphaPct = s.getInt("alpha", 100)
        gap = s.getInt("gap", 3)
        outline = s.getBoolean("outline", true)
        invalidate()
    }

    private fun d(v: Int) = v * resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val a = (alphaPct * 255 / 100).coerceIn(0, 255)
        val cx = width / 2f
        val cy = height / 2f
        if (outline) {
            p.color = Color.BLACK
            p.alpha = a
            paintShape(c, cx, cy, d(thick) + d(2), true)
        }
        p.color = color
        p.alpha = a
        paintShape(c, cx, cy, d(thick), false)
    }

    private fun paintShape(c: Canvas, cx: Float, cy: Float, t: Float, o: Boolean) {
        val r = d(size)
        val g = d(gap)
        val e = if (o) d(1) else 0f
        p.style = Paint.Style.STROKE
        p.strokeWidth = t
        p.strokeCap = if (o) Paint.Cap.SQUARE else Paint.Cap.BUTT
        when (shape) {
            0 -> {
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, r / 3f + e, p)
            }
            1 -> {
                c.drawLine(cx - g - r, cy, cx - g, cy, p)
                c.drawLine(cx + g, cy, cx + g + r, cy, p)
                c.drawLine(cx, cy - g - r, cx, cy - g, p)
                c.drawLine(cx, cy + g, cx, cy + g + r, p)
            }
            2 -> {
                val gd = g * 0.7f
                val rd = r * 0.7f
                c.drawLine(cx - gd - rd, cy - gd - rd, cx - gd, cy - gd, p)
                c.drawLine(cx + gd, cy + gd, cx + gd + rd, cy + gd + rd, p)
                c.drawLine(cx + gd, cy - gd, cx + gd + rd, cy - gd - rd, p)
                c.drawLine(cx - gd, cy + gd, cx - gd - rd, cy + gd + rd, p)
            }
            3 -> c.drawCircle(cx, cy, r, p)
            4 -> {
                c.drawCircle(cx, cy, r, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, t, p)
            }
            else -> {
                c.drawLine(cx - r, cy, cx + r, cy, p)
                c.drawLine(cx, cy - r, cx, cy + r, p)
            }
        }
    }
}
