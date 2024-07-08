package com.theseuntaylor.vane.feature.home.ui

sealed class HomeUiState {
    data object Initial : HomeUiState()
    data object Loading : HomeUiState()
    data class Error(val errorMessage: String) : HomeUiState()
    data class Success(val data: WeatherForecastUiModel) : HomeUiState()
}