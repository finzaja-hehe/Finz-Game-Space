package com.finz.gamespace

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast

object BallOverlay {

    private val views = ArrayList<View>()
    private var wm: WindowManager? = null
    private var sig = ""

    fun hide() {
        val w = wm
        for (v in views) {
            try { w?.removeView(v) } catch (e: Exception) {}
        }
        views.clear()
        sig = ""
    }

    fun show(c: Context, list: List<KeyComp>) {
        val kit = OverlayKit(c)
        val balls = list.filter { it.type == KeyComp.BALL }
        val s = balls.joinToString("|") { it.pts.joinToString(",") } +
            "#" + KeyMapStore.getInt(c, "ballalpha", 90) + "#" + kit.accent
        if (s == sig && views.isNotEmpty()) return
        hide()
        sig = s
        val w = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm = w
        val d = c.resources.displayMetrics.density
        val r = KeyMapStore.real(c)
        val size = (54 * d).toInt()
        val m = (4 * d).toInt()
        val a = KeyMapStore.getInt(c, "ballalpha", 90) / 100f
        for (k in balls) {
            val fl = FrameLayout(c)
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            bg.setColor(0xFF140F2E.toInt())
            bg.setStroke((2 * d).toInt(), kit.accent)
            fl.background = bg
            fl.addView(
                LogoView(c),
                FrameLayout.LayoutParams(size - 2 * m, size - 2 * m).apply {
                    gravity = Gravity.CENTER
                }
            )
            fl.alpha = a
            fl.setOnClickListener { KeymapMenu.open(c, w, kit) }
            fl.setOnLongClickListener {
                KeyRouter.switchEnabled(!KeyRouter.enabled)
                Toast.makeText(
                    c, if (KeyRouter.enabled) "Keymap nyala" else "Keymap mati",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
            val p = WindowManager.LayoutParams(
                size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            p.gravity = Gravity.TOP or Gravity.START
            p.x = (k.pts[0] * r[0] - size / 2f).toInt()
            p.y = (k.pts[1] * r[1] - size / 2f).toInt()
            try {
                w.addView(fl, p)
                views.add(fl)
            } catch (e: Exception) {
            }
        }
    }
}
