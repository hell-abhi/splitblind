package com.akeshari.splitblind.ui.expenses

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import com.akeshari.splitblind.data.database.entity.SettlementEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.util.Debt
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatAmount(cents: Long): String {
    val rupees = cents / 100.0
    return String.format(Locale.getDefault(), "\u20B9%.2f", rupees)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    onAddExpense: (String) -> Unit,
    onEditExpense: (groupId: String, expenseId: String) -> Unit,
    onSettle: (groupId: String, from: String, to: String, amountCents: Long) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Expenses", "Balances", "Members")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.group?.name ?: "Group")
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.isSynced) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Sync",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { onAddExpense(viewModel.groupId) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ExpensesTab(
                    expenses = state.expenses,
                    settlements = state.settlements,
                    memberNames = state.memberNames,
                    myId = state.myId,
                    onEditExpense = { expense -> onEditExpense(viewModel.groupId, expense.expenseId) },
                    onDeleteExpense = { expense -> viewModel.deleteExpense(expense) },
                    onUndoSettlement = { settlement -> viewModel.undoSettlement(settlement) }
                )
                1 -> BalancesTab(
                    debts = state.debts,
                    netBalances = state.netBalances,
                    memberNames = state.memberNames,
                    myId = state.myId,
                    groupId = viewModel.groupId,
                    onSettle = onSettle
                )
                2 -> MembersTab(
                    members = state.members,
                    myId = state.myId,
                    inviteLink = state.inviteLink,
                    groupName = state.group?.name ?: ""
                )
            }
        }
    }
}

@Composable
private fun ExpensesTab(
    expenses: List<ExpenseEntity>,
    settlements: List<SettlementEntity>,
    memberNames: Map<String, String>,
    myId: String,
    onEditExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onUndoSettlement: (SettlementEntity) -> Unit
) {
    // Dialog state
    var showExpenseDialog by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSettlementDialog by remember { mutableStateOf(false) }
    var selectedSettlement by remember { mutableStateOf<SettlementEntity?>(null) }

    // Expense actions dialog
    if (showExpenseDialog && selectedExpense != null) {
        AlertDialog(
            onDismissRequest = { showExpenseDialog = false; selectedExpense = null },
            title = { Text(selectedExpense!!.description) },
            text = { Text(formatAmount(selectedExpense!!.amountCents)) },
            confirmButton = {
                TextButton(onClick = {
                    val exp = selectedExpense!!
                    showExpenseDialog = false
                    selectedExpense = null
                    onEditExpense(exp)
                }) {
                    Text("Edit Expense")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showExpenseDialog = false; selectedExpense = null }) {
                        Text("Cancel")
                    }
                    TextButton(onClick = {
                        showExpenseDialog = false
                        showDeleteConfirm = true
                    }) {
                        Text("Delete Expense", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm && selectedExpense != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; selectedExpense = null },
            title = { Text("Delete Expense") },
            text = { Text("Delete this expense? Balances will be recalculated.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExpense(selectedExpense!!)
                    showDeleteConfirm = false
                    selectedExpense = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false; selectedExpense = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settlement undo dialog
    if (showSettlementDialog && selectedSettlement != null) {
        val s = selectedSettlement!!
        val fromName = if (s.fromMember == myId) "You" else (memberNames[s.fromMember] ?: s.fromMember.take(8))
        val toName = if (s.toMember == myId) "You" else (memberNames[s.toMember] ?: s.toMember.take(8))
        AlertDialog(
            onDismissRequest = { showSettlementDialog = false; selectedSettlement = null },
            title = { Text("Undo Settlement") },
            text = { Text("Undo this settlement ($fromName paid $toName ${formatAmount(s.amountCents)})? The debt will reappear.") },
            confirmButton = {
                TextButton(onClick = {
                    onUndoSettlement(selectedSettlement!!)
                    showSettlementDialog = false
                    selectedSettlement = null
                }) {
                    Text("Undo", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettlementDialog = false; selectedSettlement = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    data class TimelineItem(val type: String, val ts: Long, val expense: ExpenseEntity? = null, val settlement: SettlementEntity? = null)
    val items = mutableListOf<TimelineItem>()
    expenses.forEach { items.add(TimelineItem("expense", it.createdAt, expense = it)) }
    settlements.forEach { items.add(TimelineItem("settlement", it.createdAt, settlement = it)) }
    items.sortByDescending { it.ts }

    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No activity yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items.size) { index ->
                val item = items[index]
                if (item.type == "expense" && item.expense != null) {
                    val expense = item.expense
                    // Check if user is involved (payer or in split)
                    val splitMembers: List<String> = try { Json.decodeFromString(expense.splitAmong) } catch (_: Exception) { emptyList() }
                    val paidByMap: Map<String, Long>? = expense.paidByMap?.let { try { Json.decodeFromString(it) } catch (_: Exception) { null } }
                    val isInvolved = expense.paidBy == myId || splitMembers.contains(myId) || paidByMap?.containsKey(myId) == true
                    val cardAlpha = if (isInvolved) 1f else 0.55f

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedExpense = expense
                            showExpenseDialog = true
                        },
                        colors = if (isInvolved) androidx.compose.material3.CardDefaults.cardColors()
                        else androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(expense.description, style = MaterialTheme.typography.titleSmall)
                                    val tag = ExpenseTag.fromSlug(expense.tag)
                                    if (tag != null) {
                                        Box(modifier = Modifier.background(Color(tag.color), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                            Text("${tag.emoji} ${tag.label}", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                                        }
                                    }
                                }
                                // Paid by with "you" label
                                val paidByText = if (paidByMap != null && paidByMap.size > 1) {
                                    paidByMap.keys.joinToString(" + ") { id -> if (id == myId) "You" else (memberNames[id] ?: id.take(6)) } + " paid"
                                } else {
                                    if (expense.paidBy == myId) "Paid by You" else "Paid by ${memberNames[expense.paidBy] ?: expense.paidBy}"
                                }
                                Text(paidByText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!isInvolved) {
                                    Text("Not involved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                                Text(dateFormat.format(Date(expense.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(formatAmount(expense.amountCents), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isInvolved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (item.type == "settlement" && item.settlement != null) {
                    val s = item.settlement
                    val isMySettlement = s.fromMember == myId || s.toMember == myId
                    val fromName = if (s.fromMember == myId) "You" else (memberNames[s.fromMember] ?: s.fromMember.take(8))
                    val toName = if (s.toMember == myId) "You" else (memberNames[s.toMember] ?: s.toMember.take(8))
                    val bgColor = if (isMySettlement) Color(0x266BCB77) else Color(0x1A6BCB77)

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedSettlement = s
                            showSettlementDialog = true
                        },
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = bgColor)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("\uD83E\uDD1D Settlement", style = MaterialTheme.typography.titleSmall)
                                    if (isMySettlement) {
                                        Box(modifier = Modifier.background(Color(0xFF6BCB77), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                                            Text("YOU", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text("$fromName paid $toName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dateFormat.format(Date(s.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(formatAmount(s.amountCents), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF6BCB77))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalancesTab(
    debts: List<Debt>,
    netBalances: Map<String, Long>,
    memberNames: Map<String, String>,
    myId: String,
    groupId: String,
    onSettle: (String, String, String, Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (netBalances.isNotEmpty()) {
            item {
                Text("Net Balances", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
            items(netBalances.entries.toList()) { (memberId, balance) ->
                val isMe = memberId == myId
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isMe) androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    else androidx.compose.material3.CardDefaults.cardColors()
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isMe) "You" else (memberNames[memberId] ?: memberId), fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal)
                            if (isMe) {
                                Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                                    Text("YOU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = (if (balance >= 0) "+" else "") + formatAmount(balance),
                            color = if (balance >= 0) Color(0xFF6BCB77) else Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (debts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Settlements Needed", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
            items(debts) { debt ->
                val isMyDebt = debt.from == myId || debt.to == myId
                val fromName = if (debt.from == myId) "You" else (memberNames[debt.from] ?: debt.from)
                val toName = if (debt.to == myId) "You" else (memberNames[debt.to] ?: debt.to)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isMyDebt) androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    else androidx.compose.material3.CardDefaults.cardColors()
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("$fromName owes $toName", style = MaterialTheme.typography.bodyMedium)
                                if (isMyDebt) {
                                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                                        Text("YOU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(formatAmount(debt.amountCents), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        if (isMyDebt) {
                            androidx.compose.material3.Button(onClick = { onSettle(groupId, debt.from, debt.to, debt.amountCents) }) {
                                Text("Settle")
                            }
                        } else {
                            OutlinedButton(onClick = { onSettle(groupId, debt.from, debt.to, debt.amountCents) }) {
                                Text("Settle")
                            }
                        }
                    }
                }
            }
        }

        if (debts.isEmpty() && netBalances.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("\uD83C\uDF89 All settled up!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MembersTab(
    members: List<MemberEntity>,
    myId: String,
    inviteLink: String?,
    groupName: String
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(members) { member ->
            val isMe = member.memberId == myId
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = if (isMe) androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                else androidx.compose.material3.CardDefaults.cardColors()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = member.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (isMe) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("YOU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (inviteLink != null) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Join my SplitBlind group \"$groupName\":\n$inviteLink")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share invite link"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Invite Members")
                }
            }
        }
    }
}
