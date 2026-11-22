package com.finz.gamespace

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast

class OverlayActions(private val ctx: Context, private val handler: Handler) {

    private var lastRx = 0L
    private var lastT = 0L
    private var netTxt = "-"

    private fun mem(): ActivityManager.MemoryInfo {
        val mi = ActivityManager.MemoryInfo()
        ctx.getSystemService(ActivityManager::class.java).getMemoryInfo(mi)
        return mi
    }

    fun ramPct(): Int {
        val mi = mem()
        return (100 - mi.availMem * 100 / mi.totalMem).toInt()
    }

    private fun battery() =
        ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    fun tempC(): Float =
        (battery()?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

    fun batPct(): Int {
        val b = battery()
        val level = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return level * 100 / scale
    }

    fun netText(): String {
        val rx = TrafficStats.getTotalRxBytes()
        val now = SystemClock.elapsedRealtime()
        if (lastT > 0 && now > lastT && rx >= lastRx) {
            val kb = (rx - lastRx) * 1000 / (now - lastT) / 1024
            netTxt = if (kb >= 1024) "%.1f MB/s".format(kb / 1024f) else "$kb KB/s"
        }
        lastRx = rx
        lastT = now
        return netTxt
    }

    fun setBrightness(v: Int) {
        if (!Settings.System.canWrite(ctx)) {
            Toast.makeText(ctx, "Izinkan ubah pengaturan sistem dulu", Toast.LENGTH_LONG).show()
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val cr = ctx.contentResolver
        Settings.System.putInt(
            cr, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, v.coerceIn(5, 255))
    }

    fun dndOn(): Boolean {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        return nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
    }

    fun toggleDnd(): Boolean {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(ctx, "Izinkan akses Jangan Ganggu dulu", Toast.LENGTH_LONG).show()
            ctx.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return dndOn()
        }
        val on = dndOn()
        nm.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_ALL
            else NotificationManager.INTERRUPTION_FILTER_NONE
        )
        return !on
    }

    fun cleanRam() {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val before = mem().availMem / 1048576
        try {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            ctx.packageManager.queryIntentActivities(i, 0)
                .map { it.activityInfo.packageName }
                .distinct()
                .filter { it != ctx.packageName }
                .forEach { am.killBackgroundProcesses(it) }
        } catch (e: Exception) {
            Toast.makeText(ctx, "Izin belum aktif", Toast.LENGTH_LONG).show()
            return
        }
        handler.postDelayed({
            val gain = mem().availMem / 1048576 - before
            Toast.makeText(
                ctx, "RAM kosong +${if (gain > 0) gain else 0} MB", Toast.LENGTH_SHORT
            ).show()
        }, 700)
    }

    fun openSettings() {
        ctx.startActivity(
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
