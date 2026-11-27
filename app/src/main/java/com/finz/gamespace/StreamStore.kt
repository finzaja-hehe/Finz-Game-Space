package com.finz.gamespace

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Layer(
    val id: Int,
    var type: Int,
    var name: String,
    var on: Boolean,
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float,
    var alpha: Int,
    var text: String,
    var size: Int,
    var color: Int,
    var thick: Int
) {
    companion object {
        const val WEB = 0
        const val TEXT = 1
        const val MARQUEE = 2
        const val TIMER = 3
        const val LOGO = 4
        const val INFO = 5
        const val GOAL = 6
        const val COUNT = 7
        const val FRAME = 8
        val TYPE_NAMES = arrayOf(
            "Widget Web", "Teks", "Teks Berjalan", "Timer Live", "Logo / Foto / Video",
            "Info Perangkat", "Goal Donasi", "Hitung Mundur", "Bingkai Layar"
        )
    }
}

object StreamStore {

    private fun sp(c: Context) = c.getSharedPreferences("stream", Context.MODE_PRIVATE)

    fun master(c: Context): Boolean = sp(c).getBoolean("master", false)

    fun setMaster(c: Context, v: Boolean) {
        sp(c).edit().putBoolean("master", v).apply()
    }

    fun load(c: Context): MutableList<Layer> {
        val out = ArrayList<Layer>()
        try {
            val a = JSONArray(sp(c).getString("layers", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                out.add(
                    Layer(
                        o.getInt("id"), o.getInt("type"), o.getString("name"), o.getBoolean("on"),
                        o.getDouble("x").toFloat(), o.getDouble("y").toFloat(),
                        o.getDouble("w").toFloat(), o.getDouble("h").toFloat(),
                        o.getInt("alpha"), o.getString("text"), o.getInt("size"), o.getInt("color"),
                        o.optInt("thick", 0)
                    )
                )
            }
        } catch (e: Exception) {
        }
        return out
    }

    fun save(c: Context, list: List<Layer>) {
        val a = JSONArray()
        for (l in list) {
            val o = JSONObject()
            o.put("id", l.id)
            o.put("type", l.type)
            o.put("name", l.name)
            o.put("on", l.on)
            o.put("x", l.x.toDouble())
            o.put("y", l.y.toDouble())
            o.put("w", l.w.toDouble())
            o.put("h", l.h.toDouble())
            o.put("alpha", l.alpha)
            o.put("text", l.text)
            o.put("size", l.size)
            o.put("color", l.color)
            o.put("thick", l.thick)
            a.put(o)
        }
        sp(c).edit().putString("layers", a.toString()).apply()
    }

    fun create(list: List<Layer>, type: Int): Layer {
        val id = (list.maxOfOrNull { it.id } ?: 0) + 1
        val white = 0xFFFFFFFF.toInt()
        val yellow = 0xFFFFE600.toInt()
        return when (type) {
            Layer.WEB -> Layer(id, type, "Alert Donasi", true, 0.30f, 0.03f, 0.40f, 0.34f, 100, "", 16, white, 0)
            Layer.TEXT -> Layer(id, type, "Teks Donasi", true, 0.03f, 0.86f, 0.50f, 0.10f, 100,
                "Donasi: saweria.co/namamu", 18, white, 2)
            Layer.MARQUEE -> Layer(id, type, "Teks Berjalan", true, 0f, 0.92f, 1f, 0.08f, 90,
                "Terima kasih sudah nonton! Jangan lupa follow dan donasi ya", 16, yellow, 2)
            Layer.TIMER -> Layer(id, type, "Timer Live", true, 0.78f, 0.03f, 0.20f, 0.09f, 100, "", 16, white, 1)
            Layer.LOGO -> Layer(id, type, "Logo", true, 0.02f, 0.03f, 0.10f, 0.18f, 90, "", 16, white, 0)
            Layer.INFO -> Layer(id, type, "Info Perangkat", true, 0.25f, 0.02f, 0.50f, 0.09f, 100, "", 13, white, 1)
            Layer.GOAL -> Layer(id, type, "Goal Donasi", true, 0.03f, 0.68f, 0.34f, 0.16f, 100,
                "Goal Donasi|150000|500000", 14, white, 1)
            Layer.COUNT -> Layer(id, type, "Live mulai dalam", true, 0.34f, 0.40f, 0.32f, 0.16f, 100, "10", 20, white, 2)
            else -> Layer(id, type, "Bingkai Layar", true, 0f, 0f, 1f, 1f, 90, "", 16, white, 4)
        }
    }
}
