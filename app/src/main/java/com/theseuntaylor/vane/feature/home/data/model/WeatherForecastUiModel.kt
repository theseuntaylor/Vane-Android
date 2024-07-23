package com.theseuntaylor.vane.feature.home.data.model

import com.theseuntaylor.vane.core.remote.model.Current
import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import com.theseuntaylor.vane.core.utils.WmoCodes

data class WeatherForecastUiModel(
    val location: Pair<Double, Double> = Pair(0.0, 0.0),
    val timeOfLocation: String = "",
    val currentTemperature: Double = 0.0,
    val highestAndLowestTemperature: Pair<Double, Double> = Pair(0.0, 0.0),
    var summary: String = "",
    var currentLocation: String = "",
    val current: Current = Current(),
)

fun WeatherForecastResponse.toUiModel() = WeatherForecastUiModel(
    location = Pair(latitude, longitude),
    timeOfLocation = current.time,
    currentTemperature = current.temperature_2m,
    highestAndLowestTemperature = Pair(
        hourly.temperature_2m.max(),
        hourly.temperature_2m.min()
    ),
    currentLocation = "",
    summary = WmoCodes.getByValue(current.weatherCode)?.info?.second
        ?: "Can\'t get the summary for your current location",
    current = current
)