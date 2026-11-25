package com.finz.gamespace

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

class KeymapMenu(
    private val ctx: Context,
    private val wm: WindowManager,
    private val kit: OverlayKit
) {
    private var view: View? = null
    private val MATCH = LinearLayout.LayoutParams.MATCH_PARENT

    companion object {
        private var current: KeymapMenu? = null

        fun open(ctx: Context, wm: WindowManager, kit: OverlayKit) {
            current?.hide()
            val m = KeymapMenu(ctx, wm, kit)
            current = m
            m.show()
        }
    }

    fun hide() {
        view?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        view = null
    }

    private fun rebuild() {
        hide()
        show()
    }

    private fun toast(s: String) = Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show()

    private fun mp(top: Int) =
        LinearLayout.LayoutParams(MATCH, kit.WRAP).apply { topMargin = kit.dp(top) }

    private fun circle(icon: Int, action: () -> Unit): View {
        val v = GgCircle(ctx, icon)
        v.setOnClickListener { action() }
        return v
    }

    private fun tile(icon: Int, label: String, on: Boolean, action: () -> Unit): View {
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setOnClickListener { action() }
        }
        val h = GgHex(ctx, icon)
        h.active = on
        col.addView(h)
        col.addView(
            kit.tx(label, 11f, Color.WHITE).apply {
                gravity = Gravity.CENTER
                maxLines = 2
            },
            LinearLayout.LayoutParams(kit.dp(96), kit.WRAP)
        )
        return col
    }

    private fun row2(a: View, b: View): LinearLayout {
        val r = LinearLayout(ctx)
        r.addView(a, LinearLayout.LayoutParams(0, kit.WRAP, 1f))
        r.addView(b, LinearLayout.LayoutParams(0, kit.WRAP, 1f))
        return r
    }

    private fun toggleAdb() {
        if (InputBackend.ready) {
            InputBackend.stop()
            toast("Terputus")
            rebuild()
            return
        }
        toast("Menyambung lewat Debugging nirkabel...")
        InputBackend.start(ctx, InputBackend.ADB) { _, msg ->
            toast(msg)
            rebuild()
        }
    }

    fun show() {
        if (view != null) return
        KeyRouter.attach(ctx)
        val dm = ctx.resources.displayMetrics
        val bulge = kit.dp(56)
        val root = SideMenuLayout(ctx)
        root.orientation = LinearLayout.VERTICAL
        root.bulge = bulge.toFloat()
        root.setPadding(kit.dp(16), kit.dp(14), bulge + kit.dp(10), kit.dp(10))

        val top = LinearLayout(ctx)
        val cl = { LinearLayout.LayoutParams(kit.dp(46), kit.dp(46)).apply { rightMargin = kit.dp(14) } }
        top.addView(circle(0) {
            ctx.startActivity(
                Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }, cl())
        top.addView(circle(1) {
            hide()
            SettingsPanel(ctx, wm, kit).show()
        }, cl())
        top.addView(circle(8) { hide() }, cl())
        root.addView(top)

        val body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val ready = InputBackend.ready

        body.addView(
            row2(
                tile(2, "Konfigurasi Keymap", false) {
                    hide()
                    SlotPanel(ctx, wm, kit).show()
                },
                tile(3, "Edit keymap", false) {
                    if (!InputBackend.ready) {
                        toast("Ketuk Sambung ADB dulu")
                    } else {
                        hide()
                        KeyRouter.openEditor()
                    }
                }
            ), mp(14)
        )
        body.addView(
            row2(
                tile(4, "Pengaturan Keymap", false) {
                    hide()
                    SettingsPanel(ctx, wm, kit).show()
                },
                tile(5, "Mengetik dengan Keyboard", !KeyRouter.enabled) {
                    KeyRouter.switchEnabled(!KeyRouter.enabled)
                    rebuild()
                }
            ), mp(10)
        )
        body.addView(
            row2(
                tile(7, if (ready) "ADB terhubung" else "Sambung ADB", ready) { toggleAdb() },
                tile(6, "Pairing lewat notifikasi", false) {
                    hide()
                    ctx.startForegroundService(
                        Intent(ctx, PairService::class.java).setAction("START")
                    )
                    toast("Tarik layar ke bawah, tulis kode lewat notifikasi")
                }
            ), mp(10)
        )

        val st = if (ready) "Terhubung: ${InputBackend.name(InputBackend.mode)}" else "Terputus"
        body.addView(kit.tx(st, 11f, 0xFFB8BDFF.toInt()), mp(12))
        if (ready) {
            val kc = KeyRouter.kbdCount
            val ks = when {
                kc < 0 -> "Mencari keyboard..."
                kc == 0 -> "Keyboard tidak terbaca " + InputBackend.kbdNote
                else -> "Keyboard terbaca ($kc). Tombol terakhir: " + KeyRouter.lastKey
            }
            body.addView(kit.tx(ks, 11f, 0xFFB8BDFF.toInt()), mp(2))
        }
        body.addView(
            kit.tx("F10 = editor, F9 = nyala/mati", 10f, 0xFF8B93C8.toInt()), mp(6)
        )

        val sv = ScrollView(ctx)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(body)
        root.addView(sv, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val w = minOf(dm.widthPixels - kit.dp(8), kit.dp(340))
        wm.addView(root, kit.lp(w, MATCH, 0, 0, Gravity.START or Gravity.TOP))
        view = root
    }
}
