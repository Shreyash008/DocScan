package com.adhikari.domain.usecase

import android.net.Uri
import com.adhikari.domain.model.Document
import com.adhikari.domain.repository.ImageRepository

class ExportPdfUseCase(
    private val repository: ImageRepository
) {
    suspend operator fun invoke(documents: List<Document>): Uri {
        return repository.exportToPdf(documents)
    }
}
