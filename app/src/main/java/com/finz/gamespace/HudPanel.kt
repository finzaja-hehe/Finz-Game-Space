package com.finz.gamespace

import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout

class HudPanel(
    private val ctx: Context,
    private val kit: OverlayKit,
    private val act: OverlayActions,
    private val onAction: (String) -> Unit
) {
    val root = HudLayout(ctx)
    private val btn = HashMap<String, HexButton>()
    private val gauge = GaugeView(ctx)
    private val jam = kit.tx("--:--", 11f, Color.WHITE, true)
    private val mini = kit.tx("-", 10f, kit.muted, true)
    private val mini2 = kit.tx("-", 9f, kit.muted, true)
    private val statusTv = kit.tx("", 10f, 0xFFFFE600.toInt(), true)

    private fun hex(key: String, icon: String, label: String, iconId: Int = -1): HexButton {
        val h = HexButton(ctx, icon, label, iconId)
        h.accent = kit.accent
        h.setOnClickListener {
            if (key == "keymap") {
                onAction("close")
                val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                KeymapMenu.open(ctx, wm, kit)
            } else {
                onAction(key)
            }
        }
        btn[key] = h
        return h
    }

    private fun grid(rows: List<List<HexButton>>): LinearLayout {
        val g = LinearLayout(ctx)
        g.orientation = LinearLayout.VERTICAL
        for (r in rows) {
            val row = LinearLayout(ctx)
            r.forEach { row.addView(it) }
            g.addView(row)
        }
        return g
    }

    init {
        val acc = kit.accent
        root.accent = acc
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(kit.dp(24), kit.dp(12), kit.dp(24), kit.dp(10))
        gauge.accent = acc

        val left = grid(
            listOf(
                listOf(hex("dnd", "", "DND", 0), hex("keep", "", "Layar On", 1)),
                listOf(hex("cross", "", "Crosshair", 2), hex("keymap", "", "Keymap", 3)),
                listOf(hex("shot", "", "Foto", 4), hex("rec", "", "Rekam", 5))
            )
        )
        val right = grid(
            listOf(
                listOf(hex("app", "↩", "Aplikasi"), hex("off", "", "Matikan", 6)),
                listOf(hex("turbo", "", "Turbo", 7), hex("mon", "", "Monitor", 8)),
                listOf(hex("rot", "", "Rotasi", 9), hex("close", "✕", "Tutup"))
            )
        )
        val mid = LinearLayout(ctx)
        mid.orientation = LinearLayout.VERTICAL
        mid.gravity = Gravity.CENTER_HORIZONTAL
        mid.addView(jam)
        mid.addView(gauge)
        mid.addView(mini)
        mid.addView(mini2)

        val top = LinearLayout(ctx)
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(left)
        top.addView(mid, LinearLayout.LayoutParams(0, kit.WRAP, 1f))
        top.addView(right)
        root.addView(top)

        val am = ctx.getSystemService(AudioManager::class.java)
        val bright = Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        val sl = LinearLayout(ctx)
        sl.addView(
            kit.slider("Cerah", 5, 255, bright, 44) { act.setBrightness(it) },
            LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { rightMargin = kit.dp(10) }
        )
        sl.addView(
            kit.slider(
                "Suara", 0, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                am.getStreamVolume(AudioManager.STREAM_MUSIC), 44
            ) { am.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) },
            LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { leftMargin = kit.dp(10) }
        )
        root.addView(sl, LinearLayout.LayoutParams(-1, kit.WRAP))

        statusTv.gravity = Gravity.CENTER
        statusTv.visibility = View.GONE
        root.addView(statusTv, LinearLayout.LayoutParams(-1, kit.WRAP))
    }

    fun active(key: String, on: Boolean) {
        btn[key]?.active = on
    }

    fun setStatus(s: String) {
        statusTv.text = s
        statusTv.visibility = if (s.isEmpty()) View.GONE else View.VISIBLE
    }

    fun update(ram: Int, temp: Float, net: String, time: String) {
        gauge.set(ram, "$ram%", "RAM")
        jam.text = time
        mini.text = "%.1f°C · %s".format(temp, net)
        mini2.text = "CPU ${SysInfo.cpuGhz()}\nPING ${Ping.text()}"
    }
}
