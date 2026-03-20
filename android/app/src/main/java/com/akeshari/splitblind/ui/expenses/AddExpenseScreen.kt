package com.akeshari.splitblind.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val pastelAvatarColors = listOf(
    Color(0xFFF2A0C4), // pink
    Color(0xFF8DDCC5), // mint
    Color(0xFFF8C4A4), // peach
    Color(0xFF89C4F4), // sky
    Color(0xFFA89AF2), // purple
    Color(0xFFFFE0B2), // orange
    Color(0xFFB2DFDB), // teal
    Color(0xFFFFF9C4), // yellow
    Color(0xFFFFCDD2), // red
    Color(0xFFC8E6C9), // green
)

private fun avatarColorFor(memberId: String): Color {
    val index = memberId.hashCode().let { if (it < 0) -it else it } % pastelAvatarColors.size
    return pastelAvatarColors[index]
}

@Composable
private fun MemberAvatar(memberId: String, displayName: String) {
    val color = avatarColorFor(memberId)
    val initial = displayName.firstOrNull()?.uppercaseChar() ?: '?'
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D3436)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val members by viewModel.members.collectAsState()
    var showCalculator by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    // Calculator dialog
    if (showCalculator) {
        CalculatorDialog(
            onResult = { result ->
                viewModel.setAmount(result)
                showCalculator = false
            },
            onDismiss = { showCalculator = false }
        )
    }

    // Currency picker dialog
    if (showCurrencyDialog) {
        var currencySearch by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column {
                    OutlinedTextField(
                        value = currencySearch,
                        onValueChange = { currencySearch = it },
                        placeholder = { Text("Search currencies...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (currencySearch.isBlank()) {
                        Text("Frequently Used", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    val currencies = if (currencySearch.isBlank()) {
                        CurrencyInfo.FREQUENTLY_USED
                    } else {
                        CurrencyInfo.ALL.filter {
                            it.code.contains(currencySearch, ignoreCase = true) ||
                            it.name.contains(currencySearch, ignoreCase = true)
                        }
                    }
                    Column(
                        modifier = Modifier.height(300.dp).verticalScroll(rememberScrollState())
                    ) {
                        currencies.forEach { currency ->
                            val isSelected = state.currency == currency.code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        viewModel.setCurrency(currency.code)
                                        showCurrencyDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currency.symbol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Column {
                                    Text(currency.code, fontWeight = FontWeight.SemiBold)
                                    Text(currency.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (currencySearch.isBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("All Currencies", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            CurrencyInfo.ALL.filter { it !in CurrencyInfo.FREQUENTLY_USED }.forEach { currency ->
                                val isSelected = state.currency == currency.code
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable {
                                            viewModel.setCurrency(currency.code)
                                            showCurrencyDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(currency.symbol, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Column {
                                        Text(currency.code, fontWeight = FontWeight.SemiBold)
                                        Text(currency.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Auto-select all members initially
    LaunchedEffect(members) {
        if (members.isNotEmpty() && state.splitAmong.isEmpty()) {
            viewModel.selectAllMembers()
        }
    }

    // Navigate back on save
    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Expense" else "Add Expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ---- Card: What & How Much ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "\uD83D\uDCDD What & How Much",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = viewModel::setDescription,
                        label = { Text("Description") },
                        placeholder = { Text("e.g., Dinner at restaurant") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Currency button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showCurrencyDialog = true }
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                        ) {
                            Text(
                                state.currency,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        OutlinedTextField(
                            value = state.amount,
                            onValueChange = viewModel::setAmount,
                            label = { Text("Amount (${CurrencyInfo.symbolFor(state.currency)})") },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        // Calculator button
                        IconButton(onClick = { showCalculator = true }) {
                            Icon(
                                Icons.Default.Calculate,
                                contentDescription = "Calculator",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    // Conversion preview
                    if (state.isLoadingRate) {
                        Text(
                            "Fetching exchange rate...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    } else if (state.conversionPreview != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = state.conversionPreview!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // ---- Card: Notes ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "\uD83D\uDCDD Notes",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = viewModel::setNotes,
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("Add any extra details...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ---- Card: Recurring ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "\uD83D\uDD01 Repeat",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = state.isRecurring,
                            onCheckedChange = { viewModel.setRecurring(it) }
                        )
                    }
                    if (state.isRecurring) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("weekly" to "Weekly", "monthly" to "Monthly", "yearly" to "Yearly").forEach { (value, label) ->
                                val isSelected = state.recurringFrequency == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { viewModel.setRecurringFrequency(value) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---- Card: Category ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "\uD83C\uDFF7\uFE0F Category",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ExpenseTag.ALL.forEach { tag ->
                            val isSelected = state.selectedTag == tag.slug
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) Color(tag.color)
                                        else Color(tag.color).copy(alpha = 0.35f)
                                    )
                                    .then(
                                        if (isSelected) Modifier.border(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(20.dp)
                                        ) else Modifier
                                    )
                                    .clickable { viewModel.setTag(tag.slug) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "${tag.emoji} ${tag.label}",
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // ---- Card: Paid By ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "\uD83D\uDCB3 Paid By",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Multiple",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = state.isMultiPayer,
                                onCheckedChange = { viewModel.setMultiPayer(it) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.isMultiPayer) {
                        val totalCents = (state.amount.toDoubleOrNull() ?: 0.0) * 100
                        val enteredCents = members.sumOf { m ->
                            ((state.payerAmounts[m.memberId]?.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        }
                        val remaining = totalCents.toLong() - enteredCents

                        members.forEach { member ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                MemberAvatar(member.memberId, member.displayName)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    member.displayName,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                OutlinedTextField(
                                    value = state.payerAmounts[member.memberId] ?: "",
                                    onValueChange = { viewModel.setPayerAmount(member.memberId, it) },
                                    placeholder = { Text("0") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(120.dp)
                                )
                            }
                        }
                        if (remaining != 0L && state.amount.isNotEmpty()) {
                            Text(
                                "Remaining: \u20B9${String.format("%.2f", remaining / 100.0)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (remaining == 0L) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else {
                        members.forEach { member ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setPaidBy(member.memberId) }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = state.paidBy == member.memberId,
                                    onClick = { viewModel.setPaidBy(member.memberId) }
                                )
                                MemberAvatar(member.memberId, member.displayName)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(member.displayName)
                            }
                        }
                    }
                }
            }

            // ---- Card: Split Among ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Split mode selector
                    Text(
                        "\u2702\uFE0F Split Among",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SplitMode.entries.forEach { mode ->
                            val isSelected = state.splitMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { viewModel.setSplitMode(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    mode.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Select all
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.selectAllMembers() }) {
                            Text("Select all")
                        }
                    }

                    // Member rows
                    members.forEach { member ->
                        val isInSplit = member.memberId in state.splitAmong
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isInSplit,
                                onCheckedChange = { viewModel.toggleSplitMember(member.memberId) }
                            )
                            MemberAvatar(member.memberId, member.displayName)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                member.displayName,
                                modifier = Modifier.weight(1f)
                            )

                            if (isInSplit) {
                                when (state.splitMode) {
                                    SplitMode.AMOUNT -> {
                                        OutlinedTextField(
                                            value = state.splitAmounts[member.memberId] ?: "",
                                            onValueChange = { viewModel.setSplitAmount(member.memberId, it) },
                                            placeholder = { Text("0") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.width(110.dp)
                                        )
                                    }
                                    SplitMode.PERCENTAGE -> {
                                        OutlinedTextField(
                                            value = state.splitPercentages[member.memberId] ?: "",
                                            onValueChange = { viewModel.setSplitPercentage(member.memberId, it) },
                                            placeholder = { Text("%") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.width(90.dp),
                                            suffix = { Text("%") }
                                        )
                                    }
                                    SplitMode.RATIO -> {
                                        OutlinedTextField(
                                            value = state.splitRatios[member.memberId] ?: "",
                                            onValueChange = { viewModel.setSplitRatio(member.memberId, it) },
                                            placeholder = { Text("1") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.width(80.dp)
                                        )
                                    }
                                    SplitMode.EQUAL -> { /* No extra input */ }
                                    SplitMode.ITEMS -> { /* Items handled below */ }
                                }
                            }
                        }
                    }

                    // ---- Item-wise split UI ----
                    if (state.splitMode == SplitMode.ITEMS) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Items",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        state.splitItems.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = item.name,
                                            onValueChange = { viewModel.updateSplitItemName(index, it) },
                                            placeholder = { Text("Item name") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = item.amount,
                                            onValueChange = { viewModel.updateSplitItemAmount(index, it) },
                                            placeholder = { Text("0.00") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.width(100.dp)
                                        )
                                        if (state.splitItems.size > 1) {
                                            IconButton(
                                                onClick = { viewModel.removeSplitItem(index) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Split among:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        members.filter { it.memberId in state.splitAmong }.forEach { member ->
                                            val isChecked = member.memberId in item.members
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(
                                                        if (isChecked) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .clickable { viewModel.toggleSplitItemMember(index, member.memberId) }
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    member.displayName,
                                                    fontSize = 11.sp,
                                                    color = if (isChecked) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.addSplitItem() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Add Item")
                        }

                        // Items total validation
                        if (state.amount.isNotEmpty()) {
                            val totalCents = ((state.amount.toDoubleOrNull() ?: 0.0) * 100).toLong()
                            val itemsCents = state.splitItems.sumOf { ((it.amount.toDoubleOrNull() ?: 0.0) * 100).toLong() }
                            val diff = totalCents - itemsCents
                            if (diff != 0L) {
                                Text(
                                    "Items remaining: ${CurrencyInfo.symbolFor(state.currency)}${String.format("%.2f", diff / 100.0)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Validation info
                    if (state.splitMode == SplitMode.AMOUNT && state.amount.isNotEmpty()) {
                        val totalCents = ((state.amount.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        val enteredCents = state.splitAmong.sumOf { id ->
                            ((state.splitAmounts[id]?.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        }
                        val diff = totalCents - enteredCents
                        if (diff != 0L) {
                            Text(
                                "Remaining: \u20B9${String.format("%.2f", diff / 100.0)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    if (state.splitMode == SplitMode.PERCENTAGE) {
                        val totalPct = state.splitAmong.sumOf { id ->
                            state.splitPercentages[id]?.toDoubleOrNull() ?: 0.0
                        }
                        if (totalPct > 0 && kotlin.math.abs(totalPct - 100.0) > 0.01) {
                            Text(
                                "Total: ${String.format("%.1f", totalPct)}% (must be 100%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Error message
            if (state.error != null) {
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Save button
            Button(
                onClick = { viewModel.saveExpense() },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSaving) "Saving..." else if (state.isEditing) "Save Changes" else "Add Expense")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
