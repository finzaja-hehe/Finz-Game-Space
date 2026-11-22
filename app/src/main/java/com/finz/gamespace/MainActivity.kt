package com.finz.gamespace

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class MainActivity : BaseUi() {

    private var pick = 0
    private var focusOn = false
    private var games: List<ResolveInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.statusBarColor = bg
        window.navigationBarColor = bg
        themeIndex = getSharedPreferences("gs", Context.MODE_PRIVATE)
            .getInt("theme", 0).coerceIn(0, 3)
    }

    override fun onResume() {
        super.onResume()
        val nm = getSystemService(NotificationManager::class.java)
        focusOn = nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        games = GameStore.saved(this)
        buildUi()
    }

    private fun buildUi() {
        if (pick >= games.size) pick = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(bg, mix(accent, 0.18f), bg)
            )
        }
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

        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(4))
        }
        games.forEachIndexed { i, g ->
            row.addView(gameCard(g, i == pick) { pick = i; buildUi() },
                lp(dp(160), MATCH, r = 12))
        }
        row.addView(addCard { showPicker() }, lp(dp(160), MATCH, r = 12))
        scroll.addView(row, FrameLayout.LayoutParams(WRAP, MATCH))
        main.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f).apply {
            topMargin = dp(10)
            bottomMargin = dp(10)
        })

        val name = games.getOrNull(pick)?.loadLabel(packageManager)?.toString() ?: "-"
        main.addView(statsBar(name) { launchGame() }, lp(MATCH, WRAP))

        root.addView(sideBar(focusOn) { toggleFocus() }, LinearLayout.LayoutParams(WRAP, MATCH))
        root.addView(main, LinearLayout.LayoutParams(0, MATCH, 1f))
        setContentView(root)
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
                buildUi()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun changeTheme(i: Int) {
        themeIndex = i
        getSharedPreferences("gs", Context.MODE_PRIVATE).edit().putInt("theme", i).apply()
        buildUi()
    }

    private fun launchGame() {
        val g = games.getOrNull(pick)
        if (g == null) {
            Toast.makeText(this, "Tambah game dulu lewat kartu +", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan 'Tampil di atas aplikasi lain', lalu tekan MAIN lagi",
                Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")))
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
        buildUi()
    }
}
