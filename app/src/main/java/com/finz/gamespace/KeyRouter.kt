package com.finz.gamespace

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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

    private val ui = Handler(Looper.getMainLooper())
    private var app: Context? = null
    private var wm: WindowManager? = null
    private var editor: KeyMapEditor? = null
    private var saved: MutableList<KeyComp> = ArrayList()
    private var draft: MutableList<KeyComp>? = null
    private var isDirty = false
    private var exitArmed = false
    private var cacheKey = ""
    private var addType = -1
    private var selIdx = -1
    private var slot = 0
    private val pts = ArrayList<FloatArray>()

    fun attach(ctx: Context) {
        val c = ctx.applicationContext
        app = c
        wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        DotsOverlay.attach(c)
        syncBalls()
    }

    fun syncBalls() {
        val c = app ?: return
        if (editor != null) {
            BallOverlay.hide()
            return
        }
        BallOverlay.show(c, runList())
    }

    private fun label(code: Int) = KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")

    private fun say(s: String) {
        val c = app ?: return
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show()
    }

    private fun runList(): MutableList<KeyComp> {
        val c = app ?: return saved
        val key = KeyMapStore.profileKey(c)
        if (key != cacheKey) {
            saved = KeyMapStore.load(c, key)
            cacheKey = key
        }
        return saved
    }

    private fun edit(): MutableList<KeyComp> = draft ?: runList()

    private fun markDirty() {
        isDirty = true
        exitArmed = false
    }

    fun reload() {
        cacheKey = ""
        runList()
        draft = if (editor != null) ArrayList(saved) else null
        isDirty = false
        resetFlow()
        editor?.invalidate()
        syncBalls()
    }

    fun clearProfile() {
        val c = app ?: return
        saved = ArrayList()
        cacheKey = KeyMapStore.profileKey(c)
        KeyMapStore.save(c, cacheKey, saved)
        draft = if (editor != null) ArrayList() else null
        isDirty = false
        resetFlow()
        editor?.invalidate()
        syncBalls()
    }

    fun switchEnabled(v: Boolean) {
        enabled = v
        KeyRunner.releaseAll()
    }

    private fun resetFlow() {
        addType = -1
        selIdx = -1
        slot = 0
        pts.clear()
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
        KeyRunner.track(code, isDown)
        if (isDown && code == KeyEvent.KEYCODE_F10) {
            if (editor == null) openEditor() else onExit()
            return
        }
        if (isDown && code == KeyEvent.KEYCODE_F9) {
            switchEnabled(!enabled)
            say(if (enabled) "Keymap nyala" else "Keymap mati")
            return
        }
        if (editor != null) {
            if (isDown) editorKey(code)
            return
        }
        if (!enabled) return
        val c = app ?: return
        KeyRunner.press(c, runList(), code, isDown)
    }

    // ---------- editor ----------
    fun openEditor() {
        val c = app ?: return
        val w = wm ?: return
        if (editor != null) return
        KeyRunner.releaseAll()
        draft = ArrayList(runList())
        isDirty = false
        exitArmed = false
        resetFlow()
        BallOverlay.hide()
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

    private fun closeEditor() {
        editor?.let { try { wm?.removeView(it) } catch (e: Exception) {} }
        editor = null
        draft = null
        isDirty = false
        exitArmed = false
        resetFlow()
        KeyRunner.releaseAll()
        syncBalls()
    }

    private fun needPts() = when (addType) {
        KeyComp.SWIPE, KeyComp.JOY -> 2
        else -> 1
    }

    private fun needKeys() = when (addType) {
        KeyComp.JOY -> 4
        KeyComp.BALL -> 0
        else -> 1
    }

    private fun validSel(): Boolean = selIdx >= 0 && selIdx < edit().size

    private fun selComp(): KeyComp? = if (validSel()) edit()[selIdx] else null

    override fun comps(): List<KeyComp> = edit()

    override fun pending(): List<FloatArray> = pts

    override fun flowActive(): Boolean = addType >= 0

    override fun dirty(): Boolean = isDirty

    override fun selected(): Int = if (validSel()) selIdx else -1

    override fun slotName(): String {
        val k = selComp() ?: return ""
        if (k.type != KeyComp.JOY) return ""
        return arrayOf("ATAS", "KIRI", "BAWAH", "KANAN")[slot.coerceIn(0, 3)]
    }

    override fun canFinish(): Boolean = addType == KeyComp.GROUP && pts.isNotEmpty()

    override fun prompt(): String {
        if (exitArmed) return "Belum disimpan. Ketuk Keluar lagi untuk membuang perubahan."
        if (addType >= 0) {
            return when (addType) {
                KeyComp.SWIPE ->
                    if (pts.isEmpty()) "Ketuk titik AWAL geser" else "Ketuk titik AKHIR geser"
                KeyComp.JOY ->
                    if (pts.isEmpty()) "Ketuk titik PUSAT joystick"
                    else "Ketuk titik TEPI joystick (menentukan jarak)"
                KeyComp.GROUP ->
                    if (pts.isEmpty()) "Ketuk titik-titik (bisa banyak), lalu ketuk Selesai titik"
                    else "${pts.size} titik. Ketuk titik lagi, atau ketuk Selesai titik"
                KeyComp.BALL -> "Ketuk posisi bola mengambang"
                else -> "Ketuk titik di layar"
            }
        }
        val k = selComp()
        if (k != null) {
            if (k.keys.isEmpty()) return "Komponen dipilih. Bola tidak butuh tombol."
            val sn = slotName()
            val unset = k.keys.any { it == 0 }
            return (if (unset) "Belum ada tombol" else "Tombol terpasang") +
                (if (sn.isNotEmpty()) " untuk $sn" else "") +
                ". Ketuk Pilih tombol, atau tekan tombol di keyboard."
        }
        return "Ketuk Tambah komponen. Ketuk komponen yang ada untuk mengatur tombolnya."
    }

    override fun onPick(type: Int) {
        addType = type
        selIdx = -1
        pts.clear()
        redraw()
    }

    override fun onCancel() {
        resetFlow()
        redraw()
    }

    override fun onClear() {
        edit().clear()
        markDirty()
        resetFlow()
        say("Semua dihapus (belum disimpan)")
        redraw()
    }

    override fun onExit() {
        if (isDirty && !exitArmed) {
            exitArmed = true
            redraw()
            return
        }
        closeEditor()
    }

    override fun onSave() {
        val c = app ?: return
        val d = draft ?: return
        cacheKey = KeyMapStore.profileKey(c)
        KeyMapStore.save(c, cacheKey, d)
        saved = ArrayList(d)
        isDirty = false
        exitArmed = false
        say("Tersimpan")
        redraw()
    }

    override fun onShare() {
        exportToClipboard(false)
    }

    override fun onImport() {
        val c = app ?: return
        val w = wm ?: return
        ImportPanel(c, w, OverlayKit(c)).show()
    }

    override fun onFinish() {
        if (canFinish()) commit()
    }

    override fun onDelete() {
        if (!validSel()) return
        edit().removeAt(selIdx)
        selIdx = -1
        markDirty()
        say("Dihapus (belum disimpan)")
        redraw()
    }

    override fun onDeselect() {
        selIdx = -1
        redraw()
    }

    override fun onPickKey(): Boolean {
        val k = selComp() ?: return false
        if (k.keys.isEmpty()) {
            say("Komponen ini tidak butuh tombol")
            return false
        }
        return true
    }

    override fun onAssign(code: Int) {
        assign(code)
    }

    private fun assign(code: Int) {
        val k = selComp() ?: return
        if (k.keys.isEmpty()) return
        val l = edit()
        for (i in l.indices) {
            if (i == selIdx) continue
            val o = l[i]
            for (j in o.keys.indices) {
                if (o.keys[j] == code) o.keys[j] = 0
            }
        }
        val s = slot.coerceIn(0, k.keys.size - 1)
        for (j in k.keys.indices) {
            if (j != s && k.keys[j] == code) k.keys[j] = 0
        }
        k.keys[s] = code
        val nm = if (k.type == KeyComp.JOY) arrayOf("ATAS", "KIRI", "BAWAH", "KANAN")[s] + " = " else ""
        say(nm + label(code))
        slot = (s + 1) % k.keys.size
        markDirty()
        redraw()
    }

    fun exportToClipboard(send: Boolean) {
        val c = app ?: return
        val text = KeyMapStore.export(edit())
        try {
            val cm = c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("keymap", text))
            say("Disalin ke clipboard")
        } catch (e: Exception) {
            say("Gagal menyalin")
        }
        if (send) {
            try {
                val i = Intent(Intent.ACTION_SEND)
                i.type = "text/plain"
                i.putExtra(Intent.EXTRA_TEXT, text)
                val ch = Intent.createChooser(i, "Bagikan keymap")
                ch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                c.startActivity(ch)
            } catch (e: Exception) {
            }
        }
    }

    fun applyImport(text: String): String? {
        val c = app ?: return "Belum siap"
        val l = KeyMapStore.parseImport(text) ?: return "Teks keymap tidak valid"
        if (editor != null) {
            draft = l
            markDirty()
            resetFlow()
            redraw()
            return null
        }
        KeyMapStore.save(c, KeyMapStore.profileKey(c), l)
        reload()
        return null
    }

    private fun anchors(k: KeyComp): List<FloatArray> {
        if (k.type == KeyComp.SWIPE) {
            return listOf(floatArrayOf(k.pts[0], k.pts[1]), floatArrayOf(k.pts[2], k.pts[3]))
        }
        if (k.type == KeyComp.GROUP) {
            val out = ArrayList<FloatArray>()
            var j = 0
            while (j + 1 < k.pts.size) {
                out.add(floatArrayOf(k.pts[j], k.pts[j + 1]))
                j += 2
            }
            return out
        }
        return listOf(floatArrayOf(k.pts[0], k.pts[1]))
    }

    override fun onTap(x: Float, y: Float) {
        val c = app ?: return
        if (addType >= 0) {
            if (addType == KeyComp.GROUP) {
                if (pts.size < 16) {
                    pts.add(floatArrayOf(x, y))
                    redraw()
                }
                return
            }
            if (pts.size < needPts()) {
                pts.add(floatArrayOf(x, y))
                if (pts.size >= needPts()) commit() else redraw()
            }
            return
        }
        val r = KeyMapStore.real(c)
        val d = c.resources.displayMetrics.density
        val l = edit()
        val idx = l.indexOfFirst { k ->
            anchors(k).any { hypot((it[0] - x) * r[0], (it[1] - y) * r[1]) < 30 * d }
        }
        if (idx >= 0) {
            selIdx = idx
            val e = l[idx].keys.indexOfFirst { it == 0 }
            slot = if (e < 0) 0 else e
        } else {
            selIdx = -1
        }
        redraw()
    }

    private fun editorKey(code: Int) {
        if (code == KeyEvent.KEYCODE_ESCAPE) {
            if (addType >= 0) {
                onCancel()
            } else if (selIdx >= 0) {
                selIdx = -1
                redraw()
            } else {
                onExit()
            }
            return
        }
        if (addType >= 0) {
            say("Ketuk titik di layar dulu")
            return
        }
        if (!validSel()) {
            say("Ketuk komponen yang ada dulu, lalu tekan tombol")
            return
        }
        assign(code)
    }

    private fun commit() {
        val c = app ?: return
        val r = KeyMapStore.real(c)
        val nk = needKeys()
        val fp: FloatArray = when (addType) {
            KeyComp.SWIPE -> floatArrayOf(pts[0][0], pts[0][1], pts[1][0], pts[1][1])
            KeyComp.JOY -> {
                val rad = hypot(
                    (pts[1][0] - pts[0][0]) * r[0], (pts[1][1] - pts[0][1]) * r[1]
                ) / min(r[0], r[1])
                floatArrayOf(pts[0][0], pts[0][1], max(rad, 0.03f))
            }
            KeyComp.GROUP -> {
                val a = FloatArray(pts.size * 2)
                for (i in pts.indices) {
                    a[i * 2] = pts[i][0]
                    a[i * 2 + 1] = pts[i][1]
                }
                a
            }
            else -> floatArrayOf(pts[0][0], pts[0][1])
        }
        val l = edit()
        l.add(KeyComp(addType, IntArray(nk), fp))
        val last = l.size - 1
        resetFlow()
        if (nk > 0) selIdx = last
        markDirty()
        say(if (nk > 0) "Ditambahkan. Pilih tombolnya, atau nanti saja." else "Ditambahkan.")
        redraw()
    }
}
