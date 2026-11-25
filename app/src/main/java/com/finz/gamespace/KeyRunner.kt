package com.finz.gamespace

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlin.math.min

object KeyRunner {

    private class Sw(val id: Int) {
        var released = false
        var arrived = false
    }

    private val ui = Handler(Looper.getMainLooper())
    private val held = HashSet<Int>()
    private val used = HashSet<Int>()
    private val holds = HashMap<Int, IntArray>()
    private val swipes = HashMap<Int, Sw>()
    private val joys = HashMap<Int, Int>()
    private var tapN = 0

    private fun alloc(): Int {
        for (i in 1..30) {
            if (used.add(i)) return i
        }
        return -1
    }

    private fun free(id: Int) {
        used.remove(id)
    }

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
        ui.removeCallbacksAndMessages(null)
        for (id in used.toList()) upp(id)
        holds.clear()
        swipes.clear()
        joys.clear()
        used.clear()
        held.clear()
    }

    fun press(c: Context, l: List<KeyComp>, code: Int, isDown: Boolean) {
        val r = KeyMapStore.real(c)
        for (i in l.indices) {
            val k = l[i]
            when (k.type) {
                KeyComp.TOUCH ->
                    if (isDown && k.keys[0] == code) tapAt(k.pts[0] * r[0], k.pts[1] * r[1])
                KeyComp.HOLD, KeyComp.GROUP ->
                    if (k.keys[0] == code) {
                        if (isDown) startHold(i, k, r) else endHold(i)
                    }
                KeyComp.SWIPE ->
                    if (k.keys[0] == code) {
                        if (isDown) swipeStart(i, k, r) else swipeEnd(i)
                    }
                KeyComp.JOY ->
                    if (k.keys.contains(code)) joystick(i, k, r)
                else -> {}
            }
        }
    }

    private fun tapAt(x: Float, y: Float) {
        val id = alloc()
        if (id < 0) return
        tapN++
        dn(id, x, y)
        ui.postDelayed({
            upp(id)
            free(id)
        }, 40)
    }

    private fun startHold(i: Int, k: KeyComp, r: IntArray) {
        if (holds.containsKey(i)) return
        val n = k.pts.size / 2
        val ids = IntArray(n)
        for (j in 0 until n) {
            val id = alloc()
            if (id < 0) {
                for (q in 0 until j) {
                    upp(ids[q])
                    free(ids[q])
                }
                return
            }
            ids[j] = id
            dn(id, k.pts[j * 2] * r[0], k.pts[j * 2 + 1] * r[1])
        }
        holds[i] = ids
    }

    private fun endHold(i: Int) {
        val ids = holds.remove(i) ?: return
        for (id in ids) {
            upp(id)
            free(id)
        }
    }

    private fun swipeStart(i: Int, k: KeyComp, r: IntArray) {
        if (swipes.containsKey(i)) return
        val id = alloc()
        if (id < 0) return
        val sw = Sw(id)
        swipes[i] = sw
        val x1 = k.pts[0] * r[0]
        val y1 = k.pts[1] * r[1]
        val x2 = k.pts[2] * r[0]
        val y2 = k.pts[3] * r[1]
        dn(id, x1, y1)
        val n = 8
        for (s in 1..n) {
            ui.postDelayed({
                if (swipes[i] === sw) {
                    mv(id, x1 + (x2 - x1) * s / n, y1 + (y2 - y1) * s / n)
                }
            }, 20L * s)
        }
        ui.postDelayed({
            if (swipes[i] === sw) {
                sw.arrived = true
                if (sw.released) finishSwipe(i, sw)
            }
        }, 20L * n + 10)
    }

    private fun swipeEnd(i: Int) {
        val sw = swipes[i] ?: return
        sw.released = true
        if (sw.arrived) finishSwipe(i, sw)
    }

    private fun finishSwipe(i: Int, sw: Sw) {
        upp(sw.id)
        free(sw.id)
        swipes.remove(i)
    }

    private fun joystick(i: Int, k: KeyComp, r: IntArray) {
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
            val old = joys.remove(i)
            if (old != null) {
                upp(old)
                free(old)
            }
            return
        }
        val n = if (dx != 0f && dy != 0f) 0.7071f else 1f
        var id = joys[i]
        if (id == null) {
            val a = alloc()
            if (a < 0) return
            joys[i] = a
            id = a
            dn(a, cx, cy)
        }
        mv(id, cx + dx * n * rad, cy + dy * n * rad)
    }
}
