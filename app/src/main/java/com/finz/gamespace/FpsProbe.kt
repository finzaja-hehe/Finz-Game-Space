package com.finz.gamespace

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.github.muntashirakon.adb.AdbStream
import java.util.regex.Pattern

object FpsServer {

    private val pat = Pattern.compile("u0 ([A-Za-z0-9_.]+)/")

    private fun sh(cmd: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val out = p.inputStream.bufferedReader().readText()
            p.waitFor()
            out
        } catch (e: Throwable) {
            ""
        }
    }

    private fun topPkg(): String {
        val t = sh("dumpsys activity activities | grep -E 'topResumedActivity|mResumedActivity' | head -3")
        val m = pat.matcher(t)
        return if (m.find()) (m.group(1) ?: "") else ""
    }

    private fun layersOf(pkg: String): List<String> {
        val out = ArrayList<String>()
        for (ln in sh("dumpsys SurfaceFlinger --list").lines()) {
            val s = ln.trim()
            if (s.isNotEmpty() && s.contains(pkg)) out.add(s)
        }
        return out
    }

    private fun quote(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    private fun fpsOf(layer: String): Int {
        val t = sh("dumpsys SurfaceFlinger --latency " + quote(layer))
        val now = System.nanoTime()
        var n = 0
        var first = true
        for (ln in t.lines()) {
            if (first) {
                first = false
                continue
            }
            val q = ln.trim().split(Regex("\\s+"))
            if (q.size < 3) continue
            val v = q[1].toLongOrNull() ?: continue
            if (v <= 0L || v > 9000000000000000000L) continue
            if (v > now - 1000000000L && v <= now + 100000000L) n++
        }
        return n
    }

    private fun detect(pkg: String): String {
        var best = ""
        var bestN = 0
        var tried = 0
        for (l in layersOf(pkg)) {
            if (tried >= 5) break
            tried++
            val n = fpsOf(l)
            if (n > bestN) {
                bestN = n
                best = l
            }
        }
        return best
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val t = Thread {
            try {
                while (System.`in`.read() >= 0) {
                }
            } catch (e: Throwable) {
            }
            System.exit(0)
        }
        t.isDaemon = true
        t.start()
        System.out.println("READY")
        System.out.flush()
        var pkg = ""
        var layer = ""
        var zeros = 0
        var lastPkg = 0L
        var lastDet = 0L
        while (true) {
            val now = System.nanoTime()
            if (now - lastPkg > 3000000000L) {
                val p = topPkg()
                if (p.isNotEmpty() && p != pkg) {
                    pkg = p
                    layer = ""
                }
                lastPkg = now
            }
            if (pkg.isNotEmpty() && (layer.isEmpty() || zeros >= 3 || now - lastDet > 20000000000L)) {
                layer = detect(pkg)
                lastDet = now
                zeros = 0
            }
            val f = if (layer.isEmpty()) -1 else fpsOf(layer)
            if (f <= 0) zeros++ else zeros = 0
            System.out.println("F $f")
            System.out.flush()
            try {
                Thread.sleep(700)
            } catch (e: Throwable) {
                break
            }
        }
    }
}

object FpsClient {

    @Volatile
    var fps = -1

    @Volatile
    var on = false

    private var stream: AdbStream? = null
    private var gen = 0
    private val ui = Handler(Looper.getMainLooper())

    fun start(ctx: Context, cb: (String) -> Unit) {
        if (on) return
        on = true
        fps = -1
        val app = ctx.applicationContext
        val g = ++gen
        Thread {
            try {
                val cmd = "CLASSPATH='" + app.packageCodePath +
                    "' exec app_process /system/bin com.finz.gamespace.FpsServer"
                val st = AdbConn.shell(app, cmd)
                if (g != gen) {
                    try { st.close() } catch (e: Exception) {}
                    return@Thread
                }
                stream = st
                val rd = st.openInputStream().bufferedReader()
                var line = rd.readLine()
                while (line != null && g == gen) {
                    if (line.startsWith("F ")) fps = line.substring(2).trim().toIntOrNull() ?: -1
                    line = rd.readLine()
                }
            } catch (e: Throwable) {
                ui.post { cb("FPS butuh ADB tersambung: " + (e.message ?: e.toString())) }
            }
            if (g == gen) {
                on = false
                fps = -1
            }
        }.start()
    }

    fun stop() {
        gen++
        on = false
        fps = -1
        try { stream?.close() } catch (e: Exception) {}
        stream = null
    }
}
