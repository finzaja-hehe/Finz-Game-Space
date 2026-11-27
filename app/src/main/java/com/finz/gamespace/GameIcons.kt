package com.finz.gamespace

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

object GameIcons {

    private val path = Path()
    private val rf = RectF()

    fun draw(c: Canvas, p: Paint, id: Int, cx: Float, cy: Float, s: Float, color: Int, d: Float) {
        p.shader = null
        p.pathEffect = null
        p.color = color
        p.strokeWidth = 2f * d
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        val h = s / 2f
        when (id) {
            18 -> {
                p.style = Paint.Style.FILL
                p.textAlign = Paint.Align.CENTER
                p.isFakeBoldText = true
                p.textSize = h * 0.95f
                c.drawText("FPS", cx, cy + h * 0.3f, p)
                p.isFakeBoldText = false
            }
            19 -> {
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, h * 0.2f, p)
                p.style = Paint.Style.STROKE
                for (k in 1..2) {
                    val r = h * (0.4f + 0.3f * k)
                    rf.set(cx - r, cy - r, cx + r, cy + r)
                    c.drawArc(rf, -40f, 80f, false, p)
                    c.drawArc(rf, 140f, 80f, false, p)
                }
            }
            20 -> {
                p.style = Paint.Style.STROKE
                rf.set(cx - h * 0.95f, cy - h * 0.55f, cx + h * 0.25f, cy + h * 0.55f)
                c.drawRoundRect(rf, h * 0.18f, h * 0.18f, p)
                p.style = Paint.Style.FILL
                path.reset()
                path.moveTo(cx + h * 0.4f, cy)
                path.lineTo(cx + h * 0.95f, cy - h * 0.45f)
                path.lineTo(cx + h * 0.95f, cy + h * 0.45f)
                path.close()
                c.drawPath(path, p)
            }
            21 -> {
                p.style = Paint.Style.STROKE
                for (k in -1..1) {
                    val y = cy + k * h * 0.55f
                    c.drawLine(cx - h * 0.9f, y, cx + h * 0.9f, y, p)
                }
                p.style = Paint.Style.FILL
                c.drawCircle(cx - h * 0.35f, cy - h * 0.55f, h * 0.18f, p)
                c.drawCircle(cx + h * 0.4f, cy, h * 0.18f, p)
                c.drawCircle(cx - h * 0.1f, cy + h * 0.55f, h * 0.18f, p)
            }
            22 -> {
                p.style = Paint.Style.STROKE
                rf.set(cx - h * 0.8f, cy - h * 0.8f, cx + h * 0.8f, cy + h * 0.8f)
                c.drawArc(rf, -60f, 250f, false, p)
                p.style = Paint.Style.FILL
                path.reset()
                path.moveTo(cx + h * 0.12f, cy - h * 0.45f)
                path.lineTo(cx - h * 0.28f, cy + h * 0.08f)
                path.lineTo(cx - h * 0.02f, cy + h * 0.08f)
                path.lineTo(cx - h * 0.12f, cy + h * 0.45f)
                path.lineTo(cx + h * 0.28f, cy - h * 0.08f)
                path.lineTo(cx + h * 0.02f, cy - h * 0.08f)
                path.close()
                c.drawPath(path, p)
            }
            else -> {
                p.style = Paint.Style.FILL
                path.reset()
                path.moveTo(cx - h * 0.5f, cy - h * 0.9f)
                path.lineTo(cx + h * 0.5f, cy - h * 0.9f)
                path.lineTo(cx + h * 0.3f, cy - h * 0.35f)
                path.lineTo(cx - h * 0.3f, cy - h * 0.35f)
                path.close()
                c.drawPath(path, p)
                rf.set(cx - h * 0.3f, cy - h * 0.2f, cx + h * 0.3f, cy + h * 0.95f)
                c.drawRoundRect(rf, h * 0.12f, h * 0.12f, p)
            }
        }
        p.style = Paint.Style.FILL
    }
}
