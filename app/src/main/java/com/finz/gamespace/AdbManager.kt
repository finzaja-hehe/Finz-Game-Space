package com.finz.gamespace

import android.content.Context
import android.os.Build
import android.util.Base64
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.AdbStream
import java.io.ByteArrayInputStream
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
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import sun.security.x509.*

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
            val algo = "SHA512withRSA"
            val from = Date()
            val to = Date(System.currentTimeMillis() + 3650L * 86400000L)
            val ext = CertificateExtensions()
            ext.set("SubjectKeyIdentifier",
                SubjectKeyIdentifierExtension(KeyIdentifier(kp.public).identifier))
            ext.set("PrivateKeyUsage", PrivateKeyUsageExtension(from, to))
            val name = X500Name("CN=Finz Game Space")
            val info = X509CertInfo()
            info.set("version", CertificateVersion(2))
            info.set("serialNumber", CertificateSerialNumber(Random().nextInt() and Int.MAX_VALUE))
            info.set("algorithmID", CertificateAlgorithmId(AlgorithmId.get(algo)))
            info.set("subject", CertificateSubjectName(name))
            info.set("key", CertificateX509Key(kp.public))
            info.set("validity", CertificateValidity(from, to))
            info.set("issuer", CertificateIssuerName(name))
            info.set("extensions", ext)
            val c = X509CertImpl(info)
            c.sign(kp.private, algo)
            return c
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

    fun pair(ctx: Context, port: Int, code: String) {
        val m = AdbManager.get(ctx)
        within(20) { m.pair("127.0.0.1", port, code) }
        ctx.getSharedPreferences("adbkey", Context.MODE_PRIVATE)
            .edit().putBoolean("paired", true).apply()
    }

    fun shell(ctx: Context, cmd: String): AdbStream {
        val m = AdbManager.get(ctx)
        try {
            return within(10) { m.openStream("exec:$cmd") }
        } catch (e: Exception) {
        }
        within(25) { m.connectTls(ctx, 20000) }
        return within(10) { m.openStream("exec:$cmd") }
    }
}
