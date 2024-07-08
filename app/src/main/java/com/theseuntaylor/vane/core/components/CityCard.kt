package com.theseuntaylor.vane.core.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.theseuntaylor.vane.core.remote.model.Current
import com.theseuntaylor.vane.core.remote.model.Hourly
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CityCard(
    modifier: Modifier = Modifier,
    name: String = "Home City",
    hourly: Hourly = Hourly(),
    current: Current = Current(),
) {

    var expanded by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(8.dp),
        onClick = {
            expanded = !expanded
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    modifier = modifier
                )
                Text(
                    text = "${current.temperature_2m}°C",
                    modifier = modifier
                )
            }

            if (expanded) {
                Spacer(modifier = modifier.height(5.dp))
                // show additional details for the city.
                // 1. Forecast for the rest of the week
                // Day - Mon Tues Wed Thur Fri Sat Sun
                // Temp- 1   2    3   4    5   6   7
                Column {
                    Text(current.time)
                    Text("${current.interval}")
                    Text("${current.wind_speed_10m}")
                    LazyRow {
                        // map it to ui data, then, we can handle return time in the data layer,
                        // instead of the ui layer :)

                        items(hourly.time.size) { index ->
                            ForecastedWeatherChip(
                                hourlyTime = hourly.time[index].returnTime(),
                                hourlyTemperature = hourly.temperature_2m[index]
                            )
                        }
                    }

                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun String.returnTime(): String {
    val parsedDate = LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
    return parsedDate.format(DateTimeFormatter.ofPattern("HH:mm"))
}

@Composable
fun ForecastedWeatherChip(
    hourlyTime: String, hourlyTemperature: Double,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = hourlyTime)
        Text(text = hourlyTemperature.toString())
    }
}