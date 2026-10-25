package com.theseuntaylor.vane.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Vane's Material 3 theme. Colours and fonts come from `design/seed` (see `Color.kt` and
 * `res/font`); change the seed and run `cd design && npm run generate` to re-theme.
 */
@Composable
fun VaneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = vaneTypography(),
        content = content
    )
}
