package com.adhikari.docscan.ui.screen

// --- For UI Drawing (Compose) ---
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

// --- For Bitmap Cropping (Android) ---
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.geometry.Offset
import androidx.core.net.toUri
import com.adhikari.docscan.navigation.NavRoutes
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import java.io.File
import java.io.FileOutputStream
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    imageUri: Uri?,
    navController: NavController
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Track the actual display size of the image
    var imageDisplaySize by remember { mutableStateOf(IntSize.Zero) }
    var actualImageSize by remember { mutableStateOf(IntSize.Zero) }

    // Load actual image dimensions
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                actualImageSize = IntSize(options.outWidth, options.outHeight)
                Log.d("CropScreen", "Actual image size: ${actualImageSize.width}x${actualImageSize.height}")
            } catch (e: Exception) {
                Log.e("CropScreen", "Error loading image dimensions", e)
            }
        }
    }

    // Initialize crop points as percentage of display area
    var cropPoints by remember {
        mutableStateOf(
            listOf(
                Offset(0.15f, 0.15f),   // Top-left
                Offset(0.85f, 0.15f),   // Top-right
                Offset(0.85f, 0.85f),   // Bottom-right
                Offset(0.15f, 0.85f)    // Bottom-left
            )
        )
    }
    // Remove the remembered screenCropPoints calculation since we calculate in real-time now
    var selectedPoint by remember { mutableStateOf(-1) }
    var isDragging by remember { mutableStateOf(false) }

    // Fullscreen Box
    Box(modifier = Modifier.fillMaxSize()) {
        // Load the actual captured image - fullscreen
        if (imageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(model = imageUri),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        imageDisplaySize = coordinates.size
                        Log.d("CropScreen", "Image display size: ${imageDisplaySize.width}x${imageDisplaySize.height}")
                    },
                contentScale = ContentScale.Fit
            )
        }

        // Crop overlay with smooth dragging
        if (imageDisplaySize != IntSize.Zero) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageDisplaySize) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // Find closest point with touch radius using current screen coordinates
                                val currentScreenPoints = cropPoints.map { percentPoint ->
                                    Offset(
                                        percentPoint.x * imageDisplaySize.width,
                                        percentPoint.y * imageDisplaySize.height
                                    )
                                }

                                val closestIndex = currentScreenPoints.indices.minByOrNull { index ->
                                    val point = currentScreenPoints[index]
                                    (offset - point).getDistance()
                                } ?: -1

                                if (closestIndex != -1 &&
                                    (offset - currentScreenPoints[closestIndex]).getDistance() < 80f
                                ) {
                                    selectedPoint = closestIndex
                                    isDragging = true
                                    Log.d("CropScreen", "Selected point: $selectedPoint")
                                }
                            },
                            onDrag = { change, _ ->
                                if (selectedPoint != -1 && imageDisplaySize.width > 0 && imageDisplaySize.height > 0) {
                                    // Convert screen coordinates back to percentages
                                    val newPercentPoint = Offset(
                                        (change.position.x / imageDisplaySize.width).coerceIn(0.01f, 0.99f),
                                        (change.position.y / imageDisplaySize.height).coerceIn(0.01f, 0.99f)
                                    )

                                    // Direct state update for smooth dragging
                                    cropPoints = cropPoints.mapIndexed { index, point ->
                                        if (index == selectedPoint) newPercentPoint else point
                                    }
                                }
                            },
                            onDragEnd = {
                                selectedPoint = -1
                                isDragging = false
                                Log.d("CropScreen", "Drag ended")
                            }
                        )
                    }
            ) {
                // Calculate current screen coordinates in real-time
                val currentScreenPoints = cropPoints.map { percentPoint ->
                    Offset(
                        percentPoint.x * imageDisplaySize.width,
                        percentPoint.y * imageDisplaySize.height
                    )
                }

                // Create path for the crop region using current screen coordinates
                val path = Path().apply {
                    if (currentScreenPoints.isNotEmpty()) {
                        moveTo(currentScreenPoints[0].x, currentScreenPoints[0].y)
                        currentScreenPoints.drop(1).forEach { p -> lineTo(p.x, p.y) }
                        close()
                    }
                }

                // Draw semi-transparent overlay outside crop area
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    size = size
                )

                // Cut out the crop area to show original image
                clipPath(path, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                    drawRect(
                        color = Color.Black.copy(alpha = 0.6f),
                        size = size
                    )
                }

                // Draw polygon border with glow effect
                drawPath(
                    path,
                    color = Color.White.copy(alpha = 0.3f),
                    style = Stroke(width = 6f)
                )
                drawPath(
                    path,
                    color = Color.White,
                    style = Stroke(width = 2f)
                )

                // Draw connecting lines with better visibility
                currentScreenPoints.forEachIndexed { index, point ->
                    val nextPoint = currentScreenPoints[(index + 1) % 4]
                    // Outer stroke for contrast
                    drawLine(
                        color = Color.Black.copy(alpha = 0.5f),
                        start = point,
                        end = nextPoint,
                        strokeWidth = 5f
                    )
                    // Inner line
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f),
                        start = point,
                        end = nextPoint,
                        strokeWidth = 3f
                    )
                }

                // Draw corner handles with modern design
                currentScreenPoints.forEachIndexed { index, point ->
                    val isSelected = index == selectedPoint
                    val baseSize = if (isSelected) 16f else 14f

                    // Modern square handle with rounded corners
                    // Outer shadow/glow
                    drawRect(
                        color = Color.Black.copy(alpha = 0.4f),
                        topLeft = Offset(point.x - baseSize - 2f, point.y - baseSize - 2f),
                        size = androidx.compose.ui.geometry.Size((baseSize + 2f) * 2, (baseSize + 2f) * 2)
                    )

                    // Main handle - modern square with gradient effect
                    val handleColor = when {
                        isSelected -> Color(0xFF2196F3) // Material Blue
                        isDragging -> Color.White.copy(alpha = 0.9f)
                        else -> Color.White
                    }

                    drawRect(
                        color = handleColor,
                        topLeft = Offset(point.x - baseSize, point.y - baseSize),
                        size = androidx.compose.ui.geometry.Size(baseSize * 2, baseSize * 2)
                    )

                    // Inner accent
                    drawRect(
                        color = if (isSelected) Color.White else Color(0xFF2196F3),
                        topLeft = Offset(point.x - 4f, point.y - 4f),
                        size = androidx.compose.ui.geometry.Size(8f, 8f)
                    )

                    // Selection indicator - modern ring
                    if (isSelected) {
                        drawCircle(
                            color = Color(0xFF2196F3).copy(alpha = 0.3f),
                            radius = 24f,
                            center = point,
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        }

        // Bottom controls with better positioning
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Color.Black.copy(alpha = 0.3f)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset button
                FloatingActionButton(
                    onClick = {
                        // Reset to default rectangular crop
                        cropPoints = listOf(
                            Offset(0.15f, 0.15f),
                            Offset(0.85f, 0.15f),
                            Offset(0.85f, 0.85f),
                            Offset(0.15f, 0.85f)
                        )
                    },
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = Color.Black
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Crop"
                    )
                }

                // Confirm crop button
                FloatingActionButton(
                    onClick = {
                        if (imageUri != null && actualImageSize != IntSize.Zero) {
                            val croppedUri = cropAndTransformToRectangle(
                                context,
                                imageUri,
                                cropPoints,
                                actualImageSize
                            )
                            if (croppedUri != null) {
                                Log.d("CropScreen", "Cropped and transformed file saved at: $croppedUri")
                                navController.navigate(
                                    "${NavRoutes.Preview.route}?imageUri=${Uri.encode(croppedUri.toString())}"
                                )
                            } else {
                                Toast.makeText(context, "Cropping failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    containerColor = Color.Green.copy(alpha = 0.9f),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm Crop"
                    )
                }
            }
        }
    }
}

// Extension functions for Offset calculations
private fun Offset.getDistance(): Float = sqrt(x * x + y * y)
private operator fun Offset.minus(other: Offset): Offset = Offset(x - other.x, y - other.y)

/**
 * Calculate distance between two points
 */
private fun getDistance(p1: PointF, p2: PointF): Float {
    return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
}

/**
 * Determine the best output aspect ratio and orientation based on the quadrilateral shape
 */
private fun getBestAspectRatio(points: List<PointF>): Pair<Int, Int> {
    // Calculate approximate width and height of the quadrilateral
    val topWidth = getDistance(points[0], points[1])
    val bottomWidth = getDistance(points[3], points[2])
    val leftHeight = getDistance(points[0], points[3])
    val rightHeight = getDistance(points[1], points[2])

    val avgWidth = (topWidth + bottomWidth) / 2
    val avgHeight = (leftHeight + rightHeight) / 2

    // For documents, prioritize portrait orientation (height > width)
    // Standard document ratios (always in portrait format)
    val portraitRatios = listOf(
        Pair(3, 4),    // 3:4 portrait
        Pair(21, 29),  // A4 portrait
        Pair(9, 16),   // 9:16 portrait
        Pair(2, 3)     // 2:3 portrait
    )
    GmsDocumentScanner
    // If the detected area is wider than tall, we'll still output as portrait
    // by swapping dimensions to ensure document orientation
    val outputRatio = if (avgHeight > avgWidth) {
        // Already portrait-like, choose best fit
        val currentRatio = avgWidth / avgHeight
        portraitRatios.minByOrNull { abs((it.first.toFloat() / it.second) - currentRatio) }?.let {
            Pair(it.first, it.second)
        } ?: Pair(3, 4)
    } else {
        // Landscape input, convert to portrait output
        Pair(3, 4) // Default to 3:4 portrait
    }

    Log.d("CropScreen", "Detected dimensions: ${avgWidth}x$avgHeight, Output ratio: ${outputRatio.first}:${outputRatio.second}")
    return outputRatio
}

/**
 * Crops a quadrilateral region and transforms it into a rectangle using perspective correction
 */
fun cropAndTransformToRectangle(
    context: Context,
    sourceUri: Uri,
    cropPoints: List<Offset>, // Percentage values (0.0 to 1.0)
    actualImageSize: IntSize
): Uri? {
    return try {
        // Load the original bitmap
        val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream.close()

        Log.d("CropScreen", "Original bitmap: ${originalBitmap.width}x${originalBitmap.height}")

        // Convert percentage points to actual pixel coordinates
        val pixelPoints = cropPoints.map { percentPoint ->
            PointF(
                percentPoint.x * originalBitmap.width,
                percentPoint.y * originalBitmap.height
            )
        }

        Log.d("CropScreen", "Pixel points: $pixelPoints")

        // Determine the best output aspect ratio
        val (outputWidth, outputHeight) = getBestAspectRatio(pixelPoints)

        // Calculate output dimensions (maintain reasonable resolution)
        val maxDimension = 1200 // Max width or height
        val actualOutputWidth: Int
        val actualOutputHeight: Int

        if (outputWidth >= outputHeight) {
            actualOutputWidth = maxDimension
            actualOutputHeight = (maxDimension * outputHeight / outputWidth.toFloat()).toInt()
        } else {
            actualOutputHeight = maxDimension
            actualOutputWidth = (maxDimension * outputWidth / outputHeight.toFloat()).toInt()
        }

        Log.d("CropScreen", "Output dimensions: ${actualOutputWidth}x$actualOutputHeight")

        // Create destination rectangle corners
        val destPoints = floatArrayOf(
            0f, 0f,                                    // Top-left
            actualOutputWidth.toFloat(), 0f,           // Top-right
            actualOutputWidth.toFloat(), actualOutputHeight.toFloat(), // Bottom-right
            0f, actualOutputHeight.toFloat()           // Bottom-left
        )

        // Create source quadrilateral corners
        val srcPoints = floatArrayOf(
            pixelPoints[0].x, pixelPoints[0].y,  // Top-left
            pixelPoints[1].x, pixelPoints[1].y,  // Top-right
            pixelPoints[2].x, pixelPoints[2].y,  // Bottom-right
            pixelPoints[3].x, pixelPoints[3].y   // Bottom-left
        )

        // Create perspective transformation matrix
        val matrix = Matrix()
        if (!matrix.setPolyToPoly(srcPoints, 0, destPoints, 0, 4)) {
            Log.e("CropScreen", "Failed to create transformation matrix")
            return null
        }

        // Create result bitmap with white background
        val resultBitmap = Bitmap.createBitmap(
            actualOutputWidth,
            actualOutputHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = AndroidCanvas(resultBitmap)

        // Fill with white background
        canvas.drawColor(android.graphics.Color.WHITE)

        // Apply transformation and draw
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        canvas.save()
        canvas.concat(matrix)
        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
        canvas.restore()

        // Save the transformed bitmap
        val file = File(context.cacheDir, "cropped_transformed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        // Clean up
        originalBitmap.recycle()
        resultBitmap.recycle()

        Log.d("CropScreen", "Successfully cropped, transformed and saved to: ${file.absolutePath}")
        file.toUri()

    } catch (e: Exception) {
        Log.e("CropScreen", "Error cropping and transforming image", e)
        null
    }
}