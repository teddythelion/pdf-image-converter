package com.delion.pdfconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.delion.pdfconverter.ui.navigation.AppNavigation
import com.delion.pdfconverter.ui.theme.PDFImageConverterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PDFImageConverterTheme {
                AppNavigation()
            }
        }
    }
}