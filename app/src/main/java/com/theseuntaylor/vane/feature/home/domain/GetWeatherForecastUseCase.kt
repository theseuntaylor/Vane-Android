package com.theseuntaylor.vane.feature.home.domain

import com.theseuntaylor.vane.core.di.AppConstants
import com.theseuntaylor.vane.core.local.FavouriteLocationsEntity
import com.theseuntaylor.vane.feature.favouriteLocations.data.FavouriteLocationRepository
import com.theseuntaylor.vane.feature.home.data.repository.WeatherForecastRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class GetWeatherForecastUseCase @Inject constructor(
    private val weatherForecastRepository: WeatherForecastRepository,
    private val favouriteLocationRepository: FavouriteLocationRepository,
    @Named(AppConstants.DEFAULT_DISPATCHER) private val defaultDispatcher: CoroutineDispatcher,
) {

    suspend fun invokeCurrentLocationWeatherForecast(
        longitude: String, latitude: String,
        forecastDays: Int = 1
    ) =
        withContext(defaultDispatcher) {
            weatherForecastRepository.getWeatherForecast(
                longitude = longitude,
                latitude = latitude,
                forecastDays = forecastDays
            )
        }

    suspend fun invokeFavouriteLocationsWeatherForecast(
        longitude: String, latitude: String,
        forecastDays: Int = 7
    ) =
        withContext(defaultDispatcher) {
            weatherForecastRepository.getFavouriteLocationsWeatherForecast(
                longitude = longitude,
                latitude = latitude,
                forecastDays = forecastDays
            )
        }

    fun invokeGetFavouriteLocations(): Flow<List<FavouriteLocationsEntity>> =
        flow {
            val favouriteLocations = favouriteLocationRepository.getFavouriteLocations()
            emit(favouriteLocations)
        }

}