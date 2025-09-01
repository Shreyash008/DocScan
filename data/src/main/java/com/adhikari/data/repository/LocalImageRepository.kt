package com.adhikari.data.repository

import android.content.Context
import android.net.Uri
import com.adhikari.domain.model.Document
import com.adhikari.domain.repository.ImageRepository
import com.adhikari.domain.service.PdfExporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.UUID

class LocalImageRepository(
    private val context: Context,
    private val pdfExporter: PdfExporter
) : ImageRepository {
    
    private val documentsFlow = MutableStateFlow<List<Document>>(emptyList())
    
    override suspend fun saveImage(uri: Uri, name: String): Document {
        val document = Document(
            id = UUID.randomUUID().toString(),
            name = name,
            imageUri = uri,
            scanQuality = com.adhikari.domain.model.ScanQuality.HIGH
        )
        
        val currentList = documentsFlow.value.toMutableList()
        currentList.add(document)
        documentsFlow.value = currentList
        
        return document
    }
    
    override suspend fun getDocuments(): Flow<List<Document>> {
        return documentsFlow
    }
    
    override suspend fun deleteDocument(document: Document) {
        val currentList = documentsFlow.value.toMutableList()
        currentList.removeAll { it.id == document.id }
        documentsFlow.value = currentList
    }
    
    override suspend fun exportToPdf(documents: List<Document>): Uri {
        return pdfExporter.exportToPdf(documents)
    }
}
