package com.akeshari.splitblind.ui.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.akeshari.splitblind.util.generateQrBitmap

@Composable
fun QrCodeDialog(
    inviteLink: String,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(inviteLink) {
        generateQrBitmap(inviteLink, 512).asImageBitmap()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Group QR Code") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Scan this QR code to join the group",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
