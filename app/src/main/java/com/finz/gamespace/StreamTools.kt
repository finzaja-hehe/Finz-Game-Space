package com.finz.gamespace

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import java.text.NumberFormat
import java.util.Locale

object StreamTools {

    private val ui = Handler(Looper.getMainLooper())

    fun rupiah(s: String): String {
        val digits = s.filter { it.isDigit() }
        val n = digits.toLongOrNull() ?: return s
        return "Rp " + NumberFormat.getInstance(Locale("in", "ID")).format(n)
    }

    fun alert(ctx: Context, name: String, amount: String, msg: String) {
        val c = ctx.applicationContext
        if (!Settings.canDrawOverlays(c)) {
            Toast.makeText(c, "Izinkan 'Tampil di atas aplikasi lain' dulu (menu Izin)", Toast.LENGTH_LONG).show()
            return
        }
        val wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val kit = OverlayKit(c)
        val root = LinearLayout(c)
        root.orientation = LinearLayout.HORIZONTAL
        root.gravity = Gravity.CENTER_VERTICAL
        val bg = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(0xF0200F5A.toInt(), 0xF0070419.toInt())
        )
        bg.cornerRadius = kit.dp(18).toFloat()
        bg.setStroke(kit.dp(2), kit.accent)
        root.background = bg
        root.setPadding(kit.dp(14), kit.dp(10), kit.dp(18), kit.dp(10))
        root.addView(LogoView(c), LinearLayout.LayoutParams(kit.dp(44), kit.dp(44)))
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.addView(kit.tx(name, 17f, Color.WHITE, true))
        col.addView(kit.tx(rupiah(amount), 15f, 0xFFFFE600.toInt(), true))
        if (msg.isNotBlank()) col.addView(kit.tx(msg, 12f, 0xFFD9D4FF.toInt()))
        root.addView(col, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = kit.dp(10) })
        val lp = WindowManager.LayoutParams(
            -2, -2, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.y = kit.dp(18)
        root.alpha = 0f
        try {
            wm.addView(root, lp)
        } catch (e: Exception) {
            return
        }
        val off = kit.dp(90).toFloat()
        Glide.run(root, 0, 380) { u ->
            root.translationY = -off * (1f - u)
            root.alpha = u
        }
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            ui.postDelayed({ try { tg.release() } catch (e: Exception) {} }, 700)
        } catch (e: Exception) {
        }
        ui.postDelayed({
            Glide.run(root, 0, 320) { u ->
                root.translationY = -off * u
                root.alpha = 1f - u
            }
            ui.postDelayed({ try { wm.removeView(root) } catch (e: Exception) {} }, 380)
        }, 5200)
    }
}
