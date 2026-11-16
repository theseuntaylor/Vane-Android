package com.theseuntaylor.vane.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theseuntaylor.vane.BuildConfig
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.feature.alerts.ui.RainAlertsSwitch
import com.theseuntaylor.vane.core.theme.ThemeMode
import com.theseuntaylor.vane.core.units.ClockFormat
import com.theseuntaylor.vane.core.units.PrecipitationUnit
import com.theseuntaylor.vane.core.units.TemperatureUnit
import com.theseuntaylor.vane.core.units.WindSpeedUnit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow

/**
 * Settings tab, in sections. A new setting goes in an existing [SettingsSection] or a new one.
 */
@Composable
fun SettingsScreen(
    onOpenLicences: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.tab_settings), style = MaterialTheme.typography.titleLarge)

        SettingsSection(R.string.settings_appearance) {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    ThemeModeOption(
                        label = mode.label,
                        selected = mode == themeMode,
                        onSelect = { viewModel.setThemeMode(mode) },
                    )
                }
            }
        }

        SettingsSection(R.string.settings_units) {
            UnitChoice(
                title = R.string.units_temperature,
                options = TemperatureUnit.entries,
                selected = units.temperature,
                label = { it.label },
                onSelect = { viewModel.setUnits(units.copy(temperature = it)) },
            )
            UnitChoice(
                title = R.string.units_wind_speed,
                options = WindSpeedUnit.entries,
                selected = units.windSpeed,
                label = { it.label },
                onSelect = { viewModel.setUnits(units.copy(windSpeed = it)) },
            )
            UnitChoice(
                title = R.string.units_precipitation,
                options = PrecipitationUnit.entries,
                selected = units.precipitation,
                label = { it.label },
                onSelect = { viewModel.setUnits(units.copy(precipitation = it)) },
            )
            UnitChoice(
                title = R.string.units_clock,
                options = ClockFormat.entries,
                selected = units.clock,
                label = { it.label },
                onSelect = { viewModel.setUnits(units.copy(clock = it)) },
            )
        }

        SettingsSection(R.string.settings_notifications) {
            RainAlertsSwitch(modifier = Modifier.padding(horizontal = 16.dp))
        }

        SettingsSection(R.string.settings_about) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onOpenLicences)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.open_source_licences),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
            }
            Text(
                text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

private val ThemeMode.label: Int
    @StringRes get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

private val TemperatureUnit.label: Int
    @StringRes get() = when (this) {
        TemperatureUnit.CELSIUS -> R.string.unit_celsius
        TemperatureUnit.FAHRENHEIT -> R.string.unit_fahrenheit
    }

private val WindSpeedUnit.label: Int
    @StringRes get() = when (this) {
        WindSpeedUnit.KILOMETRES_PER_HOUR -> R.string.unit_kmh
        WindSpeedUnit.MILES_PER_HOUR -> R.string.unit_mph
        WindSpeedUnit.METRES_PER_SECOND -> R.string.unit_ms
    }

private val PrecipitationUnit.label: Int
    @StringRes get() = when (this) {
        PrecipitationUnit.MILLIMETRES -> R.string.unit_mm
        PrecipitationUnit.INCHES -> R.string.unit_inches
    }

private val ClockFormat.label: Int
    @StringRes get() = when (this) {
        ClockFormat.SYSTEM -> R.string.clock_system
        ClockFormat.TWELVE_HOUR -> R.string.clock_12_hour
        ClockFormat.TWENTY_FOUR_HOUR -> R.string.clock_24_hour
    }

/** One unit setting: its name, then a Material 3 segmented button row with one segment per option. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> UnitChoice(
    @StringRes title: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(stringResource(label(option)), maxLines = 1)
                }
            }
        }
    }
}

/** A heading in the primary colour, then the section's settings on a card. */
@Composable
private fun SettingsSection(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun ThemeModeOption(@StringRes label: Int, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The row handles clicks, so each option is one target for TalkBack.
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
