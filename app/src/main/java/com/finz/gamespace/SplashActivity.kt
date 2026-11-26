package com.finz.gamespace

import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.math.min

class SplashActivity : AppCompatActivity() {

    private var gone = false
    private val ui = Handler(Looper.getMainLooper())
    private val anims = ArrayList<ValueAnimator>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(s: String, size: Float, color: Int, bold: Boolean, spacing: Float): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setTextColor(color)
        t.letterSpacing = spacing
        t.gravity = Gravity.CENTER
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun gradient(t: TextView) {
        val w = t.paint.measureText(t.text.toString())
        t.paint.shader = LinearGradient(
            0f, 0f, max(w, 1f), 0f, Palette.a(this), Palette.b(this), Shader.TileMode.CLAMP
        )
    }

    private fun run(a: ValueAnimator, update: (Float) -> Unit) {
        a.addUpdateListener { update(it.animatedValue as Float) }
        anims.add(a)
        a.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        goImmersive(this)
        val dm = resources.displayMetrics
        val root = FrameLayout(this)
        root.setBackgroundColor(Palette.BG)
        root.addView(LobbyBg(this), FrameLayout.LayoutParams(-1, -1))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        val side = (min(dm.widthPixels, dm.heightPixels) * 0.42f).toInt()
        val logo = LogoView(this)
        col.addView(logo, LinearLayout.LayoutParams(side, side))

        val title = label("FINZ GAME SPACE", 22f, 0xFFFFFFFF.toInt(), true, 0.25f)
        gradient(title)
        col.addView(title, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) })
        val sub = label("BOOST  •  OVERLAY  •  KEYMAP", 11f, Palette.MUTED, false, 0.3f)
        col.addView(sub, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
        val bar = ProgressLine(this)
        col.addView(bar, LinearLayout.LayoutParams(dp(200), dp(3)).apply { topMargin = dp(18) })
        root.addView(col, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        logo.alpha = 0f
        logo.scaleX = 0.4f
        logo.scaleY = 0.4f
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(650)
            .setInterpolator(OvershootInterpolator(1.4f)).start()
        title.alpha = 0f
        title.translationY = dp(22).toFloat()
        title.animate().alpha(1f).translationY(0f).setStartDelay(650).setDuration(520)
            .setInterpolator(DecelerateInterpolator()).start()
        sub.alpha = 0f
        sub.translationY = dp(16).toFloat()
        sub.animate().alpha(1f).translationY(0f).setStartDelay(850).setDuration(520)
            .setInterpolator(DecelerateInterpolator()).start()

        run(ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            startDelay = 350
        }) { logo.burst = it }
        run(ValueAnimator.ofFloat(0.3f, 1f).apply {
            duration = 1100
            startDelay = 600
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }) { logo.pulse = it }
        run(ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2100
            startDelay = 500
            interpolator = LinearInterpolator()
        }) { bar.progress = it }

        root.setOnClickListener { go() }
        ui.postDelayed({ go() }, 3000)
    }

    @Suppress("DEPRECATION")
    private fun go() {
        if (gone) return
        gone = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        ui.removeCallbacksAndMessages(null)
        for (a in anims) a.cancel()
        anims.clear()
        super.onDestroy()
    }
}
