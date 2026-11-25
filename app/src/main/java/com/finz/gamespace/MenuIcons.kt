package com.finz.gamespace

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object MenuIcons {

    private val path = Path()
    private val rf = RectF()

    private fun tri(c: Canvas, p: Paint, cx: Float, cy: Float, dx: Float, dy: Float, h: Float) {
        val tx = cx + dx * h * 0.95f
        val ty = cy + dy * h * 0.95f
        val bx = cx + dx * h * 0.5f
        val by = cy + dy * h * 0.5f
        val px = -dy
        val py = dx
        path.reset()
        path.moveTo(tx, ty)
        path.lineTo(bx + px * h * 0.3f, by + py * h * 0.3f)
        path.lineTo(bx - px * h * 0.3f, by - py * h * 0.3f)
        path.close()
        c.drawPath(path, p)
    }

    fun draw(c: Canvas, p: Paint, icon: Int, cx: Float, cy: Float, s: Float, color: Int, d: Float) {
        p.color = color
        p.strokeWidth = 2f * d
        p.strokeCap = Paint.Cap.ROUND
        p.pathEffect = null
        val h = s / 2f
        when (icon) {
            0 -> {
                p.style = Paint.Style.STROKE
                c.drawCircle(cx, cy, h * 0.85f, p)
                c.drawCircle(cx, cy, h * 0.5f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, h * 0.18f, p)
            }
            1 -> {
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, h * 0.45f, p)
                p.style = Paint.Style.STROKE
                c.drawCircle(cx, cy, h * 0.7f, p)
                c.drawCircle(cx, cy, h * 0.95f, p)
            }
            2 -> {
                val x1 = cx - h * 0.7f
                val y1 = cy + h * 0.5f
                val x2 = cx + h * 0.7f
                val y2 = cy - h * 0.5f
                p.style = Paint.Style.STROKE
                c.drawLine(x1, y1, x2, y2, p)
                val a = atan2(y2 - y1, x2 - x1)
                val l = h * 0.45f
                c.drawLine(x2, y2, x2 - l * cos(a - 0.5f), y2 - l * sin(a - 0.5f), p)
                c.drawLine(x2, y2, x2 - l * cos(a + 0.5f), y2 - l * sin(a + 0.5f), p)
                p.style = Paint.Style.FILL
                c.drawCircle(x1, y1, h * 0.17f, p)
            }
            3 -> {
                p.style = Paint.Style.FILL
                tri(c, p, cx, cy, 0f, -1f, h)
                tri(c, p, cx, cy, 0f, 1f, h)
                tri(c, p, cx, cy, -1f, 0f, h)
                tri(c, p, cx, cy, 1f, 0f, h)
                c.drawCircle(cx, cy, h * 0.14f, p)
            }
            4 -> {
                p.style = Paint.Style.STROKE
                p.pathEffect = DashPathEffect(floatArrayOf(4 * d, 3 * d), 0f)
                rf.set(cx - h, cy - h * 0.8f, cx + h, cy + h * 0.8f)
                c.drawRoundRect(rf, 6 * d, 6 * d, p)
                p.pathEffect = null
                p.style = Paint.Style.FILL
                c.drawCircle(cx - h * 0.4f, cy + h * 0.2f, h * 0.18f, p)
                c.drawCircle(cx + h * 0.4f, cy + h * 0.2f, h * 0.18f, p)
                c.drawCircle(cx, cy - h * 0.3f, h * 0.18f, p)
            }
            5 -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3f * d
                c.drawCircle(cx, cy, h * 0.85f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, h * 0.22f, p)
            }
            6 -> {
                p.style = Paint.Style.STROKE
                rf.set(cx - h * 0.5f, cy - h * 0.95f, cx + h * 0.5f, cy + h * 0.95f)
                c.drawRoundRect(rf, h * 0.5f, h * 0.5f, p)
                c.drawLine(cx, cy - h * 0.6f, cx, cy - h * 0.2f, p)
            }
            7 -> {
                p.style = Paint.Style.STROKE
                c.drawCircle(cx, cy, h * 0.55f, p)
                c.drawLine(cx, cy - h * 0.55f, cx, cy - h, p)
                c.drawLine(cx, cy + h * 0.55f, cx, cy + h, p)
                c.drawLine(cx - h * 0.55f, cy, cx - h, cy, p)
                c.drawLine(cx + h * 0.55f, cy, cx + h, cy, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, h * 0.1f, p)
            }
            8 -> {
                p.style = Paint.Style.STROKE
                path.reset()
                path.moveTo(cx - h, cy)
                path.quadTo(cx, cy - h * 0.95f, cx + h, cy)
                path.quadTo(cx, cy + h * 0.95f, cx - h, cy)
                c.drawPath(path, p)
                c.drawCircle(cx, cy, h * 0.3f, p)
            }
            else -> {
                p.style = Paint.Style.STROKE
                c.drawCircle(cx, cy, h * 0.6f, p)
                c.drawCircle(cx, cy, h * 0.22f, p)
                c.drawLine(cx, cy - h * 0.8f, cx, cy - h, p)
                c.drawLine(cx, cy + h * 0.8f, cx, cy + h, p)
                c.drawLine(cx - h * 0.8f, cy, cx - h, cy, p)
                c.drawLine(cx + h * 0.8f, cy, cx + h, cy, p)
            }
        }
        p.style = Paint.Style.FILL
    }
}
