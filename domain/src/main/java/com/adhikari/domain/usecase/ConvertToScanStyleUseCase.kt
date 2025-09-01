package com.adhikari.domain.usecase

import android.net.Uri
import com.adhikari.domain.service.ImageProcessor

class ConvertToScanStyleUseCase(
    private val processor: ImageProcessor
) {
    suspend operator fun invoke(imageUri: Uri): Uri {
        return processor.convertToScanStyle(imageUri)
    }
}
