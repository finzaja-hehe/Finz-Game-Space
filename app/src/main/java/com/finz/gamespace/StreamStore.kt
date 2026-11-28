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
    var thick: Int,
    var font: Int = 0,
    var fx: Int = 0,
    var color2: Int = 0xFF38BDF8.toInt(),
    var ext: Int = 0,
    var anim: Int = 0
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
        const val CLOCK = 9
        const val ROTATE = 10
        const val COUNTER = 11
        const val SHAPE = 12
        const val SCENE = 13
        val TYPE_NAMES = arrayOf(
            "Widget Web", "Teks", "Teks Berjalan", "Timer Live", "Logo / Foto / Video",
            "Info Perangkat", "Goal Donasi", "Hitung Mundur", "Bingkai Layar",
            "Jam dan Tanggal", "Teks Bergilir", "Penghitung", "Panel Hias", "Layar BRB / Starting"
        )
        val ANIM_NAMES = arrayOf("Tidak", "Berkedip", "Denyut", "Melayang", "Goyang")
    }
}

object StreamStore {

    private fun sp(c: Context) = c.getSharedPreferences("stream", Context.MODE_PRIVATE)

    fun master(c: Context): Boolean = sp(c).getBoolean("master", false)

    fun setMaster(c: Context, v: Boolean) {
        sp(c).edit().putBoolean("master", v).apply()
    }

    private fun enc(list: List<Layer>, strip: Boolean): JSONArray {
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
            o.put("text", if (strip && (l.text.startsWith("img:") || l.text.startsWith("vid:"))) "" else l.text)
            o.put("size", l.size)
            o.put("color", l.color)
            o.put("thick", l.thick)
            o.put("font", l.font)
            o.put("fx", l.fx)
            o.put("color2", l.color2)
            o.put("ext", l.ext)
            o.put("anim", l.anim)
            a.put(o)
        }
        return a
    }

    private fun dec(a: JSONArray): MutableList<Layer> {
        val out = ArrayList<Layer>()
        for (i in 0 until a.length()) {
            try {
                val o = a.getJSONObject(i)
                out.add(
                    Layer(
                        o.getInt("id"), o.getInt("type"), o.getString("name"), o.getBoolean("on"),
                        o.getDouble("x").toFloat(), o.getDouble("y").toFloat(),
                        o.getDouble("w").toFloat(), o.getDouble("h").toFloat(),
                        o.getInt("alpha"), o.getString("text"), o.getInt("size"), o.getInt("color"),
                        o.optInt("thick", 0), o.optInt("font", 0), o.optInt("fx", 0),
                        o.optInt("color2", 0xFF38BDF8.toInt()), o.optInt("ext", 0), o.optInt("anim", 0)
                    )
                )
            } catch (e: Exception) {
            }
        }
        return out
    }

    fun load(c: Context): MutableList<Layer> {
        return try {
            dec(JSONArray(sp(c).getString("layers", "[]") ?: "[]"))
        } catch (e: Exception) {
            ArrayList()
        }
    }

    fun save(c: Context, list: List<Layer>) {
        sp(c).edit().putString("layers", enc(list, false).toString()).apply()
    }

    fun export(list: List<Layer>): String = "FINZSTREAM1|" + enc(list, true).toString()

    fun importAppend(c: Context, text: String): Int {
        val t = text.trim().removePrefix("FINZSTREAM1|")
        val src = try {
            dec(JSONArray(t))
        } catch (e: Exception) {
            return -1
        }
        if (src.isEmpty()) return -1
        val list = load(c)
        var id = list.maxOfOrNull { it.id } ?: 0
        for (l in src) {
            id++
            list.add(
                Layer(
                    id, l.type, l.name, l.on, l.x, l.y, l.w, l.h, l.alpha, l.text, l.size, l.color,
                    l.thick, l.font, l.fx, l.color2, l.ext, l.anim
                )
            )
        }
        save(c, list)
        return src.size
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
            Layer.CLOCK -> Layer(id, type, "Jam dan Tanggal", true, 0.80f, 0.13f, 0.18f, 0.12f, 100, "", 16, white, 1)
                .apply { fx = TextStyle.BOX }
            Layer.ROTATE -> Layer(id, type, "Teks Bergilir", true, 0.03f, 0.78f, 0.45f, 0.08f, 100,
                "Follow YouTube: @namamu\nTikTok: @namamu\nInstagram: @namamu", 16, yellow, 2)
                .apply { ext = 4; fx = TextStyle.BOX }
            Layer.COUNTER -> Layer(id, type, "Penghitung", true, 0.84f, 0.30f, 0.14f, 0.16f, 100, "KILL", 18, white, 2)
                .apply { fx = TextStyle.BOX }
            Layer.SHAPE -> Layer(id, type, "Panel Hias", true, 0.30f, 0.30f, 0.40f, 0.25f, 60, "", 16,
                0xFFB45CFF.toInt(), 2).apply { color2 = 0xFF38BDF8.toInt() }
            Layer.SCENE -> Layer(id, type, "BRB", true, 0f, 0f, 1f, 1f, 88,
                "SEBENTAR LAGI KEMBALI\nBRB", 28, white, 2)
                .apply { fx = TextStyle.NEON; color2 = 0xFFB45CFF.toInt() }
            else -> Layer(id, type, "Bingkai Layar", true, 0f, 0f, 1f, 1f, 90, "", 16, white, 4)
        }
    }

    fun template(list: List<Layer>, n: Int): List<Layer> {
        val tmp = ArrayList(list)
        val out = ArrayList<Layer>()
        fun mk(t: Int, mod: (Layer) -> Unit = {}): Layer {
            val l = create(tmp, t)
            mod(l)
            tmp.add(l)
            out.add(l)
            return l
        }
        when (n) {
            0 -> {
                mk(Layer.INFO) { it.x = 0.27f; it.y = 0.015f; it.w = 0.46f; it.h = 0.08f }
                mk(Layer.TIMER) { it.x = 0.78f; it.y = 0.015f }
                mk(Layer.MARQUEE) { it.fx = TextStyle.NEON }
                mk(Layer.FRAME) { it.thick = 2 }
            }
            1 -> {
                mk(Layer.WEB)
                mk(Layer.TEXT) { it.fx = TextStyle.NEON }
                mk(Layer.GOAL)
            }
            2 -> {
                mk(Layer.LOGO)
                mk(Layer.TIMER) { it.x = 0.78f; it.y = 0.03f }
            }
            else -> {
                mk(Layer.SCENE) {
                    it.name = "Starting Soon"
                    it.text = "LIVE SEGERA DIMULAI\nStarting Soon"
                }
                mk(Layer.COUNT) { it.x = 0.34f; it.y = 0.70f }
            }
        }
        return out
    }
}
