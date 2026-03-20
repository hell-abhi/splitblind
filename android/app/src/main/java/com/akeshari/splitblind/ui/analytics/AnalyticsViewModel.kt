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
    val yourCents: Long,
    val percentage: Float,
    val color: Long
)

data class MonthStat(
    val label: String,
    val totalCents: Long,
    val yourCents: Long,
    val expenseCount: Int
)

data class MonthCategoryStat(
    val label: String,
    val totalCents: Long,
    val categories: List<Pair<ExpenseTag?, Long>> // tag to amount
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
    val monthCategoryStats: List<MonthCategoryStat> = emptyList(),
    val memberStats: List<MemberStat> = emptyList(),
    val totalSpent: Long = 0,
    val yourTotalShare: Long = 0,
    val groups: List<GroupOption> = emptyList(),
    val selectedGroupId: String? = null,
    // New rich summary fields
    val topCategory: String? = null,
    val topCategoryPercent: Int = 0,
    val monthlyAvg: Long = 0,
    val expenseCount: Int = 0,
    val monthTrend: Int = 0, // percentage change vs last month
    val totalSettled: Long = 0,
    val totalOutstanding: Long = 0,
    // Chart mode toggles
    val chartModes: Map<String, String> = mapOf(
        "category" to "donut",
        "month" to "chart",
        "member" to "donut"
    )
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

    fun setChartMode(chart: String, mode: String) {
        _state.value = _state.value.copy(
            chartModes = _state.value.chartModes + (chart to mode)
        )
    }

    private fun getMyShare(expense: ExpenseEntity): Long {
        val myId = identity.memberId
        // Check splitDetails first (custom split)
        if (expense.splitDetails != null) {
            val details: Map<String, Long> = try {
                Json.decodeFromString(expense.splitDetails)
            } catch (_: Exception) { emptyMap() }
            return details[myId] ?: 0
        }
        // Equal split fallback
        val splitAmong: List<String> = try {
            Json.decodeFromString(expense.splitAmong)
        } catch (_: Exception) { emptyList() }
        if (splitAmong.contains(myId)) {
            return expense.amountCents / splitAmong.size
        }
        return 0
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
            val yourTotalShare = expenses.sumOf { getMyShare(it) }
            val expenseCount = expenses.size

            // --- By Category ---
            val byCat = expenses.groupBy { it.tag ?: "other" }
            val categoryStats = byCat.map { (slug, exps) ->
                val total = exps.sumOf { it.amountCents }
                val yourTotal = exps.sumOf { getMyShare(it) }
                val tag = ExpenseTag.fromSlug(slug)
                CategoryStat(
                    tag = tag,
                    slug = slug,
                    totalCents = total,
                    yourCents = yourTotal,
                    percentage = if (totalSpent > 0) (total.toFloat() / totalSpent) * 100f else 0f,
                    color = tag?.color ?: 0xFFCFD8DC
                )
            }.sortedByDescending { it.totalCents }

            // Top category
            val topCat = categoryStats.firstOrNull()
            val topCategory = topCat?.let { "${it.tag?.emoji ?: ""} ${it.tag?.label ?: it.slug}" }
            val topCategoryPercent = topCat?.percentage?.toInt() ?: 0

            // --- By Month (last 6 months) ---
            val monthStats = mutableListOf<MonthStat>()
            val monthCategoryStats = mutableListOf<MonthCategoryStat>()
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

                val monthExpenses = expenses.filter { it.createdAt in start until end }
                val total = monthExpenses.sumOf { it.amountCents }
                val yourTotal = monthExpenses.sumOf { getMyShare(it) }
                monthStats.add(MonthStat(label, total, yourTotal, monthExpenses.size))

                // Category breakdown per month
                val catBreakdown = monthExpenses.groupBy { it.tag ?: "other" }
                    .map { (slug, exps) ->
                        val tag = ExpenseTag.fromSlug(slug)
                        Pair(tag, exps.sumOf { it.amountCents })
                    }
                    .sortedByDescending { it.second }
                monthCategoryStats.add(MonthCategoryStat(label, total, catBreakdown))
            }

            // Monthly average (over months that have expenses)
            val monthsWithExpenses = monthStats.filter { it.totalCents > 0 }
            val monthlyAvg = if (monthsWithExpenses.isNotEmpty()) {
                totalSpent / monthsWithExpenses.size
            } else 0L

            // Month trend: compare current month to previous month
            val currentMonthTotal = monthStats.lastOrNull()?.totalCents ?: 0L
            val prevMonthTotal = if (monthStats.size >= 2) monthStats[monthStats.size - 2].totalCents else 0L
            val monthTrend = if (prevMonthTotal > 0) {
                (((currentMonthTotal - prevMonthTotal).toFloat() / prevMonthTotal) * 100).toInt()
            } else if (currentMonthTotal > 0) 100 else 0

            // --- Settlement summary ---
            val totalSettled = settlements.sumOf { it.amountCents }
            val netBalances = BalanceCalculator.computeNetBalances(expenses, settlements)
            val totalOutstanding = netBalances.values.filter { it > 0 }.sum()

            // --- By Member ---
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

            val memberStatsList = (paidByMember.keys + netBalances.keys).distinct().map { memberId ->
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
                monthCategoryStats = monthCategoryStats,
                memberStats = memberStatsList,
                totalSpent = totalSpent,
                yourTotalShare = yourTotalShare,
                groups = groups.map { GroupOption(it.groupId, it.name) },
                topCategory = topCategory,
                topCategoryPercent = topCategoryPercent,
                monthlyAvg = monthlyAvg,
                expenseCount = expenseCount,
                monthTrend = monthTrend,
                totalSettled = totalSettled,
                totalOutstanding = totalOutstanding
            )
        }
    }
}
