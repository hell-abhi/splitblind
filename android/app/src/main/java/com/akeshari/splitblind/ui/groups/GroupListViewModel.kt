package com.akeshari.splitblind.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.util.UUID
import javax.inject.Inject

data class DashboardState(
    val groups: List<GroupEntity> = emptyList(),
    val groupBalances: Map<String, Long> = emptyMap()
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

        DashboardState(
            groups = groups,
            groupBalances = groupBalances
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
}
