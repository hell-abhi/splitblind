package com.akeshari.splitblind.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRestore: () -> Unit = {},
    onRecover: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo icon
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF7C6FE0), Color(0xFFA89AF2))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(60.dp)) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val r = size.width / 2 * 0.9f
                    // Three pie segments
                    drawArc(Color(0xFFE8E0FF), -90f, 120f, true, alpha = 0.95f, topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
                    drawArc(Color(0xFFF2C4D8), 30f, 120f, true, alpha = 0.9f, topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
                    drawArc(Color(0xFFB8E8D8), 150f, 120f, true, alpha = 0.9f, topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
                    // Center circle
                    drawCircle(Color(0xFF7C6FE0), radius = r * 0.25f)
                    drawCircle(Color.White, radius = r * 0.20f)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SplitBlind",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Split. Settle. Secure.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "End-to-end encrypted expense splitting.\nNo one can read your data — not even us.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            OutlinedTextField(
                value = name,
                onValueChange = viewModel::setName,
                label = { Text("What's your name?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "That's it. No phone number. No email. No account. Just your name.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )

            Button(
                onClick = {
                    if (viewModel.save()) {
                        onComplete()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Let's Go")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onRecover,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Recover with Passphrase")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sync from Another Device")
            }
        }
    }
}
