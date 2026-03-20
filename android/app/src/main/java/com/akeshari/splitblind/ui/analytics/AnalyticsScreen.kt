package com.akeshari.splitblind.ui.analytics

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale

private fun formatAmount(cents: Long): String {
    val rupees = cents / 100.0
    return String.format(Locale.getDefault(), "\u20B9%.2f", rupees)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var viewMode by remember { mutableStateOf("group") }
    val isYours = viewMode == "yours"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending Analytics") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Group selector dropdown
            if (state.groups.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                val selectedLabel = if (state.selectedGroupId == null) "Overall (All Groups)"
                    else state.groups.find { it.groupId == state.selectedGroupId }?.name ?: "Overall"

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Overall (All Groups)") },
                            onClick = {
                                viewModel.selectGroup(null)
                                expanded = false
                            }
                        )
                        state.groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    viewModel.selectGroup(group.groupId)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // View toggle: Group / Yours
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
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

            Spacer(modifier = Modifier.height(4.dp))

        if (state.totalSpent == 0L) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No expenses to analyze yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Total spending - single value based on toggle
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (isYours) "Your Spending" else "Total Group Spending",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                formatAmount(if (isYours) state.yourTotalShare else state.totalSpent),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isYours) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // By Category (single bar)
                item {
                    Text(
                        "By Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    val maxCat = state.categoryStats.maxOfOrNull {
                        if (isYours) it.yourCents else it.totalCents
                    } ?: 1L
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            state.categoryStats.forEach { stat ->
                                val amt = if (isYours) stat.yourCents else stat.totalCents
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(stat.color))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "${stat.tag?.emoji ?: ""} ${stat.tag?.label ?: stat.slug}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Text(
                                            formatAmount(amt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        val fraction = if (maxCat > 0) (amt.toFloat() / maxCat) else 0f
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(stat.color))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // By Month (single bar)
                item {
                    Text(
                        "By Month (Last 6 Months)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    val maxMonth = state.monthStats.maxOfOrNull {
                        if (isYours) it.yourCents else it.totalCents
                    } ?: 1L

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            state.monthStats.forEach { month ->
                                val amt = if (isYours) month.yourCents else month.totalCents
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            month.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            formatAmount(amt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        val fraction = if (maxMonth > 0) (amt.toFloat() / maxMonth) else 0f
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Category by Month (stacked bars)
                item {
                    Text(
                        "Category by Month",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    val maxMonthCat = state.monthCategoryStats.maxOfOrNull { it.totalCents } ?: 1L
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            state.monthCategoryStats.forEach { monthCat ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            monthCat.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            formatAmount(monthCat.totalCents),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Stacked bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        if (monthCat.totalCents > 0) {
                                            monthCat.categories.forEach { (tag, amt) ->
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

                // By Member (hidden in "yours" mode)
                if (!isYours) {
                    item {
                        Text(
                            "By Member",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        val maxPaid = state.memberStats.maxOfOrNull { it.totalPaid } ?: 1L

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                state.memberStats.forEach { member ->
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                member.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    "Paid: ${formatAmount(member.totalPaid)}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    "Balance: ${(if (member.netBalance >= 0) "+" else "") + formatAmount(member.netBalance)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (member.netBalance >= 0) Color(0xFF6BCB77) else Color(0xFFFF6B6B)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            val fraction = if (maxPaid > 0) (member.totalPaid.toFloat() / maxPaid) else 0f
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF89C4F4))
                                            )
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
        } // Column
    }
}
