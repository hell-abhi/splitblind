package com.akeshari.splitblind.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.akeshari.splitblind.ui.components.AppTitle
import java.util.Locale

private fun formatAmount(cents: Long): String {
    val rupees = cents / 100.0
    return String.format(Locale.getDefault(), "\u20B9%.2f", rupees)
}

private fun formatAmountShort(cents: Long): String {
    val rupees = cents / 100.0
    return if (rupees >= 100000) {
        String.format(Locale.getDefault(), "\u20B9%.1fL", rupees / 100000)
    } else if (rupees >= 1000) {
        String.format(Locale.getDefault(), "\u20B9%.1fK", rupees / 1000)
    } else {
        String.format(Locale.getDefault(), "\u20B9%.0f", rupees)
    }
}

// Reusable member/donut colors
val donutColors = listOf(
    0xFF42A5F5, 0xFFEF5350, 0xFF66BB6A, 0xFFFFCA28,
    0xFFAB47BC, 0xFF26C6DA, 0xFFFF7043, 0xFF8D6E63,
    0xFFEC407A, 0xFF5C6BC0
)

@Composable
fun ChartIconToggle(
    options: List<Pair<String, String>>, // mode to icon type: "donut", "bars", "line"
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.Center
    ) {
        options.forEach { (mode, iconType) ->
            val isActive = selected == mode
            val fgColor = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(36.dp, 28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center
            ) {
                when (iconType) {
                    "donut" -> {
                        // Mini donut: circle with hole
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawCircle(color = fgColor, style = Stroke(width = 3.dp.toPx()))
                        }
                    }
                    "bars" -> {
                        // Mini bars: 3 horizontal lines of different widths
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(Modifier.width(12.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(fgColor))
                            Box(Modifier.width(8.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(fgColor))
                            Box(Modifier.width(10.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(fgColor))
                        }
                    }
                    "line" -> {
                        // Mini line chart: dots connected in a zigzag pattern
                        Canvas(modifier = Modifier.size(16.dp, 12.dp)) {
                            val w = size.width
                            val h = size.height
                            val pts = listOf(
                                Offset(0f, h * 0.7f),
                                Offset(w * 0.25f, h * 0.3f),
                                Offset(w * 0.5f, h * 0.5f),
                                Offset(w * 0.75f, h * 0.15f),
                                Offset(w, h * 0.4f)
                            )
                            for (i in 0 until pts.size - 1) {
                                drawLine(fgColor, pts[i], pts[i + 1], strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                            }
                            pts.forEach { drawCircle(fgColor, 1.5.dp.toPx(), it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    segments: List<Triple<String, Long, Long>>, // label, amount, color
    total: Long,
    centerText: String,
    centerSubText: String? = null,
    modifier: Modifier = Modifier
) {
    if (total <= 0) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val strokeWidth = 28.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                var startAngle = -90f
                segments.forEach { (_, amount, color) ->
                    val sweep = (amount.toFloat() / total) * 360f
                    drawArc(
                        color = Color(color),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    centerText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (centerSubText != null) {
                    Text(
                        centerSubText,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            segments.forEach { (label, amount, color) ->
                val pct = if (total > 0) ((amount.toFloat() / total) * 100).toInt() else 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        label,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        "$pct%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        formatAmountShort(amount),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                title = { AppTitle("Spending Analytics") },
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
                // ===== Rich Summary Card =====
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
                            // Big number
                            Text(
                                formatAmount(if (isYours) state.yourTotalShare else state.totalSpent),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isYours) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            // Subtext: complementary amount
                            Text(
                                if (isYours) "Group Total: ${formatAmount(state.totalSpent)}"
                                else "Your Share: ${formatAmount(state.yourTotalShare)}",
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
                                    "${formatAmountShort(state.monthlyAvg)}/mo avg",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    " \u00B7 ",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                )
                                Text(
                                    "${state.expenseCount} expenses",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Top category
                            if (state.topCategory != null) {
                                Text(
                                    "${state.topCategory} is top (${state.topCategoryPercent}%)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }

                            // Trend
                            if (state.monthTrend != 0) {
                                val trendIcon = if (state.monthTrend > 0) "\uD83D\uDCC8" else "\uD83D\uDCC9"
                                val trendSign = if (state.monthTrend > 0) "+" else ""
                                Text(
                                    "$trendIcon ${trendSign}${state.monthTrend}% vs last month",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }

                            // Settlement summary
                            if (state.totalSettled > 0 || state.totalOutstanding > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "${formatAmountShort(state.totalSettled)} settled \u00B7 ${formatAmountShort(state.totalOutstanding)} outstanding",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
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
                        Text(
                            "By Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        ChartIconToggle(
                            options = listOf("donut" to "donut", "bars" to "bars"),
                            selected = state.chartModes["category"] ?: "donut",
                            onSelect = { viewModel.setChartMode("category", it) }
                        )
                    }
                }

                item {
                    val catMode = state.chartModes["category"] ?: "donut"
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (catMode == "donut") {
                                val total = if (isYours) state.yourTotalShare else state.totalSpent
                                val segments = state.categoryStats.map { stat ->
                                    val amt = if (isYours) stat.yourCents else stat.totalCents
                                    Triple(
                                        "${stat.tag?.emoji ?: ""} ${stat.tag?.label ?: stat.slug}",
                                        amt,
                                        stat.color
                                    )
                                }
                                DonutChart(
                                    segments = segments,
                                    total = total,
                                    centerText = formatAmountShort(total)
                                )
                            } else {
                                // Bars view (enhanced with colored dot and percentage)
                                val maxCat = state.categoryStats.maxOfOrNull {
                                    if (isYours) it.yourCents else it.totalCents
                                } ?: 1L
                                val total = if (isYours) state.yourTotalShare else state.totalSpent
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    state.categoryStats.forEach { stat ->
                                        val amt = if (isYours) stat.yourCents else stat.totalCents
                                        val pct = if (total > 0) ((amt.toFloat() / total) * 100).toInt() else 0
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
                                                    "$pct% \u00B7 ${formatAmount(amt)}",
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
                    }
                }

                // ===== By Month with Chart/Table toggle =====
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "By Month (Last 6 Months)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        ChartIconToggle(
                            options = listOf("chart" to "bars", "line" to "line"),
                            selected = state.chartModes["month"] ?: "chart",
                            onSelect = { viewModel.setChartMode("month", it) }
                        )
                    }
                }

                item {
                    val monthMode = state.chartModes["month"] ?: "chart"
                    val maxMonth = state.monthStats.maxOfOrNull {
                        if (isYours) it.yourCents else it.totalCents
                    } ?: 1L
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (monthMode == "line") {
                                // Line chart with Canvas
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val textColor = MaterialTheme.colorScheme.onSurfaceVariant
                                val months = state.monthStats
                                val amounts = months.map { if (isYours) it.yourCents else it.totalCents }
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

                                    // Gradient area under line
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

                                    // Line
                                    for (i in 0 until points.size - 1) {
                                        drawLine(
                                            primaryColor, points[i], points[i + 1],
                                            strokeWidth = 2.5.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }

                                    // Dots
                                    points.forEach { drawCircle(primaryColor, 4.dp.toPx(), it) }

                                    // Labels
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
                                    months.forEachIndexed { i, month ->
                                        drawContext.canvas.nativeCanvas.drawText(
                                            month.label.take(3),
                                            points[i].x, padTop + chartH + 16.dp.toPx(),
                                            paint
                                        )
                                        drawContext.canvas.nativeCanvas.drawText(
                                            formatAmountShort(amounts[i]),
                                            points[i].x, points[i].y - 8.dp.toPx(),
                                            amtPaint
                                        )
                                    }
                                }
                            } else {
                                // Bar chart (original)
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    }
                }

                // ===== Category by Month (stacked bars + legend) =====
                item {
                    Text(
                        "Category by Month",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    val maxMonthCat = state.monthCategoryStats.maxOfOrNull { it.totalCents } ?: 1L
                    // Collect all unique categories for legend
                    val allCats = state.monthCategoryStats.flatMap { it.categories }
                        .map { it.first }
                        .distinct()

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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

                // ===== By Member (Donut/Bars toggle) - hidden in "yours" mode =====
                if (!isYours) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "By Member",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            ChartIconToggle(
                                options = listOf("donut" to "donut", "bars" to "bars"),
                                selected = state.chartModes["member"] ?: "donut",
                                onSelect = { viewModel.setChartMode("member", it) }
                            )
                        }
                    }

                    item {
                        val memberMode = state.chartModes["member"] ?: "donut"
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (memberMode == "donut") {
                                    val totalPaid = state.memberStats.sumOf { it.totalPaid }
                                    val segments = state.memberStats.mapIndexed { index, member ->
                                        Triple(
                                            member.name,
                                            member.totalPaid,
                                            donutColors[index % donutColors.size]
                                        )
                                    }
                                    DonutChart(
                                        segments = segments,
                                        total = totalPaid,
                                        centerText = "${state.memberStats.size}",
                                        centerSubText = "members"
                                    )
                                } else {
                                    // Bars only
                                    val totalPaid = state.memberStats.sumOf { it.totalPaid }
                                    val maxPaid = state.memberStats.maxOfOrNull { it.totalPaid } ?: 1L
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        state.memberStats.forEachIndexed { index, member ->
                                            val barColor = Color(donutColors[index % donutColors.size])
                                            val pct = if (totalPaid > 0) ((member.totalPaid.toFloat() / totalPaid) * 100).toInt() else 0
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
                                                                .background(barColor)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            member.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                    Text(
                                                        "$pct% \u00B7 ${formatAmount(member.totalPaid)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(10.dp)
                                                        .clip(RoundedCornerShape(5.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                ) {
                                                    val fraction = if (maxPaid > 0) (member.totalPaid.toFloat() / maxPaid) else 0f
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                                                            .height(10.dp)
                                                            .clip(RoundedCornerShape(5.dp))
                                                            .background(barColor)
                                                    )
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
        } // Column
    }
}
