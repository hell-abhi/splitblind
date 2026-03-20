package com.akeshari.splitblind.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.data.database.dao.ExpenseDao
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.dao.SettlementDao
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import com.akeshari.splitblind.ui.expenses.ExpenseTag
import com.akeshari.splitblind.util.BalanceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.Calendar
import javax.inject.Inject

data class CategoryStat(
    val tag: ExpenseTag?,
    val slug: String,
    val totalCents: Long,
    val percentage: Float,
    val color: Long
)

data class MonthStat(
    val label: String,
    val totalCents: Long
)

data class MemberStat(
    val memberId: String,
    val name: String,
    val totalPaid: Long,
    val netBalance: Long
)

data class GroupOption(
    val groupId: String,
    val name: String
)

data class AnalyticsState(
    val categoryStats: List<CategoryStat> = emptyList(),
    val monthStats: List<MonthStat> = emptyList(),
    val memberStats: List<MemberStat> = emptyList(),
    val totalSpent: Long = 0,
    val groups: List<GroupOption> = emptyList(),
    val selectedGroupId: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao,
    private val groupDao: GroupDao,
    private val identity: Identity
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state

    init {
        loadAnalytics()
    }

    fun selectGroup(groupId: String?) {
        _state.value = _state.value.copy(selectedGroupId = groupId)
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            val selectedGroupId = _state.value.selectedGroupId
            val allExpenses = expenseDao.getAllActiveExpensesList()
            val allSettlements = settlementDao.getAllActiveSettlementsList()
            val groups = groupDao.getAllGroupsList()

            val expenses = if (selectedGroupId != null) allExpenses.filter { it.groupId == selectedGroupId } else allExpenses
            val settlements = if (selectedGroupId != null) allSettlements.filter { it.groupId == selectedGroupId } else allSettlements

            // Gather all member names
            val memberNames = mutableMapOf<String, String>()
            for (group in groups) {
                val members = groupDao.getMembersList(group.groupId)
                members.forEach { memberNames[it.memberId] = it.displayName }
            }

            val totalSpent = expenses.sumOf { it.amountCents }

            // --- By Category ---
            val byCat = expenses.groupBy { it.tag ?: "other" }
            val categoryStats = byCat.map { (slug, exps) ->
                val total = exps.sumOf { it.amountCents }
                val tag = ExpenseTag.fromSlug(slug)
                CategoryStat(
                    tag = tag,
                    slug = slug,
                    totalCents = total,
                    percentage = if (totalSpent > 0) (total.toFloat() / totalSpent) * 100f else 0f,
                    color = tag?.color ?: 0xFFCFD8DC
                )
            }.sortedByDescending { it.totalCents }

            // --- By Month (last 6 months) ---
            val monthStats = mutableListOf<MonthStat>()
            val monthNames = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
            for (i in 5 downTo 0) {
                val c = Calendar.getInstance()
                c.add(Calendar.MONTH, -i)
                val year = c.get(Calendar.YEAR)
                val month = c.get(Calendar.MONTH)
                val label = "${monthNames[month]} ${year % 100}"
                val start = Calendar.getInstance().apply {
                    set(year, month, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = Calendar.getInstance().apply {
                    set(year, month, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MONTH, 1)
                }.timeInMillis

                val total = expenses.filter { it.createdAt in start until end }.sumOf { it.amountCents }
                monthStats.add(MonthStat(label, total))
            }

            // --- By Member ---
            val netBalances = BalanceCalculator.computeNetBalances(expenses, settlements)
            // Track total paid per member
            val paidByMember = mutableMapOf<String, Long>()
            for (expense in expenses) {
                val paidByMap: Map<String, Long>? = expense.paidByMap?.let {
                    try { Json.decodeFromString(it) } catch (_: Exception) { null }
                }
                if (paidByMap != null) {
                    for ((id, amt) in paidByMap) {
                        paidByMember[id] = (paidByMember[id] ?: 0) + amt
                    }
                } else {
                    paidByMember[expense.paidBy] = (paidByMember[expense.paidBy] ?: 0) + expense.amountCents
                }
            }

            val memberStats = (paidByMember.keys + netBalances.keys).distinct().map { memberId ->
                MemberStat(
                    memberId = memberId,
                    name = memberNames[memberId] ?: memberId.take(8),
                    totalPaid = paidByMember[memberId] ?: 0,
                    netBalance = netBalances[memberId] ?: 0
                )
            }.sortedByDescending { it.totalPaid }

            _state.value = _state.value.copy(
                categoryStats = categoryStats,
                monthStats = monthStats,
                memberStats = memberStats,
                totalSpent = totalSpent,
                groups = groups.map { GroupOption(it.groupId, it.name) }
            )
        }
    }
}
