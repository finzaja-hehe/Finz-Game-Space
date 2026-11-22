package com.finz.gamespace

import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

object SysInfo {

    private fun readLong(path: String): Long =
        try {
            File(path).readText().trim().toLong()
        } catch (e: Exception) {
            0L
        }

    fun cpuGhz(): String {
        var best = 0L
        val n = Runtime.getRuntime().availableProcessors()
        for (i in 0 until n) {
            val f = readLong("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            if (f > best) best = f
        }
        return if (best > 0) "%.2f GHz".format(best / 1000000f) else "-"
    }
}

object Ping {

    @Volatile
    var ms = -1

    @Volatile
    private var run = false
    private var th: Thread? = null

    @Synchronized
    fun start() {
        run = true
        if (th?.isAlive == true) return
        val t = Thread {
            while (run) {
                ms = measure()
                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        t.isDaemon = true
        t.start()
        th = t
    }

    fun stop() {
        run = false
    }

    fun text(): String = if (ms >= 0) "$ms ms" else "-"

    private fun measure(): Int {
        val t0 = System.nanoTime()
        return try {
            Socket().use { it.connect(InetSocketAddress("1.1.1.1", 443), 1500) }
            ((System.nanoTime() - t0) / 1000000).toInt()
        } catch (e: Exception) {
            -1
        }
    }
}
