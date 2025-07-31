package com.guicarneirodev.gympro

import android.app.Application
import com.guicarneirodev.gympro.di.firebaseModule
import com.guicarneirodev.gympro.di.repositoryModule
import com.guicarneirodev.gympro.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class GymProApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@GymProApplication)
            modules(
                firebaseModule,
                repositoryModule,
                viewModelModule
            )
        }
    }
}