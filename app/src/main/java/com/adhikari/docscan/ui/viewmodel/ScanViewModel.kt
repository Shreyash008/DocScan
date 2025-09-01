package com.adhikari.docscan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhikari.domain.model.Document
import com.adhikari.domain.usecase.DetectAndSaveUseCase
import com.adhikari.domain.usecase.ConvertToScanStyleUseCase
import com.adhikari.domain.usecase.ExportPdfUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScanViewModel(
    private val detectAndSaveUseCase: DetectAndSaveUseCase,
    private val convertToScanStyleUseCase: ConvertToScanStyleUseCase,
    private val exportPdfUseCase: ExportPdfUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()
    
    fun scanDocument(imageUri: String, name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val document = detectAndSaveUseCase(android.net.Uri.parse(imageUri), name)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    documents = _uiState.value.documents + document,
                    message = "Document scanned successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error scanning document: ${e.message}"
                )
            }
        }
    }
    
    fun exportToPdf() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val pdfUri = exportPdfUseCase(_uiState.value.documents)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "PDF exported successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error exporting PDF: ${e.message}"
                )
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class ScanUiState(
    val isLoading: Boolean = false,
    val documents: List<Document> = emptyList(),
    val message: String? = null
)
