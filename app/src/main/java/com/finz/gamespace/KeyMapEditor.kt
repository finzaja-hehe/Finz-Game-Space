package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

interface EditorCtl {
    fun comps(): List<KeyComp>
    fun prompt(): String
    fun pending(): List<FloatArray>
    fun flowActive(): Boolean
    fun dirty(): Boolean
    fun onPick(type: Int)
    fun onTap(x: Float, y: Float)
    fun onCancel()
    fun onClear()
    fun onExit()
    fun onSave()
    fun onShare()
    fun onImport()
}

class KeyMapEditor(ctx: Context, private val ctl: EditorCtl) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val box = RectF()
    private val bar = RectF()
    private val cellR = RectF()
    private val vp = RectF()
    private val loc = IntArray(2)
    private val d = resources.displayMetrics.density
    private var menuOpen = false
    private var scroll = 0f
    private var downY = 0f
    private var lastY = 0f
    private var moved = false
    private val names = arrayOf(
        "Sentuh", "Tahan", "Geser", "Joystick", "Grup tekan lama", "Bola mengambang",
        "Gulir Mouse", "Tampilan FPS", "Lihat Bebas", "Pilih Mouse"
    )
    private val kinds = intArrayOf(0, 1, 2, 3, 4, 5, -1, -1, -1, -1)
    private val colors = intArrayOf(
        0xCC00E5A8.toInt(), 0xCCFF7A1A.toInt(), 0xCC2D9CFF.toInt(),
        0xCCB46BFF.toInt(), 0xCCFF4FD8.toInt(), 0xCCFFFFFF.toInt()
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

    private fun ring(c: Canvas, x: Float, y: Float, rad: Float, color: Int) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f * d
        p.color = color
        c.drawCircle(x, y, rad, p)
    }

    private fun arrow(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, col: Int) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3 * d
        p.color = col
        c.drawLine(x1, y1, x2, y2, p)
        val a = atan2(y2 - y1, x2 - x1)
        val l = 14 * d
        c.drawLine(x2, y2, x2 - l * cos(a - 0.5f), y2 - l * sin(a - 0.5f), p)
        c.drawLine(x2, y2, x2 - l * cos(a + 0.5f), y2 - l * sin(a + 0.5f), p)
    }

    private fun drawComp(c: Canvas, k: KeyComp, r: IntArray) {
        val col = colors[k.type]
        val x = k.pts[0] * r[0] - loc[0]
        val y = k.pts[1] * r[1] - loc[1]
        when (k.type) {
            KeyComp.HOLD -> {
                dot(c, x, y, 20 * d, col, lab(k.keys[0]))
                ring(c, x, y, 28 * d, col)
            }
            KeyComp.SWIPE -> {
                val x2 = k.pts[2] * r[0] - loc[0]
                val y2 = k.pts[3] * r[1] - loc[1]
                arrow(c, x, y, x2, y2, col)
                dot(c, x, y, 20 * d, col, lab(k.keys[0]))
            }
            KeyComp.JOY -> {
                val rad = k.pts[2] * min(r[0], r[1])
                ring(c, x, y, rad, col)
                dot(c, x, y, 5 * d, col, "")
                dot(c, x, y - rad, 15 * d, col, lab(k.keys[0]))
                dot(c, x - rad, y, 15 * d, col, lab(k.keys[1]))
                dot(c, x, y + rad, 15 * d, col, lab(k.keys[2]))
                dot(c, x + rad, y, 15 * d, col, lab(k.keys[3]))
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
                    val nx = k.pts[i] * r[0] - loc[0]
                    val ny = k.pts[i + 1] * r[1] - loc[1]
                    c.drawLine(px, py, nx, ny, p)
                    px = nx
                    py = ny
                    i += 2
                }
                p.pathEffect = null
                var j = 0
                while (j + 1 < k.pts.size) {
                    dot(
                        c, k.pts[j] * r[0] - loc[0], k.pts[j + 1] * r[1] - loc[1], 14 * d, col,
                        if (j == 0) lab(k.keys[0]) else ""
                    )
                    j += 2
                }
            }
            KeyComp.BALL -> {
                ring(c, x, y, 24 * d, col)
                p.style = Paint.Style.FILL
                p.color = Color.WHITE
                p.textAlign = Paint.Align.CENTER
                p.isFakeBoldText = true
                p.textSize = 16 * d
                c.drawText("F", x, y + 6 * d, p)
            }
            else -> dot(c, x, y, 22 * d, col, lab(k.keys[0]))
        }
    }

    private fun barBox() {
        val w = min(width - 24 * d, 400 * d)
        val l = (width - w) / 2f
        bar.set(l, 8 * d, l + w, 100 * d)
    }

    private fun barHit(x: Float, y: Float): Int {
        barBox()
        if (!bar.contains(x, y)) return -2
        val rowH = bar.height() / 2f
        if (y < bar.top + rowH) {
            return ((x - bar.left) / (bar.width() / 3f)).toInt().coerceIn(0, 2)
        }
        return if (x < bar.centerX()) 3 else 4
    }

    private fun drawBar(c: Canvas) {
        barBox()
        p.style = Paint.Style.FILL
        p.color = 0xF05B67F0.toInt()
        c.drawRoundRect(bar, 16 * d, 16 * d, p)
        val rowH = bar.height() / 2f
        val third = bar.width() / 3f
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * d
        p.color = 0xCCFFFFFF.toInt()
        p.pathEffect = DashPathEffect(floatArrayOf(4 * d, 3 * d), 0f)
        c.drawLine(bar.left + 8 * d, bar.top + rowH, bar.right - 8 * d, bar.top + rowH, p)
        c.drawLine(bar.left + third, bar.top + 6 * d, bar.left + third, bar.top + rowH - 6 * d, p)
        c.drawLine(bar.left + 2 * third, bar.top + 6 * d, bar.left + 2 * third,
            bar.top + rowH - 6 * d, p)
        c.drawLine(bar.centerX(), bar.top + rowH + 6 * d, bar.centerX(), bar.bottom - 6 * d, p)
        p.pathEffect = null
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = false
        p.textSize = 13 * d
        val y1 = bar.top + rowH / 2f + 5 * d
        val y2 = bar.top + rowH * 1.5f + 5 * d
        c.drawText("Keluar", bar.left + third / 2f, y1, p)
        c.drawText(if (ctl.flowActive()) "Batal" else "Tambah komponen",
            bar.left + third * 1.5f, y1, p)
        c.drawText(if (ctl.dirty()) "Simpan *" else "Simpan", bar.left + third * 2.5f, y1, p)
        c.drawText("Bagikan", bar.left + bar.width() / 4f, y2, p)
        c.drawText("Impor", bar.left + bar.width() * 3f / 4f, y2, p)
    }

    private fun menuBox() {
        val bw = min(width - 32 * d, 480 * d)
        val bh = min(height - 24 * d, 300 * d)
        box.set((width - bw) / 2f, (height - bh) / 2f, (width + bw) / 2f, (height + bh) / 2f)
        vp.set(box.left + 6 * d, box.top + 58 * d, box.right - 14 * d, box.bottom - 10 * d)
    }

    private fun gap() = 10 * d

    private fun cellW() = (vp.width() - 5 * gap()) / 4f

    private fun cellH() = 82 * d

    private fun contentH(): Float {
        val rows = (names.size + 3) / 4
        return rows * (cellH() + gap()) + gap()
    }

    private fun maxScroll() = max(0f, contentH() - vp.height())

    private fun cell(i: Int) {
        val col = i % 4
        val row = i / 4
        val l = vp.left + gap() + col * (cellW() + gap())
        val t = vp.top + gap() + row * (cellH() + gap()) - scroll
        cellR.set(l, t, l + cellW(), t + cellH())
    }

    private fun drawMenu(c: Canvas) {
        menuBox()
        scroll = scroll.coerceIn(0f, maxScroll())
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
        c.drawText("Pilih komponen untuk ditempatkan pada keymap", box.centerX(),
            box.top + 46 * d, p)
        c.save()
        c.clipRect(vp)
        for (i in names.indices) {
            cell(i)
            val on = kinds[i] >= 0
            p.style = Paint.Style.FILL
            p.color = if (on) 0xFF1F2430.toInt() else 0xFF14171E.toInt()
            c.drawRoundRect(cellR, 12 * d, 12 * d, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.5f * d
            p.color = if (on) 0xFF2D9CFF.toInt() else 0xFF2A2F3A.toInt()
            c.drawRoundRect(cellR, 12 * d, 12 * d, p)
            MenuIcons.draw(
                c, p, i, cellR.centerX(), cellR.top + 30 * d, 34 * d,
                if (on) Color.WHITE else 0xFF5A6070.toInt(), d
            )
            p.style = Paint.Style.FILL
            p.color = if (on) Color.WHITE else 0xFF5A6070.toInt()
            p.textAlign = Paint.Align.CENTER
            p.isFakeBoldText = true
            p.textSize = 10 * d
            while (p.measureText(names[i]) > cellR.width() - 6 * d && p.textSize > 6 * d) {
                p.textSize = p.textSize - 0.5f * d
            }
            c.drawText(names[i], cellR.centerX(), cellR.top + 62 * d, p)
            p.isFakeBoldText = false
            if (!on) {
                p.textSize = 8 * d
                c.drawText("segera", cellR.centerX(), cellR.top + 74 * d, p)
            }
        }
        c.restore()
        val ms = maxScroll()
        if (ms > 0f) {
            val th = vp.height() * vp.height() / contentH()
            val ty = vp.top + (vp.height() - th) * (scroll / ms)
            p.style = Paint.Style.FILL
            p.color = 0x992D9CFF.toInt()
            c.drawRoundRect(vp.right + 4 * d, ty, vp.right + 7 * d, ty + th, 3 * d, 3 * d, p)
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
        drawBar(c)
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = false
        p.color = 0xFFFFE600.toInt()
        p.textSize = 12 * d
        c.drawText(ctl.prompt(), width / 2f, bar.bottom + 22 * d, p)
        if (menuOpen) drawMenu(c)
    }

    private fun menuTap(lx: Float, ly: Float) {
        menuBox()
        if (vp.contains(lx, ly)) {
            for (i in names.indices) {
                cell(i)
                if (cellR.contains(lx, ly)) {
                    if (kinds[i] >= 0) {
                        menuOpen = false
                        ctl.onPick(kinds[i])
                    } else {
                        Toast.makeText(context, names[i] + ": tahap mouse (segera)",
                            Toast.LENGTH_SHORT).show()
                    }
                    return
                }
            }
            return
        }
        if (!box.contains(lx, ly)) menuOpen = false
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        getLocationOnScreen(loc)
        val lx = e.rawX - loc[0]
        val ly = e.rawY - loc[1]
        if (menuOpen) {
            menuBox()
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downY = ly
                    lastY = ly
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (abs(ly - downY) > 8 * d) moved = true
                    if (moved) {
                        scroll = (scroll - (ly - lastY)).coerceIn(0f, maxScroll())
                        invalidate()
                    }
                    lastY = ly
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) menuTap(lx, ly)
                    invalidate()
                }
            }
            return true
        }
        if (e.action != MotionEvent.ACTION_UP) return true
        val h = barHit(lx, ly)
        if (h != -2) {
            when (h) {
                0 -> ctl.onExit()
                1 -> if (ctl.flowActive()) ctl.onCancel() else {
                    menuOpen = true
                    scroll = 0f
                }
                2 -> ctl.onSave()
                3 -> ctl.onShare()
                else -> ctl.onImport()
            }
            invalidate()
            return true
        }
        val r = KeyMapStore.real(context)
        ctl.onTap(e.rawX / r[0], e.rawY / r[1])
        invalidate()
        return true
    }
}
