package com.akeshari.splitblind.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.data.database.dao.ExpenseDao
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.dao.SettlementDao
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import com.akeshari.splitblind.util.BalanceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class HomeDashboardState(
    val totalOwed: Long = 0,
    val totalOwe: Long = 0,
    val netBalance: Long = 0,
    val recentExpenses: List<ExpenseEntity> = emptyList(),
    val personalGroupId: String? = null,
    val personalMonthSpend: Long = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao,
    private val identity: Identity
) : ViewModel() {

    val dashboardState: StateFlow<HomeDashboardState> = combine(
        groupDao.getAllGroups(),
        expenseDao.getAllActiveExpenses(),
        settlementDao.getAllActiveSettlements(),
        expenseDao.getRecentExpenses(5)
    ) { groups, allExpenses, allSettlements, recentExpenses ->
        val myId = identity.memberId

        var totalOwed = 0L
        var totalOwe = 0L

        for (group in groups) {
            val gExpenses = allExpenses.filter { it.groupId == group.groupId }
            val gSettlements = allSettlements.filter { it.groupId == group.groupId }
            val netBalances = BalanceCalculator.computeNetBalances(gExpenses, gSettlements)
            val myBalance = netBalances[myId] ?: 0L
            if (myBalance > 0) totalOwed += myBalance
            else if (myBalance < 0) totalOwe += -myBalance
        }

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

        HomeDashboardState(
            totalOwed = totalOwed,
            totalOwe = totalOwe,
            netBalance = totalOwed - totalOwe,
            recentExpenses = recentExpenses,
            personalGroupId = personalGroup?.groupId,
            personalMonthSpend = personalMonthSpend
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeDashboardState())
}
