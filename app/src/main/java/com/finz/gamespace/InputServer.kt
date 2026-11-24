package com.finz.gamespace

import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.nio.ByteOrder

object InputServer {

    private val ids = ArrayList<Int>()
    private val xs = HashMap<Int, Float>()
    private val ys = HashMap<Int, Float>()
    private var downTime = 0L
    private var target: Any? = null
    private var inject: Method? = null
    private val outLock = Any()

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

    private fun handle(p: List<String>) {
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

    private fun keyboardDevices(): List<String> {
        val res = ArrayList<String>()
        try {
            val txt = File("/proc/bus/input/devices").readText()
            for (block in txt.split("\n\n")) {
                val h = block.lines().firstOrNull { it.startsWith("H: Handlers=") } ?: continue
                if (!h.contains("kbd")) continue
                for (tok in h.split(" ")) {
                    if (tok.startsWith("event")) res.add("/dev/input/$tok")
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
        return res
    }

    private fun watch(fin: FileInputStream) {
        val t = Thread {
            try {
                val size = if (android.os.Process.is64Bit()) 24 else 16
                val buf = ByteArray(size)
                val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
                while (true) {
                    var n = 0
                    while (n < size) {
                        val r = fin.read(buf, n, size - n)
                        if (r < 0) return@Thread
                        n += r
                    }
                    val off = size - 8
                    val type = bb.getShort(off).toInt() and 0xFFFF
                    val code = bb.getShort(off + 2).toInt() and 0xFFFF
                    val value = bb.getInt(off + 4)
                    if (type == 1 && (value == 0 || value == 1)) emit("K $code $value")
                }
            } catch (e: Throwable) {
                emit("KERR ${e.message}")
            }
        }
        t.isDaemon = true
        t.start()
    }

    private fun startKeyboards() {
        var ok = 0
        for (p in keyboardDevices()) {
            try {
                watch(FileInputStream(p))
                ok++
            } catch (e: Throwable) {
                emit("KERR $p ${e.message}")
            }
        }
        emit("KBD $ok")
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
        startKeyboards()
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
