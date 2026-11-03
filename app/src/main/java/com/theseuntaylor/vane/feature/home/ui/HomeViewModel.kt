package com.theseuntaylor.vane.feature.home.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.local.FavouriteLocationsEntity
import com.theseuntaylor.vane.feature.home.data.model.LatAndLong
import com.theseuntaylor.vane.feature.home.data.model.WeatherForecastUiModel
import com.theseuntaylor.vane.feature.home.data.model.toUiModel
import com.theseuntaylor.vane.feature.home.domain.GetWeatherForecastUseCase
import com.theseuntaylor.vane.feature.widget.data.FavouriteWeather
import com.theseuntaylor.vane.feature.widget.data.WidgetWeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val weatherForecastUseCase: GetWeatherForecastUseCase,
    private val geocoder: Geocoder,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val widgetWeatherRepository: WidgetWeatherRepository,
) : ViewModel() {

    init {
        getFavouriteLocations()
    }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Initial)
    val uiState: StateFlow<HomeUiState> = _uiState
        .onStart {
            getCurrentLocation { lat, long, currentLocation ->
                val location = LatAndLong(lat, long)

                getWeatherForecastForCurrentLocation(
                    longitude = location.longitude,
                    latitude = location.latitude,
                    currentLocation = currentLocation
                )
            }
        }.stateIn(
            viewModelScope,
            initialValue = HomeUiState.Initial,
            started = SharingStarted.WhileSubscribed(5000)
        )

    private val _favouriteUiState = MutableStateFlow<FavouritesUiState>(FavouritesUiState.Initial)
    val favouriteUiState: StateFlow<FavouritesUiState> = _favouriteUiState.asStateFlow()

    // we need both the list and the state??
    var favouriteLocations = MutableStateFlow(mutableListOf<FavouriteLocationsEntity>())

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

    private fun getLocationFromCoordinates(long: Double, lat: Double): String {
        if (!Geocoder.isPresent()) {
            return context.getString(R.string.geocoder_unavailable)
        }

        val locationNameFromCoordinates = geocoder.getFromLocation(
            lat, long, 1
        )

        return locationNameFromCoordinates?.first()?.subAdminArea
            ?: locationNameFromCoordinates?.first()?.countryName
            ?: context.getString(R.string.location_name_unavailable)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: (Double, Double, String) -> Unit) {
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationFound(location, callback)
                } else {
                    // No cached fix (e.g. right after a reboot): ask for a fresh one.
                    requestFreshLocation(callback)
                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(callback: (Double, Double, String) -> Unit) {
        fusedLocationProviderClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationFound(location, callback)
                } else {
                    _uiState.value = HomeUiState.Error(R.string.error_current_location_not_found)
                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
    }

    private fun onLocationFound(location: Location, callback: (Double, Double, String) -> Unit) {
        val locationFromCoordinates = getLocationFromCoordinates(
            long = location.longitude,
            lat = location.latitude
        )
        callback(location.latitude, location.longitude, locationFromCoordinates)
    }

    private fun getFavouriteLocations() {
        viewModelScope.launch {
            weatherForecastUseCase.invokeGetFavouriteLocations()
                .onStart {
                    // favouriteLocations.value.clear()
                    Log.e("TAG", "Get favourite location started!!!!")
                }
                .collect {
                    if (it.isNotEmpty()) {

                        // todo: map each entity to the ui model (response + city name)

                        favouriteLocations.value = it.toMutableList()
                        getWeatherForecastForFavouriteLocations(it)
                    } else {
                        _favouriteUiState.value =
                            FavouritesUiState.Error(R.string.error_no_saved_locations)
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
