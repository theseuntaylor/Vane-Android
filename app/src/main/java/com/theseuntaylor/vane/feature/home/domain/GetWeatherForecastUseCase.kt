package com.theseuntaylor.vane.feature.home.domain

import com.theseuntaylor.vane.core.di.AppConstants
import com.theseuntaylor.vane.feature.home.data.repository.VaneRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class GetWeatherForecastUseCase @Inject constructor(
    private val vaneRepository: VaneRepository,
    @Named(AppConstants.DEFAULT_DISPATCHER) private val defaultDispatcher: CoroutineDispatcher,
) {

    suspend fun invokeWeatherForecast() =
        withContext(defaultDispatcher) { vaneRepository.getWeatherForecast() }

}