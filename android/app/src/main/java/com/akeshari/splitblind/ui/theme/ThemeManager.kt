package com.akeshari.splitblind.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("splitblind_prefs", Context.MODE_PRIVATE)
        themeMode = when (prefs.getString("theme", "system")) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    fun toggle(context: Context) {
        val isDark = when (themeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> {
                val nightMode = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
        themeMode = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
        context.getSharedPreferences("splitblind_prefs", Context.MODE_PRIVATE)
            .edit().putString("theme", if (isDark) "light" else "dark").apply()
    }
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }
