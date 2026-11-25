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
        const val GROUP = 4
        const val BALL = 5
        const val SCROLL = 6
        const val FPSV = 7
        const val FREE = 8
        const val PICK = 9
    }
}

object KeyMapStore {

    const val SLOTS = 5
    private const val HEAD = "FINZKEYMAP1|"

    @Suppress("DEPRECATION")
    fun real(c: Context): IntArray {
        val wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(dm)
        return intArrayOf(dm.widthPixels, dm.heightPixels)
    }

    private fun cfg(c: Context) = c.getSharedPreferences("kmcfg", Context.MODE_PRIVATE)

    fun getBool(c: Context, k: String, def: Boolean): Boolean = cfg(c).getBoolean(k, def)

    fun putBool(c: Context, k: String, v: Boolean) {
        cfg(c).edit().putBoolean(k, v).apply()
    }

    fun getInt(c: Context, k: String, def: Int): Int = cfg(c).getInt(k, def)

    fun putInt(c: Context, k: String, v: Int) {
        cfg(c).edit().putInt(k, v).apply()
    }

    fun slot(c: Context): Int = getInt(c, "slot", 1).coerceIn(1, SLOTS)

    fun setSlot(c: Context, n: Int) {
        putInt(c, "slot", n.coerceIn(1, SLOTS))
    }

    fun profileKey(c: Context, n: Int = slot(c)): String {
        val r = real(c)
        return "s" + n + (if (r[0] > r[1]) "_L" else "_P")
    }

    fun encode(list: List<KeyComp>): String = list.joinToString(";") {
        "${it.type}:${it.keys.joinToString(",")}:${it.pts.joinToString(",")}"
    }

    fun decode(raw0: String): MutableList<KeyComp> {
        val raw = raw0.filter { !it.isWhitespace() }
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
                4 -> ks.size == 1 && ps.size >= 2 && ps.size % 2 == 0
                5 -> ks.isEmpty() && ps.size == 2
                6 -> ks.isEmpty() && ps.size == 4
                7, 8 -> ks.size == 1 && ps.size == 3
                9 -> ks.size == 1 && ps.size == 2
                else -> false
            }
            if (ok) out.add(KeyComp(t, ks, ps))
        }
        return out
    }

    fun load(c: Context, key: String): MutableList<KeyComp> =
        decode(c.getSharedPreferences("km2", Context.MODE_PRIVATE).getString(key, "") ?: "")

    fun save(c: Context, key: String, list: List<KeyComp>) {
        c.getSharedPreferences("km2", Context.MODE_PRIVATE)
            .edit().putString(key, encode(list)).apply()
    }

    fun count(c: Context, n: Int): Int = load(c, profileKey(c, n)).size

    fun export(list: List<KeyComp>): String = HEAD + encode(list)

    fun parseImport(text: String): MutableList<KeyComp>? {
        val t = text.trim()
        val body = if (t.startsWith(HEAD)) t.substring(HEAD.length) else t
        val l = decode(body)
        return if (l.isEmpty()) null else l
    }
}
