package com.adhikari.docscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.adhikari.docscan.navigation.NavRoutes
import com.adhikari.docscan.ui.screen.*
import com.adhikari.docscan.ui.theme.DocScanTheme
import com.adhikari.docscan.ui.viewmodel.ScanViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DocScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: ScanViewModel = koinViewModel()
                    
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.Home.route
                    ) {
                        composable(NavRoutes.Home.route) {
                            HomeScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable(NavRoutes.Camera.route) {
                            CameraScreen(navController = navController)
                        }
                        composable(
                            route = "${NavRoutes.Crop.route}?imageUri={imageUri}",
                            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
                        ) {
                                backStackEntry ->
                            val imageUri = backStackEntry.arguments?.getString("imageUri")?.toUri()
                            CropScreen(navController = navController, imageUri = imageUri)
                        }
                        composable(
                            route = "${NavRoutes.Preview.route}?imageUri={imageUri}",
                            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
                        ) {
                            val imageUri = it.arguments?.getString("imageUri")
                            PreviewScreen(navController = navController, imageUri = imageUri)
                        }

                        composable(NavRoutes.PdfPreview.route) {
                            PdfPreviewScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}