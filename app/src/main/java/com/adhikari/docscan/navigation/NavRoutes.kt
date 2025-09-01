package com.adhikari.docscan.navigation

sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("home")
    object DocumentScanner : NavRoutes("document_scanner")
}
