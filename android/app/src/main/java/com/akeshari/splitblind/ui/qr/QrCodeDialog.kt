package com.akeshari.splitblind.ui.qr

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.akeshari.splitblind.util.generateQrBitmap
import java.io.File

@Composable
fun QrCodeDialog(
    inviteLink: String,
    groupName: String = "SplitBlind Group",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrBitmap = remember(inviteLink) {
        generateQrBitmap(inviteLink, 512)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                groupName,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Scan this QR code to join the group",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = {
                    // Share QR image with message
                    try {
                        val file = File(context.cacheDir, "${groupName.replace(" ", "-")}-qr.png")
                        file.outputStream().use { qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, "Join my SplitBlind group \"$groupName\"!\n\nScan the QR code or use this link:\n$inviteLink")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share QR Code"))
                    } catch (_: Exception) {
                        // Fallback: share text only
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Join my SplitBlind group \"$groupName\":\n$inviteLink")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share"))
                    }
                }) {
                    Text("Share QR")
                }
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    )
}
