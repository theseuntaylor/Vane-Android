package com.theseuntaylor.vane.feature.home.ui

import com.theseuntaylor.vane.feature.home.data.model.WeatherForecastUiModel

sealed class HomeUiState {
    data object Initial : HomeUiState()
    data object Loading : HomeUiState()
    data class Error(val errorMessage: String) : HomeUiState()
    data class Success(val data: WeatherForecastUiModel) : HomeUiState()
}

sealed class FavouritesUiState {
    data object Initial : FavouritesUiState()
    data object Loading : FavouritesUiState()
    data class Error(val errorMessage: String) : FavouritesUiState()
    data class Success(val data: MutableList<WeatherForecastUiModel> = mutableListOf()) :
        FavouritesUiState()
}