package com.finz.gamespace

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.PixelFormat
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import java.io.File
import kotlin.math.max
import kotlin.math.sin

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
    private val ui = Handler(Looper.getMainLooper())

    @Volatile
    var liveStart = SystemClock.elapsedRealtime()

    var afterEdit: (() -> Unit)? = null
    var afterPick: (() -> Unit)? = null

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

    fun bringFront(ctx: Context, id: Int) {
        val c = ctx.applicationContext
        app = c
        removeHold(id)
        sync(c)
    }

    private fun sigOf(l: Layer): String = when (l.type) {
        Layer.WEB -> "${l.type}|${l.text}|${l.ext}|${l.anim}"
        Layer.LOGO -> "${l.type}|${l.text}|${l.anim}"
        Layer.FRAME -> "${l.type}|${l.thick}|${l.anim}"
        else -> "${l.type}|${l.text}|${l.size}|${l.color}|${l.color2}|${l.thick}|${l.font}|${l.fx}|" +
            "${l.ext}|${l.anim}|${l.name}"
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
        var needFps = false
        for (l in list) {
            val show = (master && l.on) || l.id == editId
            if (!show) continue
            keep.add(l.id)
            if (l.type == Layer.INFO) needFps = true
            val sig = sigOf(l)
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
        if (needFps && !FpsClient.on && InputBackend.ready) FpsClient.start(c) { _ -> }
    }

    private fun border(c: Context, thick: Int): GradientDrawable {
        val d = c.resources.displayMetrics.density
        val g = GradientDrawable()
        g.setColor(Color.TRANSPARENT)
        g.cornerRadius = 10 * d
        g.setStroke(max(1, (thick * d).toInt()), Palette.a(c))
        return g
    }

    private fun place(h: Hold, l: Layer, r: IntArray, c: Context) {
        val minPx = (40 * c.resources.displayMetrics.density).toInt()
        h.lp.x = (l.x * r[0]).toInt()
        h.lp.y = (l.y * r[1]).toInt()
        h.lp.width = max(minPx, (l.w * r[0]).toInt())
        h.lp.height = max(minPx, (l.h * r[1]).toInt())
        h.root.alpha = l.alpha / 100f
        h.root.background =
            if (l.thick > 0 && (l.type == Layer.WEB || l.type == Layer.LOGO)) border(c, l.thick) else null
        if (h.root.isAttachedToWindow) {
            try { wm?.updateViewLayout(h.root, h.lp) } catch (e: Exception) {}
        }
    }

    private fun startAnim(v: View, mode: Int) {
        val t0 = SystemClock.uptimeMillis()
        val dens = v.resources.displayMetrics.density
        val r = object : Runnable {
            override fun run() {
                if (!v.isAttachedToWindow) return
                val t = (SystemClock.uptimeMillis() - t0) / 1000f
                when (mode) {
                    1 -> v.alpha = 0.2f + 0.8f * (0.5f + 0.5f * sin(t * 5f))
                    2 -> {
                        val s = 1f + 0.06f * sin(t * 4f)
                        v.scaleX = s
                        v.scaleY = s
                    }
                    3 -> v.translationY = sin(t * 2f) * 6f * dens
                    4 -> v.rotation = sin(t * 5f) * 3f
                }
                v.postOnAnimation(this)
            }
        }
        v.post(r)
    }

    private fun create(c: Context, l: Layer, sig: String): Hold? {
        val w = wm ?: return null
        val st = l.style()
        val content: View = when (l.type) {
            Layer.WEB -> web(c, l)
            Layer.TEXT -> TextBlock(c, l.text, st)
            Layer.MARQUEE -> MarqueeView(c, l.text, st)
            Layer.TIMER -> TimerView(c, st)
            Layer.LOGO -> logo(c, l)
            Layer.INFO -> InfoView(c, st)
            Layer.GOAL -> GoalView(c, l.text, st)
            Layer.COUNT -> CountView(c, l.id, l.name, l.text.trim().toIntOrNull() ?: 10, st)
            Layer.CLOCK -> ClockView(c, st)
            Layer.ROTATE -> RotateView(c, l.text, l.ext, st)
            Layer.COUNTER -> CounterView(c, l.id, l.text, st)
            Layer.SHAPE -> ShapeView(c, l.text, l.ext, st)
            Layer.SCENE -> SceneView(c, l.text, st)
            else -> FrameView(c, l.thick)
        }
        val root = FrameLayout(c)
        root.addView(content, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
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
        if (l.anim > 0) startAnim(content, l.anim)
        return h
    }

    private fun bitmap(path: String, maxPx: Int): Bitmap? {
        return try {
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, o)
            var s = 1
            while (o.outWidth / s > maxPx || o.outHeight / s > maxPx) s *= 2
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = s
            BitmapFactory.decodeFile(path, o2)
        } catch (e: Throwable) {
            null
        }
    }

    private fun logo(c: Context, l: Layer): View {
        val t = l.text
        if (t.startsWith("img:")) {
            val path = t.substring(4)
            val iv = ImageView(c)
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            val low = path.lowercase()
            if (Build.VERSION.SDK_INT >= 28 && (low.endsWith(".gif") || low.endsWith(".webp"))) {
                try {
                    val dr = ImageDecoder.decodeDrawable(ImageDecoder.createSource(File(path)))
                    iv.setImageDrawable(dr)
                    if (dr is AnimatedImageDrawable) {
                        dr.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        dr.start()
                    }
                    return iv
                } catch (e: Throwable) {
                }
            }
            val bmp = bitmap(path, 1024)
            if (bmp != null) {
                iv.setImageBitmap(bmp)
                return iv
            }
        }
        if (t.startsWith("vid:")) {
            val vv = VideoView(c)
            try {
                vv.setVideoPath(t.substring(4))
                vv.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    mp.setVolume(0f, 0f)
                    vv.start()
                }
                vv.setOnErrorListener { _, _, _ -> true }
                return vv
            } catch (e: Throwable) {
            }
        }
        return LogoView(c)
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
        if (l.ext > 0 && u.isNotEmpty()) {
            val every = l.ext * 60000L
            val r = object : Runnable {
                override fun run() {
                    if (!v.isAttachedToWindow) return
                    try { v.reload() } catch (e: Exception) {}
                    v.postDelayed(this, every)
                }
            }
            v.postDelayed(r, every)
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
        } else if (v is VideoView) {
            try { v.stopPlayback() } catch (e: Exception) {}
        }
    }

    fun destroyAll() {
        editBar?.let { try { wm?.removeView(it) } catch (e: Exception) {} }
        editBar = null
        editId = -1
        afterEdit = null
        afterPick = null
        for (id in holds.keys.toList()) removeHold(id)
    }

    private fun fireAfter() {
        val cb = afterEdit
        afterEdit = null
        if (cb != null) ui.post { cb() }
    }

    // ---------- mode atur posisi dan ukuran ----------
    fun startEdit(ctx: Context, id: Int) {
        val c = ctx.applicationContext
        app = c
        wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (!Settings.canDrawOverlays(c)) {
            Toast.makeText(c, "Izinkan 'Tampil di atas aplikasi lain' dulu (menu Izin)", Toast.LENGTH_LONG).show()
            fireAfter()
            return
        }
        endEdit(false, true)
        editId = id
        sync(c)
        val h = holds[id]
        if (h == null) {
            editId = -1
            fireAfter()
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
            },
            { f ->
                val cx = h.lp.x + h.lp.width / 2f
                val cy = h.lp.y + h.lp.height / 2f
                val nw = max(minPx, (h.lp.width * f).toInt())
                val nh = max(minPx, (h.lp.height * f).toInt())
                h.lp.width = nw
                h.lp.height = nh
                h.lp.x = (cx - nw / 2f).toInt()
                h.lp.y = (cy - nh / 2f).toInt()
                try { wm?.updateViewLayout(h.root, h.lp) } catch (e: Exception) {}
            }
        )
        h.root.addView(fr, FrameLayout.LayoutParams(-1, -1))
        h.frame = fr
        h.lp.flags = h.lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        try { wm?.updateViewLayout(h.root, h.lp) } catch (e: Exception) {}
        showEditBar(c, name)
    }

    fun endEdit(save: Boolean, silent: Boolean = false) {
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
        if (!silent) fireAfter()
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
