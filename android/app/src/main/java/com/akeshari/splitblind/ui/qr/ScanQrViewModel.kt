package com.akeshari.splitblind.ui.qr

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.crypto.Identity
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
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

sealed class ScanResult {
    data object Idle : ScanResult()
    data object Loading : ScanResult()
    data class Joined(val groupId: String) : ScanResult()
    data class AlreadyMember(val groupId: String) : ScanResult()
    data class Error(val message: String) : ScanResult()
}

@HiltViewModel
class ScanQrViewModel @Inject constructor(
    private val groupDao: GroupDao,
    private val identity: Identity,
    private val syncEngine: SyncEngine,
    private val firebaseDatabase: FirebaseDatabase
) : ViewModel() {

    private val _scanResult = MutableStateFlow<ScanResult>(ScanResult.Idle)
    val scanResult: StateFlow<ScanResult> = _scanResult

    fun handleScannedUrl(url: String) {
        if (_scanResult.value is ScanResult.Loading) return
        _scanResult.value = ScanResult.Loading

        viewModelScope.launch {
            try {
                val uri = Uri.parse(url)
                val fragment = uri.fragment
                val shortCode = uri.getQueryParameter("c")
                val groupId = uri.getQueryParameter("g")

                when {
                    shortCode != null -> {
                        // Resolve short code
                        val snapshot = firebaseDatabase.getReference("links/$shortCode").get().await()
                        val gid = snapshot.child("g").getValue(String::class.java)
                        val name = snapshot.child("n").getValue(String::class.java) ?: "Shared Group"
                        val key = snapshot.child("k").getValue(String::class.java)
                        val groupKey = (fragment?.takeIf { it.isNotEmpty() }) ?: key ?: ""
                        if (gid != null && groupKey.isNotEmpty()) {
                            joinGroup(gid, groupKey, name)
                        } else {
                            _scanResult.value = ScanResult.Error("Invalid QR code")
                        }
                    }
                    groupId != null && fragment != null -> {
                        val parts = fragment.split("|", limit = 2)
                        val groupKey = parts[0]
                        val groupName = if (parts.size > 1) java.net.URLDecoder.decode(parts[1], "UTF-8") else "Shared Group"
                        joinGroup(groupId, groupKey, groupName)
                    }
                    else -> {
                        _scanResult.value = ScanResult.Error("Not a valid SplitBlind QR code")
                    }
                }
            } catch (e: Exception) {
                Log.e("ScanQrViewModel", "Failed to handle scanned URL", e)
                _scanResult.value = ScanResult.Error("Failed to process QR code")
            }
        }
    }

    private suspend fun joinGroup(groupId: String, groupKeyBase64: String, groupName: String) {
        val existing = groupDao.getGroup(groupId)
        if (existing != null) {
            _scanResult.value = ScanResult.AlreadyMember(groupId)
            return
        }

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

        _scanResult.value = ScanResult.Joined(groupId)
    }
}
