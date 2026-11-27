package com.finz.gamespace

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
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
    val sdk: Int
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
        val brand = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        return DevInfo(brand, Build.MODEL, ram, st, free, hz, inch, Build.VERSION.SDK_INT)
    }

    fun generate(
        d: DevInfo, style: Int, role: Int, level: Int, cs: Set<Int>, shift: Int
    ): Pair<IntArray, List<String>> {
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
        if (d.hz >= 90) all(-3)
        if (d.ramGb <= 3) {
            add(0, 3)
            add(1, 3)
        }
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

        val tips = ArrayList<String>()
        when {
            d.ramGb <= 3 -> tips.add(
                "Grafis: Halus, FPS: Normal/Tinggi, bayangan dan efek: Mati. RAM ${d.ramGb}GB kecil, " +
                    "tutup aplikasi lain dan pakai Turbo sebelum main."
            )
            d.ramGb <= 6 -> tips.add("Grafis: Standar, FPS: Tinggi, bayangan: Mati supaya lebih stabil.")
            else -> tips.add("Grafis: Standar sampai Ultra, FPS: Maks (layar ${d.hz}Hz).")
        }
        if (d.freeGb < 5f) {
            tips.add("Penyimpanan bebas tinggal %.1fGB. Kosongkan minimal 5GB agar game tidak patah-patah.".format(d.freeGb))
        }
        if (d.hz >= 90) {
            tips.add("Layar ${d.hz}Hz: aktifkan FPS tertinggi di game. Bidikan terasa lebih cepat, jadi sensi sudah diturunkan sedikit.")
        }
        tips.add(
            when (style) {
                0 -> "2 jari: sensi dibuat lebih rendah agar bidikan stabil. Fokus latihan geser pelan dan terkontrol."
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
        if (cs.contains(1)) tips.add("Bidikan terlalu cepat: semua sensi diturunkan. Masih liar? Ketuk 'Terlalu cepat'.")
        if (cs.contains(2)) tips.add("Bidikan terlalu lambat: semua sensi dinaikkan. Pastikan layar bersih dan pelindung layar tidak licin.")
        if (cs.contains(3)) tips.add("Recoil susah: Red Dot dan 2x diturunkan agar tarikan ke bawah lebih terkontrol.")
        if (cs.contains(4)) tips.add("Sensi tidak konsisten: bersihkan layar, cek pelindung layar, dan matikan penghemat daya yang membatasi sentuhan.")
        if (cs.contains(5)) tips.add("Lag: turunkan grafis, tutup aplikasi lain, pakai Turbo, dan pantau suhu dan FPS di overlay.")
        if (cs.contains(6)) tips.add("HP panas: nyalakan Alarm Panas, kurangi grafis dan FPS, lepas casing tebal, jangan main sambil mengisi daya.")
        if (cs.contains(7)) tips.add("Jari terhalang: kecilkan ukuran tombol dan geser tombol tembak agar tidak menutup target.")
        tips.add("Izinkan Finz Game Space tanpa batasan baterai agar overlay stabil di ${d.brand} ${d.model}.")
        tips.add("Angka ini titik awal dari pola umum, bukan angka resmi Garena. Tes 3-5 menit di Training Ground lalu setel dengan tombol di bawah.")
        return Pair(v, tips)
    }

    fun format(d: DevInfo, v: IntArray): String {
        val sb = StringBuilder()
        sb.append("Sensi Free Fire - ").append(d.brand).append(' ').append(d.model).append('\n')
        for (i in v.indices) sb.append(KEYS[i]).append(": ").append(v[i]).append('\n')
        return sb.toString()
    }
}
