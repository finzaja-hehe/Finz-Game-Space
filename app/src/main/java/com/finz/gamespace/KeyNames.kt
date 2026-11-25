package com.finz.gamespace

import android.view.KeyEvent

object KeyNames {

    fun label(code: Int): String = when (code) {
        0 -> "?"
        1001 -> "Klik Kiri"
        1002 -> "Klik Kanan"
        1003 -> "Klik Tengah"
        1004 -> "Mouse 4"
        1005 -> "Mouse 5"
        else -> KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")
    }
}
