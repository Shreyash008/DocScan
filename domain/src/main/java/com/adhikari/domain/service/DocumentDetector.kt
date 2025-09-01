package com.adhikari.domain.service

import android.graphics.RectF
import android.net.Uri

interface DocumentDetector {
    suspend fun detectDocument(imageUri: Uri): RectF
}
