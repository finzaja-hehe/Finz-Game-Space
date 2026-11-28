package com.finz.gamespace

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.LinearLayout

object ColorKit {

    val ALL = intArrayOf(
        0xFFFFFFFF.toInt(), 0xFFCFCFCF.toInt(), 0xFFFFE600.toInt(), 0xFFFF7A1A.toInt(),
        0xFFFF3B47.toInt(), 0xFFFF4FD8.toInt(), 0xFFB45CFF.toInt(), 0xFF2D9CFF.toInt(),
        0xFF22E5FF.toInt(), 0xFF00FFC8.toInt(), 0xFF22E58B.toInt(), 0xFFA8FF00.toInt(),
        0xFFFFC400.toInt(), 0xFFFF6B6B.toInt(), 0xFFFF00A8.toInt(), 0xFF7C5CFF.toInt(),
        0xFF7DD3FC.toInt(), 0xFF7CFFCB.toInt(), 0xFFFFB38A.toInt(), 0xFFFFB3D9.toInt(),
        0xFFD1B3FF.toInt(), 0xFF14B8A6.toInt(), 0xFF4F46E5.toInt(), 0xFFDC143C.toInt(),
        0xFF000000.toInt(), 0xFF52525B.toInt(), 0xFF92400E.toInt(), 0xFF65A30D.toInt(),
        0xFF0EA5E9.toInt(), 0xFF0D9488.toInt(), 0xFF9333EA.toInt(), 0xFFBE123C.toInt()
    )

    fun hueOf(c: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(c, hsv)
        return hsv[0].toInt().coerceIn(0, 360)
    }

    private fun dot(kit: OverlayKit, color: Int, sel: Boolean): GradientDrawable {
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(color)
        if (sel) g.setStroke(kit.dp(3), kit.accent) else g.setStroke(kit.dp(1), 0x88FFFFFF.toInt())
        return g
    }

    fun picker(
        ctx: Context, kit: OverlayKit, current0: Int, withHue: Boolean = true,
        onPick: (Int) -> Unit
    ): LinearLayout {
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL
        var cur = current0
        val dots = ArrayList<View>()
        fun paint() {
            for (i in dots.indices) dots[i].background = dot(kit, ALL[i], ALL[i] == cur)
        }
        var i = 0
        while (i < ALL.size) {
            val row = LinearLayout(ctx)
            for (j in 0 until 8) {
                val k = i + j
                val v = View(ctx)
                v.setOnClickListener {
                    cur = ALL[k]
                    paint()
                    onPick(cur)
                }
                dots.add(v)
                row.addView(
                    v,
                    LinearLayout.LayoutParams(kit.dp(28), kit.dp(28)).apply {
                        rightMargin = kit.dp(6)
                        topMargin = kit.dp(6)
                    }
                )
            }
            root.addView(row)
            i += 8
        }
        paint()
        if (withHue) {
            root.addView(
                kit.slider("Warna bebas", 0, 360, hueOf(cur), 80) { h ->
                    cur = Color.HSVToColor(floatArrayOf(h.toFloat(), 1f, 1f))
                    paint()
                    onPick(cur)
                },
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(8) }
            )
        }
        return root
    }
}
