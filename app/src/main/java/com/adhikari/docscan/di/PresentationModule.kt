package com.adhikari.docscan.di

import com.adhikari.docscan.ui.viewmodel.ScanViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel {
        ScanViewModel(
            detectAndSaveUseCase = get(),
            convertToScanStyleUseCase = get(),
            exportPdfUseCase = get()
        )
    }
}
