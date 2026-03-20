package com.akeshari.splitblind.ui.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.data.database.dao.ExpenseDao
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.dao.HistoryDao
import com.akeshari.splitblind.data.database.dao.SettlementDao
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import com.akeshari.splitblind.data.database.entity.GroupEntity
import com.akeshari.splitblind.data.database.entity.HistoryEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.data.database.entity.SettlementEntity
import com.akeshari.splitblind.sync.OpData
import com.akeshari.splitblind.sync.OpPayload
import com.akeshari.splitblind.sync.SyncEngine
import com.akeshari.splitblind.util.BalanceCalculator
import com.akeshari.splitblind.util.Debt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

data class GroupDetailState(
    val group: GroupEntity? = null,
    val members: List<MemberEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val settlements: List<SettlementEntity> = emptyList(),
    val allExpenses: List<ExpenseEntity> = emptyList(),
    val allSettlements: List<SettlementEntity> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val netBalances: Map<String, Long> = emptyMap(),
    val memberNames: Map<String, String> = emptyMap(),
    val isSynced: Boolean = false,
    val inviteLink: String? = null,
    val myId: String = "",
    val historyMap: Map<String, List<HistoryEntity>> = emptyMap()
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao,
    private val historyDao: HistoryDao,
    private val syncEngine: SyncEngine,
    val identity: Identity
) : ViewModel() {

    val groupId: String = savedStateHandle["groupId"] ?: ""

    private val _group = MutableStateFlow<GroupEntity?>(null)
    private val _shortCode = MutableStateFlow<String?>(null)

    private val _historyMap = MutableStateFlow<Map<String, List<HistoryEntity>>>(emptyMap())

    val state: StateFlow<GroupDetailState> = combine(
        groupDao.getMembers(groupId),
        expenseDao.getAllExpensesIncludingDeleted(groupId),
        settlementDao.getAllSettlementsIncludingDeleted(groupId),
        syncEngine.syncStatus,
        _shortCode
    ) { members, allExpenses, allSettlements, syncStatus, _ ->
        // Load history for all expenses and settlements
        val expenseIds = allExpenses.map { it.expenseId }
        val settlementIds = allSettlements.map { it.settlementId }
        val expenseHistory = if (expenseIds.isNotEmpty()) historyDao.getHistoryForExpenses(expenseIds) else emptyList()
        val settlementHistory = if (settlementIds.isNotEmpty()) historyDao.getHistoryForSettlements(settlementIds) else emptyList()
        val historyMap = mutableMapOf<String, List<HistoryEntity>>()
        expenseHistory.groupBy { it.expenseId ?: "" }.forEach { (k, v) -> if (k.isNotEmpty()) historyMap[k] = v }
        settlementHistory.groupBy { it.settlementId ?: "" }.forEach { (k, v) -> if (k.isNotEmpty()) historyMap[k] = v }

        val memberNames = members.associate { it.memberId to it.displayName }
        // Balance calculation uses only non-deleted items
        val activeExpenses = allExpenses.filter { !it.isDeleted }
        val activeSettlements = allSettlements.filter { !it.isDeleted }
        val balances = BalanceCalculator.computeNetBalances(activeExpenses, activeSettlements)
        val debts = BalanceCalculator.simplifyDebts(balances)

        val group = _group.value
        val inviteLink = if (group != null && _shortCode.value != null) {
            "https://hell-abhi.github.io/splitblind/?c=${_shortCode.value}#${group.groupKeyBase64}"
        } else null

        GroupDetailState(
            group = group,
            members = members,
            expenses = activeExpenses,
            settlements = activeSettlements,
            allExpenses = allExpenses,
            allSettlements = allSettlements,
            debts = debts,
            netBalances = balances,
            memberNames = memberNames,
            isSynced = syncStatus[groupId] == true,
            inviteLink = inviteLink,
            myId = identity.memberId,
            historyMap = historyMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupDetailState())

    init {
        viewModelScope.launch {
            val group = groupDao.getGroup(groupId) ?: return@launch
            _group.value = group
            syncEngine.startListening(groupId, group.groupKeyBase64)
            // Generate a short code for invite links
            val code = syncEngine.createShortCode(groupId, group.name, group.groupKeyBase64)
            _shortCode.value = code
        }
    }

    fun refresh() {
        val group = _group.value ?: return
        syncEngine.fullSync(groupId, group.groupKeyBase64)
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val deleted = expense.copy(isDeleted = true, hlcTimestamp = now)
            expenseDao.insertExpense(deleted)

            val group = groupDao.getGroup(groupId) ?: return@launch
            val splitAmong: List<String> = try {
                Json.decodeFromString(expense.splitAmong)
            } catch (_: Exception) { emptyList() }
            val paidByMap: Map<String, Long>? = expense.paidByMap?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { null }
            }
            val splitDetails: Map<String, Long>? = expense.splitDetails?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { null }
            }

            syncEngine.pushOp(
                groupId = groupId,
                groupKeyBase64 = group.groupKeyBase64,
                payload = OpPayload(
                    id = UUID.randomUUID().toString(),
                    type = "expense",
                    data = OpData(
                        expenseId = expense.expenseId,
                        groupId = groupId,
                        description = expense.description,
                        amountCents = expense.amountCents,
                        currency = expense.currency,
                        paidBy = expense.paidBy,
                        splitAmong = splitAmong,
                        createdAt = expense.createdAt,
                        isDeleted = true,
                        tag = expense.tag,
                        paidByMap = paidByMap,
                        splitMode = expense.splitMode,
                        splitDetails = splitDetails
                    ),
                    hlc = now,
                    author = identity.memberId
                )
            )

            // Record history
            val myName = identity.displayName.ifBlank { identity.memberId.take(8) }
            val historyId = UUID.randomUUID().toString().take(16)
            val previousDataJson = Json.encodeToString(
                mapOf(
                    "description" to expense.description,
                    "amountCents" to expense.amountCents.toString(),
                    "paidBy" to expense.paidBy,
                    "tag" to (expense.tag ?: "")
                )
            )
            val historyEntry = HistoryEntity(
                historyId = historyId,
                expenseId = expense.expenseId,
                entityType = "expense",
                action = "deleted",
                previousData = previousDataJson,
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
                        expenseId = expense.expenseId,
                        entityType = "expense",
                        action = "deleted",
                        previousDataJson = previousDataJson,
                        changedBy = identity.memberId,
                        changedByName = myName,
                        changedAt = now
                    ),
                    hlc = now,
                    author = identity.memberId
                )
            )
        }
    }

    fun undoSettlement(settlement: SettlementEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val deleted = settlement.copy(isDeleted = true, hlcTimestamp = now)
            settlementDao.insertSettlement(deleted)

            val group = groupDao.getGroup(groupId) ?: return@launch
            syncEngine.pushOp(
                groupId = groupId,
                groupKeyBase64 = group.groupKeyBase64,
                payload = OpPayload(
                    id = UUID.randomUUID().toString(),
                    type = "settlement",
                    data = OpData(
                        settlementId = settlement.settlementId,
                        groupId = groupId,
                        fromMember = settlement.fromMember,
                        toMember = settlement.toMember,
                        amountCents = settlement.amountCents,
                        createdAt = settlement.createdAt,
                        isDeleted = true
                    ),
                    hlc = now,
                    author = identity.memberId
                )
            )

            // Record history
            val myName = identity.displayName.ifBlank { identity.memberId.take(8) }
            val historyId = UUID.randomUUID().toString().take(16)
            val previousDataJson = Json.encodeToString(
                mapOf(
                    "fromMember" to settlement.fromMember,
                    "toMember" to settlement.toMember,
                    "amountCents" to settlement.amountCents.toString()
                )
            )
            val historyEntry = HistoryEntity(
                historyId = historyId,
                settlementId = settlement.settlementId,
                entityType = "settlement",
                action = "deleted",
                previousData = previousDataJson,
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
                        settlementId = settlement.settlementId,
                        entityType = "settlement",
                        action = "deleted",
                        previousDataJson = previousDataJson,
                        changedBy = identity.memberId,
                        changedByName = myName,
                        changedAt = now
                    ),
                    hlc = now,
                    author = identity.memberId
                )
            )
        }
    }

    fun restoreExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val restored = expense.copy(isDeleted = false, hlcTimestamp = now)
            expenseDao.insertExpense(restored)

            val group = groupDao.getGroup(groupId) ?: return@launch
            val splitAmong: List<String> = try {
                Json.decodeFromString(expense.splitAmong)
            } catch (_: Exception) { emptyList() }
            val paidByMap: Map<String, Long>? = expense.paidByMap?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { null }
            }
            val splitDetails: Map<String, Long>? = expense.splitDetails?.let {
                try { Json.decodeFromString(it) } catch (_: Exception) { null }
            }

            syncEngine.pushOp(
                groupId = groupId,
                groupKeyBase64 = group.groupKeyBase64,
                payload = OpPayload(
                    id = UUID.randomUUID().toString(),
                    type = "expense",
                    data = OpData(
                        expenseId = expense.expenseId,
                        groupId = groupId,
                        description = expense.description,
                        amountCents = expense.amountCents,
                        currency = expense.currency,
                        paidBy = expense.paidBy,
                        splitAmong = splitAmong,
                        createdAt = expense.createdAt,
                        isDeleted = false,
                        tag = expense.tag,
                        paidByMap = paidByMap,
                        splitMode = expense.splitMode,
                        splitDetails = splitDetails
                    ),
                    hlc = now,
                    author = identity.memberId
                )
            )

            // Record history
            val myName = identity.displayName.ifBlank { identity.memberId.take(8) }
            val historyId = UUID.randomUUID().toString().take(16)
            val historyEntry = HistoryEntity(
                historyId = historyId,
                expenseId = expense.expenseId,
                entityType = "expense",
                action = "restored",
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
                        expenseId = expense.expenseId,
                        entityType = "expense",
                        action = "restored",
                        changedBy = identity.memberId,
                        changedByName = myName,
                        changedAt = now
                    ),
                    hlc = now,
                    author = identity.memberId
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncEngine.stopListening(groupId)
    }
}
