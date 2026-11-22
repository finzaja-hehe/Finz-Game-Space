package com.finz.gamespace

import android.app.Application
import android.content.Intent
import android.util.Log
import kotlin.system.exitProcess

class GameApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                val i = Intent(this, CrashActivity::class.java)
                i.putExtra("trace", Log.getStackTraceString(e).take(3000))
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(i)
                Thread.sleep(400)
            } catch (x: Throwable) {
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }
}
