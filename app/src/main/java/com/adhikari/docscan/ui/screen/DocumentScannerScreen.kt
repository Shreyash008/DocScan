package com.adhikari.docscan.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import android.util.Log
import androidx.compose.ui.res.painterResource
import com.adhikari.docscan.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Intent
import androidx.core.content.FileProvider
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image as ITextImage
import com.itextpdf.io.image.ImageDataFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isScanning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scannedPages by remember { mutableStateOf<List<ScannedPage>>(emptyList()) }
    var selectedPageIndex by remember { mutableStateOf(0) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var savedPdfPath by remember { mutableStateOf<String?>(null) }

    val documentScanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(10)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
        )
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        isScanning = false
        when (result.resultCode) {
            android.app.Activity.RESULT_OK -> {
                val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                scanningResult?.pages?.let { pages ->
                    if (pages.isNotEmpty()) {
                        // Convert ML Kit pages to our ScannedPage model
                        val convertedPages = pages.mapIndexed { index, page ->
                            ScannedPage(
                                uri = page.imageUri,
                                pageIndex = index,
                                width = 0, // ML Kit doesn't provide width/height directly
                                height = 0  // ML Kit doesn't provide width/height directly
                            )
                        }
                        scannedPages = convertedPages
                        selectedPageIndex = 0
                    }
                }
            }
            android.app.Activity.RESULT_CANCELED -> {
                Log.d("DocumentScanner", "Scanning cancelled")
            }
            else -> {
                errorMessage = "Scanning failed"
            }
        }
    }

    fun startScanning() {
        isScanning = true
        errorMessage = null
        scannedPages = emptyList()
        savedPdfPath = null

        documentScanner.getStartScanIntent(context as androidx.activity.ComponentActivity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
            .addOnFailureListener { exception ->
                isScanning = false
                errorMessage = "Failed to start scanner: ${exception.message}"
                Log.e("DocumentScanner", "Error starting scanner", exception)
            }
    }

    fun generateAndSavePdf() {
        if (scannedPages.isEmpty()) return
        
        scope.launch {
            isGeneratingPdf = true
            try {
                val pdfPath = createPdfFromScannedPages(context, scannedPages)
                savedPdfPath = pdfPath
                if (pdfPath != null) {
                    // Show success message
                    errorMessage = null
                    // Show toast and open PDF
                    Toast.makeText(context, "PDF saved successfully!", Toast.LENGTH_SHORT).show()
                    openPdfWithSystemApp(context, pdfPath)
                } else {
                    errorMessage = "Failed to create PDF"
                }
            } catch (e: Exception) {
                errorMessage = "Error creating PDF: ${e.message}"
                Log.e("DocumentScanner", "Error creating PDF", e)
            } finally {
                isGeneratingPdf = false
            }
        }
    }

    fun sharePdf() {
        savedPdfPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                showShareOptions(context, file)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Scanner") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (scannedPages.isNotEmpty() && savedPdfPath != null) {
                        IconButton(onClick = { sharePdf() }) {
                            Icon(Icons.Default.Share, contentDescription = "Share PDF")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isScanning) {
                ScanningState()
            } else if (scannedPages.isEmpty()) {
                InitialState(
                    onStartScanning = { startScanning() }
                )
            } else {
                // Show scanned pages and PDF preview
                ScannedPagesView(
                    pages = scannedPages,
                    selectedIndex = selectedPageIndex,
                    onPageSelected = { selectedPageIndex = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                ActionButtons(
                    onSavePdf = { generateAndSavePdf() },
                    onSharePdf = { sharePdf() },
                    isGenerating = isGeneratingPdf,
                    hasPdf = savedPdfPath != null
                )
            }
            
            // Error message
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                ErrorCard(error = error)
            }
        }
    }
}

@Composable
private fun ScanningState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Opening Document Scanner...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InitialState(onStartScanning: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.doc_scan),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Document Scanner",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Capture documents with automatic edge detection and perspective correction",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartScanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Scanning")
        }
    }
}

@Composable
private fun ScannedPagesView(
    pages: List<ScannedPage>,
    selectedIndex: Int,
    onPageSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Page navigation
            if (pages.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    pages.forEachIndexed { index, _ ->
                        FilterChip(
                            selected = selectedIndex == index,
                            onClick = { onPageSelected(index) },
                            label = { Text("Page ${index + 1}") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Page preview
            val currentPage = pages[selectedIndex]
            PagePreview(page = currentPage)
        }
    }
}

@Composable
private fun PagePreview(page: ScannedPage) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(page) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(page.uri)
                bitmap = inputStream?.use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                Log.e("PagePreview", "Error loading page image", e)
            }
        }
    }
    
    bitmap?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Scanned page",
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )
    } ?: CircularProgressIndicator()
}

@Composable
private fun ActionButtons(
    onSavePdf: () -> Unit,
    onSharePdf: () -> Unit,
    isGenerating: Boolean,
    hasPdf: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSavePdf,
            enabled = !isGenerating,
            modifier = Modifier.weight(1f)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                Icon(painterResource(R.drawable.save), contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save PDF")
            }
        }
        
        Button(
            onClick = onSharePdf,
            enabled = hasPdf && !isGenerating,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share PDF")
        }
    }
}

@Composable
private fun ErrorCard(error: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

// Data model for scanned pages
data class ScannedPage(
    val uri: Uri,
    val pageIndex: Int,
    val width: Int = 0,
    val height: Int = 0
)

// PDF generation
private suspend fun createPdfFromScannedPages(
    context: android.content.Context,
    pages: List<ScannedPage>
): String? = withContext(Dispatchers.IO) {
    try {
        // Use cache directory which is more accessible
        val pdfFile = File(context.cacheDir, "scanned_document_${System.currentTimeMillis()}.pdf")
        val writer = PdfWriter(pdfFile)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)
        
        pages.forEach { page ->
            try {
                val inputStream = context.contentResolver.openInputStream(page.uri)
                val imageBytes = inputStream?.readBytes()
                inputStream?.close()
                
                if (imageBytes != null) {
                    val image = ITextImage(ImageDataFactory.create(imageBytes))
                    image.scaleToFit(pdfDoc.defaultPageSize.width, pdfDoc.defaultPageSize.height)
                    document.add(image)
                }
            } catch (e: Exception) {
                Log.e("PDF Creation", "Error adding page ${page.pageIndex}", e)
            }
        }
        
        document.close()
        Log.d("PDF Creation", "PDF saved to: ${pdfFile.absolutePath}")
        return@withContext pdfFile.absolutePath
    } catch (e: Exception) {
        Log.e("PDF Creation", "Error creating PDF", e)
        return@withContext null
    }
}

// PDF sharing with specific app options
private fun showShareOptions(context: android.content.Context, file: File) {
    try {
        var uri: android.net.Uri? = null
        
        // Try FileProvider first
        try {
            uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.w("PDF Sharing", "FileProvider failed, trying direct file access", e)
            // Fallback to direct file URI (for older Android versions)
            uri = android.net.Uri.fromFile(file)
        }
        
        if (uri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Create chooser with specific app options
            val chooserIntent = Intent.createChooser(shareIntent, "Share PDF")
            
            // Add specific app intents
            val specificIntents = mutableListOf<Intent>()
            
            // Gmail intent
            val gmailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Scanned Document")
                putExtra(Intent.EXTRA_TEXT, "Please find the scanned document attached.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.google.android.gm")
            }
            specificIntents.add(gmailIntent)
            
            // WhatsApp intent
            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")
            }
            specificIntents.add(whatsappIntent)
            
            // Telegram intent
            val telegramIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("org.telegram.messenger")
            }
            specificIntents.add(telegramIntent)
            
            // Drive intent
            val driveIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.google.android.apps.docs")
            }
            specificIntents.add(driveIntent)
            
            // Add all specific intents to chooser
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, specificIntents.toTypedArray())
            
            context.startActivity(chooserIntent)
        } else {
            Toast.makeText(context, "Failed to prepare PDF for sharing", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("PDF Sharing", "Error sharing PDF", e)
        Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Open PDF with system default app
private fun openPdfWithSystemApp(context: android.content.Context, pdfPath: String) {
    try {
        val file = File(pdfPath)
        if (file.exists()) {
            // Try FileProvider first
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w("PDF Opening", "FileProvider failed, trying direct file access", e)
            }
            
            // Fallback: try direct file access
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(android.net.Uri.fromFile(file), "application/pdf")
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w("PDF Opening", "Direct file access failed", e)
            }
            
            // Last resort: show file location
            Toast.makeText(
                context, 
                "PDF saved to: ${file.absolutePath}", 
                Toast.LENGTH_LONG
            ).show()
        }
    } catch (e: Exception) {
        Log.e("PDF Opening", "Error opening PDF", e)
        Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
    }
}