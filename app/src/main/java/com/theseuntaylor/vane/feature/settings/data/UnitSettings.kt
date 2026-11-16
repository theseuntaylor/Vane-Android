package com.theseuntaylor.vane.feature.settings.data

import android.content.Context
import androidx.core.content.edit
import com.theseuntaylor.vane.core.units.ClockFormat
import com.theseuntaylor.vane.core.units.PrecipitationUnit
import com.theseuntaylor.vane.core.units.TemperatureUnit
import com.theseuntaylor.vane.core.units.Units
import com.theseuntaylor.vane.core.units.WindSpeedUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The units the user picked in Settings, saved across restarts and read by the app and the widget. */
@Singleton
class UnitSettings @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _units = MutableStateFlow(read())
    val units: StateFlow<Units> = _units.asStateFlow()

    fun setUnits(units: Units) {
        preferences.edit {
            putString(KEY_TEMPERATURE, units.temperature.name)
            putString(KEY_WIND_SPEED, units.windSpeed.name)
            putString(KEY_PRECIPITATION, units.precipitation.name)
            putString(KEY_CLOCK, units.clock.name)
        }
        _units.value = units
    }

    private fun read() = Units(
        temperature = enumOrDefault(KEY_TEMPERATURE, TemperatureUnit.CELSIUS),
        windSpeed = enumOrDefault(KEY_WIND_SPEED, WindSpeedUnit.KILOMETRES_PER_HOUR),
        precipitation = enumOrDefault(KEY_PRECIPITATION, PrecipitationUnit.MILLIMETRES),
        clock = enumOrDefault(KEY_CLOCK, ClockFormat.SYSTEM),
    )

    // An unknown saved name (e.g. from a later version) falls back to the default.
    private inline fun <reified E : Enum<E>> enumOrDefault(key: String, default: E): E =
        preferences.getString(key, null)?.let { saved -> enumValues<E>().firstOrNull { it.name == saved } } ?: default

    private companion object {
        // Shared with AppearanceSettings: one file for everything on the Settings tab.
        const val PREFERENCES_NAME = "vane_settings"
        const val KEY_TEMPERATURE = "temperature_unit"
        const val KEY_WIND_SPEED = "wind_speed_unit"
        const val KEY_PRECIPITATION = "precipitation_unit"
        const val KEY_CLOCK = "clock_format"
    }
}
