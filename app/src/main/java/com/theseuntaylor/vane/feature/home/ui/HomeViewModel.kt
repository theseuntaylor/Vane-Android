package com.theseuntaylor.vane.feature.home.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theseuntaylor.vane.feature.home.data.model.toUiModel
import com.theseuntaylor.vane.feature.home.domain.GetWeatherForecastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val weatherForecastUseCase: GetWeatherForecastUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Initial)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    var searchedLocation by mutableStateOf("")

    // getWeatherForecastForCurrentLocation()

    fun getWeatherForecastForCurrentLocation(longitude: Double, latitude: Double) =
        viewModelScope.launch {
            weatherForecastUseCase.invokeWeatherForecast(
                longitude = longitude,
                latitude = latitude,
            )
                .onStart { _uiState.value = HomeUiState.Loading }
                .catch {
                    _uiState.value =
                        HomeUiState.Error(it.message ?: "There was an error loading forecasts")
                }
                .collect {
                    _uiState.value = HomeUiState.Success(it.toUiModel())
                }
        }

    // search for input.
    fun searchForNewFavouriteLocation() {}

    // forcast for favourite locations
    // Check that there are favourite locations from the db
    fun getWeatherForecastForFavouriteLocations() {}

    // Search for location on google maps.
    fun searchLocation(newSearchedLocation: String) {
        searchedLocation = newSearchedLocation
        println("Searched location is $searchedLocation")
    }

}