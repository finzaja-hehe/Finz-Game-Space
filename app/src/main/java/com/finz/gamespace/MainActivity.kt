package com.finz.gamespace

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val themes = intArrayOf(
        0xFFFF7A1A.toInt(), 0xFF2D9CFF.toInt(),
        0xFFFF3B47.toInt(), 0xFF22E58B.toInt()
    )
    private var themeIndex = 0
    private var pick = 0
    private var focusOn = false
    private var games: List<ResolveInfo> = emptyList()

    private val bg = 0xFF0B0D12.toInt()
    private val panel = 0xFF161A22.toInt()
    private val muted = 0xFF8B93A1.toInt()
    private val accent: Int get() = themes[themeIndex]
    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
    private val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.statusBarColor = bg
        window.navigationBarColor = bg
        themeIndex = getSharedPreferences("gs", Context.MODE_PRIVATE).getInt("theme", 0)
        loadGames()
    }

    override fun onResume() {
        super.onResume()
        val nm = getSystemService(NotificationManager::class.java)
        focusOn = nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        buildUi()
    }

    private fun loadGames() {
        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val all = pm.queryIntentActivities(launcher, 0)
            .filter { it.activityInfo.packageName != packageName }
        val g = all.filter {
            it.activityInfo.applicationInfo.category == ApplicationInfo.CATEGORY_GAME
        }
        games = (if (g.isEmpty()) all else g)
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
    }

    // ---------- helper ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun mix(c: Int, r: Float): Int = Color.rgb(
        (Color.red(bg) + (Color.red(c) - Color.red(bg)) * r).toInt(),
        (Color.green(bg) + (Color.green(c) - Color.green(bg)) * r).toInt(),
        (Color.blue(bg) + (Color.blue(c) - Color.blue(bg)) * r).toInt()
    )

    private fun box(color: Int, radius: Int, strokeColor: Int = 0, strokeDp: Int = 0) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeDp > 0) setStroke(dp(strokeDp), strokeColor)
        }

    private fun lp(w: Int, h: Int, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }

    private fun tv(s: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun stat(label: String, value: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        addView(tv(label, 9f, muted, true).apply { letterSpacing = 0.15f })
        addView(tv(value, 16f, Color.WHITE, true).apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
    }

    // ---------- UI ----------
    private fun buildUi() {
        val pm = packageManager
        if (pick >= games.size) pick = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(bg, mix(accent, 0.18f), bg)
            )
        }

        // Sidebar kiri
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
        val focusBtn = tv(
            if (focusOn) "ON" else "DND", 11f,
            if (focusOn) Color.BLACK else Color.WHITE, true
        ).apply {
            gravity = Gravity.CENTER
            background = box(if (focusOn) accent else panel, 22)
            setOnClickListener { toggleFocus() }
        }
        side.addView(focusBtn, lp(dp(44), dp(44)))
        side.addView(tv("Fokus", 9f, muted).apply { gravity = Gravity.CENTER },
            lp(WRAP, WRAP, t = 4))

        // Kolom utama
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(12))
        }

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(tv("FINZ GAME SPACE", 10f, accent, true).apply { letterSpacing = 0.25f })
        titles.addView(tv("Pilih Game", 22f, Color.WHITE, true))
        header.addView(titles, LinearLayout.LayoutParams(0, WRAP, 1f))
        for (i in themes.indices) {
            val dot = View(this).apply {
                background = box(themes[i], 11, Color.WHITE, if (i == themeIndex) 2 else 0)
                setOnClickListener { changeTheme(i) }
            }
            header.addView(dot, lp(dp(22), dp(22), l = 10))
        }
        main.addView(header, lp(MATCH, WRAP))

        // Kartu game
        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(4))
        }
        games.forEachIndexed { i, g -> row.addView(gameCard(i, g), lp(dp(150), MATCH, r = 12)) }
        scroll.addView(row, FrameLayout.LayoutParams(WRAP, MATCH))
        main.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f).apply {
            topMargin = dp(10)
            bottomMargin = dp(10)
        })

        // Bar statistik + tombol main
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
        val name = games.getOrNull(pick)?.loadLabel(pm)?.toString() ?: "-"

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
            setOnClickListener { launchGame() }
        }
        bottom.addView(launch, lp(WRAP, WRAP, l = 8))
        main.addView(bottom, lp(MATCH, WRAP))

        root.addView(side, LinearLayout.LayoutParams(WRAP, MATCH))
        root.addView(main, LinearLayout.LayoutParams(0, MATCH, 1f))
        setContentView(root)
    }

    private fun gameCard(i: Int, g: ResolveInfo): View {
        val pm = packageManager
        val on = i == pick
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(mix(accent, if (on) 0.35f else 0.12f), panel)
            ).apply {
                cornerRadius = dp(18).toFloat()
                setStroke(dp(if (on) 2 else 1), if (on) accent else 0xFF262B36.toInt())
            }
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener {
                pick = i
                buildUi()
            }
        }
        card.addView(ImageView(this).apply { setImageDrawable(g.loadIcon(pm)) },
            lp(dp(72), dp(72)))
        card.addView(tv(g.loadLabel(pm).toString(), 13f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }, lp(MATCH, WRAP, t = 10))
        return card
    }

    private fun changeTheme(i: Int) {
        themeIndex = i
        getSharedPreferences("gs", Context.MODE_PRIVATE).edit().putInt("theme", i).apply()
        buildUi()
    }

    private fun launchGame() {
        val g = games.getOrNull(pick) ?: return
        packageManager.getLaunchIntentForPackage(g.activityInfo.packageName)
            ?.let { startActivity(it) }
    }

    private fun toggleFocus() {
        val nm = getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Izinkan akses Jangan Ganggu dulu", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }
        focusOn = !focusOn
        nm.setInterruptionFilter(
            if (focusOn) NotificationManager.INTERRUPTION_FILTER_NONE
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
        buildUi()
    }
}
