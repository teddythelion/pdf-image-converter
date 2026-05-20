package com.delion.pdfconverter.conversion

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class Quality(val dpi: Int) {
    LOW(72), MEDIUM(150), HIGH(300)
}

data class ConversionProgress(val currentPage: Int, val totalPages: Int)

class PdfToImageConverter(private val context: Context) {

    suspend fun convert(
        pdfUri: Uri,
        quality: Quality,
        onProgress: (ConversionProgress) -> Unit
    ): List<Uri> = withContext(Dispatchers.IO) {
        val savedUris = mutableListOf<Uri>()
        val baseName = getDisplayName(pdfUri).substringBeforeLast('.', "pdf_export")

        context.contentResolver.openFileDescriptor(pdfUri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val pageCount = renderer.pageCount
                for (i in 0 until pageCount) {
                    onProgress(ConversionProgress(i + 1, pageCount))
                    renderer.openPage(i).use { page ->
                        val scale = quality.dpi / 72f
                        val width = (page.width * scale).toInt()
                        val height = (page.height * scale).toInt()
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val uri = saveBitmap(bitmap, "${baseName}_page_${i + 1}.jpg")
                        if (uri != null) savedUris.add(uri)
                        bitmap.recycle()
                    }
                }
            }
        }
        savedUris
    }

    private fun getDisplayName(uri: Uri): String {
        var name = "pdf_export"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx)
        }
        return name
    }

    private fun saveBitmap(bitmap: Bitmap, filename: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveBitmapMediaStore(bitmap, filename)
        } else {
            saveBitmapLegacy(bitmap, filename)
        }
    }

    private fun saveBitmapMediaStore(bitmap: Bitmap, filename: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, os)
            }
        }
        return uri
    }

    private fun saveBitmapLegacy(bitmap: Bitmap, filename: String): Uri? {
        @Suppress("DEPRECATION")
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) downloads.mkdirs()
        val file = File(downloads, filename)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        return Uri.fromFile(file)
    }
}