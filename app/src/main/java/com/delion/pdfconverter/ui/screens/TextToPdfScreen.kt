package com.delion.pdfconverter.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.delion.pdfconverter.conversion.ConversionProgress
import com.delion.pdfconverter.conversion.TextToPdfConverter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedText by remember { mutableStateOf<Uri?>(null) }
    var selectedTextName by remember { mutableStateOf<String?>(null) }
    var outputName by remember { mutableStateOf("converted") }
    var progress by remember { mutableStateOf<ConversionProgress?>(null) }
    var resultUri by remember { mutableStateOf<Uri?>(null) }
    var isConverting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pickText = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedText = uri
            selectedTextName = uri.lastPathSegment
            resultUri = null
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Text → PDF") },
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
                onClick = { pickText.launch("text/*") },
                enabled = !isConverting,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (selectedText == null) "Select Text File" else "Change File") }

            selectedTextName?.let {
                Text("Selected: $it", style = MaterialTheme.typography.bodyMedium)
            }

            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Output filename") },
                singleLine = true,
                enabled = !isConverting,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val uri = selectedText ?: return@Button
                    scope.launch {
                        isConverting = true
                        errorMessage = null
                        resultUri = null
                        try {
                            val converter = TextToPdfConverter(context)
                            val result = converter.convert(uri, outputName.ifBlank { "converted" }) { p ->
                                progress = p
                            }
                            resultUri = result
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Conversion failed"
                        } finally {
                            isConverting = false
                            progress = null
                        }
                    }
                },
                enabled = selectedText != null && !isConverting,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Convert to PDF") }

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

            resultUri?.let { uri ->
                Text("Saved to Downloads.", style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share PDF"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Share") }
            }
        }
    }
}