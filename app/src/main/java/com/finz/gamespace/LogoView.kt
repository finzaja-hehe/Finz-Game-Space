package com.finz.gamespace

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min

class LogoView(ctx: Context) : View(ctx) {

    var pulse = 0.6f
        set(v) {
            field = v
            invalidate()
        }
    var burst = 0f
        set(v) {
            field = v
            invalidate()
        }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val body = Path()
    private val hi = Path()
    private val chip1 = Path()
    private val chip2 = Path()
    private val screen = Paint(Paint.FILTER_BITMAP_FLAG)
    private val src = Rect()
    private val dst = RectF()
    private var bmp: Bitmap? = null

    init {
        val id = resources.getIdentifier("logo_f", "drawable", ctx.packageName)
        if (id != 0) {
            val d = ContextCompat.getDrawable(ctx, id)
            if (d is BitmapDrawable) bmp = d.bitmap
        }
        screen.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        body.moveTo(26f, 30f)
        body.lineTo(90f, 22f)
        body.lineTo(80f, 38f)
        body.lineTo(58f, 38f)
        body.lineTo(54f, 48f)
        body.lineTo(76f, 46f)
        body.lineTo(68f, 60f)
        body.lineTo(46f, 60f)
        body.lineTo(24f, 96f)
        body.lineTo(14f, 94f)
        body.close()
        hi.moveTo(30f, 31f)
        hi.lineTo(86f, 24.5f)
        hi.lineTo(83f, 30f)
        hi.lineTo(34f, 35f)
        hi.close()
        chip1.moveTo(6f, 46f)
        chip1.lineTo(20f, 44f)
        chip1.lineTo(17f, 55f)
        chip1.close()
        chip2.moveTo(2f, 62f)
        chip2.lineTo(17f, 60f)
        chip2.lineTo(14f, 71f)
        chip2.close()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val s = min(w, h)
        val a = Palette.a(context)
        val b = Palette.b(context)
        val cx = w / 2f
        val cy = h / 2f
        if (burst > 0f && burst < 1f) {
            p.style = Paint.Style.STROKE
            p.shader = null
            p.strokeWidth = max(1f, s * 0.03f * (1f - burst))
            p.color = Palette.alpha(b, (200 * (1f - burst)).toInt())
            c.drawCircle(cx, cy, s * (0.25f + 0.55f * burst), p)
        }
        val img = bmp
        if (img != null) {
            src.set(0, 0, img.width, img.height)
            val k = s * (0.9f + 0.06f * pulse)
            dst.set(cx - k / 2f, cy - k / 2f, cx + k / 2f, cy + k / 2f)
            c.drawBitmap(img, src, dst, screen)
            return
        }
        c.save()
        c.translate((w - s) / 2f, (h - s) / 2f)
        c.scale(s / 100f, s / 100f)
        p.style = Paint.Style.STROKE
        p.strokeJoin = Paint.Join.ROUND
        p.shader = null
        for (i in 0..3) {
            p.strokeWidth = 3f + i * 5f
            p.color = Palette.alpha(a, ((14 + 46 * pulse) / (i + 1)).toInt())
            c.drawPath(body, p)
        }
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(16f, 22f, 92f, 96f, a, b, Shader.TileMode.CLAMP)
        c.drawPath(body, p)
        p.shader = null
        p.color = Palette.alpha(Color.WHITE, 90)
        c.drawPath(hi, p)
        p.color = Palette.alpha(a, 230)
        c.drawPath(chip1, p)
        c.drawPath(chip2, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.4f
        p.color = Palette.alpha(Color.WHITE, 200)
        c.drawPath(body, p)
        p.color = Palette.alpha(b, (120 + 120 * pulse).toInt())
        p.strokeWidth = 1.2f
        c.drawLine(88f, 44f, 98f, 58f, p)
        c.drawLine(96f, 66f, 86f, 76f, p)
        c.drawLine(8f, 84f, 18f, 76f, p)
        c.restore()
    }
}
