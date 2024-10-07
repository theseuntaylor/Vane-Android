package com.theseuntaylor.vane.feature.home.data.repository

import com.theseuntaylor.vane.core.remote.RemoteDataSource
import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class WeatherForecastRepository @Inject constructor(
    private val networkDataSource: RemoteDataSource,
) {

    fun getWeatherForecast(
        longitude: String,
        latitude: String,
        forecastDays: Int,
    ): Flow<WeatherForecastResponse> = flow {
        try {
            val weatherForecastResponse = networkDataSource.getWeatherForecast(
                longitude = longitude,
                latitude = latitude,
                forecastDays = forecastDays
            )
            emit(value = weatherForecastResponse)
        } catch (e: Exception) {
            e.localizedMessage
            throw e
        }
    }

    fun getFavouriteLocationsWeatherForecast(
        longitude: String,
        latitude: String,
        forecastDays: Int,
    ): Flow<List<WeatherForecastResponse>> = flow {
        try {

            val res = mutableListOf<WeatherForecastResponse>()

            val weatherForecastResponse = networkDataSource.getFavouriteLocationsWeatherForecast(
                longitude = longitude,
                latitude = latitude,
                forecastDays = forecastDays
            )

            res.addAll(weatherForecastResponse)

            emit(value = res)

        } catch (e: Exception) {
            e.localizedMessage
            throw e
        }
    }
}