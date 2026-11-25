package com.finz.gamespace

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object CompDraw {

    private val colors = intArrayOf(
        0xCC00E5A8.toInt(), 0xCCFF7A1A.toInt(), 0xCC2D9CFF.toInt(), 0xCCB46BFF.toInt(),
        0xCCFF4FD8.toInt(), 0xCCFFFFFF.toInt(), 0xCCFFD400.toInt(), 0xCCFF5B5B.toInt(),
        0xCC00E5FF.toInt(), 0xCC9BFF3B.toInt()
    )
    private val path = Path()

    private fun dot(
        c: Canvas, p: Paint, d: Float, x: Float, y: Float, rad: Float, color: Int, text: String
    ) {
        p.style = Paint.Style.FILL
        p.pathEffect = null
        p.color = color
        c.drawCircle(x, y, rad, p)
        if (text.isNotEmpty()) {
            p.color = Color.BLACK
            p.textAlign = Paint.Align.CENTER
            p.isFakeBoldText = true
            p.textSize = 9 * d
            while (p.measureText(text) > rad * 1.8f && p.textSize > 5 * d) {
                p.textSize = p.textSize - 0.5f * d
            }
            c.drawText(text, x, y + p.textSize * 0.35f, p)
        }
    }

    private fun ring(
        c: Canvas, p: Paint, d: Float, x: Float, y: Float, rad: Float, color: Int, dashed: Boolean
    ) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f * d
        p.color = color
        p.pathEffect = if (dashed) DashPathEffect(floatArrayOf(8 * d, 5 * d), 0f) else null
        c.drawCircle(x, y, rad, p)
        p.pathEffect = null
    }

    private fun arrow(
        c: Canvas, p: Paint, d: Float, x1: Float, y1: Float, x2: Float, y2: Float, col: Int
    ) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3 * d
        p.color = col
        p.pathEffect = null
        c.drawLine(x1, y1, x2, y2, p)
        val a = atan2(y2 - y1, x2 - x1)
        val l = 14 * d
        c.drawLine(x2, y2, x2 - l * cos(a - 0.5f), y2 - l * sin(a - 0.5f), p)
        c.drawLine(x2, y2, x2 - l * cos(a + 0.5f), y2 - l * sin(a + 0.5f), p)
    }

    private fun cursor(c: Canvas, p: Paint, d: Float, x: Float, y: Float, s: Float, col: Int) {
        path.reset()
        path.moveTo(x, y)
        path.lineTo(x, y + s)
        path.lineTo(x + s * 0.28f, y + s * 0.75f)
        path.lineTo(x + s * 0.5f, y + s * 1.1f)
        path.lineTo(x + s * 0.65f, y + s * 1.02f)
        path.lineTo(x + s * 0.45f, y + s * 0.68f)
        path.lineTo(x + s * 0.78f, y + s * 0.62f)
        path.close()
        p.style = Paint.Style.FILL
        p.pathEffect = null
        p.color = col
        c.drawPath(path, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * d
        p.color = Color.BLACK
        c.drawPath(path, p)
    }

    fun draw(c: Canvas, p: Paint, d: Float, ox: Int, oy: Int, k: KeyComp, r: IntArray) {
        val col = colors[k.type.coerceIn(0, colors.size - 1)]
        val x = k.pts[0] * r[0] - ox
        val y = k.pts[1] * r[1] - oy
        when (k.type) {
            KeyComp.HOLD -> {
                dot(c, p, d, x, y, 20 * d, col, KeyNames.label(k.keys[0]))
                ring(c, p, d, x, y, 28 * d, col, false)
            }
            KeyComp.SWIPE -> {
                val x2 = k.pts[2] * r[0] - ox
                val y2 = k.pts[3] * r[1] - oy
                arrow(c, p, d, x, y, x2, y2, col)
                dot(c, p, d, x, y, 20 * d, col, KeyNames.label(k.keys[0]))
            }
            KeyComp.JOY -> {
                val rad = k.pts[2] * min(r[0], r[1])
                ring(c, p, d, x, y, rad, col, false)
                dot(c, p, d, x, y, 5 * d, col, "")
                dot(c, p, d, x, y - rad, 15 * d, col, KeyNames.label(k.keys[0]))
                dot(c, p, d, x - rad, y, 15 * d, col, KeyNames.label(k.keys[1]))
                dot(c, p, d, x, y + rad, 15 * d, col, KeyNames.label(k.keys[2]))
                dot(c, p, d, x + rad, y, 15 * d, col, KeyNames.label(k.keys[3]))
            }
            KeyComp.GROUP -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2 * d
                p.color = col
                p.pathEffect = DashPathEffect(floatArrayOf(6 * d, 4 * d), 0f)
                var px = x
                var py = y
                var i = 2
                while (i + 1 < k.pts.size) {
                    val nx = k.pts[i] * r[0] - ox
                    val ny = k.pts[i + 1] * r[1] - oy
                    c.drawLine(px, py, nx, ny, p)
                    px = nx
                    py = ny
                    i += 2
                }
                p.pathEffect = null
                var j = 0
                while (j + 1 < k.pts.size) {
                    dot(
                        c, p, d, k.pts[j] * r[0] - ox, k.pts[j + 1] * r[1] - oy, 14 * d, col,
                        if (j == 0) KeyNames.label(k.keys[0]) else ""
                    )
                    j += 2
                }
            }
            KeyComp.BALL -> {
                ring(c, p, d, x, y, 24 * d, col, false)
                p.style = Paint.Style.FILL
                p.color = Color.WHITE
                p.textAlign = Paint.Align.CENTER
                p.isFakeBoldText = true
                p.textSize = 16 * d
                c.drawText("F", x, y + 6 * d, p)
            }
            KeyComp.SCROLL -> {
                val x2 = k.pts[2] * r[0] - ox
                val y2 = k.pts[3] * r[1] - oy
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2 * d
                p.color = col
                p.pathEffect = DashPathEffect(floatArrayOf(6 * d, 4 * d), 0f)
                c.drawLine(x, y, x2, y2, p)
                p.pathEffect = null
                dot(c, p, d, x, y, 18 * d, col, "▲")
                dot(c, p, d, x2, y2, 18 * d, col, "▼")
            }
            KeyComp.FPSV, KeyComp.FREE -> {
                val rad = k.pts[2] * min(r[0], r[1])
                ring(c, p, d, x, y, rad, col, k.type == KeyComp.FREE)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2 * d
                p.color = col
                c.drawLine(x - 9 * d, y, x + 9 * d, y, p)
                c.drawLine(x, y - 9 * d, x, y + 9 * d, p)
                val t = if (k.keys[0] == 0) {
                    if (k.type == KeyComp.FPSV) "AUTO" else "?"
                } else {
                    KeyNames.label(k.keys[0])
                }
                dot(c, p, d, x, y - rad, 17 * d, col, t)
            }
            KeyComp.PICK -> {
                cursor(c, p, d, x, y, 28 * d, col)
                dot(c, p, d, x + 38 * d, y + 16 * d, 16 * d, col, KeyNames.label(k.keys[0]))
            }
            else -> dot(c, p, d, x, y, 22 * d, col, KeyNames.label(k.keys[0]))
        }
    }

    fun highlight(c: Canvas, p: Paint, d: Float, ox: Int, oy: Int, k: KeyComp, r: IntArray) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3 * d
        p.color = Color.YELLOW
        p.pathEffect = null
        if (k.type == KeyComp.JOY || k.type == KeyComp.FPSV || k.type == KeyComp.FREE) {
            val x = k.pts[0] * r[0] - ox
            val y = k.pts[1] * r[1] - oy
            c.drawCircle(x, y, k.pts[2] * min(r[0], r[1]) + 24 * d, p)
            return
        }
        val n = if (k.type == KeyComp.SWIPE || k.type == KeyComp.GROUP ||
            k.type == KeyComp.SCROLL
        ) k.pts.size else 2
        var j = 0
        while (j + 1 < n) {
            c.drawCircle(k.pts[j] * r[0] - ox, k.pts[j + 1] * r[1] - oy, 34 * d, p)
            j += 2
        }
    }
}
