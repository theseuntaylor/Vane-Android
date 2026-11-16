package com.theseuntaylor.vane.feature.settings.ui

import androidx.lifecycle.ViewModel
import com.theseuntaylor.vane.feature.settings.data.AppearanceSettings
import com.theseuntaylor.vane.core.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appearanceSettings: AppearanceSettings,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = appearanceSettings.themeMode

    fun setThemeMode(mode: ThemeMode) = appearanceSettings.setThemeMode(mode)
}
