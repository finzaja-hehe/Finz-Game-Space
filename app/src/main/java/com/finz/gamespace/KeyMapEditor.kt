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
    fun selected(): Int
    fun slotName(): String
    fun canFinish(): Boolean
    fun onPick(type: Int)
    fun onTap(x: Float, y: Float)
    fun onCancel()
    fun onClear()
    fun onExit()
    fun onSave()
    fun onShare()
    fun onImport()
    fun onFinish()
    fun onDelete()
    fun onDeselect()
    fun onPickKey(): Boolean
    fun onAssign(code: Int)
}

class KeyMapEditor(ctx: Context, private val ctl: EditorCtl) : View(ctx) {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val box = RectF()
    private val bar = RectF()
    private val cellR = RectF()
    private val vp = RectF()
    private val actR = RectF()
    private val loc = IntArray(2)
    private val d = resources.displayMetrics.density
    private var menuOpen = false
    private var pickerOpen = false
    private var scroll = 0f
    private var downY = 0f
    private var lastY = 0f
    private var moved = false
    private val names = arrayOf(
        "Sentuh", "Geser", "Gulir Mouse", "Joystick", "Tampilan FPS", "Lihat Bebas",
        "Pilih Mouse", "Grup tekan lama", "Tampilan bola mengambang", "Tahan"
    )
    private val kinds = intArrayOf(0, 2, -1, 3, -1, -1, -1, 4, 5, 1)
    private val iconIds = intArrayOf(0, 2, 6, 3, 7, 8, 9, 4, 5, 1)
    private val colors = intArrayOf(
        0xCC00E5A8.toInt(), 0xCCFF7A1A.toInt(), 0xCC2D9CFF.toInt(),
        0xCCB46BFF.toInt(), 0xCCFF4FD8.toInt(), 0xCCFFFFFF.toInt()
    )
    private val kc = ArrayList<Int>()
    private val kn = ArrayList<String>()
    private val help =
        "Cara: Tambah komponen > ketuk titik di layar > ketuk komponen > Pilih tombol " +
            "(atau tekan tombol keyboard). Esc/F10 = keluar."

    private fun addKey(code: Int, name: String) {
        kc.add(code)
        kn.add(name)
    }

    init {
        for (i in 0 until 26) addKey(KeyEvent.KEYCODE_A + i, ('A' + i).toString())
        for (i in 0..9) addKey(KeyEvent.KEYCODE_0 + i, i.toString())
        addKey(KeyEvent.KEYCODE_SPACE, "Space")
        addKey(KeyEvent.KEYCODE_SHIFT_LEFT, "Shift")
        addKey(KeyEvent.KEYCODE_CTRL_LEFT, "Ctrl")
        addKey(KeyEvent.KEYCODE_ALT_LEFT, "Alt")
        addKey(KeyEvent.KEYCODE_TAB, "Tab")
        addKey(KeyEvent.KEYCODE_ENTER, "Enter")
        addKey(KeyEvent.KEYCODE_DEL, "Bksp")
        addKey(KeyEvent.KEYCODE_DPAD_UP, "Atas")
        addKey(KeyEvent.KEYCODE_DPAD_LEFT, "Kiri")
        addKey(KeyEvent.KEYCODE_DPAD_DOWN, "Bawah")
        addKey(KeyEvent.KEYCODE_DPAD_RIGHT, "Kanan")
        for (i in 1..8) addKey(KeyEvent.KEYCODE_F1 + i - 1, "F$i")
        addKey(KeyEvent.KEYCODE_F11, "F11")
        addKey(KeyEvent.KEYCODE_F12, "F12")
        addKey(KeyEvent.KEYCODE_MINUS, "-")
        addKey(KeyEvent.KEYCODE_EQUALS, "=")
        addKey(KeyEvent.KEYCODE_LEFT_BRACKET, "[")
        addKey(KeyEvent.KEYCODE_RIGHT_BRACKET, "]")
        addKey(KeyEvent.KEYCODE_BACKSLASH, "\\")
        addKey(KeyEvent.KEYCODE_SEMICOLON, ";")
        addKey(KeyEvent.KEYCODE_APOSTROPHE, "'")
        addKey(KeyEvent.KEYCODE_COMMA, ",")
        addKey(KeyEvent.KEYCODE_PERIOD, ".")
        addKey(KeyEvent.KEYCODE_SLASH, "/")
        addKey(KeyEvent.KEYCODE_GRAVE, "`")
    }

    private fun lab(code: Int): String =
        if (code == 0) "?" else KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")

    private fun fit(c: Canvas, s: String, cx: Float, y: Float, size: Float, maxW: Float) {
        p.textSize = size
        while (p.measureText(s) > maxW && p.textSize > 7 * d) p.textSize = p.textSize - 0.5f * d
        c.drawText(s, cx, y, p)
    }

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

    private fun highlight(c: Canvas, k: KeyComp, r: IntArray) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3 * d
        p.color = Color.YELLOW
        if (k.type == KeyComp.JOY) {
            val x = k.pts[0] * r[0] - loc[0]
            val y = k.pts[1] * r[1] - loc[1]
            c.drawCircle(x, y, k.pts[2] * min(r[0], r[1]) + 24 * d, p)
            return
        }
        val n = if (k.type == KeyComp.SWIPE || k.type == KeyComp.GROUP) k.pts.size else 2
        var j = 0
        while (j + 1 < n) {
            c.drawCircle(k.pts[j] * r[0] - loc[0], k.pts[j + 1] * r[1] - loc[1], 34 * d, p)
            j += 2
        }
    }

    // ---------- bar atas ----------
    private fun barBox() {
        val w = min(width - 24 * d, 420 * d)
        val l = (width - w) / 2f
        bar.set(l, 8 * d, l + w, 104 * d)
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
        p.color = 0xE5666BEA.toInt()
        c.drawRoundRect(bar, 18 * d, 18 * d, p)
        val rowH = bar.height() / 2f
        val third = bar.width() / 3f
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * d
        p.color = 0xDDFFFFFF.toInt()
        p.pathEffect = DashPathEffect(floatArrayOf(4 * d, 3 * d), 0f)
        c.drawLine(bar.left + 8 * d, bar.top + rowH, bar.right - 8 * d, bar.top + rowH, p)
        c.drawLine(bar.left + third, bar.top + 8 * d, bar.left + third, bar.top + rowH - 8 * d, p)
        c.drawLine(bar.left + 2 * third, bar.top + 8 * d, bar.left + 2 * third,
            bar.top + rowH - 8 * d, p)
        c.drawLine(bar.centerX(), bar.top + rowH + 8 * d, bar.centerX(), bar.bottom - 8 * d, p)
        p.pathEffect = null
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = false
        p.textSize = 14 * d
        val y1 = bar.top + rowH / 2f + 5 * d
        val y2 = bar.top + rowH * 1.5f + 5 * d
        c.drawText("Keluar", bar.left + third / 2f, y1, p)
        c.drawText(if (ctl.flowActive()) "Batal" else "Tambah komponen",
            bar.left + third * 1.5f, y1 + 3 * d, p)
        c.drawText(if (ctl.dirty()) "Simpan *" else "Simpan", bar.left + third * 2.5f, y1, p)
        c.drawText("Bagikan", bar.left + bar.width() / 4f, y2, p)
        c.drawText("Impor", bar.left + bar.width() * 3f / 4f, y2, p)
        p.color = 0xFF4F57D8.toInt()
        c.drawCircle(bar.centerX(), -6 * d, 22 * d, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3 * d
        p.color = 0xFFA8AEFF.toInt()
        c.drawCircle(bar.centerX(), -6 * d, 22 * d, p)
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        p.isFakeBoldText = true
        p.textSize = 15 * d
        c.drawText("F", bar.centerX(), 11 * d, p)
    }

    // ---------- bar aksi bawah ----------
    private fun actLabels(): List<String> {
        if (ctl.canFinish()) return listOf("Selesai titik")
        if (ctl.selected() >= 0) return listOf("Pilih tombol", "Hapus", "Tutup")
        return emptyList()
    }

    private fun actRect(i: Int, n: Int) {
        val w = 112 * d
        val g = 8 * d
        val total = n * w + (n - 1) * g
        val l = (width - total) / 2f + i * (w + g)
        actR.set(l, height - 62 * d, l + w, height - 18 * d)
    }

    private fun drawActions(c: Canvas) {
        val labs = actLabels()
        for (i in labs.indices) {
            actRect(i, labs.size)
            val s = labs[i]
            p.style = Paint.Style.FILL
            p.color = when (s) {
                "Selesai titik" -> 0xFF22E58B.toInt()
                "Pilih tombol" -> 0xFF5B67F0.toInt()
                "Hapus" -> 0xFFFF3B47.toInt()
                else -> 0xFF2A2F66.toInt()
            }
            c.drawRoundRect(actR, 12 * d, 12 * d, p)
            p.color = if (s == "Selesai titik") Color.BLACK else Color.WHITE
            p.textAlign = Paint.Align.CENTER
            p.isFakeBoldText = true
            fit(c, s, actR.centerX(), actR.centerY() + 5 * d, 13 * d, actR.width() - 8 * d)
        }
        p.isFakeBoldText = false
    }

    private fun actionTap(s: String) {
        when (s) {
            "Selesai titik" -> ctl.onFinish()
            "Pilih tombol" -> if (ctl.onPickKey()) {
                pickerOpen = true
                scroll = 0f
            }
            "Hapus" -> ctl.onDelete()
            else -> ctl.onDeselect()
        }
    }

    // ---------- dialog ----------
    private fun menuBox() {
        val bw = min(width - 32 * d, 500 * d)
        val bh = min(height - 24 * d, 310 * d)
        box.set((width - bw) / 2f, (height - bh) / 2f, (width + bw) / 2f, (height + bh) / 2f)
        vp.set(box.left + 6 * d, box.top + 66 * d, box.right - 16 * d, box.bottom - 14 * d)
    }

    private fun gap() = 10 * d

    private fun cellW() = (vp.width() - 5 * gap()) / 4f

    private fun cellH() = 88 * d

    private fun menuContentH(): Float {
        val rows = (names.size + 3) / 4
        return rows * (cellH() + gap()) + gap()
    }

    private fun pickContentH(): Float {
        val rows = (kc.size + 7) / 8
        return rows * (40 * d + 6 * d) + 6 * d
    }

    private fun contentH(): Float = if (pickerOpen) pickContentH() else menuContentH()

    private fun maxScroll() = max(0f, contentH() - vp.height())

    private fun cell(i: Int) {
        val col = i % 4
        val row = i / 4
        val l = vp.left + gap() + col * (cellW() + gap())
        val t = vp.top + gap() + row * (cellH() + gap()) - scroll
        cellR.set(l, t, l + cellW(), t + cellH())
    }

    private fun pcell(i: Int) {
        val g = 6 * d
        val cw = (vp.width() - 9 * g) / 8f
        val ch = 40 * d
        val col = i % 8
        val row = i / 8
        val l = vp.left + g + col * (cw + g)
        val t = vp.top + g + row * (ch + g) - scroll
        cellR.set(l, t, l + cw, t + ch)
    }

    private fun glow(c: Canvas, r: RectF, rad: Float, color: Int, k: Float) {
        p.style = Paint.Style.STROKE
        val rgb = color and 0xFFFFFF
        p.strokeWidth = 9 * d * k
        p.color = (0x22 shl 24) or rgb
        c.drawRoundRect(r, rad, rad, p)
        p.strokeWidth = 5 * d * k
        p.color = (0x44 shl 24) or rgb
        c.drawRoundRect(r, rad, rad, p)
        p.strokeWidth = 2 * d * k
        p.color = color
        c.drawRoundRect(r, rad, rad, p)
    }

    private fun corner(c: Canvas, x: Float, y: Float, sx: Int, sy: Int, len: Float) {
        c.drawLine(x, y, x + sx * len, y, p)
        c.drawLine(x, y, x, y + sy * len, p)
    }

    private fun brackets(c: Canvas, r: RectF) {
        val len = 28 * d
        val o = 4 * d
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4 * d
        p.color = 0xFF4D7CFF.toInt()
        p.strokeCap = Paint.Cap.SQUARE
        corner(c, r.left + o, r.top + o, 1, 1, len)
        corner(c, r.right - o, r.top + o, -1, 1, len)
        corner(c, r.left + o, r.bottom - o, 1, -1, len)
        corner(c, r.right - o, r.bottom - o, -1, -1, len)
        p.strokeCap = Paint.Cap.BUTT
    }

    private fun label2(c: Canvas, s: String, cx: Float, y: Float, maxW: Float) {
        if (p.measureText(s) <= maxW) {
            c.drawText(s, cx, y, p)
            return
        }
        var sp = s.indexOf(' ', max(0, s.length / 2 - 3))
        if (sp < 0) sp = s.lastIndexOf(' ')
        if (sp < 0) {
            c.drawText(s, cx, y, p)
            return
        }
        c.drawText(s.substring(0, sp), cx, y - 5 * d, p)
        c.drawText(s.substring(sp + 1), cx, y + 7 * d, p)
    }

    private fun dialogFrame(c: Canvas, title: String, sub: String) {
        menuBox()
        scroll = scroll.coerceIn(0f, maxScroll())
        p.style = Paint.Style.FILL
        p.color = 0xF20B0E3A.toInt()
        c.drawRoundRect(box, 8 * d, 8 * d, p)
        glow(c, box, 8 * d, 0xFF2F5BFF.toInt(), 1f)
        brackets(c, box)
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = true
        p.color = Color.WHITE
        p.textSize = 20 * d
        c.drawText(title, box.centerX(), box.top + 32 * d, p)
        p.isFakeBoldText = false
        fit(c, sub, box.centerX(), box.top + 54 * d, 13 * d, box.width() - 40 * d)
    }

    private fun scrollbar(c: Canvas) {
        val ms = maxScroll()
        if (ms > 0f) {
            val th = vp.height() * vp.height() / contentH()
            val ty = vp.top + (vp.height() - th) * (scroll / ms)
            p.style = Paint.Style.FILL
            p.color = 0xAA3E5BFF.toInt()
            c.drawRoundRect(vp.right + 5 * d, ty, vp.right + 8 * d, ty + th, 3 * d, 3 * d, p)
        }
    }

    private fun drawMenu(c: Canvas) {
        dialogFrame(c, "Tambah komponen", "Pilih komponen untuk ditempatkan pada keymap")
        c.save()
        c.clipRect(vp)
        for (i in names.indices) {
            cell(i)
            val on = kinds[i] >= 0
            p.style = Paint.Style.FILL
            p.color = if (on) 0xFF111552.toInt() else 0xFF0E1140.toInt()
            c.drawRoundRect(cellR, 8 * d, 8 * d, p)
            glow(c, cellR, 8 * d, if (on) 0xFF3F5BFF.toInt() else 0xFF2A2F66.toInt(), 0.6f)
            MenuIcons.draw(
                c, p, iconIds[i], cellR.centerX(), cellR.top + 32 * d, 36 * d,
                if (on) Color.WHITE else 0xFF5A6070.toInt(), d
            )
            p.style = Paint.Style.FILL
            p.color = if (on) Color.WHITE else 0xFF5A6070.toInt()
            p.textAlign = Paint.Align.CENTER
            p.isFakeBoldText = false
            p.textSize = 11 * d
            label2(c, names[i], cellR.centerX(), cellR.top + 68 * d, cellR.width() - 6 * d)
            if (!on) {
                p.textSize = 8 * d
                c.drawText("segera", cellR.centerX(), cellR.top + 82 * d, p)
            }
        }
        c.restore()
        scrollbar(c)
    }

    private fun drawPicker(c: Canvas) {
        val sn = ctl.slotName()
        val sub = if (sn.isNotEmpty()) "Pilih tombol untuk arah $sn"
        else "Ketuk tombol untuk dipasang. Ketuk di luar kotak untuk menutup."
        dialogFrame(c, "Pilih tombol", sub)
        c.save()
        c.clipRect(vp)
        for (i in kc.indices) {
            pcell(i)
            p.style = Paint.Style.FILL
            p.color = 0xFF111552.toInt()
            c.drawRoundRect(cellR, 8 * d, 8 * d, p)
            glow(c, cellR, 8 * d, 0xFF3F5BFF.toInt(), 0.5f)
            p.style = Paint.Style.FILL
            p.color = Color.WHITE
            p.textAlign = Paint.Align.CENTER
            p.isFakeBoldText = true
            fit(c, kn[i], cellR.centerX(), cellR.centerY() + 4 * d, 12 * d, cellR.width() - 4 * d)
        }
        p.isFakeBoldText = false
        c.restore()
        scrollbar(c)
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(0x77000000)
        val r = KeyMapStore.real(context)
        getLocationOnScreen(loc)
        val list = ctl.comps()
        val sel = ctl.selected()
        for (i in list.indices) {
            drawComp(c, list[i], r)
            if (i == sel) highlight(c, list[i], r)
        }
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
        fit(c, ctl.prompt(), width / 2f, bar.bottom + 22 * d, 12 * d, width - 24 * d)
        p.color = 0xFFB8BDFF.toInt()
        fit(c, help, width / 2f, bar.bottom + 40 * d, 10 * d, width - 24 * d)
        drawActions(c)
        if (menuOpen) drawMenu(c)
        if (pickerOpen) drawPicker(c)
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

    private fun pickerTap(lx: Float, ly: Float) {
        menuBox()
        if (vp.contains(lx, ly)) {
            for (i in kc.indices) {
                pcell(i)
                if (cellR.contains(lx, ly)) {
                    ctl.onAssign(kc[i])
                    val sn = ctl.slotName()
                    if (sn.isEmpty() || sn == "ATAS") pickerOpen = false
                    return
                }
            }
            return
        }
        if (!box.contains(lx, ly)) pickerOpen = false
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        getLocationOnScreen(loc)
        val lx = e.rawX - loc[0]
        val ly = e.rawY - loc[1]
        if (menuOpen || pickerOpen) {
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
                    if (!moved) {
                        if (pickerOpen) pickerTap(lx, ly) else menuTap(lx, ly)
                    }
                    invalidate()
                }
            }
            return true
        }
        if (e.action != MotionEvent.ACTION_UP) return true
        val labs = actLabels()
        for (i in labs.indices) {
            actRect(i, labs.size)
            if (actR.contains(lx, ly)) {
                actionTap(labs[i])
                invalidate()
                return true
            }
        }
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
