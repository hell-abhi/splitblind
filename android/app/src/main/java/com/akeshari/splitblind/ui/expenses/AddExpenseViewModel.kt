package com.akeshari.splitblind.ui.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.data.database.dao.ExpenseDao
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.dao.HistoryDao
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import com.akeshari.splitblind.data.database.entity.HistoryEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.sync.OpData
import com.akeshari.splitblind.sync.OpPayload
import com.akeshari.splitblind.sync.SyncEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.UUID
import javax.inject.Inject

enum class SplitMode(val slug: String, val label: String) {
    EQUAL("equal", "Equal"),
    AMOUNT("amount", "Amount"),
    PERCENTAGE("percentage", "Percent"),
    RATIO("ratio", "Ratio")
}

data class ExpenseTag(
    val slug: String,
    val label: String,
    val emoji: String,
    val color: Long // ARGB
) {
    companion object {
        val ALL = listOf(
            ExpenseTag("food", "Food", "\uD83C\uDF54", 0xFFFFE0B2),
            ExpenseTag("transport", "Transport", "\uD83D\uDE97", 0xFFB3E5FC),
            ExpenseTag("shopping", "Shopping", "\uD83D\uDECD\uFE0F", 0xFFF8BBD0),
            ExpenseTag("entertainment", "Entertainment", "\uD83C\uDFAC", 0xFFE1BEE7),
            ExpenseTag("travel", "Travel", "\u2708\uFE0F", 0xFFB2DFDB),
            ExpenseTag("bills", "Bills", "\uD83D\uDCB3", 0xFFFFF9C4),
            ExpenseTag("groceries", "Groceries", "\uD83E\uDED2", 0xFFC8E6C9),
            ExpenseTag("health", "Health", "\uD83C\uDFE5", 0xFFFFCDD2),
            ExpenseTag("rent", "Rent", "\uD83C\uDFE0", 0xFFD7CCC8),
            ExpenseTag("other", "Other", "\uD83D\uDCCC", 0xFFCFD8DC)
        )

        fun fromSlug(slug: String?): ExpenseTag? = ALL.find { it.slug == slug }
    }
}

data class AddExpenseState(
    val description: String = "",
    val amount: String = "",
    val paidBy: String = "",
    val splitAmong: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    // New fields
    val selectedTag: String = "other",
    val isMultiPayer: Boolean = false,
    val payerAmounts: Map<String, String> = emptyMap(),
    val splitMode: SplitMode = SplitMode.EQUAL,
    val splitAmounts: Map<String, String> = emptyMap(),
    val splitPercentages: Map<String, String> = emptyMap(),
    val splitRatios: Map<String, String> = emptyMap(),
    // Edit mode
    val isEditing: Boolean = false
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val historyDao: HistoryDao,
    private val identity: Identity,
    private val syncEngine: SyncEngine
) : ViewModel() {

    val groupId: String = savedStateHandle["groupId"] ?: ""
    private val editExpenseId: String? = savedStateHandle["editExpenseId"]

    val members: StateFlow<List<MemberEntity>> = groupDao.getMembers(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(AddExpenseState(paidBy = identity.memberId))
    val state: StateFlow<AddExpenseState> = _state

    init {
        if (editExpenseId != null) {
            viewModelScope.launch {
                val expense = expenseDao.getExpense(editExpenseId) ?: return@launch
                val splitMembers: Set<String> = try {
                    Json.decodeFromString<List<String>>(expense.splitAmong).toSet()
                } catch (_: Exception) { emptySet() }
                val paidByMap: Map<String, Long>? = expense.paidByMap?.let {
                    try { Json.decodeFromString(it) } catch (_: Exception) { null }
                }
                val splitDetails: Map<String, Long>? = expense.splitDetails?.let {
                    try { Json.decodeFromString(it) } catch (_: Exception) { null }
                }
                val splitMode = SplitMode.entries.find { it.slug == expense.splitMode } ?: SplitMode.EQUAL

                _state.value = AddExpenseState(
                    description = expense.description,
                    amount = String.format("%.2f", expense.amountCents / 100.0),
                    paidBy = expense.paidBy,
                    splitAmong = splitMembers,
                    selectedTag = expense.tag ?: "other",
                    isMultiPayer = paidByMap != null && paidByMap.size > 1,
                    payerAmounts = paidByMap?.mapValues { (_, v) -> String.format("%.2f", v / 100.0) } ?: emptyMap(),
                    splitMode = splitMode,
                    splitAmounts = if (splitMode == SplitMode.AMOUNT && splitDetails != null)
                        splitDetails.mapValues { (_, v) -> String.format("%.2f", v / 100.0) } else emptyMap(),
                    splitPercentages = emptyMap(), // Can't reverse percentages from cents
                    splitRatios = emptyMap(), // Can't reverse ratios from cents
                    isEditing = true
                )
            }
        }
    }

    fun setDescription(desc: String) {
        _state.value = _state.value.copy(description = desc)
    }

    fun setAmount(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _state.value = _state.value.copy(amount = amount)
        }
    }

    fun setPaidBy(memberId: String) {
        _state.value = _state.value.copy(paidBy = memberId)
    }

    fun toggleSplitMember(memberId: String) {
        val current = _state.value.splitAmong
        val updated = if (memberId in current) current - memberId else current + memberId
        _state.value = _state.value.copy(splitAmong = updated)
    }

    fun selectAllMembers() {
        val allIds = members.value.map { it.memberId }.toSet()
        _state.value = _state.value.copy(splitAmong = allIds)
    }

    fun setTag(tag: String?) {
        _state.value = _state.value.copy(selectedTag = tag ?: "other")
    }

    fun setMultiPayer(enabled: Boolean) {
        _state.value = _state.value.copy(isMultiPayer = enabled)
    }

    fun setPayerAmount(memberId: String, amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            val updated = _state.value.payerAmounts + (memberId to amount)
            _state.value = _state.value.copy(payerAmounts = updated)
        }
    }

    fun setSplitMode(mode: SplitMode) {
        _state.value = _state.value.copy(splitMode = mode)
    }

    fun setSplitAmount(memberId: String, amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            val updated = _state.value.splitAmounts + (memberId to amount)
            _state.value = _state.value.copy(splitAmounts = updated)
        }
    }

    fun setSplitPercentage(memberId: String, pct: String) {
        if (pct.isEmpty() || pct.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            val updated = _state.value.splitPercentages + (memberId to pct)
            _state.value = _state.value.copy(splitPercentages = updated)
        }
    }

    fun setSplitRatio(memberId: String, ratio: String) {
        if (ratio.isEmpty() || ratio.matches(Regex("^\\d*$"))) {
            val updated = _state.value.splitRatios + (memberId to ratio)
            _state.value = _state.value.copy(splitRatios = updated)
        }
    }

    fun saveExpense() {
        val s = _state.value
        if (s.description.isBlank()) {
            _state.value = s.copy(error = "Enter a description")
            return
        }
        val amountDouble = s.amount.toDoubleOrNull()
        if (amountDouble == null || amountDouble <= 0) {
            _state.value = s.copy(error = "Enter a valid amount")
            return
        }
        if (s.splitAmong.isEmpty()) {
            _state.value = s.copy(error = "Select at least one person to split with")
            return
        }

        val amountCents = (amountDouble * 100).toLong()

        // Validate multi-payer
        var paidByMapCents: Map<String, Long>? = null
        if (s.isMultiPayer) {
            val map = mutableMapOf<String, Long>()
            for (memberId in members.value.map { it.memberId }) {
                val amt = s.payerAmounts[memberId]?.toDoubleOrNull() ?: 0.0
                if (amt > 0) map[memberId] = (amt * 100).toLong()
            }
            val sum = map.values.sum()
            if (sum != amountCents) {
                _state.value = s.copy(error = "Payer amounts must sum to total (off by ${String.format("%.2f", (amountCents - sum) / 100.0)})")
                return
            }
            paidByMapCents = map
        }

        // Compute splitDetails
        val splitMembers = s.splitAmong.toList()
        var splitDetailsCents: Map<String, Long>? = null
        when (s.splitMode) {
            SplitMode.EQUAL -> {
                // No splitDetails needed — BalanceCalculator handles equal split
                splitDetailsCents = null
            }
            SplitMode.AMOUNT -> {
                val map = mutableMapOf<String, Long>()
                for (memberId in splitMembers) {
                    val amt = s.splitAmounts[memberId]?.toDoubleOrNull() ?: 0.0
                    if (amt > 0) map[memberId] = (amt * 100).toLong()
                }
                val sum = map.values.sum()
                if (sum != amountCents) {
                    _state.value = s.copy(error = "Split amounts must sum to total (off by ${String.format("%.2f", (amountCents - sum) / 100.0)})")
                    return
                }
                splitDetailsCents = map
            }
            SplitMode.PERCENTAGE -> {
                val pcts = mutableMapOf<String, Double>()
                for (memberId in splitMembers) {
                    val pct = s.splitPercentages[memberId]?.toDoubleOrNull() ?: 0.0
                    pcts[memberId] = pct
                }
                val totalPct = pcts.values.sum()
                if (kotlin.math.abs(totalPct - 100.0) > 0.01) {
                    _state.value = s.copy(error = "Percentages must sum to 100% (currently ${String.format("%.1f", totalPct)}%)")
                    return
                }
                val map = mutableMapOf<String, Long>()
                var allocated = 0L
                val sorted = pcts.entries.toList()
                for ((i, entry) in sorted.withIndex()) {
                    if (i == sorted.size - 1) {
                        map[entry.key] = amountCents - allocated
                    } else {
                        val share = (amountCents * entry.value / 100.0).toLong()
                        map[entry.key] = share
                        allocated += share
                    }
                }
                splitDetailsCents = map
            }
            SplitMode.RATIO -> {
                val ratios = mutableMapOf<String, Long>()
                for (memberId in splitMembers) {
                    val r = s.splitRatios[memberId]?.toLongOrNull() ?: 0L
                    ratios[memberId] = r
                }
                val totalRatio = ratios.values.sum()
                if (totalRatio <= 0) {
                    _state.value = s.copy(error = "Enter at least one ratio")
                    return
                }
                val map = mutableMapOf<String, Long>()
                var allocated = 0L
                val sorted = ratios.entries.toList()
                for ((i, entry) in sorted.withIndex()) {
                    if (i == sorted.size - 1) {
                        map[entry.key] = amountCents - allocated
                    } else {
                        val share = amountCents * entry.value / totalRatio
                        map[entry.key] = share
                        allocated += share
                    }
                }
                splitDetailsCents = map
            }
        }

        _state.value = s.copy(isSaving = true, error = null)

        viewModelScope.launch {
            val expenseId = editExpenseId ?: UUID.randomUUID().toString().take(16)
            val now = System.currentTimeMillis()
            val splitList = splitMembers
            val oldExpense = if (editExpenseId != null) expenseDao.getExpense(editExpenseId) else null

            // Legacy paidBy: first payer (or single payer)
            val legacyPaidBy = if (s.isMultiPayer && paidByMapCents != null) {
                paidByMapCents.maxByOrNull { it.value }?.key ?: s.paidBy
            } else {
                s.paidBy
            }

            val paidByMapJson = paidByMapCents?.let { Json.encodeToString(it) }
            val splitDetailsJson = splitDetailsCents?.let { Json.encodeToString(it) }
            val splitModeStr = if (s.splitMode != SplitMode.EQUAL) s.splitMode.slug else null

            val expense = ExpenseEntity(
                expenseId = expenseId,
                groupId = groupId,
                description = s.description.trim(),
                amountCents = amountCents,
                currency = "INR",
                paidBy = legacyPaidBy,
                splitAmong = Json.encodeToString(splitList),
                createdAt = now,
                hlcTimestamp = now,
                tag = s.selectedTag,
                paidByMap = paidByMapJson,
                splitMode = splitModeStr,
                splitDetails = splitDetailsJson
            )

            expenseDao.insertExpense(expense)

            // Push to Firebase
            val group = groupDao.getGroup(groupId)
            if (group != null) {
                syncEngine.pushOp(
                    groupId = groupId,
                    groupKeyBase64 = group.groupKeyBase64,
                    payload = OpPayload(
                        id = UUID.randomUUID().toString(),
                        type = "expense",
                        data = OpData(
                            expenseId = expenseId,
                            groupId = groupId,
                            description = s.description.trim(),
                            amountCents = amountCents,
                            currency = "INR",
                            paidBy = legacyPaidBy,
                            splitAmong = splitList,
                            createdAt = now,
                            tag = s.selectedTag,
                            paidByMap = paidByMapCents,
                            splitMode = splitModeStr,
                            splitDetails = splitDetailsCents
                        ),
                        hlc = now,
                        author = identity.memberId
                    )
                )

                // Record history
                val isEditing = oldExpense != null
                val historyAction = if (isEditing) "edited" else "created"
                val myName = identity.displayName.ifBlank { identity.memberId.take(8) }
                val historyId = UUID.randomUUID().toString().take(16)
                val previousDataJson = if (isEditing && oldExpense != null) {
                    Json.encodeToString(
                        mapOf(
                            "description" to oldExpense.description,
                            "amountCents" to oldExpense.amountCents.toString(),
                            "paidBy" to oldExpense.paidBy,
                            "splitAmong" to oldExpense.splitAmong,
                            "tag" to (oldExpense.tag ?: "")
                        )
                    )
                } else null
                val newDataJson = Json.encodeToString(
                    mapOf(
                        "description" to s.description.trim(),
                        "amountCents" to amountCents.toString(),
                        "paidBy" to legacyPaidBy,
                        "splitAmong" to Json.encodeToString(splitList),
                        "tag" to s.selectedTag
                    )
                )

                val historyEntry = HistoryEntity(
                    historyId = historyId,
                    expenseId = expenseId,
                    entityType = "expense",
                    action = historyAction,
                    previousData = previousDataJson,
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
                            expenseId = expenseId,
                            entityType = "expense",
                            action = historyAction,
                            previousDataJson = previousDataJson,
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

            _state.value = _state.value.copy(isSaving = false, saved = true)
        }
    }
}
