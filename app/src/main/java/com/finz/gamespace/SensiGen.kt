package com.finz.gamespace

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.pow
import kotlin.math.sqrt

class DevInfo(
    val brand: String,
    val model: String,
    val ramGb: Int,
    val storageGb: Int,
    val freeGb: Float,
    val hz: Int,
    val inch: Float,
    val sdk: Int,
    val soc: String
)

class SensiResult(
    val v: IntArray,
    val tier: Int,
    val tierName: String,
    val mode: String,
    val settings: List<Pair<String, String>>,
    val brandTips: List<String>,
    val tips: List<String>
)

object SensiGen {

    val STYLES = arrayOf("2 Jari", "3 Jari", "4 Jari", "5-6 Jari")
    val ROLES = arrayOf("Rusher", "Sniper", "Support")
    val LEVELS = arrayOf("Pemula", "Menengah", "Pro")
    val COMPLAINTS = arrayOf(
        "Drag headshot susah", "Bidikan terlalu cepat/liar", "Bidikan terlalu lambat/berat",
        "Recoil susah dikontrol", "Sensi tidak konsisten", "Layar lag/patah-patah",
        "HP cepat panas", "Jari sering terhalang"
    )
    val KEYS = arrayOf("Umum", "Red Dot", "Scope 2x", "Scope 4x", "Sniper (AWM)", "Lihat Bebas")
    val TIERS = arrayOf("Entry / Ringan", "Menengah", "Flagship / Gaming")

    private class BP(val keys: List<String>, val bias: Int, val mode: String, val tips: List<String>)

    private val PROFILES = listOf(
        BP(
            listOf("infinix", "tecno", "itel", "transsion"), 2, "Game Mode (Smart Panel / XOS)",
            listOf(
                "Layar kelas entry cenderung kurang responsif di tepi layar. Geser tombol penting sedikit ke tengah.",
                "Aktifkan Game Mode bawaan dan matikan penghemat baterai ketat untuk Free Fire dan Finz.",
                "Pelindung layar tebal atau licin bisa mengurangi respons sentuhan."
            )
        ),
        BP(
            listOf("xiaomi", "redmi", "poco"), -1, "Game Turbo",
            listOf(
                "Aktifkan Game Turbo dan pilih profil performa untuk Free Fire.",
                "Kunci aplikasi di Recent supaya tidak ditutup sistem saat bermain lama."
            )
        ),
        BP(
            listOf("samsung"), 0, "Game Booster / Game Launcher",
            listOf(
                "Di Game Booster pilih prioritas performa dan atur kehalusan gerakan ke tinggi.",
                "Matikan optimalisasi baterai untuk Free Fire agar FPS stabil."
            )
        ),
        BP(
            listOf("oppo", "realme", "oneplus"), 0, "Game Space / Game Assistant",
            listOf(
                "Masukkan Free Fire ke Game Space dan aktifkan mode performa.",
                "Jika tersedia, nyalakan opsi respons sentuhan di Game Space."
            )
        ),
        BP(
            listOf("vivo", "iqoo"), 0, "Ultra Game Mode / Monster Mode",
            listOf(
                "Aktifkan Ultra Game Mode dan atur performa ke tinggi saat main.",
                "Blokir notifikasi selama bermain supaya sentuhan tidak terganggu."
            )
        ),
        BP(
            listOf("asus", "rog phone"), -3, "Armoury Crate / X Mode",
            listOf(
                "Pakai profil X Mode di Armoury Crate untuk performa maksimal.",
                "Sensor sentuh HP gaming sangat cepat, jadi sensi sudah diturunkan sedikit."
            )
        ),
        BP(
            listOf("redmagic", "nubia", "black shark", "blackshark"), -3, "Game Space (gaming)",
            listOf(
                "Aktifkan mode performa tinggi bawaan HP gaming.",
                "Sentuhan HP gaming sangat responsif, jadi sensi sudah diturunkan sedikit."
            )
        ),
        BP(
            listOf("huawei", "honor"), 0, "Game Suite / Game Mode",
            listOf(
                "Aktifkan Game Mode agar notifikasi dan proses latar dibatasi.",
                "Atur performa ke mode tinggi di pengaturan baterai."
            )
        )
    )

    @Suppress("DEPRECATION")
    fun detect(c: Context): DevInfo {
        val am = c.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val ram = Math.round(mi.totalMem / 1073741824.0).toInt().coerceAtLeast(1)
        val sf = StatFs(Environment.getDataDirectory().path)
        val total = sf.blockCountLong * sf.blockSizeLong / 1e9
        val free = (sf.availableBlocksLong * sf.blockSizeLong / 1e9).toFloat()
        var st = total.toInt()
        for (s in intArrayOf(16, 32, 64, 128, 256, 512, 1024)) {
            if (s >= total) {
                st = s
                break
            }
        }
        val wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val hz = wm.defaultDisplay.refreshRate.toInt()
        val dm = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(dm)
        val inch = sqrt((dm.widthPixels / dm.xdpi).pow(2) + (dm.heightPixels / dm.ydpi).pow(2))
        val brand = Build.BRAND.replaceFirstChar { it.uppercase() }
        val name = try {
            Settings.Global.getString(c.contentResolver, "device_name")
        } catch (e: Exception) {
            null
        }
        val model = if (name.isNullOrBlank()) Build.MODEL else name
        var soc = Build.HARDWARE
        if (Build.VERSION.SDK_INT >= 31) {
            val s = (Build.SOC_MANUFACTURER + " " + Build.SOC_MODEL).trim()
            if (s.isNotEmpty() && !s.lowercase().contains("unknown")) soc = s
        }
        return DevInfo(brand, model, ram, st, free, hz, inch, Build.VERSION.SDK_INT, soc)
    }

    fun tierOf(d: DevInfo): Int {
        val s = (d.soc + " " + Build.HARDWARE + " " + Build.BOARD).lowercase()
        val high = listOf(
            "snapdragon 8", "sm8", "kalama", "taro", "lahaina", "waipio", "dimensity 9",
            "mt6985", "mt6989", "mt6983", "exynos 2", "tensor"
        )
        val low = listOf(
            "helio a", "helio g3", "helio p2", "mt6765", "mt6762", "mt6761", "mt6739", "sc9863",
            "sc9832", "t606", "t610", "t612", "t616", "t618", "sm4", "sdm4", "bengal", "trinket",
            "msm89", "snapdragon 4"
        )
        if (high.any { s.contains(it) }) return 2
        if (low.any { s.contains(it) }) return 0
        return when {
            d.ramGb <= 3 -> 0
            d.ramGb <= 6 -> 1
            d.ramGb >= 12 -> 2
            else -> 1
        }
    }

    private fun profileOf(d: DevInfo): BP? {
        val s = (d.brand + " " + d.model + " " + Build.MANUFACTURER + " " + Build.BRAND).lowercase()
        return PROFILES.firstOrNull { p -> p.keys.any { s.contains(it) } }
    }

    fun generate(
        d: DevInfo, style: Int, role: Int, level: Int, cs: Set<Int>, shift: Int
    ): SensiResult {
        val tier = tierOf(d)
        val bp = profileOf(d)
        val base = arrayOf(
            intArrayOf(62, 60, 55, 50, 38, 60),
            intArrayOf(75, 72, 66, 60, 45, 65),
            intArrayOf(85, 82, 76, 70, 52, 70),
            intArrayOf(92, 90, 84, 78, 58, 75)
        )[style.coerceIn(0, 3)]
        val v = base.copyOf()
        fun add(i: Int, n: Int) {
            v[i] += n
        }
        fun all(n: Int) {
            for (i in v.indices) v[i] += n
        }
        if (d.inch >= 6.7f) all(4) else if (d.inch < 6.2f) all(-3)
        if (d.hz >= 120) all(-5) else if (d.hz >= 90) all(-3)
        when (tier) {
            0 -> all(3)
            2 -> all(-2)
        }
        if (bp != null) all(bp.bias)
        when (level) {
            0 -> all(-8)
            2 -> all(6)
        }
        when (role) {
            0 -> {
                add(0, 5)
                add(1, 5)
            }
            1 -> {
                add(3, 6)
                add(4, 8)
                add(0, -3)
            }
        }
        if (cs.contains(0)) {
            add(0, 4)
            add(1, 6)
            add(2, 4)
        }
        if (cs.contains(1)) {
            all(-8)
            add(1, -2)
        }
        if (cs.contains(2)) all(8)
        if (cs.contains(3)) {
            add(1, -4)
            add(2, -4)
        }
        if (cs.contains(4)) add(5, -5)
        if (cs.contains(5)) add(0, 3)
        all(shift)
        for (i in v.indices) v[i] = v[i].coerceIn(20, 100)
        for (i in 1..4) {
            if (v[i] > v[i - 1]) v[i] = v[i - 1]
        }

        val gfx = when (tier) {
            0 -> "Halus"
            1 -> "Standar"
            else -> "Ultra / Lanjutan"
        }
        val fps = when (tier) {
            0 -> "Tinggi (turunkan jika patah-patah)"
            1 -> "Tinggi"
            else -> "Ekstrem / Maks"
        }
        val btn = when (style) {
            0 -> "60-70%"
            1 -> "55-65%"
            2 -> "50-60%"
            else -> "45-55%"
        }
        val settings = ArrayList<Pair<String, String>>()
        settings.add(Pair("Grafis", gfx))
        settings.add(Pair("FPS", fps))
        settings.add(Pair("Bayangan", if (tier <= 1) "Mati" else "Hidup"))
        settings.add(Pair("Bloom dan anti-aliasing", if (tier == 0) "Mati" else "Standar"))
        settings.add(Pair("Ukuran tombol tembak", btn))
        settings.add(Pair("Layar HP", "${d.hz}Hz"))

        val bt = ArrayList<String>()
        if (bp != null) bt.addAll(bp.tips)
        else bt.add("Cari fitur Game Mode atau Game Booster di pengaturan HP dan aktifkan untuk Free Fire.")
        bt.add("Izinkan Finz Game Space tanpa batasan baterai agar overlay stabil di ${d.brand} ${d.model}.")

        val tips = ArrayList<String>()
        if (d.ramGb <= 3) tips.add("RAM ${d.ramGb}GB kecil. Tutup aplikasi lain dan pakai Turbo sebelum main.")
        if (d.freeGb < 5f) {
            tips.add("Penyimpanan bebas tinggal %.1fGB. Kosongkan minimal 5GB agar game tidak patah-patah.".format(d.freeGb))
        }
        if (d.hz >= 90) tips.add("Layar ${d.hz}Hz: aktifkan FPS tertinggi di game. Sensi sudah diturunkan sedikit.")
        tips.add(
            when (style) {
                0 -> "2 jari: sensi lebih rendah agar bidikan stabil. Latih geser pelan dan terkontrol."
                1 -> "3 jari: sensi sedang agar bisa drag cepat tanpa kehilangan kendali."
                2 -> "4 jari: sensi lebih tinggi karena gerakan bidik pendek. Atur posisi tombol agar tidak saling menutup."
                else -> "5-6 jari: sensi tinggi. Pastikan layar bersih dan kurangi sensi jika bidikan melayang."
            }
        )
        when (role) {
            0 -> tips.add("Rusher: Umum dan Red Dot dinaikkan sedikit untuk duel jarak dekat.")
            1 -> tips.add("Sniper: Scope 4x dan Sniper dinaikkan agar pindah target di scope lebih cepat.")
        }
        if (cs.contains(0)) tips.add("Drag headshot susah: Red Dot dan 2x dinaikkan. Latih satu gerakan geser pendek ke kepala.")
        if (cs.contains(1)) tips.add("Bidikan terlalu cepat: semua sensi diturunkan. Masih liar? Ketuk Terlalu cepat.")
        if (cs.contains(2)) tips.add("Bidikan terlalu lambat: semua sensi dinaikkan. Pastikan layar bersih dan tidak licin.")
        if (cs.contains(3)) tips.add("Recoil susah: Red Dot dan 2x diturunkan agar tarikan ke bawah lebih terkontrol.")
        if (cs.contains(4)) tips.add("Sensi tidak konsisten: bersihkan layar, cek pelindung layar, dan matikan penghemat daya yang membatasi sentuhan.")
        if (cs.contains(5)) tips.add("Lag: turunkan grafis, tutup aplikasi lain, pakai Turbo, dan pantau FPS di overlay.")
        if (cs.contains(6)) tips.add("HP panas: nyalakan Alarm Panas, kurangi grafis dan FPS, lepas casing tebal, jangan main sambil mengisi daya.")
        if (cs.contains(7)) tips.add("Jari terhalang: kecilkan tombol dan geser tombol tembak agar tidak menutup target.")
        tips.add("Angka ini titik awal dari pola umum, bukan angka resmi Garena. Tes 3-5 menit di Training Ground, lalu setel dengan tombol di bawah.")
        return SensiResult(v, tier, TIERS[tier], bp?.mode ?: "Game Mode bawaan", settings, bt, tips)
    }

    fun format(d: DevInfo, r: SensiResult): String {
        val sb = StringBuilder()
        sb.append("Sensi Free Fire - ").append(d.brand).append(' ').append(d.model).append('\n')
        sb.append("Kelas HP: ").append(r.tierName).append('\n')
        for (i in r.v.indices) sb.append(KEYS[i]).append(": ").append(r.v[i]).append('\n')
        for (s in r.settings) sb.append(s.first).append(": ").append(s.second).append('\n')
        return sb.toString()
    }
}
