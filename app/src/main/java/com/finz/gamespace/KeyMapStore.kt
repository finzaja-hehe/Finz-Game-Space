package com.finz.gamespace

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

class KeyComp(val type: Int, val keys: IntArray, val pts: FloatArray) {
    companion object {
        const val TOUCH = 0
        const val HOLD = 1
        const val SWIPE = 2
        const val JOY = 3
    }
}

object KeyMapStore {

    @Suppress("DEPRECATION")
    fun real(c: Context): IntArray {
        val wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(dm)
        return intArrayOf(dm.widthPixels, dm.heightPixels)
    }

    fun load(c: Context, profile: String): MutableList<KeyComp> {
        val raw = c.getSharedPreferences("km2", Context.MODE_PRIVATE)
            .getString(profile, "") ?: ""
        val out = ArrayList<KeyComp>()
        for (item in raw.split(";")) {
            val p = item.split(":")
            if (p.size != 3) continue
            val t = p[0].toIntOrNull() ?: continue
            val ks = p[1].split(",").mapNotNull { it.toIntOrNull() }.toIntArray()
            val ps = p[2].split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
            val ok = when (t) {
                0, 1 -> ks.size == 1 && ps.size == 2
                2 -> ks.size == 1 && ps.size == 4
                3 -> ks.size == 4 && ps.size == 3
                else -> false
            }
            if (ok) out.add(KeyComp(t, ks, ps))
        }
        return out
    }

    fun save(c: Context, profile: String, list: List<KeyComp>) {
        val s = list.joinToString(";") {
            "${it.type}:${it.keys.joinToString(",")}:${it.pts.joinToString(",")}"
        }
        c.getSharedPreferences("km2", Context.MODE_PRIVATE)
            .edit().putString(profile, s).apply()
    }
}
