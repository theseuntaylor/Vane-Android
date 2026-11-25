package com.theseuntaylor.vane.feature.favouriteLocations.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.utils.forecastErrorMessage
import com.theseuntaylor.vane.core.local.FavouriteLocationsEntity
import com.theseuntaylor.vane.feature.home.data.model.WeatherForecastUiModel
import com.theseuntaylor.vane.feature.home.data.model.toUiModel
import com.theseuntaylor.vane.feature.home.domain.GetWeatherForecastUseCase
import com.theseuntaylor.vane.feature.widget.data.FavouriteWeather
import com.theseuntaylor.vane.feature.widget.data.WidgetWeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The saved places and their forecasts, for the Favourites tab. It also refreshes the widget's
 * favourites, so MainActivity creates it at launch rather than when the tab first opens.
 */
@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val weatherForecastUseCase: GetWeatherForecastUseCase,
    private val widgetWeatherRepository: WidgetWeatherRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FavouritesUiState>(FavouritesUiState.Initial)
    val uiState: StateFlow<FavouritesUiState> = _uiState.asStateFlow()

    private var favouritesJob: Job? = null
    private var forecastsJob: Job? = null

    init {
        loadFavourites()
    }

    /** Reloads the saved places and their forecasts. Also used by the "Try again" button. */
    fun loadFavourites() {
        favouritesJob?.cancel()
        // Room emits again whenever a favourite is added, so the list stays current without a restart.
        favouritesJob = viewModelScope.launch {
            weatherForecastUseCase.invokeGetFavouriteLocations().collect { places ->
                // A newer list replaces any forecast request still running for the old one.
                forecastsJob?.cancel()
                if (places.isEmpty()) {
                    _uiState.value = FavouritesUiState.Empty
                } else {
                    forecastsJob = loadForecasts(places)
                }
            }
        }
    }

    private fun loadForecasts(places: List<FavouriteLocationsEntity>): Job = viewModelScope.launch {
        val longitudes = places.joinToString(separator = "") { "${it.longitude}," }
        val latitudes = places.joinToString(separator = "") { "${it.latitude}," }

        // Open-Meteo answers one place with an object and several with a list.
        val forecasts = if (places.size == 1) {
            weatherForecastUseCase.invokeCurrentLocationWeatherForecast(
                longitude = longitudes, latitude = latitudes, forecastDays = 7
            ).map { listOf(it.toUiModel()) }
        } else {
            weatherForecastUseCase.invokeFavouriteLocationsWeatherForecast(
                longitude = longitudes, latitude = latitudes,
            ).map { response -> response.map { it.toUiModel() } }
        }

        forecasts
            .onStart { _uiState.value = FavouritesUiState.Loading }
            .catch {
                Log.e(TAG, "Couldn't load favourite forecasts", it)
                _uiState.value = FavouritesUiState.Error(it.forecastErrorMessage())
            }
            .collect { response ->
                _uiState.value = FavouritesUiState.Success(places.zip(response))
                cacheFavouritesForWidget(places, response)
            }
    }

    private suspend fun cacheFavouritesForWidget(
        places: List<FavouriteLocationsEntity>,
        forecasts: List<WeatherForecastUiModel>,
    ) {
        val favourites = places.zip(forecasts) { place, forecast ->
            FavouriteWeather(
                name = place.name,
                temperature = forecast.currentTemperature,
                weatherCode = forecast.current.weatherCode,
                high = forecast.todayHighAndLow?.first,
                low = forecast.todayHighAndLow?.second,
            )
        }
        // Widget caching must never break the Favourites tab.
        runCatching { widgetWeatherRepository.onFavouritesForecast(favourites) }
            .onFailure { e -> Log.e(TAG, "Couldn't update widget favourites", e) }
    }
}

private const val TAG = "FavouritesViewModel"
