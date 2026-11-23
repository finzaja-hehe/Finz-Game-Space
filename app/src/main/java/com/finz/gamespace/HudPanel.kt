package com.finz.gamespace

import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.provider.Settings
import android.view.Gravity
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

    private fun hex(key: String, icon: String, label: String): HexButton {
        val h = HexButton(ctx, icon, label)
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

    private fun grid(a: List<HexButton>, b: List<HexButton>): LinearLayout {
        val g = LinearLayout(ctx)
        g.orientation = LinearLayout.VERTICAL
        for (r in listOf(a, b)) {
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
        root.setPadding(kit.dp(26), kit.dp(14), kit.dp(26), kit.dp(12))
        gauge.accent = acc

        val left = grid(
            listOf(hex("dnd", "🔕", "DND"), hex("keep", "💡", "Layar On"), hex("set", "⚙", "Setelan")),
            listOf(hex("cross", "🎯", "Crosshair"), hex("hz", "60", "Refresh"), hex("keymap", "🎮", "Keymap"))
        )
        val right = grid(
            listOf(hex("app", "↩", "Aplikasi"), hex("off", "⛔", "Matikan"), hex("turbo", "🚀", "Turbo")),
            listOf(hex("mon", "📊", "Monitor"), hex("rot", "🔄", "Rotasi"), hex("close", "✕", "Tutup"))
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
            kit.slider("☀", 5, 255, bright, 22) { act.setBrightness(it) },
            LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { rightMargin = kit.dp(14) }
        )
        sl.addView(
            kit.slider(
                "🔊", 0, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                am.getStreamVolume(AudioManager.STREAM_MUSIC), 22
            ) { am.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) },
            LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { leftMargin = kit.dp(14) }
        )
        root.addView(sl, LinearLayout.LayoutParams(-1, kit.WRAP))
    }

    fun active(key: String, on: Boolean) {
        btn[key]?.active = on
    }

    fun setHz(hz: Int) {
        btn["hz"]?.icon = "$hz"
    }

    fun update(ram: Int, temp: Float, net: String, time: String) {
        gauge.set(ram, "$ram%", "RAM")
        jam.text = time
        mini.text = "%.1f°C · %s".format(temp, net)
        mini2.text = "CPU ${SysInfo.cpuGhz()}  •  PING ${Ping.text()}"
    }
}
