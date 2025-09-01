package com.adhikari.docscan.di

import com.adhikari.data.repository.LocalImageRepository
import com.adhikari.data.service.OpenCvDocumentDetector
import com.adhikari.data.service.OpenCvImageProcessor
import com.adhikari.data.service.PdfExporterImpl
import com.adhikari.domain.repository.ImageRepository
import com.adhikari.domain.service.DocumentDetector
import com.adhikari.domain.service.ImageProcessor
import com.adhikari.domain.service.PdfExporter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single<DocumentDetector> { OpenCvDocumentDetector() }
    single<ImageProcessor> { OpenCvImageProcessor() }
    single<PdfExporter> { PdfExporterImpl(androidContext()) }
    single<ImageRepository> { LocalImageRepository(androidContext(), get()) }
}
