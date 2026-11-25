package com.finz.gamespace

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

object GgIcons {

    private val rf = RectF()
    private val path = Path()

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

    private fun home(c: Canvas, p: Paint, cx: Float, cy: Float, h: Float) {
        p.style = Paint.Style.FILL
        path.reset()
        path.moveTo(cx - h, cy - h * 0.1f)
        path.lineTo(cx, cy - h * 0.9f)
        path.lineTo(cx + h, cy - h * 0.1f)
        path.lineTo(cx + h * 0.7f, cy - h * 0.1f)
        path.lineTo(cx + h * 0.7f, cy + h * 0.8f)
        path.lineTo(cx + h * 0.2f, cy + h * 0.8f)
        path.lineTo(cx + h * 0.2f, cy + h * 0.3f)
        path.lineTo(cx - h * 0.2f, cy + h * 0.3f)
        path.lineTo(cx - h * 0.2f, cy + h * 0.8f)
        path.lineTo(cx - h * 0.7f, cy + h * 0.8f)
        path.lineTo(cx - h * 0.7f, cy - h * 0.1f)
        path.close()
        c.drawPath(path, p)
    }

    private fun gear(c: Canvas, p: Paint, cx: Float, cy: Float, h: Float) {
        p.style = Paint.Style.STROKE
        c.drawCircle(cx, cy, h * 0.3f, p)
        c.drawCircle(cx, cy, h * 0.58f, p)
        for (i in 0 until 8) {
            val a = Math.toRadians((i * 45).toDouble())
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            c.drawLine(
                cx + ca * h * 0.58f, cy + sa * h * 0.58f,
                cx + ca * h * 0.9f, cy + sa * h * 0.9f, p
            )
        }
    }

    private fun doc(c: Canvas, p: Paint, cx: Float, cy: Float, h: Float) {
        p.style = Paint.Style.STROKE
        rf.set(cx - h * 0.7f, cy - h * 0.9f, cx + h * 0.7f, cy + h * 0.9f)
        c.drawRoundRect(rf, h * 0.15f, h * 0.15f, p)
        for (i in 0..2) {
            val y = cy - h * 0.45f + i * h * 0.45f
            c.drawLine(cx - h * 0.4f, y, cx + h * 0.4f, y, p)
        }
    }

    private fun bolt(c: Canvas, p: Paint, cx: Float, cy: Float, h: Float) {
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

    fun draw(c: Canvas, p: Paint, id: Int, cx: Float, cy: Float, s: Float, color: Int, d: Float) {
        p.shader = null
        p.pathEffect = null
        p.color = color
        p.strokeWidth = 2f * d
        p.strokeCap = Paint.Cap.ROUND
        val h = s / 2f
        when (id) {
            0 -> home(c, p, cx, cy, h)
            1 -> gear(c, p, cx, cy, h)
            2 -> doc(c, p, cx, cy, h)
            3 -> {
                keyboard(c, p, cx - h * 0.1f, cy - h * 0.2f, h * 0.85f)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3f * d
                c.drawLine(cx - h * 0.1f, cy + h * 0.95f, cx + h * 0.9f, cy + h * 0.3f, p)
            }
            4 -> {
                keyboard(c, p, cx - h * 0.05f, cy - h * 0.2f, h * 0.85f)
                p.style = Paint.Style.STROKE
                c.drawCircle(cx + h * 0.6f, cy + h * 0.65f, h * 0.3f, p)
                c.drawCircle(cx + h * 0.6f, cy + h * 0.65f, h * 0.1f, p)
            }
            5 -> {
                keyboard(c, p, cx, cy - h * 0.2f, h * 0.9f)
                p.style = Paint.Style.STROKE
                c.drawLine(cx, cy + h * 0.6f, cx, cy + h * 0.95f, p)
                c.drawLine(cx - h * 0.2f, cy + h * 0.6f, cx + h * 0.2f, cy + h * 0.6f, p)
                c.drawLine(cx - h * 0.2f, cy + h * 0.95f, cx + h * 0.2f, cy + h * 0.95f, p)
            }
            6 -> {
                p.style = Paint.Style.STROKE
                c.drawCircle(cx - h * 0.38f, cy, h * 0.5f, p)
                c.drawCircle(cx + h * 0.38f, cy, h * 0.5f, p)
            }
            7 -> bolt(c, p, cx, cy, h)
            else -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3f * d
                c.drawLine(cx - h * 0.7f, cy - h * 0.7f, cx + h * 0.7f, cy + h * 0.7f, p)
                c.drawLine(cx + h * 0.7f, cy - h * 0.7f, cx - h * 0.7f, cy + h * 0.7f, p)
            }
        }
        p.style = Paint.Style.FILL
    }
}
