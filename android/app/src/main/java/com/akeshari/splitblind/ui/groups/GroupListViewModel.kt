package com.akeshari.splitblind.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.crypto.CryptoEngine
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.entity.GroupEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.sync.OpPayload
import com.akeshari.splitblind.sync.SyncEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val groupDao: GroupDao,
    private val identity: Identity,
    private val syncEngine: SyncEngine
) : ViewModel() {

    val groups: StateFlow<List<GroupEntity>> = groupDao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun joinGroup(groupId: String, groupKeyBase64: String, groupName: String) {
        viewModelScope.launch {
            val existing = groupDao.getGroup(groupId)
            if (existing != null) return@launch

            val now = System.currentTimeMillis()
            groupDao.insertGroup(
                GroupEntity(
                    groupId = groupId,
                    name = groupName,
                    createdBy = identity.memberId,
                    createdAt = now,
                    groupKeyBase64 = groupKeyBase64,
                    hlcTimestamp = now
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

            // Push member_join op
            syncEngine.startListening(groupId, groupKeyBase64)
            syncEngine.pushOp(
                groupId = groupId,
                groupKeyBase64 = groupKeyBase64,
                payload = OpPayload(
                    type = "member_join",
                    id = UUID.randomUUID().toString().take(16),
                    groupId = groupId,
                    memberId = identity.memberId,
                    displayName = identity.displayName,
                    createdAt = now,
                    hlcTimestamp = now
                )
            )
        }
    }
}
