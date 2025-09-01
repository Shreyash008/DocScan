package com.adhikari.docscan.ui.screen

import android.R.attr.contentDescription
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.adhikari.docscan.helper.toGrayscale
import com.adhikari.docscan.helper.toHighContrast
import com.adhikari.docscan.navigation.NavRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import android.util.Log
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import com.adhikari.docscan.R

enum class FilterType(val title: String) {
    ORIGINAL("Original"),
    GRAYSCALE("Grayscale"),
    HIGH_CONTRAST("High Contrast")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    navController: NavController,
    imageUri: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(FilterType.ORIGINAL) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var filteredBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rotationAngle by remember { mutableStateOf(0f) } // Track rotation
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load the image when the screen starts
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                isLoading = true
                errorMessage = null

                imageUri?.let { uriString ->
                    Log.d("PreviewScreen", "Loading image from URI: $uriString")

                    // Parse the URI and decode with proper options
                    val uri = Uri.parse(uriString)
                    val inputStream = context.contentResolver.openInputStream(uri)

                    if (inputStream != null) {
                        // Use options to avoid memory issues with large images
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = false
                            inSampleSize = 1 // Keep original size for cropped images
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }

                        originalBitmap = BitmapFactory.decodeStream(inputStream, null, options)
                        inputStream.close()

                        if (originalBitmap != null) {
                            Log.d("PreviewScreen", "Bitmap loaded successfully: ${originalBitmap!!.width}x${originalBitmap!!.height}")
                            // Apply initial filter
                            filteredBitmap = originalBitmap
                        } else {
                            errorMessage = "Failed to decode image"
                            Log.e("PreviewScreen", "Bitmap decoding returned null")
                        }
                    } else {
                        errorMessage = "Failed to open image stream"
                        Log.e("PreviewScreen", "Could not open input stream for URI: $uri")
                    }
                } ?: run {
                    errorMessage = "No image URI provided"
                    Log.e("PreviewScreen", "No image URI provided")
                }
            } catch (e: Exception) {
                errorMessage = "Error loading image: ${e.message}"
                Log.e("PreviewScreen", "Error loading image", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Apply filter whenever selection changes
    LaunchedEffect(selectedFilter, originalBitmap) {
        if (originalBitmap != null && !originalBitmap!!.isRecycled) {
            withContext(Dispatchers.Default) {
                try {
                    filteredBitmap = when (selectedFilter) {
                        FilterType.ORIGINAL -> originalBitmap
                        FilterType.GRAYSCALE -> originalBitmap?.toGrayscale()
                        FilterType.HIGH_CONTRAST -> originalBitmap?.toHighContrast()
                    }
                    Log.d("PreviewScreen", "Applied filter: $selectedFilter")
                } catch (e: Exception) {
                    Log.e("PreviewScreen", "Error applying filter", e)
                    filteredBitmap = originalBitmap // Fallback to original
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Rotation button
                    IconButton(
                        onClick = {
                            rotationAngle = (rotationAngle + 90f) % 360f
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rotate_right),
                            contentDescription = "Rotate Image"
                        )
                    }

                    // Continue button
                    IconButton(
                        onClick = {
                            // Save the filtered bitmap with current rotation and navigate
                            filteredBitmap?.let { bitmap ->
                                if (!bitmap.isRecycled) {
                                    val rotatedBitmap = if (rotationAngle != 0f) {
                                        rotateBitmap(bitmap, rotationAngle)
                                    } else {
                                        bitmap
                                    }

                                    val savedUri = saveFilteredBitmap(context, rotatedBitmap)
                                    if (savedUri != null) {
                                        navController.navigate(
                                            "${NavRoutes.PdfPreview.route}?imageUri=${Uri.encode(savedUri.toString())}"
                                        )
                                    } else {
                                        Log.e("PreviewScreen", "Failed to save filtered bitmap")
                                    }

                                    // Clean up rotated bitmap if it's different
                                    if (rotatedBitmap != bitmap && !rotatedBitmap.isRecycled) {
                                        rotatedBitmap.recycle()
                                    }
                                }
                            }
                        },
                        enabled = filteredBitmap != null && !filteredBitmap!!.isRecycled
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Continue")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Image Preview Area - Using consistent ContentScale.Fit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .background(
                        color = Color.Gray.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator()
                    }
                    errorMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Failed to load image",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    filteredBitmap != null && !filteredBitmap!!.isRecycled -> {
                        Image(
                            bitmap = filteredBitmap!!.asImageBitmap(),
                            contentDescription = "Preview Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit // Consistent with CropScreen
                        )
                    }
                    else -> {
                        Text("No image available")
                    }
                }
            }

            // Filter Options
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Choose Filter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(FilterType.values()) { filter ->
                            FilterOption(
                                filter = filter,
                                isSelected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                enabled = originalBitmap != null && !originalBitmap!!.isRecycled
                            )
                        }
                    }
                }
            }
        }
    }

    // Clean up bitmaps when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (originalBitmap != null && !originalBitmap!!.isRecycled && originalBitmap != filteredBitmap) {
                    originalBitmap!!.recycle()
                }
                if (filteredBitmap != null && !filteredBitmap!!.isRecycled && filteredBitmap != originalBitmap) {
                    filteredBitmap!!.recycle()
                }
            } catch (e: Exception) {
                Log.e("PreviewScreen", "Error cleaning up bitmaps", e)
            }
        }
    }
}

@Composable
fun FilterOption(
    filter: FilterType,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(80.dp)
            .clickable(enabled = enabled) { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !enabled -> Color.Gray.copy(alpha = 0.1f)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Filter preview indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when (filter) {
                            FilterType.ORIGINAL -> Color.Blue.copy(alpha = if (enabled) 1f else 0.3f)
                            FilterType.GRAYSCALE -> Color.Gray.copy(alpha = if (enabled) 1f else 0.3f)
                            FilterType.HIGH_CONTRAST -> Color.Black.copy(alpha = if (enabled) 1f else 0.3f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = filter.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (enabled) Color.Unspecified else Color.Gray
            )
        }
    }
}

// Helper function to rotate bitmap
private fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
    val matrix = android.graphics.Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// Helper function to save the filtered bitmap
private fun saveFilteredBitmap(context: android.content.Context, bitmap: Bitmap): Uri? {
    return try {
        val file = File(context.cacheDir, "filtered_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        Log.d("PreviewScreen", "Saved filtered bitmap to: ${file.absolutePath}")
        file.toUri()
    } catch (e: Exception) {
        Log.e("PreviewScreen", "Failed to save filtered bitmap", e)
        null
    }
}