package com.adhikari.domain.service

import android.net.Uri
import com.adhikari.domain.model.Document

interface PdfExporter {
    suspend fun exportToPdf(documents: List<Document>): Uri
}
