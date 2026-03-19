package com.akeshari.splitblind.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.sync.SyncDeviceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncDeviceViewModel @Inject constructor(
    private val syncDeviceManager: SyncDeviceManager
) : ViewModel() {

    private val _syncCode = MutableStateFlow<String?>(null)
    val syncCode: StateFlow<String?> = _syncCode

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _restoreSuccess = MutableStateFlow(false)
    val restoreSuccess: StateFlow<Boolean> = _restoreSuccess

    private var timerJob: Job? = null

    fun generateCode(pin: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val encBundle = syncDeviceManager.generateSyncBundle(pin)
                val code = syncDeviceManager.generateSyncCode()
                syncDeviceManager.uploadSyncBundle(code, encBundle)
                _syncCode.value = code
                startTimer(code)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to generate sync code"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startTimer(code: String) {
        timerJob?.cancel()
        _timerSeconds.value = 300 // 5 minutes
        timerJob = viewModelScope.launch {
            while (_timerSeconds.value > 0) {
                delay(1000)
                _timerSeconds.value -= 1
            }
            // Code expired, clean up
            try {
                syncDeviceManager.deleteSyncEntry(code)
            } catch (_: Exception) {}
            _syncCode.value = null
        }
    }

    fun restore(code: String, pin: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val encBundle = syncDeviceManager.downloadSyncBundle(code)
                if (encBundle == null) {
                    _error.value = "Sync code not found or expired"
                    return@launch
                }
                val bundle = try {
                    syncDeviceManager.restoreFromBundle(encBundle, pin)
                } catch (e: Exception) {
                    _error.value = "Wrong PIN or corrupted data"
                    return@launch
                }
                syncDeviceManager.applyRestore(bundle)
                // Clean up the sync entry
                try {
                    syncDeviceManager.deleteSyncEntry(code)
                } catch (_: Exception) {}
                _restoreSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Restore failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
