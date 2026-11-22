package com.finz.gamespace

import android.app.ActivityManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

abstract class BaseUi : AppCompatActivity() {

    protected val themes = intArrayOf(
        0xFFFF7A1A.toInt(), 0xFF2D9CFF.toInt(),
        0xFFFF3B47.toInt(), 0xFF22E58B.toInt()
    )
    protected var themeIndex = 0
    protected val bg = 0xFF0B0D12.toInt()
    protected val panel = 0xFF161A22.toInt()
    protected val muted = 0xFF8B93A1.toInt()
    protected val accent: Int get() = themes[themeIndex]
    protected val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    protected val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

    protected fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    protected fun mix(c: Int, r: Float): Int = Color.rgb(
        (Color.red(bg) + (Color.red(c) - Color.red(bg)) * r).toInt(),
        (Color.green(bg) + (Color.green(c) - Color.green(bg)) * r).toInt(),
        (Color.blue(bg) + (Color.blue(c) - Color.blue(bg)) * r).toInt()
    )

    protected fun box(color: Int, radius: Int, strokeColor: Int = 0, strokeDp: Int = 0) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
        }

    protected fun lp(w: Int, h: Int, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }

    protected fun tv(s: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    protected fun stat(label: String, value: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        addView(tv(label, 9f, muted, true).apply { letterSpacing = 0.15f })
        addView(tv(value, 16f, Color.WHITE, true).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
    }

    protected fun sideBar(focusOn: Boolean, onFocus: () -> Unit): View {
        val side = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0x33000000)
            setPadding(dp(12), dp(16), dp(12), dp(16))
        }
        val logo = tv("F", 20f, Color.BLACK, true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accent)
            }
        }
        side.addView(logo, lp(dp(44), dp(44)))
        side.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        val btn = tv(
            if (focusOn) "ON" else "DND", 11f,
            if (focusOn) Color.BLACK else Color.WHITE, true
        ).apply {
            gravity = Gravity.CENTER
            background = box(if (focusOn) accent else panel, 22)
            setOnClickListener { onFocus() }
        }
        side.addView(btn, lp(dp(44), dp(44)))
        side.addView(tv("Fokus", 9f, muted).apply { gravity = Gravity.CENTER },
            lp(WRAP, WRAP, t = 4))
        return side
    }

    protected fun statsBar(name: String, onMain: () -> Unit): View {
        val am = getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val total = mi.totalMem / 1048576
        val used = total - mi.availMem / 1048576
        val ramPct = (used * 100 / total).toInt()
        val bi = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = (bi?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val level = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = bi?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val bat = level * 100 / scale

        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = box(panel, 16)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        val w1 = { LinearLayout.LayoutParams(0, WRAP, 1f) }
        bottom.addView(stat("GAME", name), LinearLayout.LayoutParams(0, WRAP, 1.6f))
        bottom.addView(stat("SUHU", "%.1f°C".format(temp)), w1())
        bottom.addView(stat("RAM", "$ramPct%"), w1())
        bottom.addView(stat("BATERAI", "$bat%"), w1())
        bottom.addView(stat("CPU", "${Runtime.getRuntime().availableProcessors()} core"), w1())
        val launch = tv("▶  MAIN", 14f, Color.BLACK, true).apply {
            gravity = Gravity.CENTER
            background = box(accent, 14)
            setPadding(dp(22), dp(12), dp(22), dp(12))
            setOnClickListener { onMain() }
        }
        bottom.addView(launch, lp(WRAP, WRAP, l = 8))
        return bottom
    }

    protected fun gameCard(g: ResolveInfo, on: Boolean, onClick: () -> Unit): View {
        val pm = packageManager
        val card = FrameLayout(this).apply {
            background = box(panel, 20)
            clipToOutline = true
            setOnClickListener { onClick() }
        }
        card.addView(ImageView(this).apply {
            setImageDrawable(g.loadIcon(pm))
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = if (on) 0.5f else 0.25f
        }, FrameLayout.LayoutParams(MATCH, MATCH))
        card.addView(View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x00000000, 0xEE0B0D12.toInt())
            )
        }, FrameLayout.LayoutParams(MATCH, MATCH))
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            setPadding(dp(10), dp(10), dp(10), dp(16))
        }
        col.addView(ImageView(this).apply { setImageDrawable(g.loadIcon(pm)) },
            lp(dp(84), dp(84)))
        col.addView(tv(g.loadLabel(pm).toString(), 14f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }, lp(MATCH, WRAP, t = 8))
        card.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))
        card.addView(View(this).apply {
            background = box(
                Color.TRANSPARENT, 20,
                if (on) accent else 0xFF262B36.toInt(), if (on) 3 else 1
            )
        }, FrameLayout.LayoutParams(MATCH, MATCH))
        return card
    }

    protected fun addCard(onClick: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        background = box(0x22FFFFFF, 20, muted, 1)
        setOnClickListener { onClick() }
        addView(tv("+", 44f, accent, true).apply { gravity = Gravity.CENTER })
        addView(tv("Tambah Game", 13f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
    }
}
