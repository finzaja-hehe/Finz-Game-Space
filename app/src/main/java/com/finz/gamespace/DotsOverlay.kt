package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager

class DotsView(ctx: Context) : View(ctx) {

    val pts = HashMap<Int, FloatArray>()
    var radius = 18f
    var alphaPct = 80
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val loc = IntArray(2)

    override fun onDraw(c: Canvas) {
        getLocationOnScreen(loc)
        val a = (alphaPct * 255 / 100).coerceIn(0, 255)
        for (v in pts.values) {
            val x = v[0] - loc[0]
            val y = v[1] - loc[1]
            p.style = Paint.Style.FILL
            p.color = 0xFFFFFFFF.toInt()
            p.alpha = a / 2
            c.drawCircle(x, y, radius, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = radius / 5f
            p.color = 0xFF00E5A8.toInt()
            p.alpha = a
            c.drawCircle(x, y, radius, p)
        }
    }
}

object DotsOverlay {

    private var app: Context? = null
    private var wm: WindowManager? = null
    private var view: DotsView? = null

    fun attach(ctx: Context) {
        val c = ctx.applicationContext
        app = c
        wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        refresh()
    }

    fun refresh() {
        val c = app ?: return
        val on = KeyMapStore.getBool(c, "dots", false)
        if (on && view == null) add(c)
        if (!on) remove()
        val v = view ?: return
        v.radius = KeyMapStore.getInt(c, "dotsize", 18) * c.resources.displayMetrics.density
        v.alphaPct = KeyMapStore.getInt(c, "dotalpha", 80)
        v.invalidate()
    }

    private fun add(c: Context) {
        val w = wm ?: return
        val v = DotsView(c)
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= 28) {
            p.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        try {
            w.addView(v, p)
            view = v
        } catch (e: Exception) {
        }
    }

    private fun remove() {
        view?.let { try { wm?.removeView(it) } catch (e: Exception) {} }
        view = null
    }

    fun down(id: Int, x: Float, y: Float) {
        val v = view ?: return
        v.pts[id] = floatArrayOf(x, y)
        v.invalidate()
    }

    fun move(id: Int, x: Float, y: Float) {
        val v = view ?: return
        v.pts[id] = floatArrayOf(x, y)
        v.invalidate()
    }

    fun up(id: Int) {
        val v = view ?: return
        v.pts.remove(id)
        v.invalidate()
    }
}
