package com.theseuntaylor.vane.feature.home.data.repository

import com.theseuntaylor.vane.core.remote.RemoteDataSource
import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class WeatherForecastRepository @Inject constructor(
    private val networkDataSource: RemoteDataSource,
) {

    suspend fun getWeatherForecast(
        longitude: String,
        latitude: String,
    ): Flow<WeatherForecastResponse> = flow {
        try {
            val weatherForecastResponse = networkDataSource.getWeatherForecast(
                longitude = longitude.toString(),
                latitude = latitude.toString(),
            )
            emit(value = weatherForecastResponse)
        } catch (e: Exception) {
            e.localizedMessage
            throw e
        }
    }
}