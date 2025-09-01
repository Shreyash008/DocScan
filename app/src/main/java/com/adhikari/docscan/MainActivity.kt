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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.adhikari.docscan.navigation.NavRoutes
import com.adhikari.docscan.ui.screen.DocumentScannerScreen
import com.adhikari.docscan.ui.screen.HomeScreen
import com.adhikari.docscan.ui.screen.SavedScansScreen
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
                        composable(NavRoutes.DocumentScanner.route) {
                            DocumentScannerScreen(navController = navController)
                        }
                        composable(NavRoutes.SavedScans.route) {
                            SavedScansScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}