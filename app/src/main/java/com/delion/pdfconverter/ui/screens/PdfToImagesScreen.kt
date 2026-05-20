package com.delion.pdfconverter.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.delion.pdfconverter.conversion.ConversionProgress
import com.delion.pdfconverter.conversion.PdfToImageConverter
import com.delion.pdfconverter.conversion.Quality
import kotlinx.coroutines.launch
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToImagesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf<String?>(null) }
    var quality by remember { mutableStateOf(Quality.MEDIUM) }
    var progress by remember { mutableStateOf<ConversionProgress?>(null) }
    var resultUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isConverting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pickPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPdf = uri
            selectedPdfName = uri.lastPathSegment
            resultUris = emptyList()
            errorMessage = null
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF → Images") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { pickPdf.launch("application/pdf")},
                enabled = !isConverting,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (selectedPdf == null) "Select PDF" else "Change PDF") }

            selectedPdfName?.let {
                Text("Selected: $it", style = MaterialTheme.typography.bodyMedium)
            }

            Text("Quality", style = MaterialTheme.typography.titleMedium)
            Quality.values().forEach { q ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = quality == q,
                            enabled = !isConverting,
                            onClick = { quality = q }
                        )
                ) {
                    RadioButton(
                        selected = quality == q,
                        onClick = { quality = q },
                        enabled = !isConverting
                    )
                    Text("${q.name} (${q.dpi} DPI)")
                }
            }

            Button(
                onClick = {
                    val uri = selectedPdf ?: return@Button
                    scope.launch {
                        isConverting = true
                        errorMessage = null
                        resultUris = emptyList()
                        try {
                            val converter = PdfToImageConverter(context)
                            val uris = converter.convert(uri, quality) { p ->
                                progress = p
                            }
                            resultUris = uris
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Conversion failed"
                        } finally {
                            isConverting = false
                            progress = null
                        }
                    }
                },
                enabled = selectedPdf != null && !isConverting,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Convert") }

            if (isConverting) {
                progress?.let { p ->
                    LinearProgressIndicator(
                        progress = { p.currentPage.toFloat() / p.totalPages },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Page ${p.currentPage} of ${p.totalPages}")
                } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (resultUris.isNotEmpty()) {
                Text(
                    "Converted ${resultUris.size} pages. Saved to Downloads.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "image/jpeg"
                            putParcelableArrayListExtra(
                                Intent.EXTRA_STREAM,
                                ArrayList(resultUris)
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share images"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Share") }
            }
        }
    }
}