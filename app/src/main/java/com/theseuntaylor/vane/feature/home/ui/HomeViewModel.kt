package com.theseuntaylor.vane.feature.home.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.local.FavouriteLocationsEntity
import com.theseuntaylor.vane.feature.home.data.model.WeatherForecastUiModel
import com.theseuntaylor.vane.feature.home.data.model.toUiModel
import com.theseuntaylor.vane.feature.home.data.repository.CurrentLocationRepository
import com.theseuntaylor.vane.feature.home.data.repository.CurrentLocationResult
import com.theseuntaylor.vane.feature.home.domain.GetWeatherForecastUseCase
import com.theseuntaylor.vane.feature.widget.data.FavouriteWeather
import com.theseuntaylor.vane.feature.widget.data.WidgetWeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val weatherForecastUseCase: GetWeatherForecastUseCase,
    private val currentLocationRepository: CurrentLocationRepository,
    private val widgetWeatherRepository: WidgetWeatherRepository,
) : ViewModel() {

    init {
        getFavouriteLocations()
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Initial)
    val uiState: StateFlow<HomeUiState> = _uiState
        .onStart { loadCurrentLocationForecast() }
        .stateIn(
            viewModelScope,
            initialValue = HomeUiState.Initial,
            started = SharingStarted.WhileSubscribed(5000)
        )

    private val _favouriteUiState = MutableStateFlow<FavouritesUiState>(FavouritesUiState.Initial)
    val favouriteUiState: StateFlow<FavouritesUiState> = _favouriteUiState.asStateFlow()

    // we need both the list and the state??
    var favouriteLocations = MutableStateFlow(mutableListOf<FavouriteLocationsEntity>())

    /** Finds the device's location and loads its forecast. Also used by the "Try again" button. */
    fun loadCurrentLocationForecast() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            when (val location = currentLocationRepository.currentLocation()) {
                is CurrentLocationResult.LocationOff ->
                    _uiState.value = HomeUiState.Error(R.string.error_location_off)

                is CurrentLocationResult.NotFound ->
                    _uiState.value = HomeUiState.Error(R.string.error_current_location_not_found)

                is CurrentLocationResult.Found -> getWeatherForecastForCurrentLocation(
                    longitude = location.longitude,
                    latitude = location.latitude,
                    currentLocation = location.name,
                )
            }
        }
    }

    /** Reloads the saved locations and their forecasts. Also used by the "Try again" button. */
    fun loadFavourites() = getFavouriteLocations()

    private fun getWeatherForecastForCurrentLocation(
        longitude: Double,
        latitude: Double,
        currentLocation: String,
    ) = viewModelScope.launch {
        weatherForecastUseCase.invokeCurrentLocationWeatherForecast(
            longitude = longitude.toString(),
            latitude = latitude.toString(),
        )
            .onStart { _uiState.value = HomeUiState.Loading }
            .catch {
                Log.e("HomeViewModel", "Couldn't load the forecast", it)
                _uiState.value = HomeUiState.Error(R.string.error_loading_forecasts)
            }
            .collect {
                _uiState.value = HomeUiState.Success(
                    it.toUiModel().copy(currentLocation = currentLocation)
                )
                // Widget caching must never break the home screen.
                runCatching { widgetWeatherRepository.onCurrentLocationForecast(it, currentLocation) }
                    .onFailure { e -> Log.e("HomeViewModel", "Couldn't update widget", e) }
            }
    }

    private fun getFavouriteLocations() {
        viewModelScope.launch {
            weatherForecastUseCase.invokeGetFavouriteLocations()
                .collect {
                    if (it.isNotEmpty()) {

                        // todo: map each entity to the ui model (response + city name)

                        favouriteLocations.value = it.toMutableList()
                        getWeatherForecastForFavouriteLocations(it)
                    } else {
                        _favouriteUiState.value = FavouritesUiState.Empty
                    }
                }
        }
    }

    private fun getWeatherForecastForFavouriteLocations(favouriteLocationsEntities: List<FavouriteLocationsEntity>) {
        var longitudeString = ""
        var latitudeString = ""

        viewModelScope.launch {

            for (favouriteLocation in favouriteLocationsEntities) {
                longitudeString += "${favouriteLocation.longitude},"
                latitudeString += "${favouriteLocation.latitude},"
            }

            if (favouriteLocationsEntities.size == 1) {
                weatherForecastUseCase.invokeCurrentLocationWeatherForecast(
                    longitude = longitudeString,
                    latitude = latitudeString,
                    forecastDays = 7
                ).onStart {
                    _favouriteUiState.value = FavouritesUiState.Loading
                }.catch {
                    Log.e("HomeViewModel", "Couldn't load favourite forecasts", it)
                    _favouriteUiState.value =
                        FavouritesUiState.Error(R.string.error_loading_forecasts)
                }.map { response ->
                    response.toUiModel()
                }.collect { response ->
                    _favouriteUiState.value = FavouritesUiState.Success(
                        data = mutableListOf(response)
                    )
                    cacheFavouritesForWidget(favouriteLocationsEntities, listOf(response))
                }
            } else {
                weatherForecastUseCase.invokeFavouriteLocationsWeatherForecast(
                    longitude = longitudeString,
                    latitude = latitudeString,
                ).onStart {
                    _favouriteUiState.value = FavouritesUiState.Loading
                }.catch {
                    Log.e("HomeViewModel", "Couldn't load favourite forecasts", it)
                    _favouriteUiState.value =
                        FavouritesUiState.Error(R.string.error_loading_forecasts)
                }.map { response ->
                    response.map { it.toUiModel() }
                }.collect { response ->
                    _favouriteUiState.value = FavouritesUiState.Success(
                        data = response.toMutableList()
                    )
                    cacheFavouritesForWidget(favouriteLocationsEntities, response)
                }
            }
        }
    }

    private suspend fun cacheFavouritesForWidget(
        entities: List<FavouriteLocationsEntity>,
        forecasts: List<WeatherForecastUiModel>,
    ) {
        val favourites = entities.zip(forecasts) { entity, forecast ->
            FavouriteWeather(
                name = entity.name,
                temperature = forecast.currentTemperature,
                weatherCode = forecast.current.weatherCode,
                high = forecast.todayHighAndLow?.first,
                low = forecast.todayHighAndLow?.second,
            )
        }
        // Widget caching must never break the home screen.
        runCatching { widgetWeatherRepository.onFavouritesForecast(favourites) }
            .onFailure { e -> Log.e("HomeViewModel", "Couldn't update widget favourites", e) }
    }
}
