package com.finz.gamespace

import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = TextView(this)
        t.text = "ERROR:\n" + (intent.getStringExtra("trace") ?: "-")
        t.setTextColor(Color.WHITE)
        t.textSize = 11f
        t.setPadding(40, 80, 40, 40)
        val s = ScrollView(this)
        s.setBackgroundColor(Color.BLACK)
        s.addView(t)
        setContentView(s)
    }
}
