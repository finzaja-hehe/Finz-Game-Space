package com.finz.gamespace

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max

object StreamLayers {

    private class Hold(
        val sig: String,
        val root: FrameLayout,
        val lp: WindowManager.LayoutParams,
        val content: View
    ) {
        var frame: View? = null
    }

    private val holds = HashMap<Int, Hold>()
    private var app: Context? = null
    private var wm: WindowManager? = null
    private var editId = -1
    private var editBar: View? = null

    @Volatile
    var liveStart = SystemClock.elapsedRealtime()

    private val NORMAL = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

    fun setMaster(ctx: Context, on: Boolean) {
        StreamStore.setMaster(ctx, on)
        if (on) liveStart = SystemClock.elapsedRealtime()
        sync(ctx)
    }

    fun sync(ctx: Context) {
        val c = ctx.applicationContext
        app = c
        wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (!Settings.canDrawOverlays(c)) return
        val master = StreamStore.master(c)
        val list = StreamStore.load(c)
        val r = KeyMapStore.real(c)
        val keep = HashSet<Int>()
        for (l in list) {
            if (!((master && l.on) || l.id == editId)) continue
            keep.add(l.id)
            val sig = "${l.type}|${l.text}|${l.size}|${l.color}"
            var h = holds[l.id]
            if (h != null && h.sig != sig) {
                removeHold(l.id)
                h = null
            }
            if (h == null) h = create(c, l, sig)
            if (h != null && l.id != editId) place(h, l, r, c)
        }
        for (id in holds.keys.toList()) {
            if (!keep.contains(id)) removeHold(id)
        }
    }

    private fun place(h: Hold, l: Layer, r: IntArray, c: Context) {
        val minPx = (40 * c.resources.displayMetrics.density).toInt()
        h.lp.x = (l.x * r[0]).toInt()
        h.lp.y = (l.y * r[1]).toInt()
        h.lp.width = max(minPx, (l.w * r[0]).toInt())
        h.lp.height = max(minPx, (l.h * r[1]).toInt())
        h.root.alpha = l.alpha / 100f
        if (h.root.isAttachedToWindow) {
            try { wm?.updateViewLayout(h.root, h.lp) } catch (e: Exception) {}
        }
    }

    private fun create(c: Context, l: Layer, sig: String): Hold? {
        val w = wm ?: return null
        val content: View = when (l.type) {
            Layer.WEB -> web(c, l)
            Layer.TEXT -> textView(c, l)
            Layer.MARQUEE -> MarqueeView(c, l.text, l.size, l.color)
            Layer.TIMER -> TimerView(c, l.size, l.color)
            else -> LogoView(c)
        }
        val root = FrameLayout(c)
        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        val lp = WindowManager.LayoutParams(
            1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, NORMAL, PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        val h = Hold(sig, root, lp, content)
        place(h, l, KeyMapStore.real(c), c)
        try {
            w.addView(root, lp)
        } catch (e: Exception) {
            return null
        }
        holds[l.id] = h
        return h
    }

    private fun textView(c: Context, l: Layer): View {
        val t = TextView(c)
        t.text = l.text
        t.textSize = l.size.toFloat()
        t.setTextColor(l.color)
        t.typeface = Typeface.DEFAULT_BOLD
        t.gravity = Gravity.CENTER_VERTICAL
        t.setShadowLayer(4f, 0f, 0f, Color.BLACK)
        return t
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun web(c: Context, l: Layer): View {
        val v = WebView(c)
        v.setBackgroundColor(Color.TRANSPARENT)
        v.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val s = v.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.mediaPlaybackRequiresUserGesture = false
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        v.webViewClient = WebViewClient()
        v.isVerticalScrollBarEnabled = false
        v.isHorizontalScrollBarEnabled = false
        val u = l.text.trim()
        if (u.isEmpty()) {
            v.loadData(
                "<html><body style='color:#fff;font:14px sans-serif'>Tempel link widget di Stream Studio</body></html>",
                "text/html", "UTF-8"
            )
        } else {
            v.loadUrl(if (u.startsWith("http")) u else "https://$u")
        }
        return v
    }

    private fun removeHold(id: Int) {
        val h = holds.remove(id) ?: return
        try { wm?.removeView(h.root) } catch (e: Exception) {}
        val v = h.content
        if (v is WebView) {
            try {
                v.loadUrl("about:blank")
                v.destroy()
            } catch (e: Exception) {
            }
        }
    }

    fun destroyAll() {
        editBar?.let { try { wm?.removeView(it) } catch (e: Exception) {} }
        editBar = null
        editId = -1
        for (id in holds.keys.toList()) removeHold(id)
    }

    // ---------- mode atur posisi dan ukuran ----------
    fun startEdit(ctx: Context, id: Int) {
        val c = ctx.applicationContext
        if (!Settings.canDrawOverlays(c)) return
        endEdit(false)
        editId = id
        sync(c)
        val h = holds[id]
        if (h == null) {
            editId = -1
            return
        }
        val name = StreamStore.load(c).firstOrNull { it.id == id }?.name ?: "Layer"
        val minPx = (40 * c.resources.displayMetrics.density).toInt()
        val fr = EditFrame(
            c, name,
            { dx, dy ->
                h.lp.x += dx
                h.lp.y += dy
                try { wm?.updateViewLayout(h.root, h.lp) } catch (e: Exception) {}
            },
            { dx, dy ->
                h.lp.width = max(minPx, h.lp.width + dx)
                h.lp.height = max(minPx, h.lp.height + dy)
                try { wm?.updateViewLayout(h.root, h.lp) } catch (e: Exception) {}
            }
        )
        h.root.addView(fr, FrameLayout.LayoutParams(-1, -1))
        h.frame = fr
        h.lp.flags = h.lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        try { wm?.updateViewLayout(h.root, h.lp) } catch (e: Exception) {}
        showEditBar(c, name)
    }

    fun endEdit(save: Boolean) {
        if (editId < 0) return
        val id = editId
        val c = app
        val h = holds[id]
        if (h != null) {
            if (save && c != null) {
                val r = KeyMapStore.real(c)
                val list = StreamStore.load(c)
                val l = list.firstOrNull { it.id == id }
                if (l != null) {
                    l.x = (h.lp.x / r[0].toFloat()).coerceIn(0f, 0.98f)
                    l.y = (h.lp.y / r[1].toFloat()).coerceIn(0f, 0.98f)
                    l.w = (h.lp.width / r[0].toFloat()).coerceIn(0.05f, 1f)
                    l.h = (h.lp.height / r[1].toFloat()).coerceIn(0.04f, 1f)
                    StreamStore.save(c, list)
                }
            }
            h.frame?.let { h.root.removeView(it) }
            h.frame = null
            h.lp.flags = h.lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            try { wm?.updateViewLayout(h.root, h.lp) } catch (e: Exception) {}
        }
        editBar?.let { try { wm?.removeView(it) } catch (e: Exception) {} }
        editBar = null
        editId = -1
        if (c != null) sync(c)
    }

    private fun editBtn(kit: OverlayKit, label: String, main: Boolean, action: () -> Unit): TextView {
        val t = kit.tx(label, 12f, Color.WHITE, true)
        t.gravity = Gravity.CENTER
        t.background = if (main) kit.grad(10) else kit.box(0xFF1F2430.toInt(), 10, kit.accent, 1)
        t.setPadding(kit.dp(14), kit.dp(8), kit.dp(14), kit.dp(8))
        t.setOnClickListener { action() }
        return t
    }

    private fun showEditBar(c: Context, name: String) {
        val kit = OverlayKit(c)
        val row = LinearLayout(c)
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = kit.box(0xF012151C.toInt(), 16, kit.accent, 1)
        row.setPadding(kit.dp(12), kit.dp(8), kit.dp(12), kit.dp(8))
        row.addView(
            kit.tx("Atur: $name", 12f, Color.WHITE, true),
            LinearLayout.LayoutParams(kit.WRAP, kit.WRAP).apply { rightMargin = kit.dp(10) }
        )
        row.addView(
            editBtn(kit, "Simpan", true) { endEdit(true) },
            LinearLayout.LayoutParams(kit.WRAP, kit.WRAP).apply { rightMargin = kit.dp(6) }
        )
        row.addView(editBtn(kit, "Batal", false) { endEdit(false) })
        val p = WindowManager.LayoutParams(
            kit.WRAP, kit.WRAP, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        p.y = kit.dp(8)
        try {
            wm?.addView(row, p)
            editBar = row
        } catch (e: Exception) {
        }
    }
}
