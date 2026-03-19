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
    primary = PastelPrimaryDark,
    onPrimary = SurfaceDark,
    primaryContainer = PastelPrimaryBgDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = PastelPrimarySoftDark,
    onSecondary = TextPrimaryDark,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark,
    error = Negative,
    onError = TextPrimaryDark,
    outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = PastelPrimary,
    onPrimary = CardLight,
    primaryContainer = PastelPrimaryContainer,
    onPrimaryContainer = TextPrimary,
    secondary = PastelPrimarySoft,
    onSecondary = CardLight,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = PastelPrimaryContainer,
    onSurfaceVariant = TextSecondary,
    error = Negative,
    onError = CardLight,
    outline = BorderLight
)

@Composable
fun SplitBlindTheme(
    content: @Composable () -> Unit
) {
    val darkTheme = when (ThemeManager.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
