package com.finz.gamespace

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.widget.Toast

class PairService : Service() {

    private var nsd: NsdManager? = null
    private var listener: NsdManager.DiscoveryListener? = null
    private val ui = Handler(Looper.getMainLooper())

    @Volatile
    private var host: String? = null

    @Volatile
    private var port = 0
    private var busy = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "CODE" -> handleCode(intent)
            "STOP" -> shutdown()
            else -> begin()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopDiscovery()
        super.onDestroy()
    }

    private fun begin() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("pair", "Pairing", NotificationManager.IMPORTANCE_HIGH)
        )
        val n = build("Buka Opsi pengembang > Debugging nirkabel > Pasangkan perangkat dengan kode, lalu tulis kodenya di sini.")
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(3, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(3, n)
        }
        if (!nm.areNotificationsEnabled()) {
            Toast.makeText(this, "Aktifkan notifikasi untuk Finz Game Space", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        discover()
    }

    private fun build(text: String): Notification {
        val ri = RemoteInput.Builder("code").setLabel("Kode 6 digit").build()
        val flag = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        val pi = PendingIntent.getService(
            this, 1, Intent(this, PairService::class.java).setAction("CODE"), flag
        )
        val act = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_edit), "Tulis kode", pi
        ).addRemoteInput(ri).build()
        val stop = PendingIntent.getService(
            this, 2, Intent(this, PairService::class.java).setAction("STOP"),
            PendingIntent.FLAG_IMMUTABLE
        )
        val act2 = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
            "Batal", stop
        ).build()
        return Notification.Builder(this, "pair")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pairing Finz Game Space")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setOnlyAlertOnce(true)
            .addAction(act)
            .addAction(act2)
            .build()
    }

    private fun say(text: String) {
        getSystemService(NotificationManager::class.java).notify(3, build(text))
    }

    private fun discover() {
        val m = getSystemService(Context.NSD_SERVICE) as NsdManager
        nsd = m
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(t: String) {}
            override fun onDiscoveryStopped(t: String) {}
            override fun onStartDiscoveryFailed(t: String, e: Int) {
                ui.post { say("Gagal mencari layanan pairing. Pastikan Wi-Fi nyala.") }
            }
            override fun onStopDiscoveryFailed(t: String, e: Int) {}
            override fun onServiceLost(s: NsdServiceInfo) {
                host = null
                port = 0
                ui.post { say("Jendela pairing tertutup. Buka lagi 'Pasangkan perangkat dengan kode'.") }
            }
            override fun onServiceFound(s: NsdServiceInfo) {
                resolve(s, 0)
            }
        }
        listener = l
        m.discoverServices("_adb-tls-pairing._tcp", NsdManager.PROTOCOL_DNS_SD, l)
    }

    private fun resolve(s: NsdServiceInfo, tries: Int) {
        val m = nsd ?: return
        m.resolveService(s, object : NsdManager.ResolveListener {
            override fun onResolveFailed(i: NsdServiceInfo, e: Int) {
                if (tries < 6) ui.postDelayed({ resolve(s, tries + 1) }, 600)
            }
            override fun onServiceResolved(i: NsdServiceInfo) {
                host = i.host?.hostAddress
                port = i.port
                ui.post { say("Siap (port ${i.port}). Tulis kode 6 digit dari jendela pairing.") }
            }
        })
    }

    private fun stopDiscovery() {
        try {
            listener?.let { nsd?.stopServiceDiscovery(it) }
        } catch (e: Exception) {
        }
        listener = null
    }

    private fun handleCode(intent: Intent) {
        val r = RemoteInput.getResultsFromIntent(intent)
        val code = r?.getCharSequence("code")?.toString()?.filter { it.isDigit() } ?: ""
        if (code.length != 6) {
            say("Kode harus 6 digit. Tulis lagi.")
            return
        }
        val p = port
        if (p <= 0) {
            say("Port belum ditemukan. Buka 'Pasangkan perangkat dengan kode', lalu tulis kodenya lagi.")
            return
        }
        if (busy) return
        busy = true
        say("Memasangkan...")
        val h = host
        Thread {
            val msg = try {
                AdbLink.pair(applicationContext, h, p, code)
                "Berhasil! Buka menu Keymap lalu pilih ADB."
            } catch (e: Throwable) {
                "Gagal: " + (e.message ?: e.toString())
            }
            ui.post {
                busy = false
                say(msg)
                if (msg.startsWith("Berhasil")) ui.postDelayed({ shutdown() }, 4000)
            }
        }.start()
    }

    private fun shutdown() {
        stopDiscovery()
        stopForeground(Service.STOP_FOREGROUND_DETACH)
        stopSelf()
    }
}
