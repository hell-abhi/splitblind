package com.akeshari.splitblind.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppLogo(size: Int = 24) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val cx = this.size.width / 2
        val cy = this.size.height / 2
        val r = this.size.width / 2 * 0.9f
        drawArc(Color(0xFFE8E0FF), -90f, 120f, true, alpha = 0.95f, topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
        drawArc(Color(0xFFF2C4D8), 30f, 120f, true, alpha = 0.9f, topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
        drawArc(Color(0xFFB8E8D8), 150f, 120f, true, alpha = 0.9f, topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
        drawCircle(Color(0xFF7C6FE0), radius = r * 0.25f)
        drawCircle(Color.White, radius = r * 0.20f)
    }
}

@Composable
fun AppTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppLogo(22)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title)
    }
}
