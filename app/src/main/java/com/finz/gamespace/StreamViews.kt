package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object TextStyle {

    fun setup(p: TextPaint, sizePx: Float, color: Int, thick: Int, d: Float) {
        p.textSize = sizePx
        p.color = color
        p.textAlign = Paint.Align.LEFT
        p.isFakeBoldText = true
        p.style = if (thick > 0) Paint.Style.FILL_AND_STROKE else Paint.Style.FILL
        p.strokeWidth = thick * 0.55f * d
        p.strokeJoin = Paint.Join.ROUND
        p.setShadowLayer(3f * d, 0f, 0f, Color.BLACK)
    }
}

class TextBlock(
    ctx: Context,
    private val txt: String,
    private val sizeSp: Int,
    private val col: Int,
    private val thick: Int
) : View(ctx) {

    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        if (width <= 0 || txt.isEmpty()) return
        TextStyle.setup(p, sizeSp * d, col, thick, d)
        val w = max(1, width - (8 * d).toInt())
        val lay = StaticLayout.Builder.obtain(txt, 0, txt.length, p, w)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        c.save()
        c.translate(4 * d, max(0f, (height - lay.height) / 2f))
        lay.draw(c)
        c.restore()
    }
}

class MarqueeView(
    ctx: Context,
    private val txt: String,
    private val sizeSp: Int,
    private val col: Int,
    private val thick: Int
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
        TextStyle.setup(p, sizeSp * d, col, thick, d)
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

class TimerView(
    ctx: Context,
    private val sizeSp: Int,
    private val col: Int,
    private val thick: Int
) : View(ctx) {

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
        TextStyle.setup(p, min(sizeSp * d, h * 0.5f), col, thick, d)
        val x = h * 0.8f
        while (p.measureText(text) > width - x - 8 * d && p.textSize > 6 * d) {
            p.textSize = p.textSize - 0.5f * d
        }
        c.drawText(text, x, h / 2f - (p.ascent() + p.descent()) / 2f, p)
    }
}

class InfoView(
    ctx: Context,
    private val sizeSp: Int,
    private val col: Int,
    private val thick: Int
) : View(ctx) {

    private val act = OverlayActions(ctx, Handler(Looper.getMainLooper()))
    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val d = resources.displayMetrics.density
    private val parts = ArrayList<Pair<String, Int>>()
    private var going = false
    private val loop = object : Runnable {
        override fun run() {
            if (!going) return
            Ping.start()
            refresh()
            invalidate()
            postDelayed(this, 1000)
        }
    }

    private fun refresh() {
        parts.clear()
        val f = if (FpsClient.on) FpsClient.fps else -1
        val fc = when {
            f < 0 -> col
            f >= 55 -> 0xFF22E58B.toInt()
            f >= 30 -> 0xFFFFE600.toInt()
            else -> 0xFFFF5B5B.toInt()
        }
        parts.add(Pair("FPS " + (if (f < 0) "--" else f.toString()), fc))
        parts.add(Pair("PING " + Ping.text().replace(" ms", "ms"), col))
        val t = act.tempC()
        parts.add(Pair("%.0f°C".format(t), if (t >= 42f) 0xFFFF5B5B.toInt() else col))
        parts.add(Pair("RAM ${act.ramPct()}%", col))
        parts.add(Pair("BAT ${act.batPct()}%", col))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        going = true
        refresh()
        post(loop)
    }

    override fun onDetachedFromWindow() {
        going = false
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, 0f, w, h)
        bg.color = 0xAA000000.toInt()
        c.drawRoundRect(rect, h / 2f, h / 2f, bg)
        TextStyle.setup(p, min(sizeSp * d, h * 0.55f), col, thick, d)
        val gap = 14 * d
        fun total(): Float {
            var s = 0f
            for (q in parts) s += p.measureText(q.first)
            return s + gap * max(0, parts.size - 1)
        }
        while (total() > w - 20 * d && p.textSize > 6 * d) p.textSize = p.textSize - 0.5f * d
        var x = (w - total()) / 2f
        val y = h / 2f - (p.ascent() + p.descent()) / 2f
        for (q in parts) {
            p.color = q.second
            c.drawText(q.first, x, y, p)
            x += p.measureText(q.first) + gap
        }
    }
}

class GoalView(
    ctx: Context,
    private val txt: String,
    private val sizeSp: Int,
    private val col: Int,
    private val thick: Int
) : View(ctx) {

    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val d = resources.displayMetrics.density
    private val nf: NumberFormat = NumberFormat.getInstance(Locale("in", "ID"))

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val parts = txt.split("|")
        val title = parts.getOrNull(0)?.trim().orEmpty()
        val cur = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
        val tgt = max(1L, parts.getOrNull(2)?.trim()?.toLongOrNull() ?: 1L)
        val pct = min(100L, cur * 100 / tgt).toInt()
        rect.set(0f, 0f, w, h)
        bg.shader = null
        bg.color = 0xB0080414.toInt()
        c.drawRoundRect(rect, 14 * d, 14 * d, bg)
        val pad = 10 * d
        val ts = min(sizeSp * d, h * 0.24f)
        TextStyle.setup(p, ts, col, thick, d)
        c.drawText(title, pad, h * 0.30f, p)
        p.textAlign = Paint.Align.RIGHT
        c.drawText("$pct%", w - pad, h * 0.30f, p)
        val bt = h * 0.40f
        val bb = h * 0.60f
        rect.set(pad, bt, w - pad, bb)
        bg.color = 0x33FFFFFF
        c.drawRoundRect(rect, (bb - bt) / 2f, (bb - bt) / 2f, bg)
        if (pct > 0) {
            rect.set(pad, bt, pad + (w - 2 * pad) * pct / 100f, bb)
            bg.shader = LinearGradient(pad, 0f, w - pad, 0f, Palette.a(context), Palette.b(context), Shader.TileMode.CLAMP)
            c.drawRoundRect(rect, (bb - bt) / 2f, (bb - bt) / 2f, bg)
            bg.shader = null
        }
        TextStyle.setup(p, ts * 0.8f, col, thick, d)
        p.textAlign = Paint.Align.CENTER
        c.drawText("Rp ${nf.format(cur)} / Rp ${nf.format(tgt)}", w / 2f, h * 0.88f, p)
    }
}

class CountView(
    ctx: Context,
    private val key: Int,
    private val name: String,
    private val minutes: Int,
    private val sizeSp: Int,
    private val col: Int,
    private val thick: Int
) : View(ctx) {

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
        val end = context.getSharedPreferences("stream", Context.MODE_PRIVATE).getLong("end_$key", 0L)
        val left = if (end > 0L) max(0L, (end - System.currentTimeMillis()) / 1000) else minutes * 60L
        val t = String.format(Locale.US, "%02d:%02d", left / 60, left % 60)
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, 0f, w, h)
        bg.color = 0xB0080414.toInt()
        c.drawRoundRect(rect, 16 * d, 16 * d, bg)
        TextStyle.setup(p, min(sizeSp * d * 0.6f, h * 0.2f), Palette.MUTED, 0, d)
        p.textAlign = Paint.Align.CENTER
        c.drawText(name, w / 2f, h * 0.30f, p)
        TextStyle.setup(p, min(sizeSp * d * 1.6f, h * 0.5f), col, thick, d)
        p.textAlign = Paint.Align.CENTER
        c.drawText(t, w / 2f, h * 0.80f, p)
    }
}

class FrameView(ctx: Context, private val thick: Int) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val t = max(1, thick) * d
        rect.set(t / 2f, t / 2f, width - t / 2f, height - t / 2f)
        p.style = Paint.Style.STROKE
        p.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            Palette.a(context), Palette.b(context), Shader.TileMode.CLAMP
        )
        p.strokeWidth = t * 3f
        p.alpha = 40
        c.drawRoundRect(rect, 14 * d, 14 * d, p)
        p.strokeWidth = t * 1.8f
        p.alpha = 90
        c.drawRoundRect(rect, 14 * d, 14 * d, p)
        p.strokeWidth = t
        p.alpha = 255
        c.drawRoundRect(rect, 14 * d, 14 * d, p)
        p.shader = null
    }
}

class EditFrame(
    ctx: Context,
    private val title: String,
    private val onMove: (Int, Int) -> Unit,
    private val onResize: (Int, Int) -> Unit,
    private val onPinch: (Float) -> Unit
) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density
    private var lx = 0f
    private var ly = 0f
    private var resizing = false
    private val sg = ScaleGestureDetector(
        ctx,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(det: ScaleGestureDetector): Boolean {
                onPinch(det.scaleFactor)
                return true
            }
        }
    )

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
        c.drawCircle(w - 16 * d, h - 16 * d, 14 * d, p)
        p.color = Color.WHITE
        p.textAlign = Paint.Align.LEFT
        p.textSize = 11 * d
        p.isFakeBoldText = true
        c.drawText(title, 8 * d, 16 * d, p)
        p.isFakeBoldText = false
        p.textSize = 9 * d
        c.drawText("Seret = geser  •  bulatan = ukuran  •  cubit = perbesar", 8 * d, 30 * d, p)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        sg.onTouchEvent(e)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lx = e.rawX
                ly = e.rawY
                val hs = min(56 * d, min(width, height) / 3f)
                resizing = e.x > width - hs && e.y > height - hs
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
                lx = e.rawX
                ly = e.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                if (e.pointerCount == 1 && !sg.isInProgress) {
                    val dx = (e.rawX - lx).toInt()
                    val dy = (e.rawY - ly).toInt()
                    if (dx != 0 || dy != 0) {
                        lx = e.rawX
                        ly = e.rawY
                        if (resizing) onResize(dx, dy) else onMove(dx, dy)
                    }
                }
            }
        }
        return true
    }
}
