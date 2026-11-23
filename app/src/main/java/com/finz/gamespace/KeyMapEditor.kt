package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

class KeyMapEditor(
    ctx: Context,
    private val onTap: (Float, Float) -> Unit,
    private val onClear: () -> Unit
) : View(ctx) {

    private var points: Map<Int, FloatArray> = emptyMap()
    private var pending: FloatArray? = null
    private var title = ""
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val loc = IntArray(2)
    private val d = resources.displayMetrics.density

    fun show(m: Map<Int, FloatArray>, pend: FloatArray?, t: String) {
        points = m
        pending = pend
        title = t
        invalidate()
    }

    private fun label(code: Int) = KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")

    override fun onDraw(c: Canvas) {
        c.drawColor(0x77000000)
        val r = KeyMapStore.real(context)
        getLocationOnScreen(loc)
        p.textAlign = Paint.Align.CENTER
        for ((k, v) in points) {
            val x = v[0] * r[0] - loc[0]
            val y = v[1] * r[1] - loc[1]
            p.style = Paint.Style.FILL
            p.color = 0xCC00E5A8.toInt()
            c.drawCircle(x, y, 22 * d, p)
            p.color = Color.BLACK
            p.textSize = 9 * d
            p.isFakeBoldText = true
            c.drawText(label(k), x, y + 3 * d, p)
        }
        pending?.let {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3 * d
            p.color = Color.YELLOW
            c.drawCircle(it[0] * r[0] - loc[0], it[1] * r[1] - loc[1], 22 * d, p)
        }
        p.style = Paint.Style.FILL
        p.isFakeBoldText = true
        p.color = Color.WHITE
        p.textSize = 13 * d
        c.drawText(title, width / 2f, 30 * d, p)
        p.isFakeBoldText = false
        p.textSize = 11 * d
        c.drawText("Ketuk layar = taruh titik, lalu tekan tombol keyboard", width / 2f, 50 * d, p)
        c.drawText("Ketuk titik = hapus  |  F10 atau Esc = selesai", width / 2f, 68 * d, p)
        rect.set(width - 130 * d, 10 * d, width - 10 * d, 46 * d)
        p.color = 0xFFFF3B47.toInt()
        c.drawRoundRect(rect, 10 * d, 10 * d, p)
        p.color = Color.WHITE
        p.textSize = 12 * d
        c.drawText("Hapus semua", rect.centerX(), rect.centerY() + 4 * d, p)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP) {
            getLocationOnScreen(loc)
            val lx = e.rawX - loc[0]
            val ly = e.rawY - loc[1]
            if (lx > width - 130 * d && ly < 50 * d) {
                onClear()
                return true
            }
            val r = KeyMapStore.real(context)
            onTap(e.rawX / r[0], e.rawY / r[1])
        }
        return true
    }
}
