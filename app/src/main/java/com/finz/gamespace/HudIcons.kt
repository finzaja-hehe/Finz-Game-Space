package com.finz.gamespace

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

object HudIcons {

    private val path = Path()
    private val rf = RectF()

    private fun keyboard(c: Canvas, p: Paint, cx: Float, cy: Float, h: Float) {
        p.style = Paint.Style.STROKE
        rf.set(cx - h, cy - h * 0.6f, cx + h, cy + h * 0.6f)
        c.drawRoundRect(rf, h * 0.2f, h * 0.2f, p)
        p.style = Paint.Style.FILL
        for (r in 0..2) {
            for (k in 0..3) {
                c.drawCircle(
                    cx - h * 0.6f + k * h * 0.4f, cy - h * 0.3f + r * h * 0.3f, h * 0.07f, p
                )
            }
        }
    }

    fun draw(c: Canvas, p: Paint, id: Int, cx: Float, cy: Float, s: Float, color: Int, d: Float) {
        p.shader = null
        p.pathEffect = null
        p.color = color
        p.strokeWidth = 2f * d
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        val h = s / 2f
        when (id) {
            0 -> {
                p.style = Paint.Style.STROKE
                c.drawCircle(cx, cy, h * 0.85f, p)
                p.style = Paint.Style.FILL
                rf.set(cx - h * 0.45f, cy - h * 0.12f, cx + h * 0.45f, cy + h * 0.12f)
                c.drawRoundRect(rf, h * 0.12f, h * 0.12f, p)
            }
            1 -> {
                p.style = Paint.Style.STROKE
                c.drawCircle(cx, cy - h * 0.2f, h * 0.55f, p)
                c.drawLine(cx - h * 0.28f, cy + h * 0.55f, cx + h * 0.28f, cy + h * 0.55f, p)
                c.drawLine(cx - h * 0.2f, cy + h * 0.85f, cx + h * 0.2f, cy + h * 0.85f, p)
            }
            2 -> {
                p.style = Paint.Style.STROKE
                c.drawCircle(cx, cy, h * 0.55f, p)
                c.drawLine(cx, cy - h * 0.55f, cx, cy - h, p)
                c.drawLine(cx, cy + h * 0.55f, cx, cy + h, p)
                c.drawLine(cx - h * 0.55f, cy, cx - h, cy, p)
                c.drawLine(cx + h * 0.55f, cy, cx + h, cy, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, h * 0.1f, p)
            }
            3 -> keyboard(c, p, cx, cy, h * 0.95f)
            4 -> {
                p.style = Paint.Style.STROKE
                rf.set(cx - h * 0.9f, cy - h * 0.5f, cx + h * 0.9f, cy + h * 0.7f)
                c.drawRoundRect(rf, h * 0.2f, h * 0.2f, p)
                c.drawCircle(cx, cy + h * 0.1f, h * 0.35f, p)
                c.drawLine(cx - h * 0.35f, cy - h * 0.5f, cx - h * 0.2f, cy - h * 0.8f, p)
                c.drawLine(cx - h * 0.2f, cy - h * 0.8f, cx + h * 0.2f, cy - h * 0.8f, p)
                c.drawLine(cx + h * 0.2f, cy - h * 0.8f, cx + h * 0.35f, cy - h * 0.5f, p)
            }
            5 -> {
                p.style = Paint.Style.STROKE
                c.drawCircle(cx, cy, h * 0.85f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, h * 0.45f, p)
            }
            6 -> {
                p.style = Paint.Style.STROKE
                rf.set(cx - h * 0.7f, cy - h * 0.6f, cx + h * 0.7f, cy + h * 0.8f)
                c.drawArc(rf, -60f, 300f, false, p)
                c.drawLine(cx, cy - h * 0.95f, cx, cy - h * 0.05f, p)
            }
            7 -> {
                p.style = Paint.Style.STROKE
                rf.set(cx - h * 0.85f, cy - h * 0.65f, cx + h * 0.85f, cy + h * 1.05f)
                c.drawArc(rf, 180f, 180f, false, p)
                c.drawLine(cx - h * 0.85f, cy + h * 0.2f, cx + h * 0.85f, cy + h * 0.2f, p)
                c.drawLine(cx, cy + h * 0.2f, cx + h * 0.45f, cy - h * 0.35f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy + h * 0.2f, h * 0.12f, p)
            }
            8 -> {
                p.style = Paint.Style.FILL
                rf.set(cx - h * 0.8f, cy + h * 0.1f, cx - h * 0.4f, cy + h * 0.8f)
                c.drawRect(rf, p)
                rf.set(cx - h * 0.2f, cy - h * 0.4f, cx + h * 0.2f, cy + h * 0.8f)
                c.drawRect(rf, p)
                rf.set(cx + h * 0.4f, cy - h * 0.8f, cx + h * 0.8f, cy + h * 0.8f)
                c.drawRect(rf, p)
            }
            9 -> {
                p.style = Paint.Style.STROKE
                rf.set(cx - h * 0.7f, cy - h * 0.7f, cx + h * 0.7f, cy + h * 0.7f)
                c.drawArc(rf, -40f, 280f, false, p)
                val a = Math.toRadians(240.0)
                val nx = cos(a).toFloat()
                val ny = sin(a).toFloat()
                val ex = cx + nx * h * 0.7f
                val ey = cy + ny * h * 0.7f
                val tx = -ny
                val ty = nx
                path.reset()
                path.moveTo(ex + tx * h * 0.45f, ey + ty * h * 0.45f)
                path.lineTo(ex + nx * h * 0.32f, ey + ny * h * 0.32f)
                path.lineTo(ex - nx * h * 0.32f, ey - ny * h * 0.32f)
                path.close()
                p.style = Paint.Style.FILL
                c.drawPath(path, p)
            }
            10 -> {
                p.style = Paint.Style.FILL
                path.reset()
                path.moveTo(cx - h * 0.9f, cy - h * 0.3f)
                path.lineTo(cx - h * 0.45f, cy - h * 0.3f)
                path.lineTo(cx + h * 0.05f, cy - h * 0.75f)
                path.lineTo(cx + h * 0.05f, cy + h * 0.75f)
                path.lineTo(cx - h * 0.45f, cy + h * 0.3f)
                path.lineTo(cx - h * 0.9f, cy + h * 0.3f)
                path.close()
                c.drawPath(path, p)
                p.style = Paint.Style.STROKE
                c.drawLine(cx + h * 0.35f, cy - h * 0.35f, cx + h * 0.9f, cy + h * 0.35f, p)
                c.drawLine(cx + h * 0.35f, cy + h * 0.35f, cx + h * 0.9f, cy - h * 0.35f, p)
            }
            11 -> {
                val a = Path()
                a.addCircle(cx, cy, h * 0.8f, Path.Direction.CW)
                val b = Path()
                b.addCircle(cx + h * 0.45f, cy - h * 0.25f, h * 0.7f, Path.Direction.CW)
                a.op(b, Path.Op.DIFFERENCE)
                p.style = Paint.Style.FILL
                c.drawPath(a, p)
            }
            12 -> {
                p.style = Paint.Style.STROKE
                c.drawCircle(cx, cy + h * 0.1f, h * 0.75f, p)
                c.drawLine(cx, cy + h * 0.1f, cx, cy - h * 0.35f, p)
                c.drawLine(cx, cy + h * 0.1f, cx + h * 0.35f, cy + h * 0.25f, p)
                c.drawLine(cx - h * 0.2f, cy - h * 0.85f, cx + h * 0.2f, cy - h * 0.85f, p)
            }
            13 -> {
                p.style = Paint.Style.STROKE
                rf.set(cx - h * 0.22f, cy - h * 0.9f, cx + h * 0.22f, cy + h * 0.3f)
                c.drawRoundRect(rf, h * 0.22f, h * 0.22f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy + h * 0.55f, h * 0.38f, p)
                c.drawRect(cx - h * 0.08f, cy - h * 0.3f, cx + h * 0.08f, cy + h * 0.4f, p)
            }
            14 -> {
                p.style = Paint.Style.STROKE
                val oy = cy + h * 0.6f
                for (k in 1..3) {
                    val r = h * 0.3f * k
                    rf.set(cx - r, oy - r, cx + r, oy + r)
                    c.drawArc(rf, -135f, 90f, false, p)
                }
                p.style = Paint.Style.FILL
                c.drawCircle(cx, oy, h * 0.1f, p)
            }
            15 -> {
                p.style = Paint.Style.STROKE
                rf.set(cx - h * 0.9f, cy - h * 0.65f, cx + h * 0.9f, cy + h * 0.45f)
                c.drawRoundRect(rf, h * 0.15f, h * 0.15f, p)
                c.drawLine(cx, cy + h * 0.45f, cx, cy + h * 0.8f, p)
                c.drawLine(cx - h * 0.4f, cy + h * 0.8f, cx + h * 0.4f, cy + h * 0.8f, p)
            }
            16 -> {
                p.style = Paint.Style.STROKE
                c.drawLine(cx - h * 0.85f, cy, cx + h * 0.85f, cy, p)
                c.drawLine(cx - h * 0.85f, cy, cx - h * 0.5f, cy - h * 0.3f, p)
                c.drawLine(cx - h * 0.85f, cy, cx - h * 0.5f, cy + h * 0.3f, p)
                c.drawLine(cx + h * 0.85f, cy, cx + h * 0.5f, cy - h * 0.3f, p)
                c.drawLine(cx + h * 0.85f, cy, cx + h * 0.5f, cy + h * 0.3f, p)
            }
            17 -> {
                p.style = Paint.Style.FILL
                path.reset()
                path.moveTo(cx + h * 0.25f, cy - h)
                path.lineTo(cx - h * 0.5f, cy + h * 0.15f)
                path.lineTo(cx - h * 0.05f, cy + h * 0.15f)
                path.lineTo(cx - h * 0.25f, cy + h)
                path.lineTo(cx + h * 0.5f, cy - h * 0.2f)
                path.lineTo(cx + h * 0.05f, cy - h * 0.2f)
                path.close()
                c.drawPath(path, p)
            }
            else -> {
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, h * 0.3f, p)
            }
        }
        p.style = Paint.Style.FILL
    }
}
