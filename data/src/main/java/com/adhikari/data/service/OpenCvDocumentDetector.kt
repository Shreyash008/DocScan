package com.adhikari.data.service

import android.graphics.RectF
import android.net.Uri
import com.adhikari.domain.service.DocumentDetector

class OpenCvDocumentDetector : DocumentDetector {
    override suspend fun detectDocument(imageUri: Uri): RectF {
        // TODO: Implement actual OpenCV document detection
        // For now, return a default rectangle (full image)
        return RectF(0f, 0f, 1f, 1f)
    }
}
