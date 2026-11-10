package com.theseuntaylor.vane.feature.home.ui

import androidx.annotation.StringRes
import com.theseuntaylor.vane.feature.home.data.model.WeatherForecastUiModel

sealed class HomeUiState {
    data object Initial : HomeUiState()
    data object Loading : HomeUiState()
    data class Error(@StringRes val errorMessage: Int) : HomeUiState()
    data class Success(val data: WeatherForecastUiModel) : HomeUiState()
}

sealed class FavouritesUiState {
    data object Initial : FavouritesUiState()
    data object Loading : FavouritesUiState()
    data object Empty : FavouritesUiState()
    data class Error(@StringRes val errorMessage: Int) : FavouritesUiState()
    data class Success(val data: MutableList<WeatherForecastUiModel> = mutableListOf()) :
        FavouritesUiState()
}