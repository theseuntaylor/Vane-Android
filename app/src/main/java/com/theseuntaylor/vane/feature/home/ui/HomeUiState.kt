package com.theseuntaylor.vane.feature.home.ui

import androidx.annotation.StringRes
import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import com.theseuntaylor.vane.feature.home.data.model.WeatherForecastUiModel

sealed class HomeUiState {
    data object Initial : HomeUiState()
    data object Loading : HomeUiState()
    data class Error(@StringRes val errorMessage: Int) : HomeUiState()

    /** [data] fills the location card; [forecast] fills the hourly forecast and conditions below it. */
    data class Success(val data: WeatherForecastUiModel, val forecast: WeatherForecastResponse) : HomeUiState()
}
