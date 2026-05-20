package com.delion.pdfconverter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPdfToImagesClick: () -> Unit,
    onImagesToPdfClick: () -> Unit,
    onTextToPdfClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF & Image Converter") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onPdfToImagesClick,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) { Text("PDF → Images") }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onImagesToPdfClick,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) { Text("Images → PDF") }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onTextToPdfClick,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) { Text("Text → PDF") }
        }
    }
}