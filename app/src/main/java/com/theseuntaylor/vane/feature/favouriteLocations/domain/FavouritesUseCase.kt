package com.theseuntaylor.vane.feature.favouriteLocations.domain

import android.location.Address
import com.theseuntaylor.vane.core.di.AppConstants
import com.theseuntaylor.vane.feature.favouriteLocations.data.FavouriteLocationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class FavouritesUseCase @Inject constructor(
    private val repository: FavouriteLocationRepository,
    @Named(AppConstants.DEFAULT_DISPATCHER) private val defaultDispatcher: CoroutineDispatcher,
) {

    suspend fun invokeSaveNewFavouriteLocation(address: Address) {
        withContext(defaultDispatcher) {
            repository.addNewFavouriteLocation(address = address)
        }
    }
}