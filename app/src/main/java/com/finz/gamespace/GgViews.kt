package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.widget.LinearLayout

class GgHex(ctx: Context, private val icon: Int) : View(ctx) {

    var active = false
        set(v) {
            field = v
            invalidate()
        }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val d = resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension((78 * d).toInt(), (80 * d).toInt())
    }

    private fun hex(cx: Float, cy: Float, r: Float) {
        path.reset()
        for (i in 0 until 6) {
            val a = Math.toRadians((60 * i - 30).toDouble())
            val x = cx + (r * Math.cos(a)).toFloat()
            val y = cy + (r * Math.sin(a)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val top = if (active) 0xFF7A83FF.toInt() else 0xFF3B3F9E.toInt()
        val bot = if (active) 0xFF3A3FBF.toInt() else 0xFF171A4A.toInt()
        hex(cx, cy, 36 * d)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(cx, cy - 36 * d, cx, cy + 36 * d, top, bot, Shader.TileMode.CLAMP)
        c.drawPath(path, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f * d
        p.color = if (active) Color.WHITE else 0xFF6C72FF.toInt()
        c.drawPath(path, p)
        hex(cx, cy, 29 * d)
        p.strokeWidth = 1.2f * d
        p.color = 0x886C72FF.toInt()
        c.drawPath(path, p)
        GgIcons.draw(c, p, icon, cx, cy, 30 * d, Color.WHITE, d)
    }
}

class GgCircle(ctx: Context, private val icon: Int) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension((46 * d).toInt(), (46 * d).toInt())
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        p.style = Paint.Style.FILL
        p.color = 0xFF0D0F2E.toInt()
        c.drawCircle(cx, cy, 22 * d, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * d
        p.color = 0xFF3A3F9E.toInt()
        c.drawCircle(cx, cy, 22 * d, p)
        GgIcons.draw(c, p, icon, cx, cy, 22 * d, Color.WHITE, d)
    }
}

class GgToggle(ctx: Context) : View(ctx) {

    var on = false
        set(v) {
            field = v
            invalidate()
        }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val d = resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension((48 * d).toInt(), (28 * d).toInt())
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, h * 0.15f, w, h * 0.85f)
        p.style = Paint.Style.FILL
        p.color = if (on) 0xFF22E58B.toInt() else 0xFF2C34A8.toInt()
        c.drawRoundRect(rect, h, h, p)
        p.color = Color.WHITE
        val r = h * 0.42f
        val cx = if (on) w - r - 2 * d else r + 2 * d
        c.drawCircle(cx, h / 2f, r, p)
    }
}

class SideMenuLayout(ctx: Context) : LinearLayout(ctx) {

    var bulge = 0f
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val arc = Path()

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val d = resources.displayMetrics.density
        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(w - bulge, 0f)
        path.quadTo(w + bulge, h / 2f, w - bulge, h)
        path.lineTo(0f, h)
        path.close()
        fill.style = Paint.Style.FILL
        fill.shader = LinearGradient(
            0f, 0f, w, 0f, 0xF04A4FD6.toInt(), 0xF0121548.toInt(), Shader.TileMode.CLAMP
        )
        c.drawPath(path, fill)
        fill.shader = null
        arc.reset()
        arc.moveTo(w - bulge, 0f)
        arc.quadTo(w + bulge, h / 2f, w - bulge, h)
        line.style = Paint.Style.STROKE
        line.strokeWidth = 7 * d
        line.color = 0xCC8A90FF.toInt()
        line.pathEffect = DashPathEffect(floatArrayOf(12 * d, 7 * d), 0f)
        c.drawPath(arc, line)
    }
}
