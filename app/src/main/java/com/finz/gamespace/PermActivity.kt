package com.finz.gamespace

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PermActivity : AppCompatActivity() {

    private lateinit var kit: OverlayKit
    private lateinit var body: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        kit = OverlayKit(this)
        window.setBackgroundDrawable(ColorDrawable(Palette.BG))
        window.statusBarColor = Palette.BG
        window.navigationBarColor = Palette.BG
        val sv = ScrollView(this)
        body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(20), dp(16), dp(20), dp(24))
        sv.addView(body)
        setContentView(sv)
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun open(i: Intent) {
        try {
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak bisa membuka pengaturan itu", Toast.LENGTH_LONG).show()
        }
    }

    private fun row(title: String, use: String, status: Int, label: String, action: (() -> Unit)?) {
        val c = LinearLayout(this)
        c.gravity = Gravity.CENTER_VERTICAL
        c.background = kit.box(0xFF140C34.toInt(), 16, Palette.alpha(kit.accent, 110), 1)
        c.setPadding(dp(14), dp(12), dp(14), dp(12))
        val left = LinearLayout(this)
        left.orientation = LinearLayout.VERTICAL
        left.addView(kit.tx(title, 14f, Color.WHITE, true))
        left.addView(
            kit.tx(use, 11f, kit.muted),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3) }
        )
        c.addView(left, LinearLayout.LayoutParams(0, -2, 1f))
        val right = LinearLayout(this)
        right.orientation = LinearLayout.VERTICAL
        right.gravity = Gravity.CENTER_HORIZONTAL
        val chip = kit.tx(
            when (status) {
                1 -> "AKTIF"
                0 -> "BELUM"
                else -> "OTOMATIS"
            }, 10f, if (status == 2) Color.WHITE else Color.BLACK, true
        )
        chip.gravity = Gravity.CENTER
        chip.background = kit.box(
            when (status) {
                1 -> 0xFF22E58B.toInt()
                0 -> 0xFFFF5B5B.toInt()
                else -> 0xFF3A3470.toInt()
            }, 8
        )
        chip.setPadding(dp(10), dp(3), dp(10), dp(3))
        right.addView(chip)
        if (action != null) {
            val b = kit.tx(label, 12f, Color.WHITE, true)
            b.gravity = Gravity.CENTER
            b.background = if (status == 0) kit.grad(12) else kit.box(0xFF1F2430.toInt(), 12, kit.accent, 1)
            b.setPadding(dp(14), dp(8), dp(14), dp(8))
            b.setOnClickListener { action() }
            right.addView(b, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
        }
        c.addView(right)
        body.addView(c, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
    }

    private fun build() {
        body.removeAllViews()
        body.addView(kit.tx("Izin Aplikasi", 24f, Color.WHITE, true))
        body.addView(
            kit.tx(
                "Izinkan hanya yang kamu butuhkan. Tidak ada tombol izinkan semua, " +
                    "dan tiap izin dijelaskan fungsinya.",
                12f, kit.muted
            ),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(4) }
        )

        val nm = getSystemService(NotificationManager::class.java)
        val notifOk = if (Build.VERSION.SDK_INT >= 33) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            nm.areNotificationsEnabled()
        }
        row(
            "Notifikasi",
            "Dipakai untuk: pairing ADB (balas kode di notifikasi), layanan overlay, hasil foto dan " +
                "rekam layar, serta pengingat.",
            if (notifOk) 1 else 0, if (notifOk) "Kelola" else "Izinkan"
        ) {
            if (Build.VERSION.SDK_INT >= 33 && !notifOk) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 21)
            } else {
                open(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                )
            }
        }

        val ovOk = Settings.canDrawOverlays(this)
        row(
            "Tampil di atas aplikasi lain",
            "Wajib untuk overlay: dock, bubble logo, crosshair, keymap, dan layer stream.",
            if (ovOk) 1 else 0, if (ovOk) "Kelola" else "Izinkan"
        ) {
            open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }

        val wrOk = Settings.System.canWrite(this)
        row(
            "Ubah pengaturan sistem",
            "Dipakai untuk: slider kecerahan, Hemat Layar, dan Rotasi. Lewati kalau tidak memakai fitur ini.",
            if (wrOk) 1 else 0, if (wrOk) "Kelola" else "Izinkan"
        ) {
            open(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
        }

        val dndOk = nm.isNotificationPolicyAccessGranted
        row(
            "Jangan Ganggu (DND)",
            "Dipakai untuk: tombol DND, Mode Game, dan Mode Stream. Lewati kalau tidak memakainya.",
            if (dndOk) 1 else 0, if (dndOk) "Kelola" else "Izinkan"
        ) {
            open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }

        val pm = getSystemService(PowerManager::class.java)
        val batOk = pm.isIgnoringBatteryOptimizations(packageName)
        row(
            "Tanpa batasan baterai (opsional)",
            "Agar overlay tidak dimatikan sistem saat bermain lama. Boleh dilewati.",
            if (batOk) 1 else 0, if (batOk) "Kelola" else "Atur"
        ) {
            open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }

        val adbOk = AdbLink.paired(this)
        row(
            "Debugging nirkabel",
            "Bukan izin Android biasa. Dipakai untuk: keymap, foto dan rekam layar, FPS. " +
                "Aktifkan di Opsi pengembang, lalu pairing lewat menu Keymap. " +
                (if (InputBackend.ready) "Saat ini terhubung." else "Saat ini belum terhubung."),
            if (adbOk) 1 else 0, "Buka Opsi Pengembang"
        ) {
            open(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }

        row("Internet", "Dipakai untuk: ping, widget web stream, dan pairing. Diberikan otomatis.", 2, "", null)
        row("Getar", "Dipakai untuk: alarm panas dan pengingat istirahat. Diberikan otomatis.", 2, "", null)
        row("Layanan latar belakang", "Menjaga overlay tetap berjalan. Diberikan otomatis.", 2, "", null)
        row("Senter", "Tombol Senter di dock tidak butuh izin kamera.", 2, "", null)
        body.addView(View(this), LinearLayout.LayoutParams(-1, dp(12)))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 21 && grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
            open(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        }
        build()
    }
}
