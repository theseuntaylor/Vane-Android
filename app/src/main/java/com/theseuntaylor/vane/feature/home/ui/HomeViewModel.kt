package com.theseuntaylor.vane.feature.home.ui

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
class HomeViewModel @Inject constructor(
    private val weatherForecastUseCase: GetWeatherForecastUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Initial)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        getWeatherForecast()
    }

    private fun getWeatherForecast() = viewModelScope.launch {
        weatherForecastUseCase.invokeWeatherForecast()
            .onStart { _uiState.value = HomeUiState.Loading }
            .catch {
                _uiState.value =
                    HomeUiState.Error(it.message ?: "There was an error loading forecasts")
            }
            .collect { it ->
                _uiState.value = HomeUiState.Success(it)
            }
    }
}