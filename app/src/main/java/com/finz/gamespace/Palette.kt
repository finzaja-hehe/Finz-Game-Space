package com.finz.gamespace

import android.content.Context
import android.graphics.Color

object Palette {

    val A = intArrayOf(
        0xFFB45CFF.toInt(), 0xFF38BDF8.toInt(), 0xFFFF4FD8.toInt(), 0xFF4F6BFF.toInt()
    )
    val B = intArrayOf(
        0xFF38BDF8.toInt(), 0xFF7C5CFF.toInt(), 0xFF6C5CFF.toInt(), 0xFF22E5FF.toInt()
    )
    val BG = 0xFF05030F.toInt()
    val BG2 = 0xFF0C0724.toInt()
    val PANEL = 0xFF140C34.toInt()
    val MUTED = 0xFF9A93C9.toInt()

    fun idx(c: Context): Int =
        c.getSharedPreferences("gs", Context.MODE_PRIVATE).getInt("theme", 0).coerceIn(0, 3)

    fun a(c: Context): Int = A[idx(c)]

    fun b(c: Context): Int = B[idx(c)]

    fun alpha(c: Int, a: Int): Int = (c and 0xFFFFFF) or (a.coerceIn(0, 255) shl 24)

    fun mix(c1: Int, c2: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        val r = Color.red(c1) + ((Color.red(c2) - Color.red(c1)) * k).toInt()
        val g = Color.green(c1) + ((Color.green(c2) - Color.green(c1)) * k).toInt()
        val bl = Color.blue(c1) + ((Color.blue(c2) - Color.blue(c1)) * k).toInt()
        return Color.rgb(r, g, bl)
    }
}
