package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MarqueeView(
    ctx: Context,
    private val txt: String,
    private val sizeSp: Int,
    private val col: Int
) : View(ctx) {

    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density
    private var t0 = 0L
    private var going = false
    private val loop = object : Runnable {
        override fun run() {
            if (!going) return
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        going = true
        t0 = SystemClock.uptimeMillis()
        postOnAnimation(loop)
    }

    override fun onDetachedFromWindow() {
        going = false
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        p.textSize = sizeSp * d
        p.color = col
        p.isFakeBoldText = true
        p.setShadowLayer(3f * d, 0f, 0f, Color.BLACK)
        val tw = p.measureText(txt)
        val gap = max(width * 0.35f, 60f * d)
        val total = tw + gap
        val off = ((SystemClock.uptimeMillis() - t0) / 1000f * 70f * d) % total
        val y = height / 2f - (p.ascent() + p.descent()) / 2f
        var x = width - off
        while (x > -tw) {
            c.drawText(txt, x, y, p)
            x -= total
        }
    }
}

class TimerView(ctx: Context, private val sizeSp: Int, private val col: Int) : View(ctx) {

    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val d = resources.displayMetrics.density
    private var going = false
    private val loop = object : Runnable {
        override fun run() {
            if (!going) return
            invalidate()
            postDelayed(this, 500)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        going = true
        post(loop)
    }

    override fun onDetachedFromWindow() {
        going = false
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val s = (SystemClock.elapsedRealtime() - StreamLayers.liveStart) / 1000
        val t = String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
        val clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val h = height.toFloat()
        rect.set(0f, 0f, width.toFloat(), h)
        bg.color = 0x99000000.toInt()
        c.drawRoundRect(rect, h / 2f, h / 2f, bg)
        bg.color = 0xFFFF3B47.toInt()
        c.drawCircle(h * 0.5f, h / 2f, h * 0.14f, bg)
        val text = "LIVE $t   $clock"
        p.textSize = sizeSp * d
        p.color = col
        p.isFakeBoldText = true
        val x = h * 0.8f
        while (p.measureText(text) > width - x - 8 * d && p.textSize > 6 * d) {
            p.textSize = p.textSize - 0.5f * d
        }
        c.drawText(text, x, h / 2f - (p.ascent() + p.descent()) / 2f, p)
    }
}

class EditFrame(
    ctx: Context,
    private val title: String,
    private val onMove: (Int, Int) -> Unit,
    private val onResize: (Int, Int) -> Unit
) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density
    private var lx = 0f
    private var ly = 0f
    private var resizing = false

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.style = Paint.Style.FILL
        p.color = 0x33FFE600
        c.drawRect(0f, 0f, w, h, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2 * d
        p.color = 0xFFFFE600.toInt()
        p.pathEffect = DashPathEffect(floatArrayOf(10 * d, 6 * d), 0f)
        c.drawRect(d, d, w - d, h - d, p)
        p.pathEffect = null
        p.style = Paint.Style.FILL
        c.drawCircle(w - 14 * d, h - 14 * d, 12 * d, p)
        p.color = Color.WHITE
        p.textAlign = Paint.Align.LEFT
        p.textSize = 11 * d
        p.isFakeBoldText = true
        c.drawText(title, 8 * d, 16 * d, p)
        p.isFakeBoldText = false
        p.textSize = 9 * d
        c.drawText("Seret = geser. Bulatan kuning = ukuran", 8 * d, 30 * d, p)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lx = e.rawX
                ly = e.rawY
                resizing = e.x > width - 48 * d && e.y > height - 48 * d
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (e.rawX - lx).toInt()
                val dy = (e.rawY - ly).toInt()
                if (dx != 0 || dy != 0) {
                    lx = e.rawX
                    ly = e.rawY
                    if (resizing) onResize(dx, dy) else onMove(dx, dy)
                }
            }
        }
        return true
    }
}
