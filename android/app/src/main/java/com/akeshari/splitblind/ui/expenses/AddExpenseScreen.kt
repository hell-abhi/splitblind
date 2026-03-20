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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = viewModel::setAmount,
                        label = { Text("Amount (\u20B9)") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth()
                    )
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
                                }
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
