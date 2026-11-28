package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class TStyle(
    val size: Int,
    val color: Int,
    val color2: Int,
    val thick: Int,
    val font: Int,
    val fx: Int
)

fun Layer.style(): TStyle = TStyle(size, color, color2, thick, font, fx)

object TextStyle {

    const val NEON = 1
    const val ITALIC = 2
    const val BOX = 4
    const val GRAD = 8
    const val UPPER = 16

    val FONTS = arrayOf(
        "Normal", "Tebal Hitam", "Ringan", "Tipis", "Sempit", "Medium",
        "Serif", "Mono", "Tulisan Tangan", "Santai", "Kapital Kecil", "Serif Mono"
    )
    private val FAM = arrayOf(
        "sans-serif", "sans-serif-black", "sans-serif-light", "sans-serif-thin",
        "sans-serif-condensed", "sans-serif-medium", "serif", "monospace",
        "cursive", "casual", "sans-serif-smallcaps", "serif-monospace"
    )
    private val boxP = Paint(Paint.ANTI_ALIAS_FLAG)

    fun faceOf(i: Int): Typeface = Typeface.create(FAM[i.coerceIn(0, FAM.size - 1)], Typeface.NORMAL)

    fun face(st: TStyle): Typeface = Typeface.create(
        FAM[st.font.coerceIn(0, FAM.size - 1)],
        if (st.fx and ITALIC != 0) Typeface.ITALIC else Typeface.NORMAL
    )

    fun text(s: String, st: TStyle): String = if (st.fx and UPPER != 0) s.uppercase() else s

    fun setup(p: TextPaint, sizePx: Float, st: TStyle, d: Float) {
        p.typeface = face(st)
        p.textSize = sizePx
        p.color = st.color
        p.textAlign = Paint.Align.LEFT
        p.isFakeBoldText = st.font == 0 || st.font == 6 || st.font == 7 || st.font == 11
        p.strokeJoin = Paint.Join.ROUND
        p.shader = null
        p.setShadowLayer(3f * d, 0f, 0f, Color.BLACK)
        p.style = if (st.thick > 0) Paint.Style.FILL_AND_STROKE else Paint.Style.FILL
        p.strokeWidth = st.thick * 0.55f * d
    }

    fun box(c: Canvas, l: Float, t: Float, r: Float, b: Float, d: Float) {
        boxP.shader = null
        boxP.style = Paint.Style.FILL
        boxP.color = 0xAA000000.toInt()
        c.drawRoundRect(l, t, r, b, 10 * d, 10 * d, boxP)
    }

    fun render(p: TextPaint, st: TStyle, d: Float, w: Float, col: Int = st.color, draw: () -> Unit) {
        if (st.fx and NEON != 0) {
            val core = Palette.mix(col, Color.WHITE, 0.6f)
            p.shader = null
            p.style = Paint.Style.FILL_AND_STROKE
            val radii = floatArrayOf(26f, 14f, 7f)
            for (r in radii) {
                p.strokeWidth = st.thick * 0.55f * d
                p.color = col
                p.setShadowLayer(r * d, 0f, 0f, col)
                draw()
            }
            p.clearShadowLayer()
            p.color = core
            p.style = if (st.thick > 0) Paint.Style.FILL_AND_STROKE else Paint.Style.FILL
            p.strokeWidth = st.thick * 0.55f * d
            draw()
            return
        }
        p.color = col
        if (st.fx and GRAD != 0) {
            p.shader = LinearGradient(0f, 0f, max(w, 1f), 0f, st.color, st.color2, Shader.TileMode.CLAMP)
        }
        draw()
        p.shader = null
    }
}

abstract class LoopView(ctx: Context, private val periodMs: Long) : View(ctx) {

    protected var t0 = 0L
    private var going = false
    private val loop = object : Runnable {
        override fun run() {
            if (!going) return
            invalidate()
            if (periodMs <= 0L) postOnAnimation(this) else postDelayed(this, periodMs)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        going = true
        t0 = SystemClock.uptimeMillis()
        post(loop)
    }

    override fun onDetachedFromWindow() {
        going = false
        super.onDetachedFromWindow()
    }

    protected fun now(): Float = (SystemClock.uptimeMillis() - t0) / 1000f
}

class TextBlock(ctx: Context, private val txt: String, private val st: TStyle) : View(ctx) {

    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        if (width <= 0) return
        val s = TextStyle.text(txt, st)
        if (s.isEmpty()) return
        TextStyle.setup(p, st.size * d, st, d)
        val w = max(1, width - (8 * d).toInt())
        val lay = StaticLayout.Builder.obtain(s, 0, s.length, p, w)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        val top = max(0f, (height - lay.height) / 2f)
        if (st.fx and TextStyle.BOX != 0) {
            TextStyle.box(
                c, 0f, max(0f, top - 4 * d), width.toFloat(),
                min(height.toFloat(), top + lay.height + 4 * d), d
            )
        }
        c.save()
        c.translate(4 * d, top)
        TextStyle.render(p, st, d, w.toFloat()) { lay.draw(c) }
        c.restore()
    }
}

class MarqueeView(ctx: Context, private val txt: String, private val st: TStyle) : LoopView(ctx, 0L) {

    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val s = TextStyle.text(txt, st)
        if (s.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        if (st.fx and TextStyle.BOX != 0) TextStyle.box(c, 0f, 0f, w, h, d)
        TextStyle.setup(p, st.size * d, st, d)
        val tw = p.measureText(s)
        val gap = max(w * 0.35f, 60f * d)
        val total = tw + gap
        val off = (now() * 70f * d) % total
        val y = h / 2f - (p.ascent() + p.descent()) / 2f
        TextStyle.render(p, st, d, w) {
            var x = w - off
            while (x > -tw) {
                c.drawText(s, x, y, p)
                x -= total
            }
        }
    }
}

class TimerView(ctx: Context, private val st: TStyle) : LoopView(ctx, 500L) {

    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val s = (SystemClock.elapsedRealtime() - StreamLayers.liveStart) / 1000
        val t = String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
        val clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, 0f, w, h)
        bg.color = 0x99000000.toInt()
        c.drawRoundRect(rect, h / 2f, h / 2f, bg)
        bg.color = 0xFFFF3B47.toInt()
        c.drawCircle(h * 0.5f, h / 2f, h * 0.14f, bg)
        val text = TextStyle.text("LIVE $t   $clock", st)
        TextStyle.setup(p, min(st.size * d, h * 0.5f), st, d)
        val x = h * 0.8f
        while (p.measureText(text) > w - x - 8 * d && p.textSize > 6 * d) p.textSize = p.textSize - 0.5f * d
        val y = h / 2f - (p.ascent() + p.descent()) / 2f
        TextStyle.render(p, st, d, w) { c.drawText(text, x, y, p) }
    }
}

class InfoView(ctx: Context, private val st: TStyle) : LoopView(ctx, 1000L) {

    private val act = OverlayActions(ctx, Handler(Looper.getMainLooper()))
    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val d = resources.displayMetrics.density
    private val parts = ArrayList<Pair<String, Int>>()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Ping.start()
    }

    private fun refresh() {
        parts.clear()
        val f = if (FpsClient.on) FpsClient.fps else -1
        val fc = when {
            f < 0 -> st.color
            f >= 55 -> 0xFF22E58B.toInt()
            f >= 30 -> 0xFFFFE600.toInt()
            else -> 0xFFFF5B5B.toInt()
        }
        parts.add(Pair("FPS " + (if (f < 0) "--" else f.toString()), fc))
        parts.add(Pair("PING " + Ping.text().replace(" ms", "ms"), st.color))
        val t = act.tempC()
        parts.add(Pair("%.0f°C".format(t), if (t >= 42f) 0xFFFF5B5B.toInt() else st.color))
        parts.add(Pair("RAM ${act.ramPct()}%", st.color))
        parts.add(Pair("BAT ${act.batPct()}%", st.color))
    }

    override fun onDraw(c: Canvas) {
        refresh()
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, 0f, w, h)
        bg.color = 0xAA000000.toInt()
        c.drawRoundRect(rect, h / 2f, h / 2f, bg)
        TextStyle.setup(p, min(st.size * d, h * 0.55f), st, d)
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
            val tx = x
            TextStyle.render(p, st, d, w, q.second) { c.drawText(q.first, tx, y, p) }
            x += p.measureText(q.first) + gap
        }
    }
}

class GoalView(ctx: Context, private val txt: String, private val st: TStyle) : View(ctx) {

    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val d = resources.displayMetrics.density
    private val nf: NumberFormat = NumberFormat.getInstance(Locale("in", "ID"))

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val parts = txt.split("|")
        val title = TextStyle.text(parts.getOrNull(0)?.trim().orEmpty(), st)
        val cur = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
        val tgt = max(1L, parts.getOrNull(2)?.trim()?.toLongOrNull() ?: 1L)
        val pct = min(100L, cur * 100 / tgt).toInt()
        rect.set(0f, 0f, w, h)
        bg.shader = null
        bg.style = Paint.Style.FILL
        bg.color = 0xB0080414.toInt()
        c.drawRoundRect(rect, 14 * d, 14 * d, bg)
        val pad = 10 * d
        val ts = min(st.size * d, h * 0.24f)
        TextStyle.setup(p, ts, st, d)
        TextStyle.render(p, st, d, w) { c.drawText(title, pad, h * 0.30f, p) }
        p.textAlign = Paint.Align.RIGHT
        TextStyle.render(p, st, d, w) { c.drawText("$pct%", w - pad, h * 0.30f, p) }
        val bt = h * 0.40f
        val bb = h * 0.60f
        rect.set(pad, bt, w - pad, bb)
        bg.color = 0x33FFFFFF
        c.drawRoundRect(rect, (bb - bt) / 2f, (bb - bt) / 2f, bg)
        if (pct > 0) {
            rect.set(pad, bt, pad + (w - 2 * pad) * pct / 100f, bb)
            bg.shader = LinearGradient(
                pad, 0f, w - pad, 0f, Palette.a(context), Palette.b(context), Shader.TileMode.CLAMP
            )
            c.drawRoundRect(rect, (bb - bt) / 2f, (bb - bt) / 2f, bg)
            bg.shader = null
        }
        TextStyle.setup(p, ts * 0.8f, st, d)
        p.textAlign = Paint.Align.CENTER
        TextStyle.render(p, st, d, w) {
            c.drawText("Rp ${nf.format(cur)} / Rp ${nf.format(tgt)}", w / 2f, h * 0.88f, p)
        }
    }
}

class CountView(
    ctx: Context,
    private val key: Int,
    private val name: String,
    private val minutes: Int,
    private val st: TStyle
) : LoopView(ctx, 500L) {

    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val end = context.getSharedPreferences("stream", Context.MODE_PRIVATE).getLong("end_$key", 0L)
        val left = if (end > 0L) max(0L, (end - System.currentTimeMillis()) / 1000) else minutes * 60L
        val t = String.format(Locale.US, "%02d:%02d", left / 60, left % 60)
        val w = width.toFloat()
        val h = height.toFloat()
        rect.set(0f, 0f, w, h)
        bg.color = 0xB0080414.toInt()
        c.drawRoundRect(rect, 16 * d, 16 * d, bg)
        val nameSt = TStyle(st.size, Palette.MUTED, Palette.MUTED, 0, st.font, 0)
        TextStyle.setup(p, min(st.size * d * 0.6f, h * 0.2f), nameSt, d)
        p.textAlign = Paint.Align.CENTER
        c.drawText(TextStyle.text(name, st), w / 2f, h * 0.30f, p)
        TextStyle.setup(p, min(st.size * d * 1.6f, h * 0.5f), st, d)
        p.textAlign = Paint.Align.CENTER
        TextStyle.render(p, st, d, w) { c.drawText(t, w / 2f, h * 0.80f, p) }
    }
}

class ClockView(ctx: Context, private val st: TStyle) : LoopView(ctx, 500L) {

    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val dt = Date()
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(dt)
        val date = TextStyle.text(SimpleDateFormat("EEEE, d MMM yyyy", Locale("id", "ID")).format(dt), st)
        if (st.fx and TextStyle.BOX != 0) TextStyle.box(c, 0f, 0f, w, h, d)
        TextStyle.setup(p, min(st.size * d * 1.4f, h * 0.5f), st, d)
        p.textAlign = Paint.Align.CENTER
        while (p.measureText(time) > w - 12 * d && p.textSize > 8 * d) p.textSize = p.textSize - 0.5f * d
        TextStyle.render(p, st, d, w) { c.drawText(time, w / 2f, h * 0.55f, p) }
        TextStyle.setup(p, min(st.size * d * 0.62f, h * 0.2f), st, d)
        p.textAlign = Paint.Align.CENTER
        while (p.measureText(date) > w - 12 * d && p.textSize > 6 * d) p.textSize = p.textSize - 0.5f * d
        TextStyle.render(p, st, d, w) { c.drawText(date, w / 2f, h * 0.86f, p) }
    }
}

class RotateView(
    ctx: Context,
    txt: String,
    private val secs: Int,
    private val st: TStyle
) : LoopView(ctx, 0L) {

    private val lines = txt.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        if (lines.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val per = max(1, secs) * 1000L
        val t = (SystemClock.uptimeMillis() - t0).coerceAtLeast(0L)
        val idx = ((t / per) % lines.size).toInt()
        val ph = t % per
        val fa = min(1f, min(ph / 300f, (per - ph) / 300f)).coerceIn(0f, 1f)
        val s = TextStyle.text(lines[idx], st)
        TextStyle.setup(p, st.size * d, st, d)
        p.textAlign = Paint.Align.CENTER
        while (p.measureText(s) > w - 16 * d && p.textSize > 8 * d) p.textSize = p.textSize - 0.5f * d
        val y = h / 2f - (p.ascent() + p.descent()) / 2f
        val sc = c.saveLayerAlpha(0f, 0f, w, h, (fa * 255).toInt())
        if (st.fx and TextStyle.BOX != 0) {
            val tw = p.measureText(s)
            TextStyle.box(c, max(0f, w / 2f - tw / 2f - 10 * d), 0f, min(w, w / 2f + tw / 2f + 10 * d), h, d)
        }
        TextStyle.render(p, st, d, w) { c.drawText(s, w / 2f, y, p) }
        c.restoreToCount(sc)
    }
}

class CounterView(
    ctx: Context,
    private val key: Int,
    private val label: String,
    private val st: TStyle
) : LoopView(ctx, 300L) {

    private val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val n = context.getSharedPreferences("stream", Context.MODE_PRIVATE).getInt("cnt_$key", 0)
        val w = width.toFloat()
        val h = height.toFloat()
        if (st.fx and TextStyle.BOX != 0) TextStyle.box(c, 0f, 0f, w, h, d)
        val ls = TStyle(st.size, st.color, st.color2, 0, st.font, st.fx and TextStyle.GRAD.inv())
        TextStyle.setup(p, min(st.size * d * 0.62f, h * 0.2f), ls, d)
        p.textAlign = Paint.Align.CENTER
        val lab = TextStyle.text(label, st)
        TextStyle.render(p, ls, d, w) { c.drawText(lab, w / 2f, h * 0.28f, p) }
        TextStyle.setup(p, min(st.size * d * 2.2f, h * 0.58f), st, d)
        p.textAlign = Paint.Align.CENTER
        TextStyle.render(p, st, d, w) { c.drawText(n.toString(), w / 2f, h * 0.84f, p) }
    }
}

class ShapeView(
    ctx: Context,
    private val label: String,
    private val dir: Int,
    private val st: TStyle
) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = 16 * d
        rect.set(2 * d, 2 * d, w - 2 * d, h - 2 * d)
        p.style = Paint.Style.FILL
        p.shader = when (dir) {
            1 -> LinearGradient(0f, 0f, 0f, h, st.color, st.color2, Shader.TileMode.CLAMP)
            2 -> LinearGradient(0f, 0f, w, h, st.color, st.color2, Shader.TileMode.CLAMP)
            else -> LinearGradient(0f, 0f, w, 0f, st.color, st.color2, Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(rect, r, r, p)
        p.shader = null
        if (st.thick > 0) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = st.thick * d
            p.color = 0xCCFFFFFF.toInt()
            c.drawRoundRect(rect, r, r, p)
        }
        if (label.isNotBlank()) {
            val ls = TStyle(st.size, Color.WHITE, Color.WHITE, 0, st.font, st.fx and TextStyle.GRAD.inv())
            val s = TextStyle.text(label, ls)
            TextStyle.setup(tp, min(st.size * d, h * 0.6f), ls, d)
            tp.textAlign = Paint.Align.CENTER
            while (tp.measureText(s) > w - 16 * d && tp.textSize > 8 * d) tp.textSize = tp.textSize - 0.5f * d
            val y = h / 2f - (tp.ascent() + tp.descent()) / 2f
            TextStyle.render(tp, ls, d, w) { c.drawText(s, w / 2f, y, tp) }
        }
    }
}

class SceneView(ctx: Context, private val txt: String, private val st: TStyle) : LoopView(ctx, 0L) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val m = Matrix()
    private val d = resources.displayMetrics.density
    private var base: LinearGradient? = null
    private var g1: RadialGradient? = null
    private var g2: RadialGradient? = null

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val r = max(w, h) * 0.6f
        val b = Palette.b(context)
        base = LinearGradient(0f, 0f, 0f, h.toFloat(), Palette.BG, Palette.BG2, Shader.TileMode.CLAMP)
        g1 = RadialGradient(0f, 0f, r, Palette.alpha(st.color2, 130), Palette.alpha(st.color2, 0), Shader.TileMode.CLAMP)
        g2 = RadialGradient(0f, 0f, r * 0.8f, Palette.alpha(b, 110), Palette.alpha(b, 0), Shader.TileMode.CLAMP)
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
        val t = now()
        p.style = Paint.Style.FILL
        p.shader = base
        c.drawRect(0f, 0f, w, h, p)
        glow(c, g1, w * (0.2f + 0.15f * sin(t * 0.6f)), h * (0.3f + 0.15f * cos(t * 0.5f)))
        glow(c, g2, w * (0.8f + 0.12f * cos(t * 0.7f)), h * (0.7f + 0.12f * sin(t * 0.6f)))
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * d
        for (i in 0..4) {
            val f = (t * 0.1f + i * 0.21f) % 1f
            val x = f * (w + h) - h
            p.color = Palette.alpha(Color.WHITE, (30 * sin(3.1415927f * f)).toInt())
            c.drawLine(x, h, x + h * 0.7f, 0f, p)
        }
        val ls = txt.split("\n")
        val title = TextStyle.text(ls.firstOrNull().orEmpty(), st)
        val sub = TextStyle.text(ls.drop(1).joinToString(" "), st)
        val pul = 1f + 0.03f * sin(t * 2f)
        c.save()
        c.scale(pul, pul, w / 2f, h / 2f)
        TextStyle.setup(tp, min(st.size * d * 1.6f, h * 0.22f), st, d)
        tp.textAlign = Paint.Align.CENTER
        while (tp.measureText(title) > w * 0.9f && tp.textSize > 10 * d) tp.textSize = tp.textSize - 1f * d
        TextStyle.render(tp, st, d, w) { c.drawText(title, w / 2f, h * 0.48f, tp) }
        if (sub.isNotEmpty()) {
            TextStyle.setup(tp, min(st.size * d * 0.8f, h * 0.1f), st, d)
            tp.textAlign = Paint.Align.CENTER
            while (tp.measureText(sub) > w * 0.9f && tp.textSize > 8 * d) tp.textSize = tp.textSize - 1f * d
            TextStyle.render(tp, st, d, w, 0xFFD9D4FF.toInt()) { c.drawText(sub, w / 2f, h * 0.64f, tp) }
        }
        c.restore()
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
