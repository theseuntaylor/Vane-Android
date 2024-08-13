package com.theseuntaylor.vane.feature.home.ui

import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
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
    private val geocoder: Geocoder,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Initial)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _listOfAddresses = MutableStateFlow<List<Address>>(emptyList())
    val listOfAddresses = _listOfAddresses.asStateFlow()

    fun getWeatherForecastForCurrentLocation(
        longitude: Double,
        latitude: Double,
        currentLocation: String,
    ) =
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
                    _uiState.value = HomeUiState.Success(
                        it.toUiModel().copy(currentLocation = currentLocation)
                    )
                }
        }

    // forecast for favourite locations
    // Check that there are favourite locations from the db
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun searchLocation(searchedAddress: String) {
        println("text entered is $searchedAddress")
        if (Geocoder.isPresent()) {
            geocoder.getFromLocationName(
                searchedAddress,
                5
            ) { p0 ->
                if (p0.isNotEmpty()) {
                    _listOfAddresses.value = p0
                }
            }
        }
    }

}