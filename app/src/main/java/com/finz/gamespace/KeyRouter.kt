package com.finz.gamespace

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

object KeyRouter : EditorCtl {

    @Volatile
    var enabled = true

    @Volatile
    var kbdCount = -1

    @Volatile
    var lastKey = "-"

    private const val JID = 9
    private val ui = Handler(Looper.getMainLooper())
    private var app: Context? = null
    private var wm: WindowManager? = null
    private var editor: KeyMapEditor? = null
    private var list: MutableList<KeyComp> = ArrayList()
    private var cacheKey = ""
    private val held = HashSet<Int>()
    private val busy = HashSet<Int>()
    private var joyOn = false
    private var tapN = 0
    private var addType = -1
    private val pts = ArrayList<FloatArray>()
    private val keys = ArrayList<Int>()

    fun attach(ctx: Context) {
        val c = ctx.applicationContext
        app = c
        wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private fun label(code: Int) = KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")

    private fun say(s: String) {
        val c = app ?: return
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show()
    }

    private fun current(): MutableList<KeyComp> {
        val c = app ?: return list
        val r = KeyMapStore.real(c)
        val key = "global" + (if (r[0] > r[1]) "_L" else "_P")
        if (key != cacheKey) {
            list = KeyMapStore.load(c, key)
            cacheKey = key
            resetFlow()
        }
        return list
    }

    private fun persist() {
        val c = app ?: return
        current()
        KeyMapStore.save(c, cacheKey, list)
    }

    private fun resetFlow() {
        addType = -1
        pts.clear()
        keys.clear()
    }

    private fun redraw() {
        editor?.invalidate()
    }

    // ---------- tombol dari keyboard ----------
    fun onKey(evdev: Int, isDown: Boolean) {
        val code = EvdevMap.toAndroid(evdev)
        if (code == 0) return
        if (isDown) lastKey = label(code)
        ui.post { handle(code, isDown) }
    }

    private fun handle(code: Int, isDown: Boolean) {
        if (isDown) held.add(code) else held.remove(code)
        if (isDown && code == KeyEvent.KEYCODE_F10) {
            if (editor == null) openEditor() else closeEditor()
            return
        }
        if (isDown && code == KeyEvent.KEYCODE_F9) {
            enabled = !enabled
            releaseAll()
            say(if (enabled) "Keymap nyala" else "Keymap mati")
            return
        }
        if (editor != null) {
            if (isDown) editorKey(code)
            return
        }
        if (!enabled) return
        run(code, isDown)
    }

    private fun releaseAll() {
        if (joyOn) {
            InputBackend.up(JID)
            joyOn = false
        }
        for (i in 0 until 10) InputBackend.up(10 + i)
        held.clear()
        busy.clear()
    }

    private fun run(code: Int, isDown: Boolean) {
        val c = app ?: return
        val r = KeyMapStore.real(c)
        val l = current()
        for (i in l.indices) {
            val k = l[i]
            when (k.type) {
                KeyComp.TOUCH ->
                    if (isDown && k.keys[0] == code) tapAt(k.pts[0] * r[0], k.pts[1] * r[1])
                KeyComp.HOLD ->
                    if (k.keys[0] == code) {
                        val id = 10 + i % 10
                        if (isDown) InputBackend.down(id, k.pts[0] * r[0], k.pts[1] * r[1])
                        else InputBackend.up(id)
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
        InputBackend.down(id, x, y)
        ui.postDelayed({ InputBackend.up(id) }, 40)
    }

    private fun swipe(i: Int, k: KeyComp, r: IntArray) {
        if (!busy.add(k.keys[0])) return
        val id = 10 + i % 10
        val x1 = k.pts[0] * r[0]
        val y1 = k.pts[1] * r[1]
        val x2 = k.pts[2] * r[0]
        val y2 = k.pts[3] * r[1]
        InputBackend.down(id, x1, y1)
        val n = 6
        for (s in 1..n) {
            ui.postDelayed({
                InputBackend.move(id, x1 + (x2 - x1) * s / n, y1 + (y2 - y1) * s / n)
            }, 25L * s)
        }
        ui.postDelayed({
            InputBackend.up(id)
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
                InputBackend.up(JID)
                joyOn = false
            }
            return
        }
        val n = if (dx != 0f && dy != 0f) 0.7071f else 1f
        if (!joyOn) {
            InputBackend.down(JID, cx, cy)
            joyOn = true
        }
        InputBackend.move(JID, cx + dx * n * rad, cy + dy * n * rad)
    }

    // ---------- editor ----------
    fun openEditor() {
        val c = app ?: return
        val w = wm ?: return
        if (editor != null) return
        releaseAll()
        current()
        resetFlow()
        val v = KeyMapEditor(c, this)
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= 28) {
            p.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        w.addView(v, p)
        editor = v
    }

    fun closeEditor() {
        editor?.let { try { wm?.removeView(it) } catch (e: Exception) {} }
        editor = null
        resetFlow()
        held.clear()
    }

    private fun needPts() = when (addType) {
        KeyComp.SWIPE, KeyComp.JOY -> 2
        else -> 1
    }

    private fun needKeys() = if (addType == KeyComp.JOY) 4 else 1

    override fun comps(): List<KeyComp> = current()

    override fun pending(): List<FloatArray> = pts

    override fun flowActive(): Boolean = addType >= 0

    override fun prompt(): String {
        if (addType < 0) return "Ketuk Tambah untuk memilih komponen. Ketuk titik yang ada untuk menghapus."
        if (pts.size < needPts()) {
            return when (addType) {
                KeyComp.SWIPE ->
                    if (pts.isEmpty()) "Ketuk titik AWAL swipe" else "Ketuk titik AKHIR swipe"
                KeyComp.JOY ->
                    if (pts.isEmpty()) "Ketuk titik PUSAT joystick"
                    else "Ketuk titik TEPI joystick (menentukan jarak)"
                else -> "Ketuk titik di layar"
            }
        }
        if (addType == KeyComp.JOY) {
            return "Tekan tombol keyboard untuk " + arrayOf("ATAS", "KIRI", "BAWAH", "KANAN")[keys.size]
        }
        return "Tekan tombol keyboard untuk titik ini"
    }

    override fun onPick(type: Int) {
        addType = type
        pts.clear()
        keys.clear()
        redraw()
    }

    override fun onCancel() {
        resetFlow()
        redraw()
    }

    override fun onClear() {
        current().clear()
        persist()
        resetFlow()
        say("Semua dihapus")
        redraw()
    }

    override fun onClose() {
        closeEditor()
    }

    private fun anchors(k: KeyComp): List<FloatArray> =
        if (k.type == KeyComp.SWIPE) {
            listOf(floatArrayOf(k.pts[0], k.pts[1]), floatArrayOf(k.pts[2], k.pts[3]))
        } else {
            listOf(floatArrayOf(k.pts[0], k.pts[1]))
        }

    override fun onTap(x: Float, y: Float) {
        val c = app ?: return
        if (addType >= 0) {
            if (pts.size < needPts()) {
                pts.add(floatArrayOf(x, y))
                redraw()
            }
            return
        }
        val r = KeyMapStore.real(c)
        val d = c.resources.displayMetrics.density
        val l = current()
        val idx = l.indexOfFirst { k ->
            anchors(k).any { hypot((it[0] - x) * r[0], (it[1] - y) * r[1]) < 28 * d }
        }
        if (idx >= 0) {
            l.removeAt(idx)
            persist()
            say("Dihapus")
            redraw()
        }
    }

    private fun editorKey(code: Int) {
        if (code == KeyEvent.KEYCODE_ESCAPE) {
            if (addType >= 0) onCancel() else closeEditor()
            return
        }
        if (addType < 0) {
            say("Ketuk Tambah dulu")
            return
        }
        if (pts.size < needPts()) {
            say("Ketuk layar dulu")
            return
        }
        keys.add(code)
        if (keys.size < needKeys()) {
            redraw()
            return
        }
        commit()
    }

    private fun commit() {
        val c = app ?: return
        val r = KeyMapStore.real(c)
        val ks = keys.toIntArray()
        val fp: FloatArray = when (addType) {
            KeyComp.SWIPE -> floatArrayOf(pts[0][0], pts[0][1], pts[1][0], pts[1][1])
            KeyComp.JOY -> {
                val rad = hypot(
                    (pts[1][0] - pts[0][0]) * r[0], (pts[1][1] - pts[0][1]) * r[1]
                ) / min(r[0], r[1])
                floatArrayOf(pts[0][0], pts[0][1], max(rad, 0.03f))
            }
            else -> floatArrayOf(pts[0][0], pts[0][1])
        }
        val l = current()
        l.removeAll { k -> k.keys.any { ks.contains(it) } }
        l.add(KeyComp(addType, ks, fp))
        persist()
        resetFlow()
        say("Tersimpan")
        redraw()
    }
}
