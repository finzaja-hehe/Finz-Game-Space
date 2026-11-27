package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.text.TextPaint
import android.view.View
import java.util.Random
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object Tween {

    fun out3(x: Float): Float {
        val k = 1f - x
        return 1f - k * k * k
    }

    fun inOut(x: Float): Float = x * x * (3f - 2f * x)

    fun run(
        v: View,
        delayMs: Long,
        durMs: Long,
        ease: (Float) -> Float,
        step: (Float) -> Unit,
        end: (() -> Unit)? = null
    ) {
        val t0 = SystemClock.uptimeMillis() + delayMs
        val r = object : Runnable {
            override fun run() {
                if (!v.isAttachedToWindow) return
                val now = SystemClock.uptimeMillis()
                if (now < t0) {
                    v.postOnAnimation(this)
                    return
                }
                val u = ((now - t0).toFloat() / durMs).coerceIn(0f, 1f)
                step(ease(u))
                if (u < 1f) v.postOnAnimation(this) else end?.invoke()
            }
        }
        v.postOnAnimation(r)
    }
}

class LiveBg(ctx: Context) : View(ctx) {

    private val n = 40
    private val bx = FloatArray(n)
    private val sp = FloatArray(n)
    private val off = FloatArray(n)
    private val ph = FloatArray(n)
    private val sz = FloatArray(n)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val m = Matrix()
    private val rib = Path()
    private val d = resources.displayMetrics.density
    private var base: LinearGradient? = null
    private var g1: RadialGradient? = null
    private var g2: RadialGradient? = null
    private var g3: RadialGradient? = null
    private var ribShader: LinearGradient? = null
    private var t0 = 0L
    private var running = false
    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            invalidate()
            postOnAnimation(this)
        }
    }

    init {
        val r = Random(11)
        for (i in 0 until n) {
            bx[i] = r.nextFloat()
            sp[i] = 12f + r.nextFloat() * 34f
            off[i] = r.nextFloat() * 1000f
            ph[i] = r.nextFloat() * 6.2831855f
            sz[i] = 1.2f + r.nextFloat() * 2.6f
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val a = Palette.a(context)
        val b = Palette.b(context)
        val mid = Palette.mix(a, b, 0.5f)
        val r = maxOf(w, h) * 0.6f
        base = LinearGradient(0f, 0f, 0f, h.toFloat(), Palette.BG, Palette.BG2, Shader.TileMode.CLAMP)
        g1 = RadialGradient(0f, 0f, r, Palette.alpha(a, 120), Palette.alpha(a, 0), Shader.TileMode.CLAMP)
        g2 = RadialGradient(0f, 0f, r * 0.85f, Palette.alpha(b, 100), Palette.alpha(b, 0), Shader.TileMode.CLAMP)
        g3 = RadialGradient(0f, 0f, r * 0.6f, Palette.alpha(mid, 80), Palette.alpha(mid, 0), Shader.TileMode.CLAMP)
        ribShader = LinearGradient(0f, 0f, w.toFloat(), 0f, a, b, Shader.TileMode.CLAMP)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        t0 = SystemClock.uptimeMillis()
        postOnAnimation(tick)
    }

    override fun onDetachedFromWindow() {
        running = false
        super.onDetachedFromWindow()
    }

    private fun glow(c: Canvas, g: RadialGradient?, x: Float, y: Float) {
        if (g == null) return
        m.setTranslate(x, y)
        g.setLocalMatrix(m)
        p.shader = g
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val tt = (SystemClock.uptimeMillis() - t0) / 1000f
        val tau = 6.2831855f
        p.style = Paint.Style.FILL
        p.shader = base
        c.drawRect(0f, 0f, w, h, p)

        val k = tt * 0.55f
        glow(c, g1, w * (0.18f + 0.20f * sin(k)), h * (0.28f + 0.18f * cos(k * 0.8f)))
        glow(c, g2, w * (0.84f + 0.16f * cos(k * 0.9f)), h * (0.72f + 0.16f * sin(k * 0.7f)))
        glow(c, g3, w * (0.55f + 0.30f * sin(k * 0.6f + 2f)), h * (0.45f + 0.22f * cos(k * 0.5f + 1f)))
        p.shader = null

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.3f * d
        for (i in 0..5) {
            val f = (tt * 0.12f + i * 0.17f) % 1f
            val x = f * (w + h) - h
            val al = (12 + 26 * sin(3.1415927f * f)).toInt()
            p.color = Palette.alpha(Color.WHITE, al)
            c.drawLine(x, h, x + h * 0.7f, 0f, p)
        }

        rib.reset()
        val steps = 32
        for (i in 0..steps) {
            val u = i / steps.toFloat()
            val x = u * w
            val y = h * 0.80f + sin(u * tau * 1.5f + tt * 0.8f) * h * 0.05f +
                sin(u * tau * 3f - tt * 1.1f) * h * 0.02f
            if (i == 0) rib.moveTo(x, y) else rib.lineTo(x, y)
        }
        p.shader = ribShader
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 14 * d
        p.alpha = 22
        c.drawPath(rib, p)
        p.strokeWidth = 5 * d
        p.alpha = 55
        c.drawPath(rib, p)
        p.strokeWidth = 1.6f * d
        p.alpha = 170
        c.drawPath(rib, p)
        p.shader = null

        p.style = Paint.Style.FILL
        val a = Palette.a(context)
        val b = Palette.b(context)
        val span = h + 40 * d
        for (i in 0 until n) {
            val travel = (tt * sp[i] * d + off[i] * d) % span
            val y = h + 20 * d - travel
            val x = bx[i] * w + sin(tt * 0.7f + ph[i]) * 18 * d
            val fade = sin(3.1415927f * (travel / span))
            val tw = 0.5f + 0.5f * sin(tt * 1.6f + ph[i])
            p.color = Palette.alpha(if (i % 2 == 0) a else b, (fade * (70 + 130 * tw)).toInt())
            c.drawCircle(x, y, sz[i] * d, p)
        }
    }
}

class PulseButton(ctx: Context, private val label: String) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val tri = Path()
    private val d = resources.displayMetrics.density
    private var t0 = 0L
    private var running = false
    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        t0 = SystemClock.uptimeMillis()
        postOnAnimation(tick)
    }

    override fun onDetachedFromWindow() {
        running = false
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val a = Palette.a(context)
        val b = Palette.b(context)
        val tt = (SystemClock.uptimeMillis() - t0) / 1000f
        val pulse = 0.5f + 0.5f * sin(tt * 2.6f)
        c.save()
        val sc = 1f + 0.03f * pulse
        c.scale(sc, sc, w / 2f, h / 2f)
        rect.set(8 * d, 8 * d, w - 8 * d, h - 8 * d)
        val r = rect.height() / 2f
        p.style = Paint.Style.STROKE
        p.shader = null
        for (i in 1..3) {
            p.strokeWidth = (2 + i * 3) * d
            p.color = Palette.alpha(a, ((40 + 50 * pulse) / i).toInt())
            c.drawRoundRect(rect, r, r, p)
        }
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, w, 0f, a, b, Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, r, r, p)
        p.shader = null
        val cy = h / 2f
        val tx = w * 0.26f
        tri.reset()
        tri.moveTo(tx - 6 * d, cy - 8 * d)
        tri.lineTo(tx - 6 * d, cy + 8 * d)
        tri.lineTo(tx + 8 * d, cy)
        tri.close()
        p.color = Color.WHITE
        c.drawPath(tri, p)
        tp.textAlign = Paint.Align.LEFT
        tp.isFakeBoldText = true
        tp.color = Color.WHITE
        tp.textSize = min(16f * d, h * 0.3f)
        tp.letterSpacing = 0.12f
        c.drawText(label, tx + 18 * d, cy + 6 * d, tp)
        c.restore()
    }
}
