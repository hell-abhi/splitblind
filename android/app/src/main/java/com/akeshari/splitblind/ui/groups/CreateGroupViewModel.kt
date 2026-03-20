package com.akeshari.splitblind.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.crypto.CryptoEngine
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.crypto.RecoveryManager
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.entity.GroupEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.sync.OpData
import com.akeshari.splitblind.sync.OpPayload
import com.akeshari.splitblind.sync.SyncEngine
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CreateGroupState(
    val groupName: String = "",
    val createdGroupId: String? = null,
    val inviteLink: String? = null,
    val isCreating: Boolean = false
)

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val groupDao: GroupDao,
    private val identity: Identity,
    private val syncEngine: SyncEngine,
    private val firebaseDatabase: FirebaseDatabase
) : ViewModel() {

    private val _state = MutableStateFlow(CreateGroupState())
    val state: StateFlow<CreateGroupState> = _state

    fun setGroupName(name: String) {
        _state.value = _state.value.copy(groupName = name)
    }

    fun createGroup() {
        val name = _state.value.groupName.trim()
        if (name.isBlank()) return

        _state.value = _state.value.copy(isCreating = true)

        viewModelScope.launch {
            val groupId = UUID.randomUUID().toString().take(16)
            val groupKeyBase64 = CryptoEngine.generateGroupKey()
            val now = System.currentTimeMillis()

            groupDao.insertGroup(
                GroupEntity(
                    groupId = groupId,
                    name = name,
                    createdBy = identity.memberId,
                    createdAt = now,
                    groupKeyBase64 = groupKeyBase64,
                    hlcTimestamp = now,
                    baseCurrency = identity.defaultCurrency
                )
            )

            groupDao.insertMember(
                MemberEntity(
                    groupId = groupId,
                    memberId = identity.memberId,
                    displayName = identity.displayName,
                    joinedAt = now,
                    hlcTimestamp = now
                )
            )

            // Start syncing and push member_join
            syncEngine.startListening(groupId, groupKeyBase64)
            syncEngine.pushOp(
                groupId = groupId,
                groupKeyBase64 = groupKeyBase64,
                payload = OpPayload(
                    id = UUID.randomUUID().toString(),
                    type = "member_join",
                    data = OpData(
                        memberId = identity.memberId,
                        displayName = identity.displayName,
                        joinedAt = now
                    ),
                    hlc = now,
                    author = identity.memberId
                )
            )

            // Create a short code in Firebase for a cleaner invite link
            val shortCode = syncEngine.createShortCode(groupId, name, groupKeyBase64)
            val inviteLink = "https://hell-abhi.github.io/splitblind/?c=$shortCode#$groupKeyBase64"

            // Auto-backup to recovery if passphrase is set
            try {
                RecoveryManager.autoBackup(identity, groupDao, firebaseDatabase)
            } catch (_: Exception) { }

            _state.value = _state.value.copy(
                createdGroupId = groupId,
                inviteLink = inviteLink,
                isCreating = false
            )
        }
    }
}
