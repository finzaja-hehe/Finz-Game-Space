package com.finz.gamespace

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SensiActivity : AppCompatActivity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val kit = OverlayKit(this)
        window.setBackgroundDrawable(ColorDrawable(Palette.BG))
        window.statusBarColor = Palette.BG
        window.navigationBarColor = Palette.BG
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(18), dp(14), dp(18), dp(10))
        root.addView(kit.tx("Generator Sensi Free Fire", 24f, Color.WHITE, true))
        root.addView(SensiUi(this, kit).view, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8) })
        setContentView(root)
    }
}
