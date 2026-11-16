package com.theseuntaylor.vane.feature.settings.data

import android.content.Context
import androidx.core.content.edit
import com.theseuntaylor.vane.core.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The appearance the user picked in Settings, saved across restarts. Read synchronously at
 * startup so the first frame already has the right colours.
 */
@Singleton
class AppearanceSettings @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeMode.value = mode
    }

    private fun readThemeMode(): ThemeMode =
        preferences.getString(KEY_THEME_MODE, null)
            ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
            ?: ThemeMode.SYSTEM

    private companion object {
        const val PREFERENCES_NAME = "vane_settings"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
