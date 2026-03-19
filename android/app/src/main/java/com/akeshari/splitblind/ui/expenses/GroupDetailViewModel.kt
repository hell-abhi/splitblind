package com.akeshari.splitblind.ui.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.data.database.dao.ExpenseDao
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.dao.SettlementDao
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import com.akeshari.splitblind.data.database.entity.GroupEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.data.database.entity.SettlementEntity
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
import javax.inject.Inject

data class GroupDetailState(
    val group: GroupEntity? = null,
    val members: List<MemberEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val settlements: List<SettlementEntity> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val netBalances: Map<String, Long> = emptyMap(),
    val memberNames: Map<String, String> = emptyMap(),
    val isSynced: Boolean = false,
    val inviteLink: String? = null
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val settlementDao: SettlementDao,
    private val syncEngine: SyncEngine,
    val identity: Identity
) : ViewModel() {

    val groupId: String = savedStateHandle["groupId"] ?: ""

    private val _group = MutableStateFlow<GroupEntity?>(null)

    val state: StateFlow<GroupDetailState> = combine(
        groupDao.getMembers(groupId),
        expenseDao.getExpenses(groupId),
        settlementDao.getSettlements(groupId),
        syncEngine.syncStatus
    ) { members, expenses, settlements, syncStatus ->
        val memberNames = members.associate { it.memberId to it.displayName }
        val balances = BalanceCalculator.computeNetBalances(expenses, settlements)
        val debts = BalanceCalculator.simplifyDebts(balances)

        val group = _group.value
        val inviteLink = if (group != null) {
            val encodedName = java.net.URLEncoder.encode(group.name, "UTF-8")
            "https://hell-abhi.github.io/splitblind/?g=${group.groupId}#${group.groupKeyBase64}|$encodedName"
        } else null

        GroupDetailState(
            group = group,
            members = members,
            expenses = expenses,
            settlements = settlements,
            debts = debts,
            netBalances = balances,
            memberNames = memberNames,
            isSynced = syncStatus[groupId] == true,
            inviteLink = inviteLink
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupDetailState())

    init {
        viewModelScope.launch {
            val group = groupDao.getGroup(groupId) ?: return@launch
            _group.value = group
            syncEngine.startListening(groupId, group.groupKeyBase64)
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncEngine.stopListening(groupId)
    }
}
