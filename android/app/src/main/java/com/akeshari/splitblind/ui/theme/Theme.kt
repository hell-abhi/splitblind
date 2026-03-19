package com.akeshari.splitblind.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = GreenGrey80,
    onSecondary = GreenGrey30,
    background = Green10,
    onBackground = Green90,
    surface = Green10,
    onSurface = Green90,
    surfaceVariant = GreenGrey30,
    onSurfaceVariant = GreenGrey80
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Green99,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = GreenGrey50,
    onSecondary = Green99,
    background = Green99,
    onBackground = Green10,
    surface = Green99,
    onSurface = Green10,
    surfaceVariant = GreenGrey90,
    onSurfaceVariant = GreenGrey30
)

@Composable
fun SplitBlindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
