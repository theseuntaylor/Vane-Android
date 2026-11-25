package com.theseuntaylor.vane.feature.favouriteLocations.domain

import android.location.Address
import com.theseuntaylor.vane.core.di.AppConstants
import com.theseuntaylor.vane.core.local.FavouriteLocationsEntity
import com.theseuntaylor.vane.feature.favouriteLocations.data.FavouriteLocationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class FavouritesUseCase @Inject constructor(
    private val repository: FavouriteLocationRepository,
    @param:Named(AppConstants.DEFAULT_DISPATCHER) private val defaultDispatcher: CoroutineDispatcher,
) {

    suspend fun invokeSaveNewFavouriteLocation(address: Address) {
        withContext(defaultDispatcher) {
            repository.addNewFavouriteLocation(address = address)
        }
    }

    suspend fun delete(place: FavouriteLocationsEntity) = withContext(defaultDispatcher) {
        repository.deleteFavouriteLocation(place.id)
    }

    suspend fun restore(place: FavouriteLocationsEntity) = withContext(defaultDispatcher) {
        repository.restoreFavouriteLocation(place)
    }

    /** Renames a place; a blank name is ignored. */
    suspend fun rename(place: FavouriteLocationsEntity, name: String) = withContext(defaultDispatcher) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && trimmed != place.name) repository.renameFavouriteLocation(place.id, trimmed)
    }

    /**
     * Moves [place] one step up ([by] = -1) or down ([by] = 1) within [places], the list as shown.
     * Does nothing at either end.
     */
    suspend fun move(places: List<FavouriteLocationsEntity>, place: FavouriteLocationsEntity, by: Int) =
        withContext(defaultDispatcher) {
            val ids = places.map { it.id }.toMutableList()
            val from = ids.indexOf(place.id)
            val to = from + by
            if (from < 0 || to !in ids.indices) return@withContext
            ids.add(to, ids.removeAt(from))
            repository.reorderFavouriteLocations(ids)
        }
}
