package com.adhikari.domain.model

import android.net.Uri

data class Document(
    val id: String,
    val name: String,
    val imageUri: Uri,
    val scanQuality: ScanQuality,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ScanQuality {
    LOW, MEDIUM, HIGH
}
