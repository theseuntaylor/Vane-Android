package com.theseuntaylor.vane.feature.home.ui

import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse

data class WeatherForecastUiModel(
    val location: Pair<Double, Double>,
    val timeOfLocation: String,
    val currentTemperature: Double,
    val highestAndLowestTemperature: Pair<Double, Double>,
    val summary: String,
)

fun WeatherForecastResponse.toUiModel() = WeatherForecastUiModel(
    location = Pair(latitude, longitude),
    timeOfLocation = current.time,
    currentTemperature = current.temperature_2m,
    highestAndLowestTemperature = Pair(
        hourly.temperature_2m.max(),
        hourly.temperature_2m.min()
    ),
    summary = "",
)