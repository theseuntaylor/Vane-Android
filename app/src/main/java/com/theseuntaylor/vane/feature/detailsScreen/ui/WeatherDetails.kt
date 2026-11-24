package com.theseuntaylor.vane.feature.detailsScreen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.components.HourlyWeatherForecast
import com.theseuntaylor.vane.core.components.WeatherDetailChip
import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import com.theseuntaylor.vane.core.utils.returnTime

/**
 * Today's hourly forecast and conditions for one place, as items of a [androidx.compose.foundation.lazy.LazyColumn].
 * Shared by the Home tab and the details screen of a favourite.
 */
fun LazyListScope.weatherDetailsItems(data: WeatherForecastResponse) {
    item { Text(stringResource(R.string.today_s_hourly_forecast), style = MaterialTheme.typography.titleMedium) }
    item { HourlyWeatherForecast(data.hourly) }
    item { Text(stringResource(R.string.today_s_weather_conditions), style = MaterialTheme.typography.titleMedium) }
    item {
        DetailRow(
            left = {
                WeatherDetailChip(
                    Modifier.fillMaxWidth(),
                    stringResource(R.string.uv_index),
                    data.daily.uv_index_max[0].toString(),
                    data.dailyUnits.uv_index_max,
                    weatherDetailDescription = stringResource(R.string.uv_index_description)
                )
            },
            right = {
                WeatherDetailChip(
                    Modifier.fillMaxWidth(),
                    stringResource(R.string.precipitation),
                    data.daily.precipitation_sum[0].toString(),
                    data.dailyUnits.precipitation_sum,
                    weatherDetailDescription = stringResource(R.string.precipitation_description)
                )
            },
        )
    }
    item {
        DetailRow(
            left = {
                WeatherDetailChip(
                    Modifier.fillMaxWidth(),
                    stringResource(R.string.sun_rises_at),
                    data.daily.sunrise[0].returnTime(),
                    "",
                    weatherDetailDescription = stringResource(R.string.sunrise_description)
                )
            },
            right = {
                WeatherDetailChip(
                    Modifier.fillMaxWidth(),
                    stringResource(R.string.sun_sets_at),
                    data.daily.sunset[0].returnTime(),
                    "",
                    weatherDetailDescription = stringResource(R.string.sunset_description)
                )
            },
        )
    }
    item {
        DetailRow(
            left = {
                WeatherDetailChip(
                    Modifier.fillMaxWidth(),
                    stringResource(R.string.pressure),
                    data.current.pressure_msl.toString(),
                    data.currentUnits.pressure_msl,
                    weatherDetailDescription = stringResource(R.string.pressure_description)
                )
            },
            right = {
                WeatherDetailChip(
                    Modifier.fillMaxWidth(),
                    stringResource(R.string.surface_pressure),
                    data.current.surfacePressure.toString(),
                    data.currentUnits.surfacePressure,
                    weatherDetailDescription = stringResource(R.string.surface_pressure_description)
                )
            },
        )
    }
    item {
        DetailRow(
            left = {
                WeatherDetailChip(
                    Modifier.fillMaxWidth(),
                    stringResource(R.string.wind_speed),
                    data.current.wind_speed_10m.toString(),
                    data.currentUnits.wind_speed_10m,
                    weatherDetailDescription = stringResource(R.string.wind_speed_description)
                )
            },
            right = {
                WeatherDetailChip(
                    Modifier.fillMaxWidth(),
                    stringResource(R.string.wind_direction),
                    data.current.wind_direction_10m.toString(),
                    data.currentUnits.wind_direction_10m,
                    weatherDetailDescription = stringResource(R.string.wind_direction_description)
                )
            },
        )
    }
}

/** Two detail chips side by side, each half the width. */
@Composable
private fun DetailRow(left: @Composable () -> Unit, right: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.weight(1f)) { left() }
        Box(Modifier.weight(1f)) { right() }
    }
}
