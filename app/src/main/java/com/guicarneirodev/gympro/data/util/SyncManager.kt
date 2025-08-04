package com.guicarneirodev.gympro.data.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SyncManager {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var syncCount = 0

    fun startSync() {
        synchronized(this) {
            syncCount++
            _isSyncing.value = true
        }
    }

    fun endSync() {
        synchronized(this) {
            syncCount--
            if (syncCount <= 0) {
                syncCount = 0
                _isSyncing.value = false
            }
        }
    }
}