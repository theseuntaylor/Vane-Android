package com.theseuntaylor.vane.feature.detailsScreen.ui

import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse

sealed class DetailedWeatherForecastUiState {
    data object Initial : DetailedWeatherForecastUiState()
    data object Loading : DetailedWeatherForecastUiState()
    data class Error(val errorMessage: String) : DetailedWeatherForecastUiState()
    data class Success(val data: WeatherForecastResponse) : DetailedWeatherForecastUiState()
}