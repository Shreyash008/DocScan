package com.adhikari.data.service

import android.graphics.RectF
import android.net.Uri
import com.adhikari.domain.service.ImageProcessor

class OpenCvImageProcessor : ImageProcessor {
    override suspend fun processImage(imageUri: Uri, bounds: RectF): Uri {
        // TODO: Implement actual OpenCV image processing
        // For now, return the original URI
        return imageUri
    }
    
    override suspend fun convertToScanStyle(imageUri: Uri): Uri {
        // TODO: Implement scan style conversion
        // For now, return the original URI
        return imageUri
    }
}
