package com.finz.gamespace

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import rikka.shizuku.Shizuku

object InputBackend {

    const val ACC = 0
    const val SHIZUKU = 1
    const val ROOT = 2
    const val ADB = 3

    @Volatile
    var mode = ACC
        private set

    @Volatile
    var ready = false
        private set

    @Volatile
    var lastMsg = ""

    @Volatile
    var kbdNote = ""

    private var proc: Process? = null
    private var out: OutputStream? = null
    private var closer: (() -> Unit)? = null
    private var gen = 0
    private var nid = 0
    private val io = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())

    fun name(m: Int): String = when (m) {
        SHIZUKU -> "Shizuku"
        ROOT -> "Root"
        ADB -> "Debugging nirkabel"
        else -> "Mati"
    }

    fun statusText(): String =
        if (ready) "Mode aktif: ${name(mode)}" else "Mode: mati"

    fun stop() {
        ready = false
        mode = ACC
        KeyRouter.kbdCount = -1
        try { out?.close() } catch (e: Exception) {}
        try { proc?.destroy() } catch (e: Exception) {}
        try { closer?.invoke() } catch (e: Exception) {}
        out = null
        proc = null
        closer = null
    }

    fun start(ctx: Context, m: Int, cb: (Boolean, String) -> Unit) {
        val app = ctx.applicationContext
        io.execute {
            stop()
            val msg = try {
                launch(app, m)
            } catch (e: Throwable) {
                "Gagal: ${e.message}"
            }
            val ok = ready
            lastMsg = msg
            ui.post { cb(ok, msg) }
        }
    }

    private fun launch(app: Context, m: Int): String {
        KeyRouter.attach(app)
        kbdNote = ""
        val cmd = "CLASSPATH='" + app.packageCodePath +
            "' exec app_process /system/bin com.finz.gamespace.InputServer"
        val inp: InputStream
        if (m == ADB) {
            val st = AdbConn.shell(app, cmd)
            inp = st.openInputStream()
            out = st.openOutputStream()
            closer = { try { st.close() } catch (e: Exception) {} }
        } else {
            val p: Process
            if (m == SHIZUKU) {
                if (!Shizuku.pingBinder()) {
                    return "Shizuku belum berjalan. Buka aplikasi Shizuku dan nyalakan dulu."
                }
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(100)
                    return "Izinkan di jendela Shizuku, lalu pilih SHIZUKU lagi"
                }
                val mt = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java, Array<String>::class.java, String::class.java
                )
                mt.isAccessible = true
                p = mt.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            } else {
                p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            }
            proc = p
            out = p.outputStream
            inp = p.inputStream
            Thread {
                try { p.errorStream.bufferedReader().readText() } catch (e: Exception) {}
            }.start()
        }
        val g = ++gen
        val result = AtomicReference<String?>(null)
        val latch = CountDownLatch(1)
        Thread {
            try {
                val rd = inp.bufferedReader()
                var line = rd.readLine()
                while (line != null) {
                    if (result.get() == null) {
                        if (line.contains("READY")) {
                            result.set("READY")
                            latch.countDown()
                        } else if (line.startsWith("ERR")) {
                            result.set(line)
                            latch.countDown()
                        }
                    } else if (line.startsWith("K ")) {
                        val q = line.split(" ")
                        if (q.size >= 3) {
                            val c = q[1].toIntOrNull()
                            if (c != null) KeyRouter.onKey(c, q[2] == "1")
                        }
                    } else if (line.startsWith("KBD ")) {
                        KeyRouter.kbdCount = line.substring(4).trim().toIntOrNull() ?: -1
                    } else if (line.startsWith("KERR")) {
                        kbdNote = line.substring(4).trim()
                    }
                    line = rd.readLine()
                }
            } catch (e: Exception) {
            }
            latch.countDown()
            if (g == gen) ready = false
        }.start()
        val wait = when (m) {
            ROOT -> 25L
            ADB -> 12L
            else -> 8L
        }
        latch.await(wait, TimeUnit.SECONDS)
        val f = result.get()
        if (f == "READY") {
            ready = true
            mode = m
            return "Mode ${name(m)} aktif"
        }
        stop()
        val why = if (f.isNullOrEmpty()) {
            if (m == ROOT) " (HP belum di-root atau izin su ditolak)" else ""
        } else ": $f"
        return "Gagal memulai ${name(m)}$why"
    }

    private fun send(s: String) {
        io.execute {
            try {
                out?.write((s + "\n").toByteArray())
                out?.flush()
            } catch (e: Exception) {
                ready = false
            }
        }
    }

    fun down(id: Int, x: Float, y: Float) = send("d $id ${x.toInt()} ${y.toInt()}")

    fun move(id: Int, x: Float, y: Float) = send("m $id ${x.toInt()} ${y.toInt()}")

    fun up(id: Int) = send("u $id")

    fun tap(x: Float, y: Float) {
        val id = 6 + (nid++ % 4)
        down(id, x, y)
        ui.postDelayed({ up(id) }, 40)
    }
}
