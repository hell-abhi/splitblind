package com.akeshari.splitblind.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.crypto.CryptoEngine
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.data.database.dao.ExpenseDao
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.dao.SettlementDao
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import com.akeshari.splitblind.data.database.entity.GroupEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.data.database.entity.SettlementEntity
import com.akeshari.splitblind.sync.OpData
import com.akeshari.splitblind.sync.OpPayload
import com.akeshari.splitblind.sync.SyncEngine
import com.akeshari.splitblind.util.BalanceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

data class GroupPreview(
    val group: GroupEntity,
    val memberCount: Int,
    val myBalance: Long
)

data class ExpenseWithGroupName(
    val expense: ExpenseEntity,
    val groupName: String
)

data class HomeDashboardState(
    val totalOwed: Long = 0,
    val totalOwe: Long = 0,
    val netBalance: Long = 0,
    val recentExpenses: List<ExpenseWithGroupName> = emptyList(),
    val latestGroups: List<GroupPreview> = emptyList(),
    val personalGroupId: String? = null,
    val personalMonthSpend: Long = 0,
    val searchQuery: String = "",
    val allGroups: List<GroupPreview> = emptyList(),
    val allExpenses: List<ExpenseWithGroupName> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao,
    private val syncEngine: SyncEngine,
    private val identity: Identity
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    init {
        // Check for recurring expenses on app start
        viewModelScope.launch {
            processRecurringExpenses()
        }
    }

    private suspend fun processRecurringExpenses() {
        try {
            val recurringExpenses = expenseDao.getRecurringExpenses()
            val now = System.currentTimeMillis()

            for (expense in recurringExpenses) {
                val freq = expense.recurringFrequency ?: continue
                val intervalMs = when (freq) {
                    "weekly" -> 7L * 24 * 60 * 60 * 1000
                    "monthly" -> 30L * 24 * 60 * 60 * 1000
                    "yearly" -> 365L * 24 * 60 * 60 * 1000
                    else -> continue
                }

                // Check if enough time has passed since last occurrence
                val timeSince = now - expense.createdAt
                if (timeSince < intervalMs) continue

                // Check how many occurrences are due
                val occurrencesDue = (timeSince / intervalMs).toInt()
                // Only create one at a time to avoid flooding
                if (occurrencesDue < 1) continue

                // Create new expense
                val newExpenseId = UUID.randomUUID().toString().take(16)
                val splitAmong: List<String> = try {
                    Json.decodeFromString(expense.splitAmong)
                } catch (_: Exception) { emptyList() }
                val paidByMap: Map<String, Long>? = expense.paidByMap?.let {
                    try { Json.decodeFromString(it) } catch (_: Exception) { null }
                }
                val splitDetails: Map<String, Long>? = expense.splitDetails?.let {
                    try { Json.decodeFromString(it) } catch (_: Exception) { null }
                }

                val newExpense = expense.copy(
                    expenseId = newExpenseId,
                    createdAt = now,
                    hlcTimestamp = now
                )

                expenseDao.insertExpense(newExpense)

                // Push to Firebase
                val group = groupDao.getGroup(expense.groupId) ?: continue
                syncEngine.pushOp(
                    groupId = expense.groupId,
                    groupKeyBase64 = group.groupKeyBase64,
                    payload = OpPayload(
                        id = UUID.randomUUID().toString(),
                        type = "expense",
                        data = OpData(
                            expenseId = newExpenseId,
                            groupId = expense.groupId,
                            description = expense.description,
                            amountCents = expense.amountCents,
                            currency = expense.currency,
                            paidBy = expense.paidBy,
                            splitAmong = splitAmong,
                            createdAt = now,
                            tag = expense.tag,
                            paidByMap = paidByMap,
                            splitMode = expense.splitMode,
                            splitDetails = splitDetails,
                            notes = expense.notes,
                            recurringFrequency = expense.recurringFrequency,
                            splitItems = expense.splitItems
                        ),
                        hlc = now,
                        author = identity.memberId
                    )
                )
            }
        } catch (_: Exception) {
            // Silently fail - recurring is best effort
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val dashboardState: StateFlow<HomeDashboardState> = combine(
        groupDao.getAllGroups(),
        expenseDao.getAllActiveExpenses(),
        settlementDao.getAllActiveSettlements(),
        expenseDao.getRecentExpenses(5),
        _searchQuery
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val groups = args[0] as List<GroupEntity>
        @Suppress("UNCHECKED_CAST")
        val allExpenses = args[1] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val allSettlements = args[2] as List<SettlementEntity>
        @Suppress("UNCHECKED_CAST")
        val recentExpenses = args[3] as List<ExpenseEntity>
        val searchQuery = args[4] as String

        val myId = identity.memberId
        val allMembers = groupDao.getAllMembersList()

        var totalOwed = 0L
        var totalOwe = 0L
        val groupPreviews = mutableListOf<GroupPreview>()
        val groupNameMap = mutableMapOf<String, String>()

        for (group in groups) {
            groupNameMap[group.groupId] = group.name
            val gExpenses = allExpenses.filter { it.groupId == group.groupId }
            val gSettlements = allSettlements.filter { it.groupId == group.groupId }
            val netBalances = BalanceCalculator.computeNetBalances(gExpenses, gSettlements)
            val myBalance = netBalances[myId] ?: 0L
            if (myBalance > 0) totalOwed += myBalance
            else if (myBalance < 0) totalOwe += -myBalance

            if (group.groupType != "personal" && group.groupType != "iou") {
                val mc = allMembers.count { it.groupId == group.groupId && !it.isDeleted }
                groupPreviews.add(GroupPreview(group, mc, myBalance))
            }
        }

        val latestGroups = groupPreviews.sortedByDescending { it.group.createdAt }.take(3)

        // Personal tracker spending this month
        val personalGroup = groups.find { it.groupType == "personal" }
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val personalMonthSpend = if (personalGroup != null) {
            allExpenses
                .filter { it.groupId == personalGroup.groupId && it.createdAt >= monthStart }
                .sumOf { it.amountCents }
        } else 0L

        // All expenses with group names for search
        val allExpensesWithGroupName = allExpenses.map { exp ->
            ExpenseWithGroupName(exp, groupNameMap[exp.groupId] ?: "")
        }

        HomeDashboardState(
            totalOwed = totalOwed,
            totalOwe = totalOwe,
            netBalance = totalOwed - totalOwe,
            recentExpenses = recentExpenses.take(3).map { exp -> ExpenseWithGroupName(exp, groupNameMap[exp.groupId] ?: "") },
            latestGroups = latestGroups,
            personalGroupId = personalGroup?.groupId,
            personalMonthSpend = personalMonthSpend,
            searchQuery = searchQuery,
            allGroups = groupPreviews.sortedByDescending { it.group.createdAt },
            allExpenses = allExpensesWithGroupName.sortedByDescending { it.expense.createdAt }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeDashboardState())

    fun createPersonalGroup() {
        viewModelScope.launch {
            val existing = groupDao.getAllGroupsList().find { it.groupType == "personal" }
            if (existing != null) return@launch

            val now = System.currentTimeMillis()
            val groupId = UUID.randomUUID().toString().take(16)
            val groupKey = CryptoEngine.generateGroupKey()

            groupDao.insertGroup(
                GroupEntity(
                    groupId = groupId,
                    name = "Personal",
                    createdBy = identity.memberId,
                    createdAt = now,
                    groupKeyBase64 = groupKey,
                    hlcTimestamp = now,
                    groupType = "personal"
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

            syncEngine.startListening(groupId, groupKey)
            syncEngine.pushOp(
                groupId = groupId,
                groupKeyBase64 = groupKey,
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

            identity.personalGroupId = groupId
        }
    }
}
