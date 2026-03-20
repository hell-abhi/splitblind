package com.akeshari.splitblind.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    fun evaluate(expr: String): String {
        return try {
            val sanitized = expr
                .replace("\u00D7", "*")
                .replace("\u00F7", "/")
            // Simple expression evaluator: handles +, -, *, /
            val tokens = mutableListOf<String>()
            var current = ""
            for (ch in sanitized) {
                if (ch in "+-*/" && current.isNotEmpty()) {
                    tokens.add(current)
                    tokens.add(ch.toString())
                    current = ""
                } else {
                    current += ch
                }
            }
            if (current.isNotEmpty()) tokens.add(current)

            // First pass: * and /
            val simplified = mutableListOf<String>()
            var i = 0
            while (i < tokens.size) {
                if (i + 1 < tokens.size && (tokens[i + 1] == "*" || tokens[i + 1] == "/")) {
                    var acc = (if (simplified.isNotEmpty() && simplified.last().toDoubleOrNull() != null) simplified.removeLast().toDouble() else tokens[i].toDouble())
                    while (i + 1 < tokens.size && (tokens[i + 1] == "*" || tokens[i + 1] == "/")) {
                        val op = tokens[i + 1]
                        val next = tokens[i + 2].toDouble()
                        acc = if (op == "*") acc * next else if (next != 0.0) acc / next else Double.NaN
                        i += 2
                    }
                    simplified.add(acc.toString())
                    i++
                } else {
                    simplified.add(tokens[i])
                    i++
                }
            }

            // Second pass: + and -
            var total = simplified[0].toDouble()
            var j = 1
            while (j < simplified.size) {
                val op = simplified[j]
                val next = simplified[j + 1].toDouble()
                total = if (op == "+") total + next else total - next
                j += 2
            }

            if (total == total.toLong().toDouble()) {
                total.toLong().toString()
            } else {
                String.format("%.2f", total)
            }
        } catch (_: Exception) {
            ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calculator") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            expression.ifEmpty { "0" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (result.isNotEmpty()) {
                            Text(
                                "= $result",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Button grid
                val buttons = listOf(
                    listOf("7", "8", "9", "\u00F7"),
                    listOf("4", "5", "6", "\u00D7"),
                    listOf("1", "2", "3", "-"),
                    listOf("0", ".", "C", "+")
                )

                buttons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { label ->
                            val isOp = label in listOf("+", "-", "\u00D7", "\u00F7")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isOp) MaterialTheme.colorScheme.primaryContainer
                                        else if (label == "C") MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        when (label) {
                                            "C" -> {
                                                expression = ""
                                                result = ""
                                            }
                                            else -> {
                                                expression += label
                                                result = evaluate(expression)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (label == "C") MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = result.ifEmpty { expression }
                if (value.isNotEmpty() && value.toDoubleOrNull() != null) {
                    onResult(value)
                }
            }) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
