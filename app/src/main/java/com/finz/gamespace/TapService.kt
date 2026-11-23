package com.finz.gamespace

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import kotlin.math.hypot

class TapService : AccessibilityService() {

    private var wm: WindowManager? = null
    private var editor: KeyMapEditor? = null
    private var cache: MutableMap<Int, FloatArray> = LinkedHashMap()
    private var cacheKey = ""
    private var fg = "global"
    private var pending: FloatArray? = null
    private var enabled = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        say("Finz Keymap aktif. F10 editor, F9 nyala/mati, F8/F7/F6 mode")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName || pkg.startsWith("com.android.systemui") ||
            pkg.contains("inputmethod")
        ) return
        fg = pkg
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        closeEditor()
        InputBackend.stop()
        return super.onUnbind(intent)
    }

    private fun say(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun label(code: Int) = KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")

    private fun current(): MutableMap<Int, FloatArray> {
        val r = KeyMapStore.real(this)
        val key = fg + if (r[0] > r[1]) "_L" else "_P"
        if (key != cacheKey) {
            cache = KeyMapStore.load(this, key)
            cacheKey = key
            pending = null
        }
        return cache
    }

    private fun setBackend(m: Int) {
        if (m == InputBackend.ACC) {
            InputBackend.stop()
            say("Mode: Aksesibilitas")
            return
        }
        say("Menyiapkan ${InputBackend.name(m)}...")
        InputBackend.start(this, m) { _, msg -> say(msg) }
    }

    override fun onKeyEvent(ev: KeyEvent): Boolean {
        val code = ev.keyCode
        val first = ev.action == KeyEvent.ACTION_DOWN && ev.repeatCount == 0
        if (code == KeyEvent.KEYCODE_F10) {
            if (first) toggleEditor()
            return true
        }
        if (code == KeyEvent.KEYCODE_F9) {
            if (first) {
                enabled = !enabled
                say(if (enabled) "Keymap nyala" else "Keymap mati")
            }
            return true
        }
        if (code == KeyEvent.KEYCODE_F8 || code == KeyEvent.KEYCODE_F7 ||
            code == KeyEvent.KEYCODE_F6
        ) {
            if (first) {
                val m = when (code) {
                    KeyEvent.KEYCODE_F7 -> InputBackend.SHIZUKU
                    KeyEvent.KEYCODE_F6 -> InputBackend.ROOT
                    else -> InputBackend.ACC
                }
                setBackend(m)
            }
            return true
        }
        if (editor != null) {
            if (first) {
                if (code == KeyEvent.KEYCODE_ESCAPE || code == KeyEvent.KEYCODE_BACK) {
                    closeEditor()
                } else {
                    bind(code)
                }
            }
            return true
        }
        if (!enabled) return false
        val pt = current()[code] ?: return false
        if (first) tap(pt)
        return true
    }

    private fun tap(pt: FloatArray) {
        val r = KeyMapStore.real(this)
        val x = pt[0] * r[0]
        val y = pt[1] * r[1]
        if (InputBackend.ready) {
            InputBackend.tap(x, y)
            return
        }
        val path = Path()
        path.moveTo(x, y)
        val stroke = GestureDescription.StrokeDescription(path, 0, 40)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    private fun toggleEditor() {
        if (editor == null) openEditor() else closeEditor()
    }

    private fun openEditor() {
        val v = KeyMapEditor(this, { x, y -> onEditorTap(x, y) }, { clearAll() })
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= 28) {
            p.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        wm?.addView(v, p)
        editor = v
        pending = null
        refreshEditor()
    }

    private fun closeEditor() {
        editor?.let { try { wm?.removeView(it) } catch (e: Exception) {} }
        editor = null
        pending = null
    }

    private fun refreshEditor() {
        editor?.show(current(), pending, "Profil: $fg")
    }

    private fun onEditorTap(x: Float, y: Float) {
        val m = current()
        val r = KeyMapStore.real(this)
        val d = resources.displayMetrics.density
        val hit = m.entries.firstOrNull {
            hypot((it.value[0] - x) * r[0], (it.value[1] - y) * r[1]) < 28 * d
        }
        if (hit != null) {
            m.remove(hit.key)
            KeyMapStore.save(this, cacheKey, m)
            say("Dihapus: ${label(hit.key)}")
        } else {
            pending = floatArrayOf(x, y)
        }
        refreshEditor()
    }

    private fun bind(code: Int) {
        val pt = pending
        if (pt == null) {
            say("Ketuk layar dulu untuk menaruh titik")
            return
        }
        val m = current()
        m[code] = pt
        KeyMapStore.save(this, cacheKey, m)
        pending = null
        refreshEditor()
        say("${label(code)} tersimpan")
    }

    private fun clearAll() {
        current().clear()
        KeyMapStore.save(this, cacheKey, cache)
        pending = null
        refreshEditor()
        say("Semua titik dihapus")
    }
}
