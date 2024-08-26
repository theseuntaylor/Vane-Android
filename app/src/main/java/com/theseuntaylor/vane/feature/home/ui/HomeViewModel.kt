package com.theseuntaylor.vane.feature.home.ui

import android.annotation.SuppressLint
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.theseuntaylor.vane.core.local.FavouriteLocationsEntity
import com.theseuntaylor.vane.feature.home.data.model.toUiModel
import com.theseuntaylor.vane.feature.home.domain.GetWeatherForecastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val weatherForecastUseCase: GetWeatherForecastUseCase,
    private val geocoder: Geocoder,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Initial)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _favouriteUiState = MutableStateFlow<FavouritesUiState>(FavouritesUiState.Initial)
    val favouriteUiState: StateFlow<FavouritesUiState> = _favouriteUiState.asStateFlow()

    var favouriteLocations = MutableStateFlow(emptyList<FavouriteLocationsEntity>())

    private var longitudeString = MutableStateFlow("")
    private var latitudeString = MutableStateFlow("")

    fun getWeatherForecastForCurrentLocation(
        longitude: Double,
        latitude: Double,
        currentLocation: String,
    ) =
        viewModelScope.launch {
            weatherForecastUseCase.invokeCurrentLocationWeatherForecast(
                longitude = longitude,
                latitude = latitude,
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

    fun getWeatherForecastForFavouriteLocations() =
        viewModelScope.launch {

            for (favouriteLocation in favouriteLocations.value) {
                longitudeString.value += "${favouriteLocation.longitude},"
                latitudeString.value += "${favouriteLocation.latitude},"
            }

            weatherForecastUseCase.invokeFavouriteLocationsWeatherForecast(
                longitude = longitudeString.value,
                latitude = latitudeString.value,
            )

                .onStart {
                    _favouriteUiState.value = FavouritesUiState.Loading
                }
                .catch {
                    _favouriteUiState.value =
                        FavouritesUiState.Error(
                            it.message ?: "There was an error loading forecasts"
                        )
                }
                .map { response ->
                    response.map { it.toUiModel() }
                }
                .collect { response ->
                    _favouriteUiState.value = FavouritesUiState.Success(response)
                }
        }

    private fun getLocationFromCoordinates(long: Double, lat: Double): String {

        if (!Geocoder.isPresent()) {
            return "Service not available for your device."
        }
        return geocoder.getFromLocation(
            lat, long,
            1
        )?.first()?.subAdminArea ?: "Error getting your current Location :("
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: (Double, Double, String) -> Unit) {
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val long = location.longitude

                    val locationFromCoordinates =
                        getLocationFromCoordinates(
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

    fun getFavouriteLocations() {
        viewModelScope.launch {
            weatherForecastUseCase.invokeGetFavouriteLocations()
                .onStart {
                    Log.e("TAG", "Get favourite location stated!!!!")
                }
                .collect {
                    if (it.isNotEmpty()) {
                        favouriteLocations.value = it
                    }
                }
        }
    }
}