package com.finz.gamespace

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.media.AudioManager
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object Glide {

    private fun out3(x: Float): Float {
        val k = 1f - x
        return 1f - k * k * k
    }

    fun run(v: View, delayMs: Long, durMs: Long, step: (Float) -> Unit) {
        val t0 = SystemClock.uptimeMillis() + delayMs
        val r = object : Runnable {
            override fun run() {
                if (!v.isAttachedToWindow) return
                val now = SystemClock.uptimeMillis()
                if (now < t0) {
                    v.postOnAnimation(this)
                    return
                }
                val u = ((now - t0).toFloat() / durMs).coerceIn(0f, 1f)
                step(out3(u))
                if (u < 1f) v.postOnAnimation(this)
            }
        }
        v.postOnAnimation(r)
    }
}

class TileButton(
    ctx: Context,
    private val iconId: Int,
    label0: String,
    private val glyph: String = ""
) : View(ctx) {

    var label = label0
        set(v) {
            field = v
            invalidate()
        }
    var active = false
        set(v) {
            field = v
            invalidate()
        }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private fun d(v: Float) = v * resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(d(62f).toInt(), d(68f).toInt())
    }

    override fun onDraw(c: Canvas) {
        val a = Palette.a(context)
        val b = Palette.b(context)
        val r = d(21f)
        val cx = width / 2f
        val cy = d(4f) + r
        path.reset()
        for (i in 0 until 6) {
            val ang = Math.toRadians((60 * i - 30).toDouble())
            val x = cx + (r * Math.cos(ang)).toFloat()
            val y = cy + (r * Math.sin(ang)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        fill.shader = null
        if (active) {
            fill.style = Paint.Style.STROKE
            for (k in 1..3) {
                fill.strokeWidth = d(2f + k * 2.5f)
                fill.color = Palette.alpha(a, 60 / k)
                c.drawPath(path, fill)
            }
        }
        fill.style = Paint.Style.FILL
        fill.shader = if (active) {
            LinearGradient(cx - r, cy - r, cx + r, cy + r, a, b, Shader.TileMode.CLAMP)
        } else {
            LinearGradient(
                cx, cy - r, cx, cy + r, 0xFF21165A.toInt(), 0xFF0B0824.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        c.drawPath(path, fill)
        fill.style = Paint.Style.STROKE
        fill.strokeWidth = d(1.6f)
        fill.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r, a, b, Shader.TileMode.CLAMP)
        c.drawPath(path, fill)
        fill.shader = null

        if (iconId >= 18) {
            GameIcons.draw(c, txt, iconId, cx, cy, d(26f), Color.WHITE, d(1f))
        } else if (iconId >= 0) {
            HudIcons.draw(c, txt, iconId, cx, cy, d(26f), Color.WHITE, d(1f))
        } else {
            txt.shader = null
            txt.style = Paint.Style.FILL
            txt.textAlign = Paint.Align.CENTER
            txt.isFakeBoldText = true
            txt.color = Color.WHITE
            txt.textSize = d(16f)
            val fm = txt.fontMetrics
            c.drawText(glyph, cx, cy - (fm.ascent + fm.descent) / 2f, txt)
        }

        txt.shader = null
        txt.style = Paint.Style.FILL
        txt.textAlign = Paint.Align.CENTER
        txt.isFakeBoldText = false
        txt.color = if (active) Color.WHITE else 0xFFC9C2F5.toInt()
        txt.textSize = d(8.5f)
        while (txt.measureText(label) > width - d(2f) && txt.textSize > d(6f)) {
            txt.textSize = txt.textSize - d(0.3f)
        }
        c.drawText(label, cx, cy + r + d(13f), txt)
    }
}

class DockLayout(ctx: Context) : LinearLayout(ctx) {

    var dockLeft = false
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(c: Canvas) {
        val d = resources.displayMetrics.density
        val a = Palette.a(context)
        val b = Palette.b(context)
        val w = width.toFloat()
        val h = height.toFloat()
        val cut = 24f * d
        val i = 3f * d
        path.reset()
        if (!dockLeft) {
            path.moveTo(i + cut, i)
            path.lineTo(w - i, i)
            path.lineTo(w - i, h - i)
            path.lineTo(i + cut, h - i)
            path.lineTo(i, h - i - cut)
            path.lineTo(i, i + cut)
            path.close()
        } else {
            path.moveTo(i, i)
            path.lineTo(w - i - cut, i)
            path.lineTo(w - i, i + cut)
            path.lineTo(w - i, h - i - cut)
            path.lineTo(w - i - cut, h - i)
            path.lineTo(i, h - i)
            path.close()
        }
        fill.style = Paint.Style.FILL
        fill.shader = LinearGradient(
            0f, 0f, 0f, h, 0xF21B0E48.toInt(), 0xF206030F.toInt(), Shader.TileMode.CLAMP
        )
        c.drawPath(path, fill)
        fill.shader = null
        line.style = Paint.Style.STROKE
        line.shader = null
        for (k in 1..3) {
            line.strokeWidth = (2f + k * 2.5f) * d
            line.color = Palette.alpha(a, 36 / k)
            c.drawPath(path, line)
        }
        line.strokeWidth = 1.8f * d
        line.shader = LinearGradient(0f, 0f, 0f, h, a, b, Shader.TileMode.CLAMP)
        c.drawPath(path, line)
        line.shader = null
        line.strokeWidth = 3f * d
        line.strokeCap = Paint.Cap.SQUARE
        line.color = Palette.alpha(Color.WHITE, 230)
        val seg = 34f * d
        if (!dockLeft) {
            c.drawLine(i + cut, i, i + cut + seg, i, line)
            c.drawLine(i + cut, h - i, i + cut + seg, h - i, line)
        } else {
            c.drawLine(w - i - cut - seg, i, w - i - cut, i, line)
            c.drawLine(w - i - cut - seg, h - i, w - i - cut, h - i, line)
        }
        line.strokeCap = Paint.Cap.BUTT
        super.onDraw(c)
    }
}

private class T(val k: String, val l: String, val i: Int, val g: String = "")

class DockPanel(
    private val ctx: Context,
    private val kit: OverlayKit,
    private val act: OverlayActions,
    private val left: Boolean,
    private val onAction: (String) -> Unit
) {
    val root = DockLayout(ctx)
    val targetAlpha: Float = KeyMapStore.getInt(ctx, "dockalpha", 92) / 100f
    private val tiles = HashMap<String, TileButton>()
    private val vRam = kit.tx("-", 11f, Color.WHITE, true)
    private val vTemp = kit.tx("-", 11f, Color.WHITE, true)
    private val vBat = kit.tx("-", 11f, Color.WHITE, true)
    private val vPing = kit.tx("-", 11f, Color.WHITE, true)
    private val vFps = kit.tx("-", 11f, Color.WHITE, true)
    private val vNet = kit.tx("-", 11f, Color.WHITE, true)
    private val vSession = kit.tx("MAIN  00:00", 11f, kit.accent, true)
    private val statusTv = kit.tx("", 10f, 0xFFFFE600.toInt(), true)
    private val boost = kit.tx("", 12f, Color.WHITE, true)

    private fun cell(label: String, v: TextView, weight: Float): View {
        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.addView(kit.tx(label, 8f, kit.muted, true).apply { letterSpacing = 0.1f })
        v.maxLines = 1
        v.ellipsize = TextUtils.TruncateAt.END
        v.gravity = Gravity.CENTER
        col.addView(v)
        col.layoutParams = LinearLayout.LayoutParams(0, kit.WRAP, weight)
        return col
    }

    private fun paintBoost(on: Boolean) {
        boost.text = if (on) "MODE GAME  •  AKTIF" else "MODE GAME  •  MATI"
        boost.background = if (on) kit.grad(12) else kit.box(0xFF140F2E.toInt(), 12, kit.accent, 1)
    }

    private fun tile(t: T): TileButton {
        val v = TileButton(ctx, t.i, t.l, t.g)
        v.setOnClickListener { onAction(t.k) }
        tiles[t.k] = v
        return v
    }

    init {
        root.dockLeft = left
        root.orientation = LinearLayout.VERTICAL
        val pl = if (left) 12 else 22
        val pr = if (left) 22 else 12
        root.setPadding(kit.dp(pl), kit.dp(12), kit.dp(pr), kit.dp(10))

        val head = LinearLayout(ctx)
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(LogoView(ctx), LinearLayout.LayoutParams(kit.dp(40), kit.dp(40)))
        val titles = LinearLayout(ctx)
        titles.orientation = LinearLayout.VERTICAL
        titles.addView(kit.tx("FINZ GAME SPACE", 11f, Color.WHITE, true).apply { letterSpacing = 0.12f })
        titles.addView(vSession)
        head.addView(titles, LinearLayout.LayoutParams(0, kit.WRAP, 1f).apply { leftMargin = kit.dp(6) })
        val closeBtn = kit.tx("✕", 13f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = kit.circleBg(false)
            setOnClickListener { onAction("close") }
        }
        head.addView(closeBtn, LinearLayout.LayoutParams(kit.dp(30), kit.dp(30)))
        root.addView(head)

        val stats = LinearLayout(ctx)
        stats.gravity = Gravity.CENTER_VERTICAL
        stats.setPadding(kit.dp(4), kit.dp(6), kit.dp(4), kit.dp(6))
        stats.background = kit.box(0x40140C3A, 12, Palette.alpha(kit.accent, 90), 1)
        stats.addView(cell("FPS", vFps, 0.9f))
        stats.addView(cell("RAM", vRam, 0.9f))
        stats.addView(cell("SUHU", vTemp, 1f))
        stats.addView(cell("BAT", vBat, 0.9f))
        stats.addView(cell("PING", vPing, 1.1f))
        stats.addView(cell("NET", vNet, 1.3f))
        root.addView(stats, LinearLayout.LayoutParams(-1, kit.WRAP).apply { topMargin = kit.dp(8) })

        statusTv.gravity = Gravity.CENTER
        statusTv.visibility = View.GONE
        root.addView(statusTv, LinearLayout.LayoutParams(-1, kit.WRAP).apply { topMargin = kit.dp(4) })

        val body = LinearLayout(ctx)
        body.orientation = LinearLayout.VERTICAL
        boost.gravity = Gravity.CENTER
        boost.letterSpacing = 0.08f
        boost.setPadding(kit.dp(10), kit.dp(10), kit.dp(10), kit.dp(10))
        boost.setOnClickListener { onAction("boost") }
        paintBoost(false)
        body.addView(boost, LinearLayout.LayoutParams(-1, kit.WRAP).apply { topMargin = kit.dp(8) })

        val items = listOf(
            T("dnd", "DND", 0), T("keep", "Layar On", 1), T("cross", "Crosshair", 2), T("keymap", "Keymap", 3),
            T("shot", "Foto", 4), T("rec", "Rekam", 5), T("turbo", "Turbo", 7), T("mon", "Monitor", 8),
            T("rot", "Rotasi", 9), T("mute", "Bisu", 10), T("save", "Hemat Layar", 11), T("rest", "Istirahat", 12),
            T("heat", "Alarm Panas", 13), T("net", "Jaringan", 14), T("disp", "Layar", 15), T("fps", "FPS", 18),
            T("stream", "Stream", 19), T("mstream", "Mode Stream", 20), T("sensi", "Sensi", 21), T("auto", "Auto Turbo", 22),
            T("torch", "Senter", 23), T("side", "Sisi Dock", 16), T("app", "Aplikasi", -1, "↩"), T("off", "Matikan", 6)
        )
        var i = 0
        while (i < items.size) {
            val row = LinearLayout(ctx)
            for (j in 0 until 4) {
                if (i + j < items.size) {
                    row.addView(tile(items[i + j]))
                } else {
                    row.addView(View(ctx), LinearLayout.LayoutParams(kit.dp(62), kit.dp(68)))
                }
            }
            body.addView(row, LinearLayout.LayoutParams(-2, -2).apply { topMargin = kit.dp(6) })
            i += 4
        }

        val am = ctx.getSystemService(AudioManager::class.java)
        val bright = Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        body.addView(
            kit.slider("Cerah", 5, 255, bright, 46) { act.setBrightness(it) },
            LinearLayout.LayoutParams(-1, kit.WRAP).apply { topMargin = kit.dp(10) }
        )
        body.addView(
            kit.slider(
                "Suara", 0, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                am.getStreamVolume(AudioManager.STREAM_MUSIC), 46
            ) { am.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0) },
            LinearLayout.LayoutParams(-1, kit.WRAP)
        )
        body.addView(
            kit.slider("Panel", 40, 100, (targetAlpha * 100).toInt(), 46) {
                KeyMapStore.putInt(ctx, "dockalpha", it)
                root.alpha = it / 100f
            },
            LinearLayout.LayoutParams(-1, kit.WRAP)
        )

        val sv = ScrollView(ctx)
        sv.isVerticalScrollBarEnabled = false
        sv.overScrollMode = View.OVER_SCROLL_NEVER
        sv.addView(body)
        root.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    fun active(key: String, on: Boolean) {
        if (key == "boost") {
            paintBoost(on)
            return
        }
        tiles[key]?.active = on
    }

    fun setLabel(key: String, text: String) {
        tiles[key]?.label = text
    }

    fun setStatus(s: String) {
        statusTv.text = s
        statusTv.visibility = if (s.isEmpty()) View.GONE else View.VISIBLE
    }

    fun update(ram: Int, temp: Float, bat: Int, ping: String, net: String, session: String, fps: String) {
        vRam.text = "$ram%"
        vTemp.text = "%.1f°".format(temp)
        vTemp.setTextColor(if (temp >= 42f) 0xFFFF5B5B.toInt() else Color.WHITE)
        vBat.text = "$bat%"
        vBat.setTextColor(if (bat <= 15) 0xFFFF5B5B.toInt() else Color.WHITE)
        vPing.text = ping.replace(" ms", "ms")
        vFps.text = fps
        vNet.text = net
        vSession.text = "MAIN  $session"
    }
}
