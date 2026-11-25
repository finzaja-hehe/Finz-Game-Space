package com.finz.gamespace

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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

    @Volatile
    var mouseCount = -1

    @Volatile
    var grabCount = -1

    @Volatile
    var recording = false

    private var recPath = ""
    private var appCtx: Context? = null
    private var pollSent = -1
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
        recording = false
        mouseCount = -1
        grabCount = -1
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

    private fun toast(s: String) {
        val c = appCtx ?: return
        ui.post { Toast.makeText(c, s, Toast.LENGTH_LONG).show() }
    }

    private fun onLine(line: String) {
        val q = line.split(" ")
        when (q[0]) {
            "K" -> if (q.size >= 3) {
                val c = q[1].toIntOrNull()
                if (c != null) KeyRouter.onKey(c, q[2] == "1")
            }
            "R" -> if (q.size >= 3) {
                val a = q[1].toIntOrNull()
                val b = q[2].toIntOrNull()
                if (a != null && b != null) MouseRunner.rel(a, b)
            }
            "W" -> if (q.size >= 2) {
                val a = q[1].toIntOrNull()
                if (a != null) MouseRunner.wheel(a)
            }
            "KBD" -> KeyRouter.kbdCount = q.getOrNull(1)?.toIntOrNull() ?: -1
            "MSE" -> mouseCount = q.getOrNull(1)?.toIntOrNull() ?: -1
            "G" -> grabCount = q.getOrNull(1)?.toIntOrNull() ?: -1
            "KERR" -> kbdNote = line.removePrefix("KERR").trim()
            "SHOT" -> toast("Foto layar tersimpan: " + line.removePrefix("SHOT").trim())
            "SERR" -> toast("Gagal: " + line.removePrefix("SERR").trim())
            "REC" -> {
                if (q.getOrNull(1) == "1") {
                    recording = true
                    recPath = line.removePrefix("REC 1").trim()
                    toast("Merekam layar (maks 3 menit). Ketuk Rekam lagi untuk berhenti.")
                } else {
                    recording = false
                    toast("Rekaman tersimpan: $recPath")
                }
            }
            else -> {}
        }
    }

    private fun launch(app: Context, m: Int): String {
        appCtx = app
        KeyRouter.attach(app)
        kbdNote = ""
        pollSent = -1
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
                    } else {
                        onLine(line)
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
            ui.post {
                setPoll()
                KeyRouter.syncMouse()
            }
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

    fun setPoll() {
        val c = appCtx ?: return
        val ms = when (KeyMapStore.getInt(c, "mpoll", 1)) {
            0 -> 8
            1 -> 4
            2 -> 2
            else -> 1
        }
        if (ms != pollSent && ready) {
            pollSent = ms
            send("p $ms")
        }
    }

    fun grab(on: Boolean) {
        if (!ready) return
        setPoll()
        send(if (on) "g 1" else "g 0")
    }

    fun shot() {
        if (ready) send("s")
    }

    fun recToggle() {
        if (ready) send(if (recording) "e" else "r")
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
