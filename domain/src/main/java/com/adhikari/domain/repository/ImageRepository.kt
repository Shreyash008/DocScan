package com.adhikari.domain.repository

import android.net.Uri
import com.adhikari.domain.model.Document
import kotlinx.coroutines.flow.Flow

interface ImageRepository {
    suspend fun saveImage(uri: Uri, name: String): Document
    suspend fun getDocuments(): Flow<List<Document>>
    suspend fun deleteDocument(document: Document)
    suspend fun exportToPdf(documents: List<Document>): Uri
}
