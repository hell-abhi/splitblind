package com.akeshari.splitblind.ui.expenses

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import com.akeshari.splitblind.ui.analytics.ChartIconToggle
import com.akeshari.splitblind.ui.analytics.DonutChart
import com.akeshari.splitblind.ui.analytics.donutColors
import com.akeshari.splitblind.data.database.entity.ExpenseEntity
import com.akeshari.splitblind.data.database.entity.HistoryEntity
import com.akeshari.splitblind.data.database.entity.SettlementEntity
import com.akeshari.splitblind.data.database.entity.MemberEntity
import com.akeshari.splitblind.ui.qr.QrCodeDialog
import com.akeshari.splitblind.util.Debt
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
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
    val isPersonal = state.group?.groupType == "personal"
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = if (isPersonal) listOf("Spends", "Balance", "Stats") else listOf("Spends", "Balance", "People", "Stats")

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
                ),
                windowInsets = WindowInsets(0.dp)
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
                        text = { Text(title, maxLines = 1, fontSize = 13.sp) }
                    )
                }
            }

            when {
                selectedTab == 0 -> ExpensesTab(
                    expenses = state.allExpenses,
                    settlements = state.allSettlements,
                    memberNames = state.memberNames,
                    myId = state.myId,
                    historyMap = state.historyMap,
                    onEditExpense = { expense -> onEditExpense(viewModel.groupId, expense.expenseId) },
                    onDeleteExpense = { expense -> viewModel.deleteExpense(expense) },
                    onUndoSettlement = { settlement -> viewModel.undoSettlement(settlement) },
                    onRestoreExpense = { expense -> viewModel.restoreExpense(expense) }
                )
                selectedTab == 1 -> BalancesTab(
                    debts = state.debts,
                    netBalances = state.netBalances,
                    memberNames = state.memberNames,
                    myId = state.myId,
                    groupId = viewModel.groupId,
                    onSettle = onSettle
                )
                !isPersonal && selectedTab == 2 -> MembersTab(
                    members = state.members,
                    myId = state.myId,
                    inviteLink = state.inviteLink,
                    groupName = state.group?.name ?: ""
                )
                else -> GroupAnalyticsTab(
                    expenses = state.expenses,
                    memberNames = state.memberNames,
                    isPersonal = isPersonal,
                    myId = state.myId
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
    historyMap: Map<String, List<HistoryEntity>>,
    onEditExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onUndoSettlement: (SettlementEntity) -> Unit,
    onRestoreExpense: (ExpenseEntity) -> Unit
) {
    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var dateFilter by remember { mutableStateOf("All") }
    var showDateMenu by remember { mutableStateOf(false) }

    // Dialog state
    var showExpenseDialog by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSettlementDialog by remember { mutableStateOf(false) }
    var selectedSettlement by remember { mutableStateOf<SettlementEntity?>(null) }
    var expandedHistoryIds by remember { mutableStateOf(setOf<String>()) }
    var expandedNotesIds by remember { mutableStateOf(setOf<String>()) }

    // Expense actions dialog (only for non-deleted)
    if (showExpenseDialog && selectedExpense != null && !selectedExpense!!.isDeleted) {
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

    // Restore dialog for deleted expenses
    if (showExpenseDialog && selectedExpense != null && selectedExpense!!.isDeleted) {
        AlertDialog(
            onDismissRequest = { showExpenseDialog = false; selectedExpense = null },
            title = { Text("Deleted Expense") },
            text = { Text("${selectedExpense!!.description} - ${formatAmount(selectedExpense!!.amountCents)}") },
            confirmButton = {
                TextButton(onClick = {
                    onRestoreExpense(selectedExpense!!)
                    showExpenseDialog = false
                    selectedExpense = null
                }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpenseDialog = false; selectedExpense = null }) {
                    Text("Cancel")
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
        if (!s.isDeleted) {
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
        } else {
            AlertDialog(
                onDismissRequest = { showSettlementDialog = false; selectedSettlement = null },
                title = { Text("Deleted Settlement") },
                text = { Text("$fromName paid $toName ${formatAmount(s.amountCents)}") },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSettlementDialog = false; selectedSettlement = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }

    // Build timeline
    data class TimelineItem(val type: String, val ts: Long, val expense: ExpenseEntity? = null, val settlement: SettlementEntity? = null)
    val allItems = mutableListOf<TimelineItem>()
    expenses.forEach { allItems.add(TimelineItem("expense", it.createdAt, expense = it)) }
    settlements.forEach { allItems.add(TimelineItem("settlement", it.createdAt, settlement = it)) }
    allItems.sortByDescending { it.ts }

    // Apply filters
    val now = Calendar.getInstance()
    val thisMonthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val lastMonthStart = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val items = allItems.filter { item ->
        // Search filter (only expenses)
        val matchesSearch = if (searchQuery.isBlank()) true
        else if (item.type == "expense") item.expense?.description?.contains(searchQuery, ignoreCase = true) == true
        else true

        // Tag filter
        val matchesTag = if (selectedTagFilter == null) true
        else if (item.type == "expense") item.expense?.tag == selectedTagFilter
        else true

        // Date filter
        val matchesDate = when (dateFilter) {
            "This Month" -> item.ts >= thisMonthStart
            "Last Month" -> item.ts >= lastMonthStart && item.ts < thisMonthStart
            else -> true
        }

        matchesSearch && matchesTag && matchesDate
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search expenses...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )

        // Tag filter chips + date filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // "All" chip
                val allSelected = selectedTagFilter == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (allSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { selectedTagFilter = null }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        "All",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (allSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ExpenseTag.ALL.forEach { tag ->
                    val isSelected = selectedTagFilter == tag.slug
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) Color(tag.color)
                                else Color(tag.color).copy(alpha = 0.3f)
                            )
                            .clickable {
                                selectedTagFilter = if (isSelected) null else tag.slug
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "${tag.emoji} ${tag.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Date filter dropdown
            Box {
                TextButton(onClick = { showDateMenu = true }) {
                    Text(dateFilter, style = MaterialTheme.typography.labelSmall)
                }
                DropdownMenu(expanded = showDateMenu, onDismissRequest = { showDateMenu = false }) {
                    listOf("All", "This Month", "Last Month").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { dateFilter = option; showDateMenu = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (searchQuery.isNotBlank() || selectedTagFilter != null || dateFilter != "All")
                        "No matching activity"
                    else "No activity yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val dateTimeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    if (item.type == "expense" && item.expense != null) {
                        val expense = item.expense
                        val isDeleted = expense.isDeleted
                        val splitMembers: List<String> = try { Json.decodeFromString(expense.splitAmong) } catch (_: Exception) { emptyList() }
                        val paidByMap: Map<String, Long>? = expense.paidByMap?.let { try { Json.decodeFromString(it) } catch (_: Exception) { null } }
                        val isInvolved = expense.paidBy == myId || splitMembers.contains(myId) || paidByMap?.containsKey(myId) == true
                        val itemAlpha = if (isDeleted) 0.5f else 1f

                        val history = historyMap[expense.expenseId] ?: emptyList()
                        val isHistoryExpanded = expense.expenseId in expandedHistoryIds
                        val isNotesExpanded = expense.expenseId in expandedNotesIds
                        val hasNotes = !expense.notes.isNullOrBlank()

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedExpense = expense
                                        showExpenseDialog = true
                                    },
                                colors = if (isDeleted) {
                                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                } else if (isInvolved) {
                                    CardDefaults.cardColors()
                                } else {
                                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                }
                            ) {
                                Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    expense.description,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = itemAlpha)
                                                )
                                                if (isDeleted) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("Deleted", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                val tag = ExpenseTag.fromSlug(expense.tag)
                                                if (!isDeleted) {
                                                    Box(modifier = Modifier.background(Color(tag.color), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                                        Text("${tag.emoji} ${tag.label}", style = MaterialTheme.typography.labelSmall, color = Color.Black)
                                                    }
                                                }
                                                // Notes indicator
                                                if (hasNotes && !isDeleted) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                                                            .clickable {
                                                                expandedNotesIds = if (isNotesExpanded) expandedNotesIds - expense.expenseId
                                                                else expandedNotesIds + expense.expenseId
                                                            }
                                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("\uD83D\uDCDD", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                            val paidByText = if (paidByMap != null && paidByMap.size > 1) {
                                                paidByMap.keys.joinToString(" + ") { id -> if (id == myId) "You" else (memberNames[id] ?: id.take(6)) } + " paid"
                                            } else {
                                                if (expense.paidBy == myId) "Paid by You" else "Paid by ${memberNames[expense.paidBy] ?: expense.paidBy}"
                                            }
                                            Text(paidByText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = itemAlpha))
                                            if (!isInvolved && !isDeleted) {
                                                Text("Not involved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                            }
                                            Text(dateFormat.format(Date(expense.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = itemAlpha))
                                        }
                                        Text(
                                            formatAmount(expense.amountCents),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDeleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            else if (isInvolved) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Expandable notes
                                    AnimatedVisibility(visible = isNotesExpanded && hasNotes) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                            )
                                        ) {
                                            Text(
                                                expense.notes ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    // History button
                                    if (history.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                expandedHistoryIds = if (isHistoryExpanded) {
                                                    expandedHistoryIds - expense.expenseId
                                                } else {
                                                    expandedHistoryIds + expense.expenseId
                                                }
                                            },
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text(
                                                if (isHistoryExpanded) "Hide History" else "History (${history.size})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        if (isHistoryExpanded) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 8.dp, top = 4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                history.forEach { h ->
                                                    HistoryEntryRow(h, dateTimeFormat)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (item.type == "settlement" && item.settlement != null) {
                        val s = item.settlement
                        val isDeleted = s.isDeleted
                        val isMySettlement = s.fromMember == myId || s.toMember == myId
                        val fromName = if (s.fromMember == myId) "You" else (memberNames[s.fromMember] ?: s.fromMember.take(8))
                        val toName = if (s.toMember == myId) "You" else (memberNames[s.toMember] ?: s.toMember.take(8))
                        val itemAlpha = if (isDeleted) 0.5f else 1f

                        val history = historyMap[s.settlementId] ?: emptyList()
                        val isHistoryExpanded = s.settlementId in expandedHistoryIds

                        val bgColor = if (isDeleted) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        } else if (isMySettlement) {
                            Color(0x266BCB77)
                        } else {
                            Color(0x1A6BCB77)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSettlement = s
                                    showSettlementDialog = true
                                },
                            colors = CardDefaults.cardColors(containerColor = bgColor)
                        ) {
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                "\uD83E\uDD1D Settlement",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = itemAlpha)
                                            )
                                            if (isDeleted) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                                ) {
                                                    Text("Deleted", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            } else if (isMySettlement) {
                                                Box(modifier = Modifier.background(Color(0xFF6BCB77), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                                                    Text("YOU", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text("$fromName paid $toName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = itemAlpha))
                                        Text(dateFormat.format(Date(s.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = itemAlpha))
                                    }
                                    Text(
                                        formatAmount(s.amountCents),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDeleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color(0xFF6BCB77)
                                    )
                                }

                                // History button
                                if (history.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            expandedHistoryIds = if (isHistoryExpanded) {
                                                expandedHistoryIds - s.settlementId
                                            } else {
                                                expandedHistoryIds + s.settlementId
                                            }
                                        },
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            if (isHistoryExpanded) "Hide History" else "History (${history.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (isHistoryExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, top = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            history.forEach { h ->
                                                HistoryEntryRow(h, dateTimeFormat)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryRow(history: HistoryEntity, dateFormat: SimpleDateFormat) {
    val icon = when (history.action) {
        "created" -> "\uD83D\uDCDD"
        "edited" -> "\u270F\uFE0F"
        "deleted" -> "\uD83D\uDDD1\uFE0F"
        "restored" -> "\u267B\uFE0F"
        else -> "\u2022"
    }
    val actionText = when (history.action) {
        "created" -> "Created"
        "edited" -> "Edited"
        "deleted" -> "Deleted"
        "restored" -> "Restored"
        else -> history.action.replaceFirstChar { it.uppercase() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(icon, style = MaterialTheme.typography.labelSmall)
        Text(
            "$actionText by ${history.changedByName}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "\u00B7",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            dateFormat.format(Date(history.changedAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }

    if (history.action == "edited" && history.previousData != null && history.newData != null) {
        val changesSummary = remember(history.historyId) {
            try {
                val prev: Map<String, String> = Json.decodeFromString(history.previousData)
                val next: Map<String, String> = Json.decodeFromString(history.newData)
                val changes = mutableListOf<String>()
                if (prev["description"] != next["description"]) {
                    changes.add("\"${prev["description"]}\" -> \"${next["description"]}\"")
                }
                if (prev["amountCents"] != next["amountCents"]) {
                    val oldAmt = (prev["amountCents"]?.toLongOrNull() ?: 0) / 100.0
                    val newAmt = (next["amountCents"]?.toLongOrNull() ?: 0) / 100.0
                    changes.add(String.format(Locale.getDefault(), "\u20B9%.2f -> \u20B9%.2f", oldAmt, newAmt))
                }
                if (changes.isNotEmpty()) changes.joinToString(", ") else null
            } catch (_: Exception) { null }
        }
        if (changesSummary != null) {
            Text(
                changesSummary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 24.dp)
            )
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                    colors = if (isMe) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    else CardDefaults.cardColors()
                ) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                    colors = if (isMyDebt) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    else CardDefaults.cardColors()
                ) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
    var showQrDialog by remember { mutableStateOf(false) }

    if (showQrDialog && inviteLink != null) {
        QrCodeDialog(
            inviteLink = inviteLink,
            groupName = groupName,
            onDismiss = { showQrDialog = false }
        )
    }

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
                colors = if (isMe) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                else CardDefaults.cardColors()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Join my SplitBlind group \"$groupName\":\n$inviteLink")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share invite link"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invite")
                    }
                    OutlinedButton(
                        onClick = { showQrDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Show QR")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupAnalyticsTab(
    expenses: List<ExpenseEntity>,
    memberNames: Map<String, String>,
    isPersonal: Boolean,
    myId: String
) {
    if (expenses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No expenses to analyze yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var viewMode by remember { mutableStateOf("group") }
    val isYours = viewMode == "yours"

    // Chart mode toggles
    var categoryMode by remember { mutableStateOf("donut") }
    var monthMode by remember { mutableStateOf("chart") }
    var memberMode by remember { mutableStateOf("donut") }

    // Helper to compute user's share of an expense
    fun getMyShare(expense: ExpenseEntity): Long {
        if (expense.splitDetails != null) {
            val details: Map<String, Long> = try {
                Json.decodeFromString(expense.splitDetails)
            } catch (_: Exception) { emptyMap() }
            return details[myId] ?: 0
        }
        val splitAmong: List<String> = try {
            Json.decodeFromString(expense.splitAmong)
        } catch (_: Exception) { emptyList() }
        if (splitAmong.contains(myId)) {
            return expense.amountCents / splitAmong.size
        }
        return 0
    }

    val totalSpent = expenses.sumOf { it.amountCents }
    val yourTotalShare = expenses.sumOf { getMyShare(it) }
    val expenseCount = expenses.size

    // By Category
    val byCat = expenses.groupBy { it.tag ?: "other" }
    data class CatData(val tag: ExpenseTag?, val total: Long, val yours: Long, val percentage: Float, val color: Long)
    val categoryStats = byCat.map { (slug, exps) ->
        val total = exps.sumOf { it.amountCents }
        val yours = exps.sumOf { getMyShare(it) }
        val tag = ExpenseTag.fromSlug(slug)
        CatData(tag, total, yours, if (totalSpent > 0) (total.toFloat() / totalSpent) * 100f else 0f, tag?.color ?: 0xFFCFD8DC)
    }.sortedByDescending { it.total }

    // Top category
    val topCat = categoryStats.firstOrNull()
    val topCategory = topCat?.let { "${it.tag?.emoji ?: ""} ${it.tag?.label ?: "Other"}" }
    val topCategoryPercent = topCat?.percentage?.toInt() ?: 0

    // By Month (last 6 months)
    val monthNames = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    data class MonthData(val label: String, val total: Long, val yours: Long, val count: Int)
    val monthStats = mutableListOf<MonthData>()
    data class MonthCatData(val label: String, val total: Long, val yourTotal: Long, val categories: List<Pair<ExpenseTag?, Long>>, val yourCategories: List<Pair<ExpenseTag?, Long>>)
    val monthCategoryStats = mutableListOf<MonthCatData>()
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
        val monthExps = expenses.filter { it.createdAt in start until end }
        val total = monthExps.sumOf { it.amountCents }
        val yours = monthExps.sumOf { getMyShare(it) }
        monthStats.add(MonthData(label, total, yours, monthExps.size))

        val catBreakdown = monthExps.groupBy { it.tag ?: "other" }
            .map { (slug, exps) -> Pair(ExpenseTag.fromSlug(slug), exps.sumOf { it.amountCents }) }
            .sortedByDescending { it.second }
        val yourCatBreakdown = monthExps.groupBy { it.tag ?: "other" }
            .map { (slug, exps) -> Pair(ExpenseTag.fromSlug(slug), exps.sumOf { getMyShare(it) }) }
            .sortedByDescending { it.second }
        monthCategoryStats.add(MonthCatData(label, total, yours, catBreakdown, yourCatBreakdown))
    }

    // Monthly average
    val monthsWithExpenses = monthStats.filter { it.total > 0 }
    val monthlyAvg = if (monthsWithExpenses.isNotEmpty()) totalSpent / monthsWithExpenses.size else 0L

    // Month trend
    val currentMonthTotal = monthStats.lastOrNull()?.total ?: 0L
    val prevMonthTotal = if (monthStats.size >= 2) monthStats[monthStats.size - 2].total else 0L
    val monthTrend = if (prevMonthTotal > 0) {
        (((currentMonthTotal - prevMonthTotal).toFloat() / prevMonthTotal) * 100).toInt()
    } else if (currentMonthTotal > 0) 100 else 0

    // Format helpers
    fun fmtShort(cents: Long): String {
        val rupees = cents / 100.0
        return if (rupees >= 100000) String.format(Locale.getDefault(), "\u20B9%.1fL", rupees / 100000)
        else if (rupees >= 1000) String.format(Locale.getDefault(), "\u20B9%.1fK", rupees / 1000)
        else String.format(Locale.getDefault(), "\u20B9%.0f", rupees)
    }

    // By Member
    val paidByMember = mutableMapOf<String, Long>()
    if (!isPersonal) {
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
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // View toggle: Group / Yours
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("group" to "Group", "yours" to "Yours").forEach { (mode, label) ->
                val isActive = viewMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable { viewMode = mode }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isActive) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== Rich Summary Card =====
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Big number
                        Text(
                            formatAmount(if (isYours) yourTotalShare else totalSpent),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isYours) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        // Subtext
                        Text(
                            if (isYours) "Group Total: ${formatAmount(totalSpent)}"
                            else "Your Share: ${formatAmount(yourTotalShare)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "${fmtShort(monthlyAvg)}/mo avg",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                " \u00B7 ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                            Text(
                                "$expenseCount expenses",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Top category
                        if (topCategory != null) {
                            Text(
                                "$topCategory is top ($topCategoryPercent%)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        // Trend
                        if (monthTrend != 0) {
                            val trendIcon = if (monthTrend > 0) "\uD83D\uDCC8" else "\uD83D\uDCC9"
                            val trendSign = if (monthTrend > 0) "+" else ""
                            Text(
                                "$trendIcon ${trendSign}${monthTrend}% vs last month",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // ===== By Category with Donut/Bars toggle =====
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("By Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ChartIconToggle(
                        options = listOf("donut" to "donut", "bars" to "bars"),
                        selected = categoryMode,
                        onSelect = { categoryMode = it }
                    )
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (categoryMode == "donut") {
                            val total = if (isYours) yourTotalShare else totalSpent
                            val segments = categoryStats.map { stat ->
                                val amt = if (isYours) stat.yours else stat.total
                                Triple(
                                    "${stat.tag?.emoji ?: ""} ${stat.tag?.label ?: "Other"}",
                                    amt,
                                    stat.color
                                )
                            }
                            DonutChart(
                                segments = segments,
                                total = total,
                                centerText = fmtShort(total)
                            )
                        } else {
                            val maxCat = categoryStats.maxOfOrNull { if (isYours) it.yours else it.total } ?: 1L
                            val total = if (isYours) yourTotalShare else totalSpent
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                categoryStats.forEach { stat ->
                                    val amt = if (isYours) stat.yours else stat.total
                                    val pct = if (total > 0) ((amt.toFloat() / total) * 100).toInt() else 0
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(stat.color)))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("${stat.tag?.emoji ?: ""} ${stat.tag?.label ?: "Other"}", style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Text("$pct% \u00B7 ${formatAmount(amt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                            val fraction = if (maxCat > 0) (amt.toFloat() / maxCat) else 0f
                                            Box(modifier = Modifier.fillMaxWidth(fraction = fraction.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(stat.color)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===== By Month with Chart/Table toggle =====
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("By Month (Last 6 Months)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ChartIconToggle(
                        options = listOf("chart" to "bars", "line" to "line"),
                        selected = monthMode,
                        onSelect = { monthMode = it }
                    )
                }
            }
            item {
                val maxMonth = monthStats.maxOfOrNull { if (isYours) it.yours else it.total } ?: 1L
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (monthMode == "line") {
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val textColor = MaterialTheme.colorScheme.onSurfaceVariant
                            val amounts = monthStats.map { if (isYours) it.yours else it.total }
                            val maxVal = amounts.maxOrNull()?.coerceAtLeast(1L) ?: 1L

                            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                                val padTop = 30.dp.toPx()
                                val padBot = 24.dp.toPx()
                                val padX = 20.dp.toPx()
                                val chartW = size.width - padX * 2
                                val chartH = size.height - padTop - padBot
                                val count = amounts.size
                                if (count == 0) return@Canvas

                                val points = amounts.mapIndexed { i, v ->
                                    val x = if (count > 1) padX + (i.toFloat() / (count - 1)) * chartW else padX + chartW / 2
                                    val y = padTop + (1f - v.toFloat() / maxVal) * chartH
                                    Offset(x, y)
                                }

                                val areaPath = Path().apply {
                                    moveTo(points.first().x, padTop + chartH)
                                    points.forEach { lineTo(it.x, it.y) }
                                    lineTo(points.last().x, padTop + chartH)
                                    close()
                                }
                                drawPath(
                                    areaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(primaryColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0f)),
                                        startY = padTop,
                                        endY = padTop + chartH
                                    )
                                )
                                for (i in 0 until points.size - 1) {
                                    drawLine(primaryColor, points[i], points[i + 1], strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                }
                                points.forEach { drawCircle(primaryColor, 4.dp.toPx(), it) }

                                val paint = android.graphics.Paint().apply {
                                    color = textColor.hashCode()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    textSize = 10.sp.toPx()
                                    isAntiAlias = true
                                }
                                val amtPaint = android.graphics.Paint().apply {
                                    color = textColor.hashCode()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    textSize = 9.sp.toPx()
                                    isFakeBoldText = true
                                    isAntiAlias = true
                                }
                                monthStats.forEachIndexed { i, month ->
                                    drawContext.canvas.nativeCanvas.drawText(
                                        month.label.take(3), points[i].x, padTop + chartH + 16.dp.toPx(), paint
                                    )
                                    drawContext.canvas.nativeCanvas.drawText(
                                        fmtShort(amounts[i]), points[i].x, points[i].y - 8.dp.toPx(), amtPaint
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                monthStats.forEach { month ->
                                    val amt = if (isYours) month.yours else month.total
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(month.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(formatAmount(amt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                            val fraction = if (maxMonth > 0) (amt.toFloat() / maxMonth) else 0f
                                            Box(modifier = Modifier.fillMaxWidth(fraction = fraction.coerceIn(0f, 1f)).height(14.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===== Category by Month (stacked bars + legend) =====
            item {
                Text("Category by Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                val maxMonthCat = monthCategoryStats.maxOfOrNull { if (isYours) it.yourTotal else it.total } ?: 1L
                val allCats = monthCategoryStats.flatMap { it.categories }.map { it.first }.distinct()
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Legend
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            allCats.forEach { tag ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(tag?.color ?: 0xFFCFD8DC))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "${tag?.emoji ?: ""} ${tag?.label ?: "Other"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        monthCategoryStats.forEach { monthCat ->
                            val cats = if (isYours) monthCat.yourCategories else monthCat.categories
                            val total = if (isYours) monthCat.yourTotal else monthCat.total
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(monthCat.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(formatAmount(total), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (total > 0) {
                                        cats.forEach { (tag, amt) ->
                                            val segFraction = amt.toFloat() / maxMonthCat
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction = segFraction.coerceIn(0f, 1f))
                                                    .height(24.dp)
                                                    .background(Color(tag?.color ?: 0xFFCFD8DC))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===== By Member (Donut/Bars toggle) - skip for personal, hidden in "yours" mode =====
            if (!isPersonal && !isYours && paidByMember.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("By Member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        ChartIconToggle(
                            options = listOf("donut" to "donut", "bars" to "bars"),
                            selected = memberMode,
                            onSelect = { memberMode = it }
                        )
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val totalPaid = paidByMember.values.sum()
                            val sortedMembers = paidByMember.entries.sortedByDescending { it.value }
                            if (memberMode == "donut") {
                                val segments = sortedMembers.mapIndexed { index, (memberId, paid) ->
                                    Triple(
                                        memberNames[memberId] ?: memberId.take(8),
                                        paid,
                                        donutColors[index % donutColors.size]
                                    )
                                }
                                DonutChart(
                                    segments = segments,
                                    total = totalPaid,
                                    centerText = "${sortedMembers.size}",
                                    centerSubText = "members"
                                )
                            } else {
                                val maxPaid = sortedMembers.maxOfOrNull { it.value } ?: 1L
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    sortedMembers.forEachIndexed { index, (memberId, paid) ->
                                        val barColor = Color(donutColors[index % donutColors.size])
                                        val pct = if (totalPaid > 0) ((paid.toFloat() / totalPaid) * 100).toInt() else 0
                                        Column {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(barColor))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        memberNames[memberId] ?: memberId.take(8),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Text(
                                                    "$pct% \u00B7 ${formatAmount(paid)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                                val fraction = if (maxPaid > 0) (paid.toFloat() / maxPaid) else 0f
                                                Box(modifier = Modifier.fillMaxWidth(fraction = fraction.coerceIn(0f, 1f)).height(10.dp).clip(RoundedCornerShape(5.dp)).background(barColor))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
