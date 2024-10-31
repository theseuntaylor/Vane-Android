package com.theseuntaylor.vane.feature.home.ui

import android.annotation.SuppressLint
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.theseuntaylor.vane.core.local.FavouriteLocationsEntity
import com.theseuntaylor.vane.feature.home.data.model.LatAndLong
import com.theseuntaylor.vane.feature.home.data.model.toUiModel
import com.theseuntaylor.vane.feature.home.domain.GetWeatherForecastUseCase
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
    private val geocoder: Geocoder,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
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
                _uiState.value =
                    HomeUiState.Error(it.message ?: "There was an error loading forecasts")
            }
            .collect {
                _uiState.value = HomeUiState.Success(
                    it.toUiModel().copy(currentLocation = currentLocation)
                )
            }
    }

    private fun getLocationFromCoordinates(long: Double, lat: Double): String {
        if (!Geocoder.isPresent()) {
            return "Service not available for your device."
        }

        val locationNameFromCoordinates = geocoder.getFromLocation(
            lat, long, 1
        )

        return locationNameFromCoordinates?.first()?.subAdminArea
            ?: locationNameFromCoordinates?.first()?.countryName
            ?: "Error getting your current Location :("
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: (Double, Double, String) -> Unit) {
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val long = location.longitude

                    val locationFromCoordinates = getLocationFromCoordinates(
                        long = long,
                        lat = lat
                    )

                    callback(
                        lat, long, locationFromCoordinates
                    )
                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
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
                            FavouritesUiState.Error("You do not have any saved locations currently.")
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
                    _favouriteUiState.value =
                        FavouritesUiState.Error(
                            it.message ?: "There was an error loading forecasts"
                        )
                }.map { response ->
                    response.toUiModel()
                }.collect { response ->
                    _favouriteUiState.value = FavouritesUiState.Success(
                        data = mutableListOf(response)
                    )
                }
            } else {
                weatherForecastUseCase.invokeFavouriteLocationsWeatherForecast(
                    longitude = longitudeString,
                    latitude = latitudeString,
                ).onStart {
                    _favouriteUiState.value = FavouritesUiState.Loading
                }.catch {
                    _favouriteUiState.value =
                        FavouritesUiState.Error(
                            it.message ?: "There was an error loading forecasts"
                        )
                }.map { response ->
                    response.map { it.toUiModel() }
                }.collect { response ->
                    _favouriteUiState.value = FavouritesUiState.Success(
                        data = response.toMutableList()
                    )
                }
            }
        }
    }
}
