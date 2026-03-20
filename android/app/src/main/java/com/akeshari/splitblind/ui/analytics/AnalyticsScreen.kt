package com.akeshari.splitblind.ui.analytics

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
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
                // Total spending - Group vs Your Share
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Total Group Spending",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    formatAmount(state.totalSpent),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(1.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Your Share",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    formatAmount(state.yourTotalShare),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // By Category (dual bars)
                item {
                    Text(
                        "By Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    // Legend
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Group", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    val maxCat = state.categoryStats.maxOfOrNull { it.totalCents } ?: 1L
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            state.categoryStats.forEach { stat ->
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
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Group bar (faded)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        val fraction = if (maxCat > 0) (stat.totalCents.toFloat() / maxCat) else 0f
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(stat.color).copy(alpha = 0.3f))
                                        )
                                    }
                                    Text(
                                        "Group ${formatAmount(stat.totalCents)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    // Your bar (solid)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        val fraction = if (maxCat > 0) (stat.yourCents.toFloat() / maxCat) else 0f
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(stat.color))
                                        )
                                    }
                                    Text(
                                        "Yours ${formatAmount(stat.yourCents)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // By Month (dual bars)
                item {
                    Text(
                        "By Month (Last 6 Months)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    val maxMonth = state.monthStats.maxOfOrNull { it.totalCents } ?: 1L

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            state.monthStats.forEach { month ->
                                Column {
                                    Text(
                                        month.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Group bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        val fraction = if (maxMonth > 0) (month.totalCents.toFloat() / maxMonth) else 0f
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        )
                                    }
                                    Text(
                                        "Group ${formatAmount(month.totalCents)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    // Your bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        val fraction = if (maxMonth > 0) (month.yourCents.toFloat() / maxMonth) else 0f
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    Text(
                                        "Yours ${formatAmount(month.yourCents)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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

                // By Member
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

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
        } // Column
    }
}
