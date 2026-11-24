package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.min

interface EditorCtl {
    fun comps(): List<KeyComp>
    fun prompt(): String
    fun pending(): List<FloatArray>
    fun flowActive(): Boolean
    fun onPick(type: Int)
    fun onTap(x: Float, y: Float)
    fun onCancel()
    fun onClear()
    fun onClose()
}

class KeyMapEditor(ctx: Context, private val ctl: EditorCtl) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val box = RectF()
    private val cellR = RectF()
    private val loc = IntArray(2)
    private val d = resources.displayMetrics.density
    private var menuOpen = false
    private val names = arrayOf(
        "Touch", "Tahan", "Swipe", "Joystick",
        "Mouse Scroll", "FPS View", "Free Look", "Mouse Pick"
    )
    private val icons = arrayOf("👆", "✋", "↔", "🕹", "🖱", "🎯", "👁", "🔍")
    private val colors = intArrayOf(
        0xCC00E5A8.toInt(), 0xCCFF7A1A.toInt(), 0xCC2D9CFF.toInt(), 0xCCB46BFF.toInt()
    )

    private fun lab(code: Int) = KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")

    private fun dot(c: Canvas, x: Float, y: Float, rad: Float, color: Int, text: String) {
        p.style = Paint.Style.FILL
        p.color = color
        c.drawCircle(x, y, rad, p)
        if (text.isNotEmpty()) {
            p.color = Color.BLACK
            p.textAlign = Paint.Align.CENTER
            p.isFakeBoldText = true
            p.textSize = 9 * d
            c.drawText(text, x, y + 3 * d, p)
        }
    }

    private fun drawComp(c: Canvas, k: KeyComp, r: IntArray) {
        val x = k.pts[0] * r[0] - loc[0]
        val y = k.pts[1] * r[1] - loc[1]
        val col = colors[k.type]
        when (k.type) {
            KeyComp.SWIPE -> {
                val x2 = k.pts[2] * r[0] - loc[0]
                val y2 = k.pts[3] * r[1] - loc[1]
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3 * d
                p.color = col
                c.drawLine(x, y, x2, y2, p)
                dot(c, x2, y2, 8 * d, col, "")
                dot(c, x, y, 20 * d, col, lab(k.keys[0]))
            }
            KeyComp.JOY -> {
                val rad = k.pts[2] * min(r[0], r[1])
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3 * d
                p.color = col
                c.drawCircle(x, y, rad, p)
                dot(c, x, y, 5 * d, col, "")
                dot(c, x, y - rad, 15 * d, col, lab(k.keys[0]))
                dot(c, x - rad, y, 15 * d, col, lab(k.keys[1]))
                dot(c, x, y + rad, 15 * d, col, lab(k.keys[2]))
                dot(c, x + rad, y, 15 * d, col, lab(k.keys[3]))
            }
            else -> dot(c, x, y, 22 * d, col, lab(k.keys[0]))
        }
    }

    private fun btnRect(i: Int) {
        val w = 100 * d
        val right = width - 10 * d - i * (w + 8 * d)
        cellR.set(right - w, 10 * d, right, 46 * d)
    }

    private fun drawBtn(c: Canvas, i: Int, text: String, bgc: Int, fg: Int) {
        btnRect(i)
        p.style = Paint.Style.FILL
        p.color = bgc
        c.drawRoundRect(cellR, 10 * d, 10 * d, p)
        p.color = fg
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = true
        p.textSize = 11 * d
        c.drawText(text, cellR.centerX(), cellR.centerY() + 4 * d, p)
    }

    private fun menuBox() {
        val bw = min(width - 32 * d, 460 * d)
        val bh = 250 * d
        box.set((width - bw) / 2f, (height - bh) / 2f, (width + bw) / 2f, (height + bh) / 2f)
    }

    private fun cell(i: Int) {
        val gap = 10 * d
        val cw = (box.width() - 5 * gap) / 4f
        val ch = 76 * d
        val col = i % 4
        val row = i / 4
        val l = box.left + gap + col * (cw + gap)
        val t = box.top + 64 * d + row * (ch + gap)
        cellR.set(l, t, l + cw, t + ch)
    }

    private fun drawMenu(c: Canvas) {
        menuBox()
        p.style = Paint.Style.FILL
        p.color = 0xF2161A22.toInt()
        c.drawRoundRect(box, 18 * d, 18 * d, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2 * d
        p.color = 0xFF2D9CFF.toInt()
        c.drawRoundRect(box, 18 * d, 18 * d, p)
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = true
        p.color = Color.WHITE
        p.textSize = 16 * d
        c.drawText("Tambah komponen", box.centerX(), box.top + 28 * d, p)
        p.isFakeBoldText = false
        p.color = 0xFF8B93A1.toInt()
        p.textSize = 11 * d
        c.drawText("Pilih komponen untuk ditaruh di layar", box.centerX(), box.top + 46 * d, p)
        for (i in 0 until 8) {
            cell(i)
            val on = i < 4
            p.style = Paint.Style.FILL
            p.color = if (on) 0xFF1F2430.toInt() else 0xFF14171E.toInt()
            c.drawRoundRect(cellR, 12 * d, 12 * d, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.5f * d
            p.color = if (on) 0xFF2D9CFF.toInt() else 0xFF2A2F3A.toInt()
            c.drawRoundRect(cellR, 12 * d, 12 * d, p)
            p.style = Paint.Style.FILL
            p.color = if (on) Color.WHITE else 0xFF5A6070.toInt()
            p.textSize = 24 * d
            c.drawText(icons[i], cellR.centerX(), cellR.top + 36 * d, p)
            p.textSize = 10 * d
            p.isFakeBoldText = true
            c.drawText(names[i], cellR.centerX(), cellR.top + 54 * d, p)
            p.isFakeBoldText = false
            if (!on) {
                p.textSize = 8 * d
                c.drawText("segera", cellR.centerX(), cellR.top + 66 * d, p)
            }
        }
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(0x77000000)
        val r = KeyMapStore.real(context)
        getLocationOnScreen(loc)
        for (k in ctl.comps()) drawComp(c, k, r)
        for (q in ctl.pending()) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3 * d
            p.color = Color.YELLOW
            c.drawCircle(q[0] * r[0] - loc[0], q[1] * r[1] - loc[1], 22 * d, p)
        }
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = true
        p.color = Color.WHITE
        p.textSize = 13 * d
        c.drawText("Editor Keymap", width / 2f, 70 * d, p)
        p.isFakeBoldText = false
        p.color = 0xFFFFE600.toInt()
        p.textSize = 12 * d
        c.drawText(ctl.prompt(), width / 2f, 90 * d, p)
        drawBtn(c, 0, "Selesai", 0xFF22E58B.toInt(), Color.BLACK)
        drawBtn(c, 1, "Hapus semua", 0xFFFF3B47.toInt(), Color.WHITE)
        drawBtn(c, 2, if (ctl.flowActive()) "Batal" else "Tambah", 0xFF2D9CFF.toInt(), Color.WHITE)
        if (menuOpen) drawMenu(c)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_UP) return true
        getLocationOnScreen(loc)
        val lx = e.rawX - loc[0]
        val ly = e.rawY - loc[1]
        if (menuOpen) {
            menuBox()
            for (i in 0 until 8) {
                cell(i)
                if (cellR.contains(lx, ly)) {
                    if (i < 4) {
                        menuOpen = false
                        ctl.onPick(i)
                    } else {
                        Toast.makeText(context, names[i] + ": segera hadir", Toast.LENGTH_SHORT).show()
                    }
                    invalidate()
                    return true
                }
            }
            if (!box.contains(lx, ly)) menuOpen = false
            invalidate()
            return true
        }
        for (i in 0..2) {
            btnRect(i)
            if (cellR.contains(lx, ly)) {
                when (i) {
                    0 -> ctl.onClose()
                    1 -> ctl.onClear()
                    else -> if (ctl.flowActive()) ctl.onCancel() else menuOpen = true
                }
                invalidate()
                return true
            }
        }
        val r = KeyMapStore.real(context)
        ctl.onTap(e.rawX / r[0], e.rawY / r[1])
        invalidate()
        return true
    }
}
