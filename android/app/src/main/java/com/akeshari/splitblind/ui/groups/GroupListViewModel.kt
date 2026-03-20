package com.akeshari.splitblind.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.crypto.CryptoEngine
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.data.database.dao.ExpenseDao
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.dao.SettlementDao
import com.akeshari.splitblind.data.database.entity.GroupEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.sync.OpData
import com.akeshari.splitblind.sync.OpPayload
import com.akeshari.splitblind.sync.SyncEngine
import com.akeshari.splitblind.util.BalanceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

data class DashboardState(
    val groups: List<GroupEntity> = emptyList(),
    val groupBalances: Map<String, Long> = emptyMap(),
    val personalGroup: GroupEntity? = null,
    val personalMonthSpend: Long = 0,
    val personalExpenseCount: Int = 0
)

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao,
    private val identity: Identity,
    private val syncEngine: SyncEngine
) : ViewModel() {

    val groups: StateFlow<List<GroupEntity>> = groupDao.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardState: StateFlow<DashboardState> = combine(
        groupDao.getAllGroups(),
        expenseDao.getAllActiveExpenses(),
        settlementDao.getAllActiveSettlements()
    ) { groups, allExpenses, allSettlements ->
        val myId = identity.memberId
        val groupBalances = mutableMapOf<String, Long>()

        for (group in groups) {
            val gExpenses = allExpenses.filter { it.groupId == group.groupId }
            val gSettlements = allSettlements.filter { it.groupId == group.groupId }
            val netBalances = BalanceCalculator.computeNetBalances(gExpenses, gSettlements)
            val myBalance = netBalances[myId] ?: 0L
            groupBalances[group.groupId] = myBalance
        }

        // Personal tracker
        val personalGroup = groups.find { it.groupType == "personal" }
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val personalExpenses = if (personalGroup != null) {
            allExpenses.filter { it.groupId == personalGroup.groupId }
        } else emptyList()

        val personalMonthSpend = personalExpenses
            .filter { it.createdAt >= monthStart }
            .sumOf { it.amountCents }

        DashboardState(
            groups = groups,
            groupBalances = groupBalances,
            personalGroup = personalGroup,
            personalMonthSpend = personalMonthSpend,
            personalExpenseCount = personalExpenses.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    fun joinGroup(groupId: String, groupKeyBase64: String, groupName: String) {
        viewModelScope.launch {
            val existing = groupDao.getGroup(groupId)
            if (existing != null) return@launch

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
        }
    }

    fun createPersonalGroup() {
        viewModelScope.launch {
            // Check if personal group already exists
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

            // Push to Firebase for sync/recovery
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

            // Store personal group ID in identity for quick access
            identity.personalGroupId = groupId
        }
    }
}
