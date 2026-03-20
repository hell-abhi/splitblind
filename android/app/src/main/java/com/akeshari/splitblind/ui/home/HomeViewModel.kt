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
import com.akeshari.splitblind.util.BalanceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

data class HomeDashboardState(
    val totalOwed: Long = 0,
    val totalOwe: Long = 0,
    val netBalance: Long = 0,
    val recentExpenses: List<ExpenseEntity> = emptyList()
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

        HomeDashboardState(
            totalOwed = totalOwed,
            totalOwe = totalOwe,
            netBalance = totalOwed - totalOwe,
            recentExpenses = recentExpenses
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeDashboardState())

    fun createIouGroup(friendName: String, amount: Double, iOwe: Boolean, description: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val groupId = UUID.randomUUID().toString().take(16)
            val friendId = "iou_${UUID.randomUUID().toString().take(8)}"
            val groupKey = CryptoEngine.generateGroupKey()

            groupDao.insertGroup(
                GroupEntity(
                    groupId = groupId,
                    name = friendName,
                    createdBy = identity.memberId,
                    createdAt = now,
                    groupKeyBase64 = groupKey,
                    hlcTimestamp = now,
                    groupType = "iou"
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

            groupDao.insertMember(
                MemberEntity(
                    groupId = groupId,
                    memberId = friendId,
                    displayName = friendName,
                    joinedAt = now,
                    hlcTimestamp = now
                )
            )

            val amountCents = (amount * 100).toLong()
            val expenseId = UUID.randomUUID().toString().take(16)
            val paidBy = if (iOwe) friendId else identity.memberId
            val splitAmong = listOf(if (iOwe) identity.memberId else friendId)

            val expense = ExpenseEntity(
                expenseId = expenseId,
                groupId = groupId,
                description = description.ifBlank { "IOU" },
                amountCents = amountCents,
                currency = "INR",
                paidBy = paidBy,
                splitAmong = Json.encodeToString<List<String>>(splitAmong),
                createdAt = now,
                hlcTimestamp = now,
                tag = "other"
            )
            expenseDao.insertExpense(expense)
        }
    }
}
