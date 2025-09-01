package com.adhikari.docscan.ui.screen

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.adhikari.docscan.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScansScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var savedPdfs by remember { mutableStateOf<List<PdfFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var pdfToDelete by remember { mutableStateOf<PdfFile?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isGalleryView by remember { mutableStateOf(false) }
    
    // Load saved PDFs when screen is created
    LaunchedEffect(Unit) {
        loadSavedPdfs(context) { pdfs ->
            savedPdfs = pdfs
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Saved Scans",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isGalleryView = !isGalleryView }
                    ) {
                        Icon(
                            painter = if (isGalleryView) painterResource(R.drawable.grid_off) else painterResource(R.drawable.grid_on),
                            contentDescription = if (isGalleryView) "Switch to List View" else "Switch to Gallery View"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (savedPdfs.isEmpty()) {
            EmptyState(paddingValues = paddingValues)
        } else {
            if (isGalleryView) {
                SavedPdfsGallery(
                    pdfs = savedPdfs,
                    paddingValues = paddingValues,
                    onPdfDeleted = { pdf ->
                        pdfToDelete = pdf
                        showDeleteDialog = true
                    },
                    onPdfShared = { pdf ->
                        sharePdf(context, pdf)
                    },
                    onPdfViewed = { pdf ->
                        openPdf(context, pdf)
                    }
                )
            } else {
                SavedPdfsList(
                    pdfs = savedPdfs,
                    paddingValues = paddingValues,
                    onPdfDeleted = { pdf ->
                        pdfToDelete = pdf
                        showDeleteDialog = true
                    },
                    onPdfShared = { pdf ->
                        sharePdf(context, pdf)
                    },
                    onPdfViewed = { pdf ->
                        openPdf(context, pdf)
                    }
                )
            }
        }
        
        // Delete confirmation dialog
        if (showDeleteDialog && pdfToDelete != null) {
            DeleteConfirmationDialog(
                pdfName = pdfToDelete!!.name,
                onConfirm = {
                    val pdf = pdfToDelete!!
                    if (deletePdfFile(pdf.path)) {
                        savedPdfs = savedPdfs.filter { it != pdf }
                        Toast.makeText(context, "PDF deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to delete PDF", Toast.LENGTH_SHORT).show()
                    }
                    showDeleteDialog = false
                    pdfToDelete = null
                },
                onDismiss = {
                    showDeleteDialog = false
                    pdfToDelete = null
                }
            )
        }
    }
}

@Composable
private fun EmptyState(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.info),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No Saved Scans",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your scanned documents will appear here once you save them as PDFs.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SavedPdfsList(
    pdfs: List<PdfFile>,
    paddingValues: PaddingValues,
    onPdfDeleted: (PdfFile) -> Unit,
    onPdfShared: (PdfFile) -> Unit,
    onPdfViewed: (PdfFile) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Saved Documents (${pdfs.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(pdfs) { pdf ->
            PdfCard(
                pdf = pdf,
                onDelete = { onPdfDeleted(pdf) },
                onShare = { onPdfShared(pdf) },
                onView = { onPdfViewed(pdf) }
            )
        }
    }
}

@Composable
private fun SavedPdfsGallery(
    pdfs: List<PdfFile>,
    paddingValues: PaddingValues,
    onPdfDeleted: (PdfFile) -> Unit,
    onPdfShared: (PdfFile) -> Unit,
    onPdfViewed: (PdfFile) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Saved Documents (${pdfs.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(pdfs) { pdf ->
            PdfGalleryCard(
                pdf = pdf,
                onDelete = { onPdfDeleted(pdf) },
                onShare = { onPdfShared(pdf) },
                onView = { onPdfViewed(pdf) }
            )
        }
    }
}

@Composable
private fun PdfCard(
    pdf: PdfFile,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onView: () -> Unit
) {
    var isMarquee by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Info Icon → triggers marquee ONCE
            IconButton(
                onClick = {
                    scope.launch {
                        isMarquee = true
                        delay(10000) // let it scroll for ~5s
                        isMarquee = false
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = "Show full name",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // File Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = pdf.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = if (isMarquee) TextOverflow.Visible else TextOverflow.Ellipsis,
                    modifier = if (isMarquee) Modifier.basicMarquee() else Modifier
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Size: ${formatFileSize(pdf.size)} • ${formatDate(pdf.lastModified)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Actions
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onView) {
                    Icon(
                        painter = painterResource(R.drawable.visibility),
                        contentDescription = "View PDF"
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share PDF")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete PDF",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PdfGalleryCard(
    pdf: PdfFile,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onView: () -> Unit
) {
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingThumbnail by remember { mutableStateOf(true) }
    var enableMarquee by remember { mutableStateOf(false) }

    // Load PDF thumbnail
    LaunchedEffect(pdf.path) {
        thumbnail = generatePdfThumbnail(pdf.path)
        isLoadingThumbnail = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Thumbnail with overlayed actions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .combinedClickable { onView() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoadingThumbnail -> CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    thumbnail != null -> Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = "PDF thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    else -> Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Overlay actions container
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallOverlayButton(icon = R.drawable.visibility, onClick = onView)
                    SmallOverlayButton(icon = R.drawable.share, onClick = onShare)
                    SmallOverlayButton(
                        icon = R.drawable.delete,
                        onClick = onDelete,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // File info
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                if (enableMarquee) {
                    // Marquee effect once
                    LaunchedEffect(Unit) {
                        delay(5000) // stop after 5s
                        enableMarquee = false
                    }
                }

                Text(
                    text = pdf.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (enableMarquee) Int.MAX_VALUE else 1,
                    overflow = if (enableMarquee) TextOverflow.Visible else TextOverflow.Ellipsis,
                    modifier = if (enableMarquee) {
                        Modifier.basicMarquee()
                    } else Modifier
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formatFileSize(pdf.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SmallOverlayButton(
    icon: Int,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(32.dp) // slightly bigger
            .clip(RoundedCornerShape(50))
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(25.dp)
        )
    }
}



// Data class for PDF files
data class PdfFile(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long
)

// Load saved PDFs from cache and files directories
private fun loadSavedPdfs(context: android.content.Context, onLoaded: (List<PdfFile>) -> Unit) {
    val pdfs = mutableListOf<PdfFile>()
    
    try {
        // Check cache directory
        val cacheDir = context.cacheDir
        val cachePdfs = cacheDir.listFiles { file -> 
            file.extension.equals("pdf", ignoreCase = true) 
        } ?: emptyArray()
        
        cachePdfs.forEach { file ->
            pdfs.add(
                PdfFile(
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    lastModified = file.lastModified()
                )
            )
        }
        
        // Check files directory
        val filesDir = context.filesDir
        val filesPdfs = filesDir.listFiles { file -> 
            file.extension.equals("pdf", ignoreCase = true) 
        } ?: emptyArray()
        
        filesPdfs.forEach { file ->
            pdfs.add(
                PdfFile(
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    lastModified = file.lastModified()
                )
            )
        }
        
        // Sort by last modified (newest first)
        pdfs.sortByDescending { it.lastModified }
        
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    onLoaded(pdfs)
}

// Format file size
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}

// Format date
private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

// Share PDF
private fun sharePdf(context: android.content.Context, pdf: PdfFile) {
    try {
        val file = File(pdf.path)
        if (file.exists()) {
            var uri: Uri? = null
            
            // Try FileProvider first
            try {
                uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                // Fallback to direct file URI
                uri = Uri.fromFile(file)
            }
            
            if (uri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Scanned Document: ${pdf.name}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Open PDF
private fun openPdf(context: android.content.Context, pdf: PdfFile) {
    try {
        val file = File(pdf.path)
        if (file.exists()) {
            var uri: Uri? = null
            
            // Try FileProvider first
            try {
                uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                // Fallback to direct file URI
                uri = Uri.fromFile(file)
            }
            
            if (uri != null) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(intent)
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
    }
}

// Generate PDF thumbnail from first page
private fun generatePdfThumbnail(pdfPath: String): Bitmap? {
    return try {
        val file = File(pdfPath)
        if (!file.exists()) return null
        
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(fileDescriptor)
        
        if (pdfRenderer.pageCount > 0) {
            val page = pdfRenderer.openPage(0)
            val bitmap = Bitmap.createBitmap(
                page.width * 2, // Scale up for better quality
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )
            
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            pdfRenderer.close()
            fileDescriptor.close()
            
            bitmap
        } else {
            pdfRenderer.close()
            fileDescriptor.close()
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Delete PDF file
private fun deletePdfFile(filePath: String): Boolean {
    return try {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Composable
private fun DeleteConfirmationDialog(
    pdfName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete PDF",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$pdfName\"? This action cannot be undone."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
