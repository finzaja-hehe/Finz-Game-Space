package com.finz.gamespace

import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import java.io.BufferedReader
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap

object InputServer {

    private class Rd(val fin: FileInputStream) {
        @Volatile
        var stop = false
    }

    private val ids = ArrayList<Int>()
    private val xs = HashMap<Int, Float>()
    private val ys = HashMap<Int, Float>()
    private var downTime = 0L
    private var target: Any? = null
    private var inject: Method? = null
    private val outLock = Any()
    private val relLock = Any()
    private var accX = 0
    private var accY = 0

    @Volatile
    private var pollMs = 8L
    private val mice = ArrayList<String>()
    private var kbdN = 0
    private val rds = ConcurrentHashMap<String, Rd>()
    private val grabbed = HashSet<String>()

    @Volatile
    private var rec: Process? = null

    private fun emit(s: String) {
        synchronized(outLock) {
            System.out.println(s)
            System.out.flush()
        }
    }

    private fun hook(cls: String) {
        val c = Class.forName(cls)
        val g = c.getDeclaredMethod("getInstance")
        g.isAccessible = true
        target = g.invoke(null)
        inject = c.getMethod("injectInputEvent", InputEvent::class.java, Integer.TYPE)
    }

    private fun setup() {
        try {
            hook("android.hardware.input.InputManager")
        } catch (e: Throwable) {
            hook("android.hardware.input.InputManagerGlobal")
        }
    }

    private fun send(action: Int) {
        val n = ids.size
        val props = Array(n) { i ->
            MotionEvent.PointerProperties().also {
                it.id = ids[i]
                it.toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        val coords = Array(n) { i ->
            MotionEvent.PointerCoords().also {
                it.x = xs[ids[i]] ?: 0f
                it.y = ys[ids[i]] ?: 0f
                it.pressure = 1f
                it.size = 1f
            }
        }
        val now = SystemClock.uptimeMillis()
        val ev = MotionEvent.obtain(
            downTime, now, action, n, props, coords, 0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0
        )
        try {
            inject?.invoke(target, ev, 0)
        } catch (e: Throwable) {
            System.err.println("ERR inject $e")
        }
        ev.recycle()
    }

    private fun touch(p: List<String>) {
        val shift = MotionEvent.ACTION_POINTER_INDEX_SHIFT
        when (p[0]) {
            "d" -> {
                val id = p[1].toInt()
                if (ids.contains(id)) return
                xs[id] = p[2].toFloat()
                ys[id] = p[3].toFloat()
                ids.add(id)
                if (ids.size == 1) {
                    downTime = SystemClock.uptimeMillis()
                    send(MotionEvent.ACTION_DOWN)
                } else {
                    send(MotionEvent.ACTION_POINTER_DOWN or ((ids.size - 1) shl shift))
                }
            }
            "m" -> {
                val id = p[1].toInt()
                if (!ids.contains(id)) return
                xs[id] = p[2].toFloat()
                ys[id] = p[3].toFloat()
                send(MotionEvent.ACTION_MOVE)
            }
            "u" -> {
                val id = p[1].toInt()
                val idx = ids.indexOf(id)
                if (idx < 0) return
                if (ids.size == 1) {
                    send(MotionEvent.ACTION_UP)
                } else {
                    send(MotionEvent.ACTION_POINTER_UP or (idx shl shift))
                }
                ids.removeAt(idx)
                xs.remove(id)
                ys.remove(id)
            }
        }
    }

    private fun handle(p: List<String>) {
        when (p[0]) {
            "p" -> pollMs = (p.getOrNull(1)?.toLongOrNull() ?: 8L).coerceIn(1L, 20L)
            "g" -> emit("G " + grabAll(p.getOrNull(1) == "1"))
            "s" -> Thread { shot() }.start()
            "r" -> startRec()
            "e" -> stopRec()
            else -> touch(p)
        }
    }

    // ---------- foto dan rekam layar ----------
    private fun shot() {
        try {
            val path = "/sdcard/Pictures/FinzShot_" + System.currentTimeMillis() + ".png"
            val pr = Runtime.getRuntime().exec(
                arrayOf("sh", "-c", "mkdir -p /sdcard/Pictures; screencap -p " + path)
            )
            val code = pr.waitFor()
            emit(if (code == 0) "SHOT " + path else "SERR screencap gagal (kode " + code + ")")
        } catch (e: Throwable) {
            emit("SERR " + e.message)
        }
    }

    private fun startRec() {
        if (rec != null) return
        try {
            val path = "/sdcard/Movies/FinzRec_" + System.currentTimeMillis() + ".mp4"
            val pr = Runtime.getRuntime().exec(
                arrayOf(
                    "sh", "-c",
                    "mkdir -p /sdcard/Movies; exec screenrecord --time-limit 180 " + path +
                        " > /dev/null 2>&1"
                )
            )
            rec = pr
            emit("REC 1 " + path)
            Thread {
                try { pr.waitFor() } catch (e: Throwable) {}
                if (rec === pr) rec = null
                emit("REC 0")
            }.start()
        } catch (e: Throwable) {
            emit("SERR " + e.message)
        }
    }

    private fun stopRec() {
        if (rec == null) return
        try {
            Runtime.getRuntime().exec(
                arrayOf("sh", "-c", "pkill -2 screenrecord || killall -2 screenrecord")
            )
        } catch (e: Throwable) {
            rec?.destroy()
        }
    }

    // ---------- keyboard dan mouse ----------
    private fun event(type: Int, code: Int, value: Int) {
        if (type == 1) {
            if ((value == 0 || value == 1) && (code < 256 || code in 272..276)) {
                emit("K $code $value")
            }
        } else if (type == 2) {
            if (code == 0) {
                synchronized(relLock) { accX += value }
            } else if (code == 1) {
                synchronized(relLock) { accY += value }
            } else if (code == 8) {
                emit("W $value")
            }
        }
    }

    private fun loop(rd: Rd) {
        try {
            val size = if (android.os.Process.is64Bit()) 24 else 16
            val buf = ByteArray(size)
            val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
            val pfd = StructPollfd()
            pfd.fd = rd.fin.fd
            pfd.events = OsConstants.POLLIN.toShort()
            val arr = arrayOf(pfd)
            while (!rd.stop) {
                if (Os.poll(arr, 200) <= 0) continue
                val got = rd.fin.read(buf, 0, size)
                if (got != size) break
                val off = size - 8
                val type = bb.getShort(off).toInt() and 0xFFFF
                val code = bb.getShort(off + 2).toInt() and 0xFFFF
                val value = bb.getInt(off + 4)
                event(type, code, value)
            }
        } catch (e: Throwable) {
            emit("KERR " + e.message)
        }
        try { rd.fin.close() } catch (e: Throwable) {}
    }

    private fun watch(path: String): Boolean {
        val fin = try {
            FileInputStream(path)
        } catch (e: Throwable) {
            emit("KERR " + path + " " + e.message)
            return false
        }
        val rd = Rd(fin)
        rds[path] = rd
        val t = Thread { loop(rd) }
        t.isDaemon = true
        t.start()
        return true
    }

    private fun scanDevices(): List<String> {
        val res = LinkedHashSet<String>()
        mice.clear()
        kbdN = 0
        try {
            val txt = File("/proc/bus/input/devices").readText()
            for (block in txt.split("\n\n")) {
                val h = block.lines().firstOrNull { it.startsWith("H: Handlers=") } ?: continue
                val kbd = h.contains("kbd")
                val mouse = h.contains("mouse")
                if (!kbd && !mouse) continue
                for (tok in h.split(" ")) {
                    if (!tok.startsWith("event")) continue
                    val path = "/dev/input/" + tok
                    res.add(path)
                    if (mouse && !mice.contains(path)) mice.add(path)
                    if (kbd) kbdN++
                }
            }
        } catch (e: Throwable) {
        }
        if (res.isEmpty()) {
            val fs = File("/dev/input").listFiles()
            if (fs != null) {
                for (f in fs) {
                    if (f.name.startsWith("event")) res.add(f.path)
                }
            }
        }
        return res.toList()
    }

    private fun evGrab(fd: FileDescriptor): Boolean {
        var ok = false
        try {
            val f = Class.forName("libcore.io.Libcore").getDeclaredField("os")
            f.isAccessible = true
            val os = f.get(null) ?: return false
            for (m in os.javaClass.methods) {
                if (ok || m.name != "ioctlInt") continue
                val pt = m.parameterTypes
                if (pt.size == 2) {
                    m.invoke(os, fd, 0x40044590)
                    ok = true
                } else if (pt.size == 3) {
                    val ref = pt[2].getConstructor(Integer.TYPE).newInstance(1)
                    m.invoke(os, fd, 0x40044590, ref)
                    ok = true
                }
            }
        } catch (e: Throwable) {
            emit("KERR grab " + e.javaClass.simpleName)
        }
        return ok
    }

    private fun grabAll(on: Boolean): Int {
        if (on) {
            for (p in mice) {
                if (grabbed.contains(p)) continue
                val rd = rds[p] ?: continue
                if (evGrab(rd.fin.fd)) grabbed.add(p)
            }
            return grabbed.size
        }
        if (grabbed.isEmpty()) return 0
        val paths = ArrayList(grabbed)
        grabbed.clear()
        for (p in paths) rds[p]?.stop = true
        Thread {
            try { Thread.sleep(400) } catch (e: Throwable) {}
            for (p in paths) watch(p)
        }.start()
        return 0
    }

    private fun startFlush() {
        val t = Thread {
            while (true) {
                try {
                    Thread.sleep(pollMs)
                } catch (e: Throwable) {
                    break
                }
                var dx = 0
                var dy = 0
                synchronized(relLock) {
                    dx = accX
                    dy = accY
                    accX = 0
                    accY = 0
                }
                if (dx != 0 || dy != 0) emit("R $dx $dy")
            }
        }
        t.isDaemon = true
        t.start()
    }

    private fun startInputs() {
        for (p in scanDevices()) watch(p)
        emit("KBD $kbdN")
        emit("MSE ${mice.size}")
        startFlush()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            setup()
            emit("READY")
        } catch (e: Throwable) {
            emit("ERR $e")
            return
        }
        startInputs()
        val r = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            val line = r.readLine() ?: break
            try {
                handle(line.trim().split(" "))
            } catch (e: Throwable) {
                System.err.println("ERR $e")
            }
        }
        System.exit(0)
    }
}
