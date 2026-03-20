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
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
                        splitDetails = splitDetails,
                        notes = expense.notes
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
                        splitDetails = splitDetails,
                        notes = expense.notes
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

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            try {
                val s = state.value
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val memberNames = s.memberNames

                val sb = StringBuilder()
                sb.appendLine("Date,Description,Tag,Amount,Currency,Paid By,Split Among,Notes,Type")

                // Expenses
                for (expense in s.allExpenses) {
                    val date = dateFormat.format(Date(expense.createdAt))
                    val desc = expense.description.replace(",", ";").replace("\"", "'")
                    val tag = ExpenseTag.fromSlug(expense.tag).label
                    val amount = String.format("%.2f", expense.amountCents / 100.0)
                    val currency = expense.currency
                    val paidBy = memberNames[expense.paidBy] ?: expense.paidBy.take(8)
                    val splitAmong: List<String> = try {
                        Json.decodeFromString(expense.splitAmong)
                    } catch (_: Exception) { emptyList() }
                    val splitNames = splitAmong.joinToString("; ") { id -> memberNames[id] ?: id.take(8) }
                    val notes = (expense.notes ?: "").replace(",", ";").replace("\"", "'").replace("\n", " ")
                    val type = if (expense.isDeleted) "Deleted Expense" else "Expense"
                    sb.appendLine("\"$date\",\"$desc\",\"$tag\",\"$amount\",\"$currency\",\"$paidBy\",\"$splitNames\",\"$notes\",\"$type\"")
                }

                // Settlements
                for (settlement in s.allSettlements) {
                    val date = dateFormat.format(Date(settlement.createdAt))
                    val fromName = memberNames[settlement.fromMember] ?: settlement.fromMember.take(8)
                    val toName = memberNames[settlement.toMember] ?: settlement.toMember.take(8)
                    val amount = String.format("%.2f", settlement.amountCents / 100.0)
                    val type = if (settlement.isDeleted) "Deleted Settlement" else "Settlement"
                    sb.appendLine("\"$date\",\"$fromName paid $toName\",\"\",\"$amount\",\"INR\",\"$fromName\",\"$toName\",\"\",\"$type\"")
                }

                // Write to cache/exports
                val exportDir = File(context.cacheDir, "exports")
                exportDir.mkdirs()
                val groupName = (s.group?.name ?: "group").replace(" ", "_").take(20)
                val fileName = "splitblind_${groupName}_${System.currentTimeMillis()}.csv"
                val file = File(exportDir, fileName)
                file.writeText(sb.toString())

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    this.type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "SplitBlind Export - ${s.group?.name ?: "Group"}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export CSV"))
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncEngine.stopListening(groupId)
    }
}
