package com.finz.gamespace

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import io.github.muntashirakon.adb.AdbStream
import java.lang.reflect.InvocationTargetException
import java.net.Inet4Address
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

object AdbConn {

    private val pool = Executors.newCachedThreadPool()

    private fun <T> within(sec: Long, job: () -> T): T {
        val f = pool.submit(Callable<T> { job() })
        try {
            return f.get(sec, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            f.cancel(true)
            throw Exception("waktu habis")
        } catch (e: ExecutionException) {
            throw Exception(e.cause?.message ?: e.toString())
        }
    }

    private fun open(m: AdbManager, cmd: String): AdbStream =
        within(10) { m.openStream("exec:$cmd") }

    @Suppress("DEPRECATION")
    private fun wifiIp(ctx: Context): String? {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        for (n in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(n) ?: continue
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
            val lp = cm.getLinkProperties(n) ?: continue
            for (la in lp.linkAddresses) {
                val a = la.address
                if (a is Inet4Address && !a.isLoopbackAddress) return a.hostAddress
            }
        }
        return null
    }

    private fun discover(ctx: Context, type: String, sec: Long): Pair<String?, Int> {
        val nsd = ctx.getSystemService(Context.NSD_SERVICE) as NsdManager
        val latch = CountDownLatch(1)
        val port = AtomicInteger(0)
        val host = AtomicReference<String?>(null)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                latch.countDown()
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        port.set(serviceInfo.port)
                        host.set(serviceInfo.host?.hostAddress)
                        latch.countDown()
                    }
                })
            }
        }
        nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
        latch.await(sec, TimeUnit.SECONDS)
        try {
            nsd.stopServiceDiscovery(listener)
        } catch (e: Exception) {
        }
        return Pair(host.get(), port.get())
    }

    fun shell(ctx: Context, cmd: String): AdbStream {
        val m = AdbManager.get(ctx)
        try {
            return open(m, cmd)
        } catch (e: Exception) {
        }
        val svc = discover(ctx, "_adb-tls-connect._tcp", 12)
        val port = svc.second
        if (port <= 0) {
            throw Exception("Debugging nirkabel tidak ditemukan. Nyalakan dulu dan pakai Wi-Fi")
        }
        val hosts = ArrayList<String>()
        val h0 = svc.first
        if (h0 != null && !h0.contains(":")) hosts.add(h0)
        val h1 = wifiIp(ctx)
        if (h1 != null && !hosts.contains(h1)) hosts.add(h1)
        hosts.add("127.0.0.1")
        val mt = m.javaClass.methods.firstOrNull {
            it.name == "connect" && it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == String::class.java
        } ?: throw Exception("method connect tidak ditemukan di library")
        var err = "?"
        for (h in hosts) {
            try {
                within(15) {
                    try {
                        mt.invoke(m, h, port)
                    } catch (e: InvocationTargetException) {
                        throw Exception(e.targetException?.message ?: e.toString())
                    }
                }
                return open(m, cmd)
            } catch (e: Throwable) {
                err = e.message ?: e.toString()
            }
        }
        throw Exception("Gagal tersambung: $err")
    }
}
