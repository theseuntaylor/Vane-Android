package com.theseuntaylor.vane.feature.home.data.repository

import com.theseuntaylor.vane.core.remote.RemoteDataSource
import com.theseuntaylor.vane.feature.home.ui.WeatherForecastUiModel
import com.theseuntaylor.vane.feature.home.ui.toUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class VaneRepository @Inject constructor(
    private val networkDataSource: RemoteDataSource,
) {

    suspend fun getWeatherForecast(): Flow<WeatherForecastUiModel> = flow {
        try {
            val remotePhotos = networkDataSource.getWeatherForecast().toUiModel()
            emit(value = remotePhotos)
        } catch (e: Exception) {
            e.localizedMessage
            throw e
        }
    }
}