package com.finz.gamespace

import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

object InputServer {

    private val ids = ArrayList<Int>()
    private val xs = HashMap<Int, Float>()
    private val ys = HashMap<Int, Float>()
    private var downTime = 0L
    private var target: Any? = null
    private var inject: Method? = null

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

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            setup()
            System.out.println("READY")
            System.out.flush()
        } catch (e: Throwable) {
            System.out.println("ERR $e")
            System.out.flush()
            return
        }
        val r = BufferedReader(InputStreamReader(System.`in`))
        while (true) {
            val line = r.readLine() ?: break
            try {
                handle(line.trim().split(" "))
            } catch (e: Throwable) {
                System.err.println("ERR $e")
            }
        }
    }
}
