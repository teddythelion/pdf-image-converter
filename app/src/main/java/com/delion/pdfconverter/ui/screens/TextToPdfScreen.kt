package com.delion.pdfconverter.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                title = { Text("Text to PDF", fontWeight = FontWeight.SemiBold) },
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
                label = if (selectedText == null) "Select text file" else (selectedTextName ?: "File selected"),
                helper = if (selectedText == null) "Tap to browse" else "Tap to change",
                enabled = !isConverting,
                onClick = { pickText.launch("text/*") }
            )

            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Filename") },
                singleLine = true,
                enabled = !isConverting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            PrimaryButton(
                text = "Convert to PDF",
                enabled = selectedText != null && !isConverting,
                onClick = {
                    val uri = selectedText ?: return@PrimaryButton
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
                }
            )

            if (isConverting) ProgressBlock(progress, "Page")
            errorMessage?.let { ErrorBlock(it) }

            resultUri?.let { uri ->
                SuccessBlock(
                    message = "PDF saved to Downloads",
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share PDF"))
                    }
                )
            }
        }
    }
}