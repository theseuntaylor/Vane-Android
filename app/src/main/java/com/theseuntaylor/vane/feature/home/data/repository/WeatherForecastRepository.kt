package com.theseuntaylor.vane.feature.home.data.repository

import com.theseuntaylor.vane.core.remote.RemoteDataSource
import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** Forecasts from Open-Meteo. Failures reach the collector, which decides what to show. */
class WeatherForecastRepository @Inject constructor(
    private val networkDataSource: RemoteDataSource,
) {

    fun getWeatherForecast(
        longitude: String,
        latitude: String,
        forecastDays: Int,
    ): Flow<WeatherForecastResponse> = flow {
        emit(
            networkDataSource.getWeatherForecast(
                longitude = longitude,
                latitude = latitude,
                forecastDays = forecastDays,
            )
        )
    }

    fun getFavouriteLocationsWeatherForecast(
        longitude: String,
        latitude: String,
        forecastDays: Int,
    ): Flow<List<WeatherForecastResponse>> = flow {
        emit(
            networkDataSource.getFavouriteLocationsWeatherForecast(
                longitude = longitude,
                latitude = latitude,
                forecastDays = forecastDays,
            )
        )
    }
}
