package com.finz.gamespace

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    private lateinit var info: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        val title = TextView(this).apply {
            text = "Finz Game Space"
            textSize = 24f
        }

        info = TextView(this).apply {
            textSize = 14f
            setPadding(0, pad / 2, 0, pad / 2)
        }

        val focus = SwitchCompat(this).apply {
            text = "Mode Fokus (Jangan Ganggu)"
        }
        focus.setOnClickListener {
            if (!setFocus(focus.isChecked)) focus.isChecked = false
        }

        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val all = pm.queryIntentActivities(launcher, 0)
        val games = all.filter {
            it.activityInfo.applicationInfo.category == ApplicationInfo.CATEGORY_GAME
        }
        val list = (if (games.isEmpty()) all else games)
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        val names = list.map { it.loadLabel(pm).toString() }

        val listView = ListView(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_list_item_1,
                names
            )
            setOnItemClickListener { _, _, pos, _ ->
                val pkg = list[pos].activityInfo.packageName
                pm.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
            }
        }

        root.addView(title)
        root.addView(info)
        root.addView(focus)
        root.addView(listView)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refreshInfo()
    }

    private fun refreshInfo() {
        val am = getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val bm = getSystemService(BatteryManager::class.java)
        val bat = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        info.text = "RAM kosong: ${mi.availMem / 1048576} MB / " +
            "${mi.totalMem / 1048576} MB\nBaterai: $bat%"
    }

    private fun setFocus(on: Boolean): Boolean {
        val nm = getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Izinkan akses Jangan Ganggu dulu", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return false
        }
        nm.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_NONE
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
        return true
    }
}
