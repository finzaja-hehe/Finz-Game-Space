package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class CursorView(ctx: Context) : View(ctx) {

    var style = 0
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val col = when (style) {
            1 -> 0xFF22E58B.toInt()
            2 -> 0xFFFFE600.toInt()
            3 -> 0xFFFF3B47.toInt()
            4 -> 0xFF00E5FF.toInt()
            else -> Color.WHITE
        }
        if (style == 2 || style == 3) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = w / 8f
            p.color = col
            if (style == 2) {
                c.drawCircle(w / 2f, h / 2f, w * 0.38f, p)
            } else {
                c.drawLine(w * 0.1f, h / 2f, w * 0.9f, h / 2f, p)
                c.drawLine(w / 2f, h * 0.1f, w / 2f, h * 0.9f, p)
            }
            return
        }
        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(0f, h * 0.8f)
        path.lineTo(w * 0.28f, h * 0.6f)
        path.lineTo(w * 0.45f, h)
        path.lineTo(w * 0.6f, h * 0.92f)
        path.lineTo(w * 0.42f, h * 0.54f)
        path.lineTo(w * 0.75f, h * 0.5f)
        path.close()
        p.style = Paint.Style.FILL
        p.color = col
        c.drawPath(path, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f
        p.color = Color.BLACK
        c.drawPath(path, p)
    }
}

object MouseRunner {

    private const val LOOK_ID = 31
    private const val PICK_ID = 0
    private val ui = Handler(Looper.getMainLooper())
    private var app: Context? = null
    private var wm: WindowManager? = null
    private var list: List<KeyComp> = emptyList()
    private var on = false
    private val toggled = HashSet<Int>()
    private val held = HashSet<Int>()
    private var lookDown = false
    private var lx = 0f
    private var ly = 0f
    private var pickIdx = -1
    private var px = 0f
    private var py = 0f
    private var pickDown = false
    private var cursor: CursorView? = null
    private var cursorLp: WindowManager.LayoutParams? = null
    private var cursorOff = 0
    private var tapN = 0
    private val idle = Runnable { endLook() }

    private fun needMouse(l: List<KeyComp>): Boolean {
        for (k in l) {
            if (k.type >= KeyComp.SCROLL) return true
            if (k.keys.any { it >= 1001 }) return true
        }
        return false
    }

    fun sync(ctx: Context, l: List<KeyComp>, enabled: Boolean) {
        val c = ctx.applicationContext
        app = c
        wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        list = ArrayList(l)
        on = enabled
        if (!on) {
            reset()
        } else {
            toggled.removeAll { it >= list.size || list[it].type != KeyComp.FPSV }
            if (pickIdx >= list.size || (pickIdx >= 0 && list[pickIdx].type != KeyComp.PICK)) {
                hidePick()
            }
        }
        InputBackend.grab(on && needMouse(list))
    }

    private fun reset() {
        endLook()
        hidePick()
        toggled.clear()
        held.clear()
    }

    fun key(code: Int, isDown: Boolean) {
        if (!on) return
        if (isDown) held.add(code) else held.remove(code)
        for (i in list.indices) {
            val k = list[i]
            when (k.type) {
                KeyComp.FPSV ->
                    if (isDown && k.keys[0] != 0 && k.keys[0] == code) {
                        if (!toggled.add(i)) toggled.remove(i)
                        endLook()
                    }
                KeyComp.FREE ->
                    if (!isDown && k.keys[0] != 0 && k.keys[0] == code) endLook()
                KeyComp.PICK ->
                    if (isDown && k.keys[0] != 0 && k.keys[0] == code) {
                        if (pickIdx == i) hidePick() else showPick(i, k)
                    }
                else -> {}
            }
        }
        if (code == 1001 && pickIdx >= 0) pickClick(isDown)
    }

    private fun activeLook(): KeyComp? {
        if (pickIdx >= 0) return null
        for (i in list.indices) {
            val k = list[i]
            if (k.type == KeyComp.FPSV && (k.keys[0] == 0 || toggled.contains(i))) return k
            if (k.type == KeyComp.FREE && k.keys[0] != 0 && held.contains(k.keys[0])) return k
        }
        return null
    }

    fun rel(dx: Int, dy: Int) {
        ui.post { handleRel(dx, dy) }
    }

    private fun handleRel(dx: Int, dy: Int) {
        if (!on) return
        val c = app ?: return
        val s = KeyMapStore.getInt(c, "msens", 5) / 5f
        if (pickIdx >= 0) {
            movePick(c, dx * s, dy * s)
            return
        }
        val k = activeLook() ?: return
        look(c, k, dx * s, dy * s)
    }

    private fun look(c: Context, k: KeyComp, dx: Float, dy: Float) {
        val r = KeyMapStore.real(c)
        val cx = k.pts[0] * r[0]
        val cy = k.pts[1] * r[1]
        val rad = max(k.pts[2] * min(r[0], r[1]), 20f)
        if (!lookDown) {
            lx = cx
            ly = cy
            InputBackend.down(LOOK_ID, lx, ly)
            lookDown = true
        }
        lx += dx
        ly += dy
        if (hypot(lx - cx, ly - cy) > rad) {
            InputBackend.up(LOOK_ID)
            lx = cx
            ly = cy
            InputBackend.down(LOOK_ID, lx, ly)
        } else {
            InputBackend.move(LOOK_ID, lx, ly)
        }
        ui.removeCallbacks(idle)
        ui.postDelayed(idle, 150)
    }

    private fun endLook() {
        ui.removeCallbacks(idle)
        if (lookDown) {
            InputBackend.up(LOOK_ID)
            lookDown = false
        }
    }

    fun wheel(v: Int) {
        ui.post { handleWheel(v) }
    }

    private fun handleWheel(v: Int) {
        if (!on || v == 0) return
        val c = app ?: return
        val r = KeyMapStore.real(c)
        for (k in list) {
            if (k.type != KeyComp.SCROLL) continue
            val o = if (v > 0) 0 else 2
            val id = 29 + (tapN++ % 2)
            InputBackend.down(id, k.pts[o] * r[0], k.pts[o + 1] * r[1])
            ui.postDelayed({ InputBackend.up(id) }, 40)
        }
    }

    private fun showPick(i: Int, k: KeyComp) {
        val c = app ?: return
        val w = wm ?: return
        hidePick()
        endLook()
        val r = KeyMapStore.real(c)
        px = k.pts[0] * r[0]
        py = k.pts[1] * r[1]
        pickIdx = i
        val v = CursorView(c)
        val style = KeyMapStore.getInt(c, "mstyle", 0)
        v.style = style
        val size = (KeyMapStore.getInt(c, "msize", 20) * c.resources.displayMetrics.density).toInt()
        cursorOff = if (style == 2 || style == 3) size / 2 else 0
        val p = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.START
        p.x = px.toInt() - cursorOff
        p.y = py.toInt() - cursorOff
        try {
            w.addView(v, p)
            cursor = v
            cursorLp = p
        } catch (e: Exception) {
        }
    }

    private fun hidePick() {
        if (pickDown) {
            InputBackend.up(PICK_ID)
            pickDown = false
        }
        cursor?.let { try { wm?.removeView(it) } catch (e: Exception) {} }
        cursor = null
        cursorLp = null
        pickIdx = -1
    }

    private fun movePick(c: Context, dx: Float, dy: Float) {
        val r = KeyMapStore.real(c)
        px = (px + dx).coerceIn(0f, (r[0] - 1).toFloat())
        py = (py + dy).coerceIn(0f, (r[1] - 1).toFloat())
        val v = cursor
        val p = cursorLp
        if (v != null && p != null) {
            p.x = px.toInt() - cursorOff
            p.y = py.toInt() - cursorOff
            try { wm?.updateViewLayout(v, p) } catch (e: Exception) {}
        }
        if (pickDown) InputBackend.move(PICK_ID, px, py)
    }

    private fun pickClick(isDown: Boolean) {
        if (isDown && !pickDown) {
            InputBackend.down(PICK_ID, px, py)
            pickDown = true
        } else if (!isDown && pickDown) {
            InputBackend.up(PICK_ID)
            pickDown = false
        }
    }
}
