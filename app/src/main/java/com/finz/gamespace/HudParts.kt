package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.widget.LinearLayout

class HexButton(ctx: Context, icon0: String, private val label: String) : View(ctx) {

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
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private fun d(v: Float) = v * resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(d(56f).toInt(), d(60f).toInt())
    }

    override fun onDraw(c: Canvas) {
        val r = d(20f)
        val cx = width / 2f
        val cy = d(3f) + r
        path.reset()
        for (i in 0 until 6) {
            val a = Math.toRadians((60 * i - 30).toDouble())
            val x = cx + (r * Math.cos(a)).toFloat()
            val y = cy + (r * Math.sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        fill.style = Paint.Style.FILL
        fill.color = if (active) accent else 0xFF1B2733.toInt()
        c.drawPath(path, fill)
        line.style = Paint.Style.STROKE
        line.strokeWidth = d(1.5f)
        line.color = accent
        c.drawPath(path, line)

        txt.textAlign = Paint.Align.CENTER
        txt.isFakeBoldText = true
        txt.color = if (active) Color.BLACK else Color.WHITE
        txt.textSize = d(if (icon.firstOrNull()?.isDigit() == true) 13f else 16f)
        val fm = txt.fontMetrics
        c.drawText(icon, cx, cy - (fm.ascent + fm.descent) / 2f, txt)

        txt.isFakeBoldText = false
        txt.color = if (active) Color.WHITE else 0xFFB8C4D0.toInt()
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
        val s = width.toFloat()
        val pad = d(9f)
        rect.set(pad, pad, s - pad, s - pad)
        p.style = Paint.Style.STROKE
        p.strokeWidth = d(8f)
        p.strokeCap = Paint.Cap.ROUND
        p.color = 0xFF263445.toInt()
        c.drawArc(rect, 135f, 270f, false, p)
        p.color = accent
        c.drawArc(rect, 135f, 270f * value / 100f, false, p)
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = true
        p.color = Color.WHITE
        p.textSize = d(24f)
        c.drawText(big, s / 2f, s / 2f + d(6f), p)
        p.isFakeBoldText = false
        p.color = 0xFF8B93A1.toInt()
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
        val w = width.toFloat()
        val h = height.toFloat()
        val cut = 24f * d
        val i = d
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
        fill.color = 0xEE0E1620.toInt()
        c.drawPath(path, fill)
        line.style = Paint.Style.STROKE
        line.strokeWidth = 1.5f * d
        line.color = accent
        c.drawPath(path, line)
        super.onDraw(c)
    }
}
