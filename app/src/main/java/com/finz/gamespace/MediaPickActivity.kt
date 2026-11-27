package com.finz.gamespace

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class MediaPickActivity : Activity() {

    private val ui = Handler(Looper.getMainLooper())
    private var id = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = intent.getIntExtra("id", -1)
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        i.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(i, 7)
        } catch (e: Exception) {
            Toast.makeText(this, "Pemilih berkas tidak tersedia", Toast.LENGTH_LONG).show()
            done()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null || id < 0) {
            done()
            return
        }
        Thread { copy(uri) }.start()
    }

    private fun copy(uri: Uri) {
        val cr = contentResolver
        val mime = cr.getType(uri) ?: ""
        val isVid = mime.startsWith("video")
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: (if (isVid) "mp4" else "jpg")
        filesDir.listFiles()?.forEach {
            if (it.name.startsWith("stream_media_$id.")) it.delete()
        }
        val out = File(filesDir, "stream_media_$id.$ext")
        val max = 60L * 1048576L
        var total = 0L
        var err = ""
        try {
            cr.openInputStream(uri)?.use { inp ->
                FileOutputStream(out).use { o ->
                    val buf = ByteArray(65536)
                    while (true) {
                        val n = inp.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > max) throw Exception("Berkas terlalu besar (maks 60MB)")
                        o.write(buf, 0, n)
                    }
                }
            } ?: throw Exception("Berkas tidak bisa dibaca")
        } catch (e: Exception) {
            err = e.message ?: "Gagal menyalin"
            try { out.delete() } catch (x: Exception) {}
        }
        if (err.isEmpty()) {
            val list = StreamStore.load(applicationContext)
            val l = list.firstOrNull { it.id == id }
            if (l != null) {
                l.text = (if (isVid) "vid:" else "img:") + out.absolutePath
                StreamStore.save(applicationContext, list)
            }
        }
        ui.post {
            if (err.isNotEmpty()) {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show()
            } else {
                StreamLayers.sync(applicationContext)
                Toast.makeText(this, "Berhasil dipasang", Toast.LENGTH_SHORT).show()
            }
            done()
        }
    }

    private fun done() {
        val cb = StreamLayers.afterPick
        StreamLayers.afterPick = null
        if (cb != null) ui.post { cb() }
        finish()
    }
}
