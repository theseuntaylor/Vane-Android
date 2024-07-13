package com.theseuntaylor.vane.feature.home.data.repository

import com.theseuntaylor.vane.core.remote.RemoteDataSource
import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class VaneRepository @Inject constructor(
    private val networkDataSource: RemoteDataSource,
) {

    suspend fun getWeatherForecast(
        longitude: Double,
        latitude: Double,
    ): Flow<WeatherForecastResponse> = flow {
        try {
            val remotePhotos = networkDataSource.getWeatherForecast(
                longitude = longitude,
                latitude = latitude,
            )
            emit(value = remotePhotos)
        } catch (e: Exception) {
            e.localizedMessage
            throw e
        }
    }
}