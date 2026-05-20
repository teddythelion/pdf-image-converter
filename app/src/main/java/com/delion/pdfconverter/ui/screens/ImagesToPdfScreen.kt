package com.delion.pdfconverter.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.delion.pdfconverter.conversion.ConversionProgress
import com.delion.pdfconverter.conversion.ImageToPdfConverter
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesToPdfScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var outputName by remember { mutableStateOf("converted") }
    var progress by remember { mutableStateOf<ConversionProgress?>(null) }
    var resultUri by remember { mutableStateOf<Uri?>(null) }
    var isConverting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            images = uris
            resultUri = null
            errorMessage = null
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        images = images.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Images → PDF") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { pickImages.launch("image/*") },
                enabled = !isConverting,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (images.isEmpty()) "Select Images" else "Change Selection") }

            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Output filename") },
                singleLine = true,
                enabled = !isConverting,
                modifier = Modifier.fillMaxWidth()
            )

            if (images.isNotEmpty()) {
                Text(
                    "${images.size} images. Long-press and drag to reorder.",
                    style = MaterialTheme.typography.bodySmall
                )
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(images, key = { it.toString() }) { uri ->
                        ReorderableItem(reorderState, key = uri.toString()) { isDragging ->
                            Surface(
                                tonalElevation = if (isDragging) 8.dp else 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "≡  ${uri.lastPathSegment ?: uri.toString()}",
                                        modifier = Modifier.weight(1f).draggableHandle()
                                    )
                                    TextButton(
                                        onClick = {
                                            images = images.filterNot { it == uri }
                                        },
                                        enabled = !isConverting
                                    ) { Text("Remove") }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (images.isEmpty()) return@Button
                    scope.launch {
                        isConverting = true
                        errorMessage = null
                        resultUri = null
                        try {
                            val converter = ImageToPdfConverter(context)
                            val uri = converter.convert(images, outputName.ifBlank { "converted" }) { p ->
                                progress = p
                            }
                            resultUri = uri
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Conversion failed"
                        } finally {
                            isConverting = false
                            progress = null
                        }
                    }
                },
                enabled = images.isNotEmpty() && !isConverting,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Convert to PDF") }

            if (isConverting) {
                progress?.let { p ->
                    LinearProgressIndicator(
                        progress = { p.currentPage.toFloat() / p.totalPages },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Image ${p.currentPage} of ${p.totalPages}")
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