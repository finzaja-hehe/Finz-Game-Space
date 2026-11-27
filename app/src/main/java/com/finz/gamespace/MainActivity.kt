package com.finz.gamespace

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private var pick = 0
    private var pickPkg = ""
    private var focusOn = false
    private var games: List<ResolveInfo> = emptyList()
    private val cards = ArrayList<GameCardView>()
    private val vals = HashMap<String, TextView>()
    private val ui = Handler(Looper.getMainLooper())
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private val ticker = object : Runnable {
        override fun run() {
            refreshStats()
            ui.postDelayed(this, 2000)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun txt(s: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        goImmersive(this)
    }

    override fun onResume() {
        super.onResume()
        goImmersive(this)
        val nm = getSystemService(NotificationManager::class.java)
        focusOn = nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        games = GameStore.saved(this)
        val idx = games.indexOfFirst { it.activityInfo.packageName == pickPkg }
        pick = if (idx >= 0) idx else 0
        pickPkg = games.getOrNull(pick)?.activityInfo?.packageName ?: ""
        build(true)
        ui.removeCallbacks(ticker)
        ui.postDelayed(ticker, 2000)
    }

    override fun onPause() {
        ui.removeCallbacks(ticker)
        super.onPause()
    }

    private fun intro(v: View, delay: Long, dx: Float, dy: Float) {
        v.alpha = 0f
        v.translationX = dx
        v.translationY = dy
        Tween.run(v, delay, 560, { Tween.out3(it) }, { u ->
            v.alpha = u
            v.translationX = dx * (1f - u)
            v.translationY = dy * (1f - u)
        })
    }

    private fun stat(label: String, value: String, key: String): View {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        val l = txt(label, 9f, Palette.MUTED, true)
        l.letterSpacing = 0.15f
        col.addView(l)
        val v = txt(value, 16f, Color.WHITE, true)
        v.maxLines = 1
        v.ellipsize = TextUtils.TruncateAt.END
        vals[key] = v
        col.addView(v)
        return col
    }

    private fun select(i: Int) {
        pick = i
        pickPkg = games[i].activityInfo.packageName
        for (j in cards.indices) cards[j].on = j == i
        vals["name"]?.text = games[i].loadLabel(packageManager).toString()
    }

    private fun chooseTheme(i: Int) {
        getSharedPreferences("gs", MODE_PRIVATE).edit().putInt("theme", i).apply()
        build(false)
    }

    private fun build(anim: Boolean) {
        cards.clear()
        vals.clear()
        val pm = packageManager
        val kit = OverlayKit(this)
        val a = Palette.a(this)
        val b = Palette.b(this)
        val root = FrameLayout(this)
        root.addView(LiveBg(this), FrameLayout.LayoutParams(MATCH, MATCH))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL

        // sidebar
        val side = LinearLayout(this)
        side.orientation = LinearLayout.VERTICAL
        side.gravity = Gravity.CENTER_HORIZONTAL
        side.setBackgroundColor(0x40000000)
        side.setPadding(dp(14), dp(18), dp(14), dp(16))
        side.addView(LogoView(this), LinearLayout.LayoutParams(dp(56), dp(56)))
        side.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        val dnd = IconCircle(this, 0, focusOn)
        dnd.setOnClickListener { toggleFocus() }
        side.addView(dnd, LinearLayout.LayoutParams(dp(48), dp(48)))
        val fk = txt("Fokus", 9f, Palette.MUTED)
        fk.gravity = Gravity.CENTER
        side.addView(fk, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(4) })
        row.addView(side, LinearLayout.LayoutParams(WRAP, MATCH))

        // kolom utama
        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setPadding(dp(22), dp(14), dp(22), dp(12))

        val header = LinearLayout(this)
        header.gravity = Gravity.CENTER_VERTICAL
        val titles = LinearLayout(this)
        titles.orientation = LinearLayout.VERTICAL
        val t1 = txt("FINZ GAME SPACE", 11f, Color.WHITE, true)
        t1.letterSpacing = 0.3f
        t1.paint.shader = LinearGradient(
            0f, 0f, t1.paint.measureText(t1.text.toString()), 0f, a, b, Shader.TileMode.CLAMP
        )
        titles.addView(t1)
        titles.addView(txt("Pilih Game", 24f, Color.WHITE, true))
        header.addView(titles, LinearLayout.LayoutParams(0, WRAP, 1f))

        fun pill(label: String, action: () -> Unit): View {
            val t = txt(label, 12f, Color.WHITE, true)
            t.gravity = Gravity.CENTER
            t.background = kit.box(0xFF140F2E.toInt(), 14, Palette.alpha(a, 200), 1)
            t.setPadding(dp(14), dp(8), dp(14), dp(8))
            t.setOnClickListener { action() }
            return t
        }
        header.addView(
            pill("Stream") { startActivity(Intent(this, StreamActivity::class.java)) },
            LinearLayout.LayoutParams(WRAP, WRAP).apply { rightMargin = dp(8) }
        )
        header.addView(
            pill("Sensi") { startActivity(Intent(this, SensiActivity::class.java)) },
            LinearLayout.LayoutParams(WRAP, WRAP).apply { rightMargin = dp(8) }
        )
        header.addView(
            pill("Izin") { startActivity(Intent(this, PermActivity::class.java)) },
            LinearLayout.LayoutParams(WRAP, WRAP).apply { rightMargin = dp(10) }
        )

        val curTheme = Palette.idx(this)
        for (i in 0..3) {
            val dot = View(this)
            val g = GradientDrawable(
                GradientDrawable.Orientation.TL_BR, intArrayOf(Palette.A[i], Palette.B[i])
            )
            g.shape = GradientDrawable.OVAL
            if (i == curTheme) g.setStroke(dp(2), Color.WHITE)
            dot.background = g
            dot.setOnClickListener { chooseTheme(i) }
            header.addView(dot, LinearLayout.LayoutParams(dp(22), dp(22)).apply { leftMargin = dp(10) })
        }
        main.addView(header, LinearLayout.LayoutParams(MATCH, WRAP))

        // kartu game
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        hs.overScrollMode = View.OVER_SCROLL_NEVER
        val rowC = LinearLayout(this)
        rowC.orientation = LinearLayout.HORIZONTAL
        rowC.setPadding(dp(4), dp(10), dp(4), dp(10))
        games.forEachIndexed { i, g ->
            val v = GameCardView(this, g.loadIcon(pm), g.loadLabel(pm).toString())
            v.on = i == pick
            v.setOnClickListener { select(i) }
            cards.add(v)
            rowC.addView(v, LinearLayout.LayoutParams(dp(158), MATCH).apply { rightMargin = dp(12) })
        }
        val add = AddCardView(this)
        add.setOnClickListener { showPicker() }
        rowC.addView(add, LinearLayout.LayoutParams(dp(158), MATCH).apply { rightMargin = dp(12) })
        hs.addView(rowC, FrameLayout.LayoutParams(WRAP, MATCH))
        main.addView(
            hs,
            LinearLayout.LayoutParams(MATCH, 0, 1f).apply {
                topMargin = dp(6)
                bottomMargin = dp(8)
            }
        )

        // bar statistik
        val am = getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val total = max(1L, mi.totalMem / 1048576)
        val used = total - mi.availMem / 1048576
        val bi = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = (bi?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val level = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = bi?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val name = games.getOrNull(pick)?.loadLabel(pm)?.toString() ?: "-"

        val bar = LinearLayout(this)
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(16), dp(10), dp(12), dp(10))
        val bbg = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(0xCC1A1048.toInt(), 0xCC0A0620.toInt())
        )
        bbg.cornerRadius = dp(18).toFloat()
        bbg.setStroke(dp(1), Palette.alpha(a, 140))
        bar.background = bbg
        bar.addView(stat("GAME", name, "name"), LinearLayout.LayoutParams(0, WRAP, 1.6f))
        bar.addView(stat("SUHU", "%.1f°C".format(temp), "suhu"), LinearLayout.LayoutParams(0, WRAP, 1f))
        bar.addView(stat("RAM", "${used * 100 / total}%", "ram"), LinearLayout.LayoutParams(0, WRAP, 1f))
        bar.addView(
            stat("BATERAI", "${level * 100 / max(scale, 1)}%", "bat"),
            LinearLayout.LayoutParams(0, WRAP, 1f)
        )
        bar.addView(
            stat("CPU", "${Runtime.getRuntime().availableProcessors()} core", "cpu"),
            LinearLayout.LayoutParams(0, WRAP, 1f)
        )
        val go = PulseButton(this, "MAIN")
        go.setOnClickListener { launchGame() }
        bar.addView(go, LinearLayout.LayoutParams(dp(156), dp(62)).apply { leftMargin = dp(4) })
        main.addView(bar, LinearLayout.LayoutParams(MATCH, WRAP))

        row.addView(main, LinearLayout.LayoutParams(0, MATCH, 1f))
        root.addView(row, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(root)

        if (anim) {
            root.alpha = 0f
            Tween.run(root, 0, 520, { Tween.inOut(it) }, { root.alpha = it })
            intro(side, 80, -dp(30).toFloat(), 0f)
            intro(header, 160, 0f, -dp(18).toFloat())
            for (i in cards.indices) intro(cards[i], 240L + 80L * i, dp(40).toFloat(), 0f)
            intro(add, 240L + 80L * cards.size, dp(40).toFloat(), 0f)
            intro(bar, 400, 0f, dp(40).toFloat())
        }
    }

    private fun refreshStats() {
        val am = getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val total = max(1L, mi.totalMem / 1048576)
        val used = total - mi.availMem / 1048576
        val bi = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = (bi?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val level = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = bi?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        vals["suhu"]?.text = "%.1f°C".format(temp)
        vals["ram"]?.text = "${used * 100 / total}%"
        vals["bat"]?.text = "${level * 100 / max(scale, 1)}%"
    }

    private fun showPicker() {
        val pm = packageManager
        val all = GameStore.allApps(this)
        val current = games.map { it.activityInfo.packageName }.toSet()
        val names = all.map { it.loadLabel(pm).toString() }.toTypedArray()
        val checked = BooleanArray(all.size) { current.contains(all[it].activityInfo.packageName) }
        AlertDialog.Builder(this)
            .setTitle("Pilih game")
            .setMultiChoiceItems(names, checked) { _, i, c -> checked[i] = c }
            .setPositiveButton("Simpan") { _, _ ->
                val picked = all.filterIndexed { i, _ -> checked[i] }
                    .map { it.activityInfo.packageName }
                GameStore.save(this, picked)
                games = GameStore.saved(this)
                pick = 0
                pickPkg = games.getOrNull(0)?.activityInfo?.packageName ?: ""
                build(false)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun launchGame() {
        val g = games.getOrNull(pick)
        if (g == null) {
            Toast.makeText(this, "Tambah game dulu lewat kartu Tambah Game", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan 'Tampil di atas aplikasi lain' (menu Izin), lalu tekan MAIN lagi",
                Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
            return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
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
        build(false)
    }
}
