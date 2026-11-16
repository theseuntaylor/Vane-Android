package com.theseuntaylor.vane.feature.settings.ui

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theseuntaylor.vane.core.theme.ThemeMode
import com.theseuntaylor.vane.core.units.Units
import com.theseuntaylor.vane.feature.settings.data.AppearanceSettings
import com.theseuntaylor.vane.feature.settings.data.UnitSettings
import com.theseuntaylor.vane.feature.widget.ui.VaneWeatherWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appearanceSettings: AppearanceSettings,
    private val unitSettings: UnitSettings,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = appearanceSettings.themeMode

    val units: StateFlow<Units> = unitSettings.units

    fun setThemeMode(mode: ThemeMode) = appearanceSettings.setThemeMode(mode)

    fun setUnits(units: Units) {
        unitSettings.setUnits(units)
        // A widget that isn't being redrawn keeps its last picture, so redraw it in the new units.
        viewModelScope.launch {
            runCatching { VaneWeatherWidget().updateAll(context) }
                .onFailure { Log.e("SettingsViewModel", "Couldn't redraw the widget", it) }
        }
    }
}
