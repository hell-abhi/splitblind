package com.akeshari.splitblind.ui.settle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.dao.HistoryDao
import com.akeshari.splitblind.data.database.dao.SettlementDao
import com.akeshari.splitblind.data.database.entity.HistoryEntity
import com.akeshari.splitblind.data.database.entity.SettlementEntity
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.sync.OpData
import com.akeshari.splitblind.sync.OpPayload
import com.akeshari.splitblind.sync.SyncEngine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SettleUpState(
    val fromName: String = "",
    val toName: String = "",
    val amountCents: Long = 0,
    val fullAmountCents: Long = 0,
    val editedAmount: String = "",
    val isSettling: Boolean = false,
    val settled: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettleUpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupDao: GroupDao,
    private val settlementDao: SettlementDao,
    private val historyDao: HistoryDao,
    private val syncEngine: SyncEngine,
    private val identity: Identity
) : ViewModel() {

    val groupId: String = savedStateHandle["groupId"] ?: ""
    private val fromId: String = savedStateHandle["from"] ?: ""
    private val toId: String = savedStateHandle["to"] ?: ""
    private val amountCents: Long = savedStateHandle["amountCents"] ?: 0L

    private val _state = MutableStateFlow(SettleUpState(
        amountCents = amountCents,
        fullAmountCents = amountCents,
        editedAmount = String.format("%.2f", amountCents / 100.0)
    ))
    val state: StateFlow<SettleUpState> = _state

    init {
        viewModelScope.launch {
            val members = groupDao.getMembersList(groupId)
            val fromName = members.find { it.memberId == fromId }?.displayName ?: fromId
            val toName = members.find { it.memberId == toId }?.displayName ?: toId
            _state.value = _state.value.copy(fromName = fromName, toName = toName)
        }
    }

    fun setEditedAmount(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            val cents = ((amount.toDoubleOrNull() ?: 0.0) * 100).toLong()
            _state.value = _state.value.copy(
                editedAmount = amount,
                amountCents = cents,
                error = if (cents > amountCents) "Cannot exceed full amount" else null
            )
        }
    }

    fun settle() {
        val settleAmount = _state.value.amountCents
        if (settleAmount <= 0) {
            _state.value = _state.value.copy(error = "Enter a valid amount")
            return
        }
        if (settleAmount > _state.value.fullAmountCents) {
            _state.value = _state.value.copy(error = "Cannot exceed full amount")
            return
        }

        _state.value = _state.value.copy(isSettling = true, error = null)

        viewModelScope.launch {
            val settlementId = UUID.randomUUID().toString().take(16)
            val now = System.currentTimeMillis()

            settlementDao.insertSettlement(
                SettlementEntity(
                    settlementId = settlementId,
                    groupId = groupId,
                    fromMember = fromId,
                    toMember = toId,
                    amountCents = settleAmount,
                    createdAt = now,
                    hlcTimestamp = now
                )
            )

            val group = groupDao.getGroup(groupId)
            if (group != null) {
                syncEngine.pushOp(
                    groupId = groupId,
                    groupKeyBase64 = group.groupKeyBase64,
                    payload = OpPayload(
                        id = UUID.randomUUID().toString(),
                        type = "settlement",
                        data = OpData(
                            settlementId = settlementId,
                            groupId = groupId,
                            fromMember = fromId,
                            toMember = toId,
                            amountCents = settleAmount,
                            createdAt = now
                        ),
                        hlc = now,
                        author = identity.memberId
                    )
                )

                // Record history
                val myName = identity.displayName.ifBlank { identity.memberId.take(8) }
                val historyId = UUID.randomUUID().toString().take(16)
                val newDataJson = Json.encodeToString(
                    mapOf(
                        "fromMember" to fromId,
                        "toMember" to toId,
                        "amountCents" to settleAmount.toString()
                    )
                )
                val historyEntry = HistoryEntity(
                    historyId = historyId,
                    settlementId = settlementId,
                    entityType = "settlement",
                    action = "created",
                    newData = newDataJson,
                    changedBy = identity.memberId,
                    changedByName = myName,
                    changedAt = now
                )
                historyDao.insert(historyEntry)

                syncEngine.pushOp(
                    groupId = groupId,
                    groupKeyBase64 = group.groupKeyBase64,
                    payload = OpPayload(
                        id = UUID.randomUUID().toString(),
                        type = "history",
                        data = OpData(
                            historyId = historyId,
                            settlementId = settlementId,
                            entityType = "settlement",
                            action = "created",
                            newDataJson = newDataJson,
                            changedBy = identity.memberId,
                            changedByName = myName,
                            changedAt = now
                        ),
                        hlc = now,
                        author = identity.memberId
                    )
                )
            }

            _state.value = _state.value.copy(isSettling = false, settled = true)
        }
    }
}
