package com.delion.pdfconverter.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                title = { Text("Images to PDF", fontWeight = FontWeight.SemiBold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            FilePickerCard(
                label = if (images.isEmpty()) "Select images" else "${images.size} images selected",
                helper = if (images.isEmpty()) "Tap to browse" else "Tap to change selection",
                enabled = !isConverting,
                onClick = { pickImages.launch("image/*") }
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

            if (images.isNotEmpty()) {
                SectionLabel("Order (drag to reorder)")
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images, key = { it.toString() }) { uri ->
                        ReorderableItem(reorderState, key = uri.toString()) { isDragging ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = if (isDragging) 8.dp else 0.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DragHandle,
                                        contentDescription = "Drag",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.draggableHandle()
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = uri.lastPathSegment ?: uri.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { images = images.filterNot { it == uri } },
                                        enabled = !isConverting
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PrimaryButton(
                text = "Convert to PDF",
                enabled = images.isNotEmpty() && !isConverting,
                onClick = {
                    if (images.isEmpty()) return@PrimaryButton
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
                }
            )

            if (isConverting) ProgressBlock(progress, "Image")
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