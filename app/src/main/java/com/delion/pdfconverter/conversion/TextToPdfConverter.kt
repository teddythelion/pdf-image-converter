package com.delion.pdfconverter.conversion

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class TextToPdfConverter(private val context: Context) {

    private val pageWidth = 612    // 8.5" * 72
    private val pageHeight = 792   // 11" * 72
    private val margin = 48f
    private val fontSize = 12f
    private val lineSpacing = 1.4f

    suspend fun convert(
        textUri: Uri,
        outputName: String,
        onProgress: (ConversionProgress) -> Unit
    ): Uri? = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(textUri)?.use {
            it.bufferedReader().readText()
        } ?: return@withContext null

        val pdfDocument = PdfDocument()
        val paint = Paint().apply {
            textSize = fontSize
            isAntiAlias = true
        }

        try {
            val lineHeight = fontSize * lineSpacing
            val usableWidth = pageWidth - margin * 2
            val maxY = pageHeight - margin

            val wrappedLines = mutableListOf<String>()
            text.split('\n').forEach { rawLine ->
                if (rawLine.isEmpty()) {
                    wrappedLines.add("")
                    return@forEach
                }
                wrappedLines.addAll(wrapLine(rawLine, paint, usableWidth))
            }

            val linesPerPage = ((maxY - margin) / lineHeight).toInt()
            val totalPages = (wrappedLines.size + linesPerPage - 1) / linesPerPage.coerceAtLeast(1)

            wrappedLines.chunked(linesPerPage).forEachIndexed { pageIndex, pageLines ->
                onProgress(ConversionProgress(pageIndex + 1, totalPages))
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                var y = margin + fontSize
                pageLines.forEach { line ->
                    canvas.drawText(line, margin, y, paint)
                    y += lineHeight
                }
                pdfDocument.finishPage(page)
            }

            val finalName = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
            savePdf(pdfDocument, finalName)
        } finally {
            pdfDocument.close()
        }
    }

    private fun wrapLine(line: String, paint: Paint, maxWidth: Float): List<String> {
        if (paint.measureText(line) <= maxWidth) return listOf(line)
        val words = line.split(' ')
        val result = mutableListOf<String>()
        var current = StringBuilder()
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) result.add(current.toString())
                if (paint.measureText(word) > maxWidth) {
                    result.addAll(breakLongWord(word, paint, maxWidth))
                    current = StringBuilder()
                } else {
                    current = StringBuilder(word)
                }
            }
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }

    private fun breakLongWord(word: String, paint: Paint, maxWidth: Float): List<String> {
        val pieces = mutableListOf<String>()
        var current = StringBuilder()
        word.forEach { ch ->
            val candidate = "$current$ch"
            if (paint.measureText(candidate) <= maxWidth) {
                current.append(ch)
            } else {
                pieces.add(current.toString())
                current = StringBuilder(ch.toString())
            }
        }
        if (current.isNotEmpty()) pieces.add(current.toString())
        return pieces
    }

    private fun savePdf(pdfDocument: PdfDocument, filename: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            savePdfMediaStore(pdfDocument, filename)
        } else {
            savePdfLegacy(pdfDocument, filename)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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