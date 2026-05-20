package com.delion.pdfconverter.conversion

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ImageToPdfConverter(private val context: Context) {

    suspend fun convert(
        imageUris: List<Uri>,
        outputName: String,
        onProgress: (ConversionProgress) -> Unit
    ): Uri? = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val total = imageUris.size

        try {
            imageUris.forEachIndexed { index, uri ->
                onProgress(ConversionProgress(index + 1, total))
                val bitmap = loadBitmap(uri) ?: return@forEachIndexed
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas
                canvas.drawBitmap(bitmap, null, Rect(0, 0, bitmap.width, bitmap.height), null)
                pdfDocument.finishPage(page)
                bitmap.recycle()
            }

            val finalName = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
            savePdf(pdfDocument, finalName)
        } finally {
            pdfDocument.close()
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }

    private fun savePdf(pdfDocument: PdfDocument, filename: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            savePdfMediaStore(pdfDocument, filename)
        } else {
            savePdfLegacy(pdfDocument, filename)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun savePdfMediaStore(pdfDocument: PdfDocument, filename: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                pdfDocument.writeTo(os)
            }
        }
        return uri
    }

    private fun savePdfLegacy(pdfDocument: PdfDocument, filename: String): Uri? {
        @Suppress("DEPRECATION")
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) downloads.mkdirs()
        val file = File(downloads, filename)
        file.outputStream().use { pdfDocument.writeTo(it) }
        return Uri.fromFile(file)
    }
}