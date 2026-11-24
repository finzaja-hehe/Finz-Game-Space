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

object KeyRouter {

    @Volatile
    var enabled = true

    @Volatile
    var kbdCount = -1

    @Volatile
    var lastKey = "-"

    private val ui = Handler(Looper.getMainLooper())
    private var app: Context? = null
    private var wm: WindowManager? = null
    private var editor: KeyMapEditor? = null
    private var cache: MutableMap<Int, FloatArray> = LinkedHashMap()
    private var cacheKey = ""
    private var pending: FloatArray? = null

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

    private fun current(): MutableMap<Int, FloatArray> {
        val c = app ?: return cache
        val r = KeyMapStore.real(c)
        val key = "global" + (if (r[0] > r[1]) "_L" else "_P")
        if (key != cacheKey) {
            cache = KeyMapStore.load(c, key)
            cacheKey = key
            pending = null
        }
        return cache
    }

    fun onKey(evdev: Int, down: Boolean) {
        if (!down) return
        val code = EvdevMap.toAndroid(evdev)
        if (code == 0) return
        lastKey = label(code)
        ui.post { handle(code) }
    }

    private fun handle(code: Int) {
        if (code == KeyEvent.KEYCODE_F10) {
            if (editor == null) openEditor() else closeEditor()
            return
        }
        if (code == KeyEvent.KEYCODE_F9) {
            enabled = !enabled
            say(if (enabled) "Keymap nyala" else "Keymap mati")
            return
        }
        if (editor != null) {
            if (code == KeyEvent.KEYCODE_ESCAPE) closeEditor() else bind(code)
            return
        }
        if (!enabled) return
        val pt = current()[code] ?: return
        val c = app ?: return
        val r = KeyMapStore.real(c)
        InputBackend.tap(pt[0] * r[0], pt[1] * r[1])
    }

    fun openEditor() {
        val c = app ?: return
        val w = wm ?: return
        if (editor != null) return
        val v = KeyMapEditor(c, { x, y -> onEditorTap(x, y) }, { clearAll() }, { closeEditor() })
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
        pending = null
        refresh()
    }

    fun closeEditor() {
        editor?.let { try { wm?.removeView(it) } catch (e: Exception) {} }
        editor = null
        pending = null
    }

    private fun refresh() {
        editor?.show(current(), pending, "Profil: Umum")
    }

    private fun onEditorTap(x: Float, y: Float) {
        val c = app ?: return
        val m = current()
        val r = KeyMapStore.real(c)
        val d = c.resources.displayMetrics.density
        val hit = m.entries.firstOrNull {
            hypot((it.value[0] - x) * r[0], (it.value[1] - y) * r[1]) < 28 * d
        }
        if (hit != null) {
            m.remove(hit.key)
            KeyMapStore.save(c, cacheKey, m)
            say("Dihapus: ${label(hit.key)}")
        } else {
            pending = floatArrayOf(x, y)
        }
        refresh()
    }

    private fun bind(code: Int) {
        val c = app ?: return
        val pt = pending
        if (pt == null) {
            say("Ketuk layar dulu untuk menaruh titik")
            return
        }
        val m = current()
        m[code] = pt
        KeyMapStore.save(c, cacheKey, m)
        pending = null
        refresh()
        say("${label(code)} tersimpan")
    }

    private fun clearAll() {
        val c = app ?: return
        current().clear()
        KeyMapStore.save(c, cacheKey, cache)
        pending = null
        refresh()
        say("Semua titik dihapus")
    }
}
