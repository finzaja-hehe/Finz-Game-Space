package com.finz.gamespace

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlin.math.min

object KeyRunner {

    private const val JID = 9
    private val ui = Handler(Looper.getMainLooper())
    private val held = HashSet<Int>()
    private val busy = HashSet<Int>()
    private var joyOn = false
    private var tapN = 0

    private fun dn(id: Int, x: Float, y: Float) {
        InputBackend.down(id, x, y)
        DotsOverlay.down(id, x, y)
    }

    private fun mv(id: Int, x: Float, y: Float) {
        InputBackend.move(id, x, y)
        DotsOverlay.move(id, x, y)
    }

    private fun upp(id: Int) {
        InputBackend.up(id)
        DotsOverlay.up(id)
    }

    fun track(code: Int, isDown: Boolean) {
        if (isDown) held.add(code) else held.remove(code)
    }

    fun releaseAll() {
        if (joyOn) {
            upp(JID)
            joyOn = false
        }
        for (i in 0 until 10) upp(10 + i)
        held.clear()
        busy.clear()
    }

    fun press(c: Context, l: List<KeyComp>, code: Int, isDown: Boolean) {
        val r = KeyMapStore.real(c)
        for (i in l.indices) {
            val k = l[i]
            when (k.type) {
                KeyComp.TOUCH ->
                    if (isDown && k.keys[0] == code) tapAt(k.pts[0] * r[0], k.pts[1] * r[1])
                KeyComp.HOLD ->
                    if (k.keys[0] == code) {
                        val id = 10 + i % 10
                        if (isDown) dn(id, k.pts[0] * r[0], k.pts[1] * r[1]) else upp(id)
                    }
                KeyComp.SWIPE ->
                    if (isDown && k.keys[0] == code) swipe(i, k, r)
                else ->
                    if (k.keys.contains(code)) joystick(k, r)
            }
        }
    }

    private fun tapAt(x: Float, y: Float) {
        val id = 20 + (tapN++ % 4)
        dn(id, x, y)
        ui.postDelayed({ upp(id) }, 40)
    }

    private fun swipe(i: Int, k: KeyComp, r: IntArray) {
        if (!busy.add(k.keys[0])) return
        val id = 10 + i % 10
        val x1 = k.pts[0] * r[0]
        val y1 = k.pts[1] * r[1]
        val x2 = k.pts[2] * r[0]
        val y2 = k.pts[3] * r[1]
        dn(id, x1, y1)
        val n = 6
        for (s in 1..n) {
            ui.postDelayed({
                mv(id, x1 + (x2 - x1) * s / n, y1 + (y2 - y1) * s / n)
            }, 25L * s)
        }
        ui.postDelayed({
            upp(id)
            busy.remove(k.keys[0])
        }, 25L * (n + 1))
    }

    private fun joystick(k: KeyComp, r: IntArray) {
        var dx = 0f
        var dy = 0f
        if (held.contains(k.keys[0])) dy -= 1f
        if (held.contains(k.keys[1])) dx -= 1f
        if (held.contains(k.keys[2])) dy += 1f
        if (held.contains(k.keys[3])) dx += 1f
        val cx = k.pts[0] * r[0]
        val cy = k.pts[1] * r[1]
        val rad = k.pts[2] * min(r[0], r[1])
        if (dx == 0f && dy == 0f) {
            if (joyOn) {
                upp(JID)
                joyOn = false
            }
            return
        }
        val n = if (dx != 0f && dy != 0f) 0.7071f else 1f
        if (!joyOn) {
            dn(JID, cx, cy)
            joyOn = true
        }
        mv(JID, cx + dx * n * rad, cy + dy * n * rad)
    }
}
