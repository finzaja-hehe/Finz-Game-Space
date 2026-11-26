package com.finz.gamespace

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

fun goImmersive(a: Activity) {
    a.window.setBackgroundDrawable(ColorDrawable(Palette.BG))
    WindowCompat.setDecorFitsSystemWindows(a.window, false)
    val ctl = WindowInsetsControllerCompat(a.window, a.window.decorView)
    ctl.hide(WindowInsetsCompat.Type.systemBars())
    ctl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (Build.VERSION.SDK_INT >= 28) {
        val at = a.window.attributes
        at.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        a.window.attributes = at
    }
}

class LobbyBg(ctx: Context) : View(ctx) {

    private var t = 0f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val m = Matrix()
    private var base: LinearGradient? = null
    private var g1: RadialGradient? = null
    private var g2: RadialGradient? = null
    private var g3: RadialGradient? = null
    private var anim: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val a = Palette.a(context)
        val b = Palette.b(context)
        val mid = Palette.mix(a, b, 0.5f)
        val r = max(w, h) * 0.6f
        base = LinearGradient(
            0f, 0f, 0f, h.toFloat(), Palette.BG, Palette.BG2, Shader.TileMode.CLAMP
        )
        g1 = RadialGradient(
            0f, 0f, r, Palette.alpha(a, 110), Palette.alpha(a, 0), Shader.TileMode.CLAMP
        )
        g2 = RadialGradient(
            0f, 0f, r * 0.85f, Palette.alpha(b, 90), Palette.alpha(b, 0), Shader.TileMode.CLAMP
        )
        g3 = RadialGradient(
            0f, 0f, r * 0.6f, Palette.alpha(mid, 70), Palette.alpha(mid, 0), Shader.TileMode.CLAMP
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val an = ValueAnimator.ofFloat(0f, 1f)
        an.duration = 16000
        an.repeatCount = ValueAnimator.INFINITE
        an.interpolator = LinearInterpolator()
        an.addUpdateListener {
            t = it.animatedValue as Float
            invalidate()
        }
        an.start()
        anim = an
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        anim = null
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
        val tau = 6.2831855f
        p.style = Paint.Style.FILL
        p.shader = base
        c.drawRect(0f, 0f, w, h, p)
        glow(c, g1, w * (0.15f + 0.12f * sin(tau * t)), h * (0.25f + 0.15f * cos(tau * t)))
        glow(c, g2, w * (0.85f + 0.10f * cos(tau * t)), h * (0.75f + 0.12f * sin(tau * t)))
        glow(c, g3, w * (0.55f + 0.20f * sin(tau * t + 2f)), h * (0.50f + 0.20f * cos(tau * t + 1f)))
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f * resources.displayMetrics.density
        for (i in 0..5) {
            val f = (i * 0.17f + t) % 1f
            val x = f * (w + h) - h
            val al = 10 + 22 * sin(Math.PI * f).toFloat()
            p.color = Palette.alpha(Color.WHITE, al.toInt())
            c.drawLine(x, h, x + h * 0.7f, 0f, p)
        }
    }
}

class ProgressLine(ctx: Context) : View(ctx) {

    var progress = 0f
        set(v) {
            field = v
            invalidate()
        }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.shader = null
        p.style = Paint.Style.FILL
        p.color = 0x33FFFFFF
        rect.set(0f, 0f, w, h)
        c.drawRoundRect(rect, h, h, p)
        val fw = w * progress.coerceIn(0f, 1f)
        if (fw > 1f) {
            p.shader = LinearGradient(
                0f, 0f, w, 0f, Palette.a(context), Palette.b(context), Shader.TileMode.CLAMP
            )
            rect.set(0f, 0f, fw, h)
            c.drawRoundRect(rect, h, h, p)
            p.shader = null
        }
    }
}

class GameCardView(
    ctx: Context,
    private val icon: Drawable,
    private val title: String
) : View(ctx) {

    private var sa: ValueAnimator? = null
    var on = false
        set(v) {
            if (field == v) return
            field = v
            sa?.cancel()
            val an = ValueAnimator.ofFloat(scaleX, if (v) 1.05f else 1f)
            an.duration = 160
            an.addUpdateListener {
                val s = it.animatedValue as Float
                scaleX = s
                scaleY = s
            }
            an.start()
            sa = an
            invalidate()
        }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val clip = Path()
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val a = Palette.a(context)
        val b = Palette.b(context)
        val r = 20 * d
        rect.set(2 * d, 2 * d, w - 2 * d, h - 2 * d)
        clip.reset()
        clip.addRoundRect(rect, r, r, Path.Direction.CW)
        p.style = Paint.Style.FILL
        val top = if (on) Palette.mix(a, Palette.PANEL, 0.72f) else 0xFF1A1244.toInt()
        p.shader = LinearGradient(0f, 0f, 0f, h, top, Palette.BG, Shader.TileMode.CLAMP)
        c.drawPath(clip, p)
        p.shader = null
        c.save()
        c.clipPath(clip)
        icon.setBounds(0, 0, w.toInt(), w.toInt())
        icon.alpha = if (on) 80 else 36
        icon.draw(c)
        icon.alpha = 255
        p.shader = LinearGradient(
            0f, h * 0.3f, 0f, h,
            Palette.alpha(Palette.BG, 0), Palette.alpha(Palette.BG, 235), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null
        c.restore()
        val isz = (78 * d).toInt()
        val ix = ((w - isz) / 2f).toInt()
        val iy = (h * 0.14f).toInt()
        icon.setBounds(ix, iy, ix + isz, iy + isz)
        icon.draw(c)
        tp.textAlign = Paint.Align.CENTER
        tp.isFakeBoldText = true
        tp.color = Color.WHITE
        tp.textSize = 14 * d
        val txt = TextUtils.ellipsize(title, tp, w - 20 * d, TextUtils.TruncateAt.END).toString()
        c.drawText(txt, w / 2f, h - 22 * d, tp)
        p.style = Paint.Style.STROKE
        if (on) {
            p.shader = LinearGradient(0f, 0f, w, h, a, b, Shader.TileMode.CLAMP)
            p.strokeWidth = 11 * d
            p.alpha = 35
            c.drawRoundRect(rect, r, r, p)
            p.strokeWidth = 6 * d
            p.alpha = 70
            c.drawRoundRect(rect, r, r, p)
            p.strokeWidth = 2.5f * d
            p.alpha = 255
            c.drawRoundRect(rect, r, r, p)
            p.shader = null
        } else {
            p.shader = null
            p.strokeWidth = 1.2f * d
            p.color = 0xFF2D2260.toInt()
            c.drawRoundRect(rect, r, r, p)
        }
    }
}

class AddCardView(ctx: Context) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val a = Palette.a(context)
        val b = Palette.b(context)
        rect.set(2 * d, 2 * d, w - 2 * d, h - 2 * d)
        p.style = Paint.Style.FILL
        p.shader = null
        p.color = 0x1AFFFFFF
        c.drawRoundRect(rect, 20 * d, 20 * d, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.6f * d
        p.shader = LinearGradient(0f, 0f, w, h, a, b, Shader.TileMode.CLAMP)
        p.pathEffect = DashPathEffect(floatArrayOf(10 * d, 7 * d), 0f)
        c.drawRoundRect(rect, 20 * d, 20 * d, p)
        p.pathEffect = null
        p.strokeWidth = 3 * d
        p.strokeCap = Paint.Cap.ROUND
        val cx = w / 2f
        val cy = h / 2f - 12 * d
        c.drawLine(cx - 16 * d, cy, cx + 16 * d, cy, p)
        c.drawLine(cx, cy - 16 * d, cx, cy + 16 * d, p)
        p.shader = null
        tp.textAlign = Paint.Align.CENTER
        tp.isFakeBoldText = true
        tp.color = Color.WHITE
        tp.textSize = 13 * d
        c.drawText("Tambah Game", cx, cy + 46 * d, tp)
    }
}

class MainButton(ctx: Context, private val label: String) : View(ctx) {

    private var an: ValueAnimator? = null
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val tri = Path()
    private val d = resources.displayMetrics.density

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val v = ValueAnimator.ofFloat(0f, 1f)
        v.duration = 1200
        v.repeatCount = ValueAnimator.INFINITE
        v.repeatMode = ValueAnimator.REVERSE
        v.addUpdateListener {
            val s = 1f + 0.035f * (it.animatedValue as Float)
            scaleX = s
            scaleY = s
        }
        v.start()
        an = v
    }

    override fun onDetachedFromWindow() {
        an?.cancel()
        an = null
        scaleX = 1f
        scaleY = 1f
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val a = Palette.a(context)
        val b = Palette.b(context)
        rect.set(4 * d, 4 * d, w - 4 * d, h - 4 * d)
        val r = rect.height() / 2f
        p.style = Paint.Style.STROKE
        p.shader = null
        for (i in 1..3) {
            p.strokeWidth = (2 + i * 3) * d
            p.color = Palette.alpha(a, 60 / i)
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
        tp.textSize = 16 * d
        tp.letterSpacing = 0.12f
        c.drawText(label, tx + 18 * d, cy + 6 * d, tp)
    }
}

class IconCircle(ctx: Context, private val iconId: Int, on0: Boolean) : View(ctx) {

    var on = on0
        set(v) {
            field = v
            invalidate()
        }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val a = Palette.a(context)
        val b = Palette.b(context)
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f - 2 * d
        p.style = Paint.Style.FILL
        if (on) {
            p.shader = LinearGradient(0f, 0f, w, h, a, b, Shader.TileMode.CLAMP)
        } else {
            p.shader = null
            p.color = 0xFF0F0A2C.toInt()
        }
        c.drawCircle(cx, cy, r, p)
        p.shader = LinearGradient(0f, 0f, w, h, a, b, Shader.TileMode.CLAMP)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.6f * d
        c.drawCircle(cx, cy, r, p)
        p.shader = null
        HudIcons.draw(c, p, iconId, cx, cy, 22 * d, Color.WHITE, d)
    }
}
