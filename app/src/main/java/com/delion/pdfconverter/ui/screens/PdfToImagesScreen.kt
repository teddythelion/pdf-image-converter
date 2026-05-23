package com.delion.pdfconverter.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.delion.pdfconverter.conversion.ConversionProgress
import com.delion.pdfconverter.conversion.PdfToImageConverter
import com.delion.pdfconverter.conversion.Quality
import com.delion.pdfconverter.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import com.delion.pdfconverter.ui.components.AdBanner
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
                title = { Text("PDF to Images", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            FilePickerCard(
                label = if (selectedPdf == null) "Select a PDF" else (selectedPdfName ?: "PDF selected"),
                helper = if (selectedPdf == null) "Tap to browse files" else "Tap to change",
                enabled = !isConverting,
                onClick = { pickPdf.launch("application/pdf") }
            )

            SectionLabel("Quality")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
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
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = quality == q,
                                onClick = { quality = q },
                                enabled = !isConverting
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(q.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleSmall)
                                Text("${q.dpi} DPI",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            PrimaryButton(
                text = "Convert",
                enabled = selectedPdf != null && !isConverting,
                onClick = {
                    val uri = selectedPdf ?: return@PrimaryButton
                    scope.launch {
                        isConverting = true
                        errorMessage = null
                        resultUris = emptyList()
                        try {
                            val converter = PdfToImageConverter(context)
                            val uris = converter.convert(uri, quality) { p -> progress = p }
                            resultUris = uris
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Conversion failed"
                        } finally {
                            isConverting = false
                            progress = null
                        }
                    }
                }
            )

            if (isConverting) {
                ProgressBlock(progress, "Page")
            }

            errorMessage?.let { ErrorBlock(it) }

            if (resultUris.isNotEmpty()) {
                SuccessBlock(
                    message = "Saved ${resultUris.size} ${if (resultUris.size == 1) "image" else "images"} to Downloads",
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "image/jpeg"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(resultUris))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share images"))
                    }
                )
            }
            AdBanner()
        }
    }
}