package com.adhikari.docscan.navigation

sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("home")
    object Camera : NavRoutes("camera")
    object Crop : NavRoutes("crop")
    object Preview : NavRoutes("preview")
    object PdfPreview : NavRoutes("pdf_preview")
}
