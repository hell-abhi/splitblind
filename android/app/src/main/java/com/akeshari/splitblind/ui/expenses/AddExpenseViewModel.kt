package com.akeshari.splitblind.ui.expenses

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeshari.splitblind.crypto.Identity
import com.akeshari.splitblind.data.database.dao.ExpenseDao
import com.akeshari.splitblind.data.database.dao.GroupDao
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
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

data class AddExpenseState(
    val description: String = "",
    val amount: String = "",
    val paidBy: String = "",
    val splitAmong: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val identity: Identity,
    private val syncEngine: SyncEngine
) : ViewModel() {

    val groupId: String = savedStateHandle["groupId"] ?: ""

    val members: StateFlow<List<MemberEntity>> = groupDao.getMembers(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(AddExpenseState(paidBy = identity.memberId))
    val state: StateFlow<AddExpenseState> = _state

    fun setDescription(desc: String) {
        _state.value = _state.value.copy(description = desc)
    }

    fun setAmount(amount: String) {
        // Only allow valid decimal input
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

        _state.value = s.copy(isSaving = true, error = null)

        viewModelScope.launch {
            val amountCents = (amountDouble * 100).toLong()
            val expenseId = UUID.randomUUID().toString().take(16)
            val now = System.currentTimeMillis()
            val splitList = s.splitAmong.toList()

            val expense = ExpenseEntity(
                expenseId = expenseId,
                groupId = groupId,
                description = s.description.trim(),
                amountCents = amountCents,
                currency = "INR",
                paidBy = s.paidBy,
                splitAmong = Json.encodeToString(splitList),
                createdAt = now,
                hlcTimestamp = now
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
                            paidBy = s.paidBy,
                            splitAmong = splitList,
                            createdAt = now
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
