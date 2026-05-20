package com.delion.pdfconverter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.delion.pdfconverter.ui.screens.HomeScreen
import com.delion.pdfconverter.ui.screens.ImagesToPdfScreen
import com.delion.pdfconverter.ui.screens.PdfToImagesScreen
import com.delion.pdfconverter.ui.screens.TextToPdfScreen
object Routes {
    const val HOME = "home"
    const val PDF_TO_IMAGES = "pdf_to_images"
    const val IMAGES_TO_PDF = "images_to_pdf"
    const val TEXT_TO_PDF = "text_to_pdf"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onPdfToImagesClick = { navController.navigate(Routes.PDF_TO_IMAGES) },
                onImagesToPdfClick = { navController.navigate(Routes.IMAGES_TO_PDF) },
                onTextToPdfClick = { navController.navigate(Routes.TEXT_TO_PDF) }
            )
        }
        composable(Routes.PDF_TO_IMAGES) {
            PdfToImagesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.IMAGES_TO_PDF) {
            ImagesToPdfScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TEXT_TO_PDF) {
            TextToPdfScreen(onBack = { navController.popBackStack() })
        }
    }
}