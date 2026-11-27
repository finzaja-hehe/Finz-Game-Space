package com.finz.gamespace

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Random
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class SplashView(ctx: Context) : View(ctx) {

    var t = 0f
    var flash = 0f
    private val n = 44
    private val ang = FloatArray(n)
    private val spd = FloatArray(n)
    private val del = FloatArray(n)
    private val life = FloatArray(n)
    private val sz = FloatArray(n)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val m = Matrix()
    private val d = resources.displayMetrics.density
    private var base: LinearGradient? = null
    private var g1: RadialGradient? = null
    private var g2: RadialGradient? = null

    init {
        val r = Random(7)
        for (i in 0 until n) {
            ang[i] = r.nextFloat() * 6.2831855f
            spd[i] = 0.35f + r.nextFloat() * 0.9f
            del[i] = 0.4f + r.nextFloat() * 1.2f
            life[i] = 1.6f + r.nextFloat() * 1.4f
            sz[i] = 1.2f + r.nextFloat() * 2.4f
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val a = Palette.a(context)
        val b = Palette.b(context)
        val r = max(w, h) * 0.55f
        base = LinearGradient(0f, 0f, 0f, h.toFloat(), Palette.BG, Palette.BG2, Shader.TileMode.CLAMP)
        g1 = RadialGradient(0f, 0f, r, Palette.alpha(a, 120), Palette.alpha(a, 0), Shader.TileMode.CLAMP)
        g2 = RadialGradient(0f, 0f, r * 0.85f, Palette.alpha(b, 100), Palette.alpha(b, 0), Shader.TileMode.CLAMP)
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
        val a = Palette.a(context)
        val b = Palette.b(context)
        p.style = Paint.Style.FILL
        p.shader = base
        c.drawRect(0f, 0f, w, h, p)
        glow(c, g1, w * (0.2f + 0.1f * sin(t * 0.9f)), h * (0.3f + 0.12f * cos(t * 0.7f)))
        glow(c, g2, w * (0.8f + 0.1f * cos(t * 0.8f)), h * (0.7f + 0.1f * sin(t * 0.6f)))
        p.shader = null

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * d
        for (i in 0..4) {
            val f = (t * 0.35f + i * 0.21f) % 1f
            val x = f * (w + h) - h
            val al = (34 * sin(3.1415927f * f)).toInt()
            p.color = Palette.alpha(Color.WHITE, al)
            c.drawLine(x, h, x + h * 0.7f, 0f, p)
        }

        val cx = w / 2f
        val cy = h * 0.40f
        if (t > 0.4f) {
            for (k in 0..2) {
                val s = t - 0.4f - k * 0.55f
                if (s < 0f) continue
                val u = (s % 1.8f) / 1.8f
                p.strokeWidth = 1f + 2.5f * d * (1f - u)
                p.color = Palette.alpha(Palette.mix(a, b, k / 2f), ((1f - u) * 150).toInt())
                c.drawCircle(cx, cy, u * max(w, h) * 0.55f, p)
            }
        }

        p.style = Paint.Style.FILL
        for (i in 0 until n) {
            val tt = t - del[i]
            if (tt < 0f) continue
            val u = (tt % life[i]) / life[i]
            val rad = (0.08f + u * spd[i]) * min(w, h) * 0.9f
            val x = cx + cos(ang[i]) * rad
            val y = cy + sin(ang[i]) * rad - u * 20f * d
            val al = (sin(3.1415927f * u) * 230).toInt()
            p.color = Palette.alpha(if (i % 2 == 0) a else b, al)
            c.drawCircle(x, y, sz[i] * d * (1f - 0.5f * u), p)
        }

        if (flash > 0f) {
            p.color = Palette.alpha(Color.WHITE, (flash * 210).toInt())
            c.drawRect(0f, 0f, w, h, p)
        }
    }
}

class SplashActivity : AppCompatActivity() {

    private var gone = false
    private var start = 0L
    private var titleW = 1f
    private lateinit var root: FrameLayout
    private lateinit var bg: SplashView
    private lateinit var col: LinearLayout
    private lateinit var logo: LogoView
    private lateinit var title: TextView
    private lateinit var sub: TextView
    private lateinit var bar: ProgressLine

    private val loop = object : Runnable {
        override fun run() {
            if (gone) return
            frame((SystemClock.uptimeMillis() - start) / 1000f)
            if (!gone) root.postOnAnimation(this)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun clamp(x: Float) = x.coerceIn(0f, 1f)

    private fun backOut(x: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val k = x - 1f
        return 1f + c3 * k * k * k + c1 * k * k
    }

    private fun cubicOut(x: Float): Float {
        val k = 1f - x
        return 1f - k * k * k
    }

    private fun label(s: String, size: Float, color: Int, bold: Boolean, spacing: Float): TextView {
        val tv = TextView(this)
        tv.text = s
        tv.textSize = size
        tv.setTextColor(color)
        tv.letterSpacing = spacing
        tv.gravity = Gravity.CENTER
        if (bold) tv.typeface = Typeface.DEFAULT_BOLD
        return tv
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        goImmersive(this)
        val dm = resources.displayMetrics
        root = FrameLayout(this)
        root.setBackgroundColor(Palette.BG)
        bg = SplashView(this)
        root.addView(bg, FrameLayout.LayoutParams(-1, -1))

        col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        val side = (min(dm.widthPixels, dm.heightPixels) * 0.42f).toInt()
        logo = LogoView(this)
        col.addView(logo, LinearLayout.LayoutParams(side, side))
        title = label("FINZ GAME SPACE", 22f, Color.WHITE, true, 0.25f)
        titleW = max(title.paint.measureText(title.text.toString()), 1f)
        col.addView(title, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
        sub = label("BOOST  •  OVERLAY  •  KEYMAP", 11f, Palette.MUTED, false, 0.3f)
        col.addView(sub, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
        bar = ProgressLine(this)
        col.addView(bar, LinearLayout.LayoutParams(dp(200), dp(3)).apply { topMargin = dp(18) })
        root.addView(col, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        logo.alpha = 0f
        title.alpha = 0f
        sub.alpha = 0f
        root.setOnClickListener { go() }
        root.post {
            start = SystemClock.uptimeMillis()
            root.postOnAnimation(loop)
        }
    }

    private fun frame(t: Float) {
        bg.t = t
        val lx = clamp((t - 0.1f) / 0.8f)
        val s = 0.3f + 0.7f * backOut(lx)
        val fl = if (t > 1f) 1f + 0.015f * sin(t * 2.2f) else 1f
        logo.scaleX = s * fl
        logo.scaleY = s * fl
        logo.alpha = clamp(lx * 1.6f)
        logo.burst = clamp((t - 0.3f) / 0.9f)
        logo.pulse = if (t < 0.5f) 0.6f else 0.65f + 0.35f * sin(t * 3f)

        val tx = cubicOut(clamp((t - 0.7f) / 0.6f))
        title.alpha = tx
        title.translationY = (1f - tx) * dp(24)
        val off = (t * titleW * 0.6f) % (2f * titleW) - titleW
        title.paint.shader = LinearGradient(
            off, 0f, off + titleW, 0f,
            intArrayOf(Palette.a(this), Color.WHITE, Palette.b(this)),
            null, Shader.TileMode.MIRROR
        )
        title.invalidate()

        val sx = cubicOut(clamp((t - 0.95f) / 0.6f))
        sub.alpha = sx
        sub.translationY = (1f - sx) * dp(16)

        val px = clamp((t - 0.5f) / 2.3f)
        bar.progress = px * px * (3f - 2f * px)

        if (t > 3f) {
            val f = clamp((t - 3f) / 0.25f)
            bg.flash = f
            col.alpha = 1f - f
            if (f >= 1f) {
                bg.invalidate()
                go()
                return
            }
        }
        bg.invalidate()
    }

    @Suppress("DEPRECATION")
    private fun go() {
        if (gone) return
        gone = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        gone = true
        super.onDestroy()
    }
}
