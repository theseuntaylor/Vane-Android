package com.theseuntaylor.vane.feature.favouriteLocations.ui

import androidx.annotation.StringRes
import com.theseuntaylor.vane.core.local.FavouriteLocationsEntity
import com.theseuntaylor.vane.feature.home.data.model.WeatherForecastUiModel

sealed class FavouritesUiState {
    data object Initial : FavouritesUiState()
    data object Loading : FavouritesUiState()
    data object Empty : FavouritesUiState()
    data class Error(@StringRes val errorMessage: Int) : FavouritesUiState()

    /** Each saved place with its forecast, in the same order. */
    data class Success(val places: List<Pair<FavouriteLocationsEntity, WeatherForecastUiModel>>) : FavouritesUiState()
}
