package com.finz.gamespace

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Base64
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.AdbStream
import java.io.ByteArrayInputStream
import java.lang.reflect.InvocationTargetException
import java.math.BigInteger
import java.net.Inet4Address
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.Random
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

class AdbManager private constructor(ctx: Context) : AbsAdbConnectionManager() {

    private val priv: PrivateKey
    private val cert: Certificate

    init {
        setApi(Build.VERSION.SDK_INT)
        val sp = ctx.getSharedPreferences("adbkey", Context.MODE_PRIVATE)
        var pk: PrivateKey? = null
        var ce: Certificate? = null
        val ps = sp.getString("priv", null)
        val cs = sp.getString("cert", null)
        if (ps != null && cs != null) {
            try {
                pk = KeyFactory.getInstance("RSA")
                    .generatePrivate(PKCS8EncodedKeySpec(Base64.decode(ps, Base64.NO_WRAP)))
                ce = CertificateFactory.getInstance("X.509")
                    .generateCertificate(ByteArrayInputStream(Base64.decode(cs, Base64.NO_WRAP)))
            } catch (e: Exception) {
                pk = null
                ce = null
            }
        }
        if (pk == null || ce == null) {
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(2048, SecureRandom())
            val kp = kpg.generateKeyPair()
            pk = kp.private
            ce = makeCert(kp)
            sp.edit()
                .putString("priv", Base64.encodeToString(pk!!.encoded, Base64.NO_WRAP))
                .putString("cert", Base64.encodeToString(ce!!.encoded, Base64.NO_WRAP))
                .apply()
        }
        priv = pk!!
        cert = ce!!
    }

    override fun getPrivateKey(): PrivateKey = priv

    override fun getCertificate(): Certificate = cert

    override fun getDeviceName(): String = "FinzGameSpace"

    companion object {
        @Volatile
        private var inst: AdbManager? = null

        fun get(ctx: Context): AdbManager {
            val i = inst
            if (i != null) return i
            synchronized(this) {
                val j = inst ?: AdbManager(ctx.applicationContext)
                inst = j
                return j
            }
        }

        private fun makeCert(kp: KeyPair): Certificate {
            val from = Date()
            val to = Date(System.currentTimeMillis() + 3650L * 86400000L)
            val name = X500Name("CN=Finz Game Space")
            val serial = BigInteger.valueOf((Random().nextInt() and Int.MAX_VALUE).toLong())
            val builder = JcaX509v3CertificateBuilder(name, serial, from, to, name, kp.public)
            val signer = JcaContentSignerBuilder("SHA512withRSA").build(kp.private)
            return JcaX509CertificateConverter().getCertificate(builder.build(signer))
        }
    }
}

object AdbLink {

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

    fun paired(ctx: Context): Boolean =
        ctx.getSharedPreferences("adbkey", Context.MODE_PRIVATE).getBoolean("paired", false)

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

    fun pair(ctx: Context, host: String?, port: Int, code: String) {
        val m = AdbManager.get(ctx)
        val h = if (!host.isNullOrEmpty()) host else (wifiIp(ctx) ?: "127.0.0.1")
        within(20) { m.pair(h, port, code) }
        ctx.getSharedPreferences("adbkey", Context.MODE_PRIVATE)
            .edit().putBoolean("paired", true).apply()
    }

    private fun findService(ctx: Context, type: String, sec: Long): Pair<String?, Int> {
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

    private fun connectWireless(ctx: Context, m: AdbManager) {
        val svc = findService(ctx, "_adb-tls-connect._tcp", 12)
        val port = svc.second
        if (port <= 0) {
            throw Exception("Debugging nirkabel tidak ditemukan. Nyalakan dulu dan pakai Wi-Fi")
        }
        val host = svc.first ?: wifiIp(ctx) ?: "127.0.0.1"
        val mt = m.javaClass.methods.firstOrNull {
            it.name == "connect" && it.parameterTypes.size == 2 &&
                it.parameterTypes[0] == String::class.java
        } ?: throw Exception("method connect tidak ditemukan di library")
        try {
            mt.invoke(m, host, port)
        } catch (e: InvocationTargetException) {
            throw Exception(e.targetException?.message ?: e.toString())
        }
    }

    fun shell(ctx: Context, cmd: String): AdbStream {
        val m = AdbManager.get(ctx)
        try {
            return within(10) { m.openStream("exec:$cmd") }
        } catch (e: Exception) {
        }
        within(30) { connectWireless(ctx, m) }
        return within(10) { m.openStream("exec:$cmd") }
    }
}
