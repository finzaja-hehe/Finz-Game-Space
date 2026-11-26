package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.widget.LinearLayout

class HexButton(
    ctx: Context,
    icon0: String,
    private val label: String,
    private val iconId: Int = -1
) : View(ctx) {

    var icon = icon0
        set(v) {
            field = v
            invalidate()
        }
    var accent = Color.CYAN
    var active = false
        set(v) {
            field = v
            invalidate()
        }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private fun d(v: Float) = v * resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(d(56f).toInt(), d(60f).toInt())
    }

    override fun onDraw(c: Canvas) {
        val a = Palette.a(context)
        val b = Palette.b(context)
        val r = d(20f)
        val cx = width / 2f
        val cy = d(3f) + r
        path.reset()
        for (i in 0 until 6) {
            val ang = Math.toRadians((60 * i - 30).toDouble())
            val x = cx + (r * Math.cos(ang)).toFloat()
            val y = cy + (r * Math.sin(ang)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        fill.shader = null
        if (active) {
            fill.style = Paint.Style.STROKE
            for (k in 1..3) {
                fill.strokeWidth = d(2f + k * 2.5f)
                fill.color = Palette.alpha(a, 60 / k)
                c.drawPath(path, fill)
            }
        }
        fill.style = Paint.Style.FILL
        fill.shader = if (active) {
            LinearGradient(cx - r, cy - r, cx + r, cy + r, a, b, Shader.TileMode.CLAMP)
        } else {
            LinearGradient(
                cx, cy - r, cx, cy + r, 0xFF21165A.toInt(), 0xFF0B0824.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        c.drawPath(path, fill)
        fill.style = Paint.Style.STROKE
        fill.strokeWidth = d(1.6f)
        fill.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, a, b, Shader.TileMode.CLAMP)
        c.drawPath(path, fill)
        fill.shader = null

        if (iconId >= 0) {
            HudIcons.draw(c, txt, iconId, cx, cy, d(26f), Color.WHITE, d(1f))
        } else {
            txt.shader = null
            txt.style = Paint.Style.FILL
            txt.textAlign = Paint.Align.CENTER
            txt.isFakeBoldText = true
            txt.color = Color.WHITE
            txt.textSize = d(if (icon.firstOrNull()?.isDigit() == true) 13f else 16f)
            val fm = txt.fontMetrics
            c.drawText(icon, cx, cy - (fm.ascent + fm.descent) / 2f, txt)
        }

        txt.shader = null
        txt.style = Paint.Style.FILL
        txt.textAlign = Paint.Align.CENTER
        txt.isFakeBoldText = false
        txt.color = if (active) Color.WHITE else 0xFFC9C2F5.toInt()
        txt.textSize = d(8.5f)
        c.drawText(label, cx, cy + r + d(12f), txt)
    }
}

class GaugeView(ctx: Context) : View(ctx) {

    var accent = Color.CYAN
    private var value = 0
    private var big = "--"
    private var caption = "RAM"
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private fun d(v: Float) = v * resources.displayMetrics.density

    fun set(pct: Int, bigText: String, cap: String) {
        value = pct.coerceIn(0, 100)
        big = bigText
        caption = cap
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val s = d(96f).toInt()
        setMeasuredDimension(s, s)
    }

    override fun onDraw(c: Canvas) {
        val a = Palette.a(context)
        val b = Palette.b(context)
        val s = width.toFloat()
        val pad = d(9f)
        rect.set(pad, pad, s - pad, s - pad)
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(
            s / 2f, s / 2f, s * 0.42f, Palette.alpha(a, 50), Palette.alpha(a, 0),
            Shader.TileMode.CLAMP
        )
        c.drawCircle(s / 2f, s / 2f, s * 0.42f, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = d(8f)
        p.strokeCap = Paint.Cap.ROUND
        p.color = 0xFF241A56.toInt()
        c.drawArc(rect, 135f, 270f, false, p)
        p.shader = LinearGradient(pad, 0f, s - pad, 0f, a, b, Shader.TileMode.CLAMP)
        c.drawArc(rect, 135f, 270f * value / 100f, false, p)
        p.shader = null
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = true
        p.color = Color.WHITE
        p.textSize = d(24f)
        c.drawText(big, s / 2f, s / 2f + d(6f), p)
        p.isFakeBoldText = false
        p.color = Palette.MUTED
        p.textSize = d(9f)
        c.drawText(caption, s / 2f, s / 2f + d(22f), p)
    }
}

class HudLayout(ctx: Context) : LinearLayout(ctx) {

    var accent = Color.CYAN
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val a = Palette.a(context)
        val b = Palette.b(context)
        val w = width.toFloat()
        val h = height.toFloat()
        val cut = 24f * d
        val i = 3f * d
        path.reset()
        path.moveTo(cut, i)
        path.lineTo(w - cut, i)
        path.lineTo(w - i, cut)
        path.lineTo(w - i, h - cut)
        path.lineTo(w - cut, h - i)
        path.lineTo(cut, h - i)
        path.lineTo(i, h - cut)
        path.lineTo(i, cut)
        path.close()
        fill.style = Paint.Style.FILL
        fill.shader = LinearGradient(
            0f, 0f, 0f, h, 0xF2180E44.toInt(), 0xF206030F.toInt(), Shader.TileMode.CLAMP
        )
        c.drawPath(path, fill)
        fill.shader = null
        line.style = Paint.Style.STROKE
        line.shader = null
        for (k in 1..3) {
            line.strokeWidth = (2f + k * 2.5f) * d
            line.color = Palette.alpha(a, 40 / k)
            c.drawPath(path, line)
        }
        line.strokeWidth = 1.8f * d
        line.shader = LinearGradient(0f, 0f, w, 0f, a, b, Shader.TileMode.CLAMP)
        c.drawPath(path, line)
        line.shader = null
        super.onDraw(c)
    }
}
