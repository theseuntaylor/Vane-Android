package com.theseuntaylor.vane.feature.detailsScreen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class DetailedWeatherForecastViewModel @Inject constructor(
    private val weatherForecastUseCase: GetWeatherForecastUseCase,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<DetailedWeatherForecastUiState>(DetailedWeatherForecastUiState.Initial)
    val uiState: StateFlow<DetailedWeatherForecastUiState> = _uiState.asStateFlow()

    fun getWeatherForecastForLocation(
        longitude: Double,
        latitude: Double,
        forecastDays: Int
    ) = viewModelScope.launch {
        weatherForecastUseCase.invokeCurrentLocationWeatherForecast(
            longitude = longitude.toString(),
            latitude = latitude.toString(),
            forecastDays = forecastDays
        ).onStart {
            _uiState.value = DetailedWeatherForecastUiState.Loading
        }.catch {
            _uiState.value = DetailedWeatherForecastUiState.Error(
                errorMessage = it.message ?: "There was an error loading forecasts"
            )
        }.collect {
            _uiState.value = DetailedWeatherForecastUiState.Success(it)
        }
    }

}