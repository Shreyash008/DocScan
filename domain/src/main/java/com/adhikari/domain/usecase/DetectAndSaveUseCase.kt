package com.adhikari.domain.usecase

import android.net.Uri
import com.adhikari.domain.model.Document
import com.adhikari.domain.repository.ImageRepository
import com.adhikari.domain.service.DocumentDetector
import com.adhikari.domain.service.ImageProcessor

class DetectAndSaveUseCase(
    private val detector: DocumentDetector,
    private val processor: ImageProcessor,
    private val repository: ImageRepository
) {
    suspend operator fun invoke(imageUri: Uri, name: String): Document {
        // Detect document boundaries
        val detectedBounds = detector.detectDocument(imageUri)
        
        // Process the image (crop, enhance, etc.)
        val processedUri = processor.processImage(imageUri, detectedBounds)
        
        // Save the processed document
        return repository.saveImage(processedUri, name)
    }
}
