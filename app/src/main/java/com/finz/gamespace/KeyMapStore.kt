package com.finz.gamespace

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

object KeyMapStore {

    @Suppress("DEPRECATION")
    fun real(c: Context): IntArray {
        val wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(dm)
        return intArrayOf(dm.widthPixels, dm.heightPixels)
    }

    fun load(c: Context, profile: String): MutableMap<Int, FloatArray> {
        val raw = c.getSharedPreferences("km", Context.MODE_PRIVATE)
            .getString(profile, "") ?: ""
        val m = LinkedHashMap<Int, FloatArray>()
        for (item in raw.split(";")) {
            val p = item.split(",")
            if (p.size != 3) continue
            val k = p[0].toIntOrNull() ?: continue
            val x = p[1].toFloatOrNull() ?: continue
            val y = p[2].toFloatOrNull() ?: continue
            m[k] = floatArrayOf(x, y)
        }
        return m
    }

    fun save(c: Context, profile: String, m: Map<Int, FloatArray>) {
        val s = m.entries.joinToString(";") { "${it.key},${it.value[0]},${it.value[1]}" }
        c.getSharedPreferences("km", Context.MODE_PRIVATE)
            .edit().putString(profile, s).apply()
    }
}
