package com.theseuntaylor.vane.feature.home.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.feature.home.data.model.toUiModel
import com.theseuntaylor.vane.feature.home.data.repository.CurrentLocationRepository
import com.theseuntaylor.vane.feature.home.data.repository.CurrentLocationResult
import com.theseuntaylor.vane.feature.home.domain.GetWeatherForecastUseCase
import com.theseuntaylor.vane.feature.widget.data.WidgetWeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Initial)
    val uiState: StateFlow<HomeUiState> = _uiState
        .onStart { loadCurrentLocationForecast() }
        .stateIn(
            viewModelScope,
            initialValue = HomeUiState.Initial,
            started = SharingStarted.WhileSubscribed(5000)
        )

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
                    data = it.toUiModel().copy(currentLocation = currentLocation),
                    forecast = it,
                )
                // Widget caching must never break the home screen.
                runCatching { widgetWeatherRepository.onCurrentLocationForecast(it, currentLocation) }
                    .onFailure { e -> Log.e("HomeViewModel", "Couldn't update widget", e) }
            }
    }
}
