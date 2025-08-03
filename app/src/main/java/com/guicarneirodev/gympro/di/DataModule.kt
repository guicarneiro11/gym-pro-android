package com.guicarneirodev.gympro.di

import com.guicarneirodev.gympro.data.local.preferences.UserPreferencesManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { UserPreferencesManager(androidContext()) }
}