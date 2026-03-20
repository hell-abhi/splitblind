package com.akeshari.splitblind.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDeviceScreen(
    mode: String, // "generate" or "restore"
    onBack: () -> Unit,
    onRestoreComplete: () -> Unit = {},
    viewModel: SyncDeviceViewModel = hiltViewModel()
) {
    val syncCode by viewModel.syncCode.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val restoreSuccess by viewModel.restoreSuccess.collectAsState()

    LaunchedEffect(restoreSuccess) {
        if (restoreSuccess) {
            onRestoreComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (mode == "generate") "Sync Device" else "Restore Identity")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (mode == "generate") {
                GenerateContent(
                    syncCode = syncCode,
                    timerSeconds = timerSeconds,
                    isLoading = isLoading,
                    error = error,
                    onGenerate = { pin -> viewModel.generateCode(pin) }
                )
            } else {
                RestoreContent(
                    isLoading = isLoading,
                    error = error,
                    onRestore = { code, pin -> viewModel.restore(code, pin) },
                    onClearError = { viewModel.clearError() }
                )
            }
        }
    }
}

@Composable
private fun GenerateContent(
    syncCode: String?,
    timerSeconds: Int,
    isLoading: Boolean,
    error: String?,
    onGenerate: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    if (syncCode != null) {
        // Show the generated code
        Text(
            text = "Your Sync Code",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = syncCode,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                val minutes = timerSeconds / 60
                val seconds = timerSeconds % 60
                Text(
                    text = "Expires in ${minutes}:${"%02d".format(seconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (timerSeconds < 60)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Enter this code on your other device to transfer your identity and groups.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        // Show PIN entry
        Text(
            text = "Create a PIN to encrypt your sync data",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
            label = { Text("4-digit PIN") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onGenerate(pin) },
            enabled = pin.length == 4 && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Generate Sync Code")
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RestoreContent(
    isLoading: Boolean,
    error: String?,
    onRestore: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Text(
        text = "Enter the sync code and PIN from your other device",
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = code,
        onValueChange = { newValue ->
            onClearError()
            // Auto-format: uppercase, insert dash after 4 chars
            val cleaned = newValue.uppercase().filter { it.isLetterOrDigit() }.take(8)
            code = if (cleaned.length > 4) {
                "${cleaned.take(4)}-${cleaned.drop(4)}"
            } else {
                cleaned
            }
        },
        label = { Text("Sync Code (e.g. ABCD-1234)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = pin,
        onValueChange = {
            onClearError()
            if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it
        },
        label = { Text("4-digit PIN") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = { onRestore(code, pin) },
        enabled = code.length == 9 && pin.length == 4 && !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text("Restore")
        }
    }

    if (error != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
