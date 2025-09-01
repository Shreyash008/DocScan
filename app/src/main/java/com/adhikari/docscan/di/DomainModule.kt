package com.adhikari.docscan.di

import com.adhikari.domain.usecase.DetectAndSaveUseCase
import com.adhikari.domain.usecase.ConvertToScanStyleUseCase
import com.adhikari.domain.usecase.ExportPdfUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { DetectAndSaveUseCase(get(), get(), get()) }
    factory { ConvertToScanStyleUseCase(get()) }
    factory { ExportPdfUseCase(get()) }
}
