package com.akeshari.splitblind.ui.recovery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.crypto.RecoveryBackupData
import com.akeshari.splitblind.crypto.RecoveryManager
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.entity.GroupEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.sync.SyncEngine
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupPassphraseState(
    val passphrase: String = "",
    val isCustom: Boolean = false,
    val customInput: String = "",
    val saved: Boolean = false,
    val isBacking: Boolean = false,
    val backupDone: Boolean = false,
    val error: String? = null
)

data class RecoverState(
    val passphrase: String = "",
    val isRecovering: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RecoveryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val identity: Identity,
    private val groupDao: GroupDao,
    private val firebase: FirebaseDatabase,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _setupState = MutableStateFlow(SetupPassphraseState(passphrase = RecoveryManager.generatePassphrase()))
    val setupState: StateFlow<SetupPassphraseState> = _setupState

    private val _recoverState = MutableStateFlow(RecoverState())
    val recoverState: StateFlow<RecoverState> = _recoverState

    fun regeneratePassphrase() {
        _setupState.value = _setupState.value.copy(
            passphrase = RecoveryManager.generatePassphrase(),
            isCustom = false,
            customInput = ""
        )
    }

    fun toggleCustom() {
        val s = _setupState.value
        if (s.isCustom) {
            // Switch back to generated
            _setupState.value = s.copy(isCustom = false, customInput = "", error = null)
        } else {
            _setupState.value = s.copy(isCustom = true, error = null)
        }
    }

    fun setCustomInput(input: String) {
        _setupState.value = _setupState.value.copy(customInput = input, error = null)
    }

    fun setSaved(saved: Boolean) {
        _setupState.value = _setupState.value.copy(saved = saved)
    }

    fun downloadPassphrase() {
        val pp = getActivePassphrase() ?: return
        RecoveryManager.downloadPassphrase(context, pp)
    }

    fun sharePassphrase() {
        val pp = getActivePassphrase() ?: return
        RecoveryManager.sharePassphrase(context, pp)
    }

    fun confirmSetup(): Boolean {
        val pp = getActivePassphrase()
        if (pp == null || !RecoveryManager.validatePassphrase(pp)) {
            _setupState.value = _setupState.value.copy(error = "Passphrase must have at least 3 words and 8 characters")
            return false
        }

        identity.recoveryPassphrase = pp
        _setupState.value = _setupState.value.copy(isBacking = true)

        viewModelScope.launch {
            try {
                RecoveryManager.backupToRecovery(pp, identity, groupDao, firebase)
                _setupState.value = _setupState.value.copy(isBacking = false, backupDone = true)
            } catch (e: Exception) {
                _setupState.value = _setupState.value.copy(isBacking = false, error = "Backup failed: ${e.message}")
            }
        }
        return true
    }

    private fun getActivePassphrase(): String? {
        val s = _setupState.value
        val pp = if (s.isCustom) s.customInput.trim() else s.passphrase
        return pp.ifBlank { null }
    }

    // --- Recover ---

    fun setRecoverPassphrase(pp: String) {
        _recoverState.value = _recoverState.value.copy(passphrase = pp, error = null)
    }

    fun recover() {
        val pp = _recoverState.value.passphrase.trim()
        if (!RecoveryManager.validatePassphrase(pp)) {
            _recoverState.value = _recoverState.value.copy(error = "Enter at least 3 words")
            return
        }

        _recoverState.value = _recoverState.value.copy(isRecovering = true, error = null)

        viewModelScope.launch {
            try {
                val data = RecoveryManager.recoverFromPassphrase(pp, firebase)
                if (data != null) {
                    applyRecoveryData(data, pp)
                    _recoverState.value = _recoverState.value.copy(isRecovering = false, success = true)
                } else {
                    _recoverState.value = _recoverState.value.copy(
                        isRecovering = false,
                        error = "No backup found for this passphrase. Check your words and try again."
                    )
                }
            } catch (e: Exception) {
                _recoverState.value = _recoverState.value.copy(
                    isRecovering = false,
                    error = "Recovery failed: ${e.message}"
                )
            }
        }
    }

    private suspend fun applyRecoveryData(data: RecoveryBackupData, passphrase: String) {
        identity.memberId = data.memberId
        identity.displayName = data.displayName
        identity.recoveryPassphrase = passphrase

        val now = System.currentTimeMillis()
        for (g in data.groups) {
            groupDao.insertGroup(
                GroupEntity(
                    groupId = g.groupId,
                    name = g.name,
                    groupKeyBase64 = g.groupKeyBase64,
                    createdBy = g.createdBy,
                    createdAt = g.createdAt,
                    hlcTimestamp = now
                )
            )
            groupDao.insertMember(
                MemberEntity(
                    groupId = g.groupId,
                    memberId = data.memberId,
                    displayName = data.displayName,
                    joinedAt = g.createdAt,
                    hlcTimestamp = now
                )
            )
            syncEngine.startListening(g.groupId, g.groupKeyBase64)
        }
    }
}
