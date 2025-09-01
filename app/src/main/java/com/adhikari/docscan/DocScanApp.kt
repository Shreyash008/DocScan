package com.adhikari.docscan

import android.app.Application
import com.adhikari.docscan.di.dataModule
import com.adhikari.docscan.di.domainModule
import com.adhikari.docscan.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class DocScanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger() // optional
            androidContext(this@DocScanApp)
            modules(listOf(domainModule, dataModule, presentationModule))
        }
    }
}
