package com.theseuntaylor.vane.core.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.theseuntaylor.vane.core.remote.model.Hourly
import com.theseuntaylor.vane.core.utils.returnTime

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HourlyWeatherForecast(hourly: Hourly) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(hourly.time.size) { index ->
            ForecastedWeatherChip(
                hourlyTime = hourly.time[index].returnTime(),
                hourlyTemperature = hourly.temperature_2m[index]
            )
        }
    }
}

@Composable
private fun ForecastedWeatherChip(
    hourlyTime: String, hourlyTemperature: Double,
) {
    Card {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = hourlyTime)
            Text(text = hourlyTemperature.toString())
        }
    }
}