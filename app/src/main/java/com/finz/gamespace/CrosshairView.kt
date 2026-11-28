package com.finz.gamespace

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import kotlin.math.sin

class CrosshairView(ctx: Context) : View(ctx) {

    private var shape = 1
    private var color = 0xFF00FF66.toInt()
    private var size = 12
    private var thick = 2
    private var alphaPct = 100
    private var gap = 3
    private var outline = true
    private var neon = false
    private var neonInt = 5
    private var neonColor = 0
    private var pulse = false
    private var rainbow = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t0 = 0L
    private var going = false
    private val loop = object : Runnable {
        override fun run() {
            if (!going) return
            invalidate()
            if (pulse || rainbow) postOnAnimation(this) else going = false
        }
    }

    fun load(s: SharedPreferences) {
        shape = s.getInt("shape", 1)
        color = s.getInt("color", 0xFF00FF66.toInt())
        size = s.getInt("size", 12)
        thick = s.getInt("thick", 2)
        alphaPct = s.getInt("alpha", 100)
        gap = s.getInt("gap", 3)
        outline = s.getBoolean("outline", true)
        neon = s.getBoolean("neon", false)
        neonInt = s.getInt("neonint", 5)
        neonColor = s.getInt("neoncolor", 0)
        pulse = s.getBoolean("pulse", false)
        rainbow = s.getBoolean("rainbow", false)
        invalidate()
        kick()
    }

    private fun kick() {
        if ((pulse || rainbow) && isAttachedToWindow && !going) {
            going = true
            postOnAnimation(loop)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        t0 = SystemClock.uptimeMillis()
        kick()
    }

    override fun onDetachedFromWindow() {
        going = false
        super.onDetachedFromWindow()
    }

    private fun d(v: Int) = v * resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val a = (alphaPct * 255 / 100).coerceIn(0, 255)
        val cx = width / 2f
        val cy = height / 2f
        val tt = (SystemClock.uptimeMillis() - t0) / 1000f
        var main = color
        if (rainbow) main = Color.HSVToColor(floatArrayOf((tt * 90f) % 360f, 1f, 1f))
        val glow = if (neonColor != 0 && !rainbow) neonColor else main
        val pul = if (pulse) 0.6f + 0.4f * (0.5f + 0.5f * sin(tt * 4.5f)) else 1f
        val tp = d(thick)
        if (outline && !neon) {
            p.color = Color.BLACK
            p.alpha = a
            paintShape(c, cx, cy, tp + d(2), d(1), Paint.Cap.SQUARE)
        }
        if (neon) {
            for (i in 4 downTo 1) {
                p.color = glow
                p.alpha = (a * pul * (0.08f + 0.075f * (5 - i))).toInt().coerceIn(0, 255)
                paintShape(c, cx, cy, tp + d(2) + i * neonInt * 0.8f * d(1), i * neonInt * 0.4f * d(1), Paint.Cap.ROUND)
            }
            p.color = Palette.mix(main, Color.WHITE, 0.5f)
        } else {
            p.color = main
        }
        p.alpha = a
        paintShape(c, cx, cy, tp, 0f, Paint.Cap.BUTT)
    }

    private fun paintShape(c: Canvas, cx: Float, cy: Float, t: Float, e: Float, cap: Paint.Cap) {
        val r = d(size)
        val g = d(gap)
        p.style = Paint.Style.STROKE
        p.strokeWidth = t
        p.strokeCap = cap
        when (shape) {
            0 -> {
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, r / 3f + e, p)
            }
            1 -> plus(c, cx, cy, r, g)
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
            5 -> {
                c.drawLine(cx - r, cy, cx + r, cy, p)
                c.drawLine(cx, cy - r, cx, cy + r, p)
            }
            6 -> c.drawRect(cx - r, cy - r, cx + r, cy + r, p)
            7 -> {
                plus(c, cx, cy, r, g)
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, t * 0.9f, p)
            }
            else -> {
                c.drawLine(cx - r, cy + r * 0.5f, cx, cy - r * 0.5f, p)
                c.drawLine(cx, cy - r * 0.5f, cx + r, cy + r * 0.5f, p)
            }
        }
    }

    private fun plus(c: Canvas, cx: Float, cy: Float, r: Float, g: Float) {
        c.drawLine(cx - g - r, cy, cx - g, cy, p)
        c.drawLine(cx + g, cy, cx + g + r, cy, p)
        c.drawLine(cx, cy - g - r, cx, cy - g, p)
        c.drawLine(cx, cy + g, cx, cy + g + r, p)
    }
}
