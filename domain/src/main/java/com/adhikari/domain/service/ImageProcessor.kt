package com.adhikari.domain.service

import android.graphics.RectF
import android.net.Uri

interface ImageProcessor {
    suspend fun processImage(imageUri: Uri, bounds: RectF): Uri
    suspend fun convertToScanStyle(imageUri: Uri): Uri
}
