package com.theseuntaylor.vane.feature.detailsScreen.ui

import androidx.annotation.StringRes
import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse

sealed class DetailedWeatherForecastUiState {
    data object Initial : DetailedWeatherForecastUiState()
    data object Loading : DetailedWeatherForecastUiState()
    data class Error(@StringRes val errorMessage: Int) : DetailedWeatherForecastUiState()
    data class Success(val data: WeatherForecastResponse) : DetailedWeatherForecastUiState()
}