package com.theseuntaylor.vane.feature.favouriteLocations.data

import android.location.Address
import com.theseuntaylor.vane.core.local.FavouriteLocationsDao
import com.theseuntaylor.vane.core.local.FavouriteLocationsEntity
import com.theseuntaylor.vane.core.local.toEntity
import javax.inject.Inject

class FavouriteLocationRepository @Inject constructor(
    private val locationsDao: FavouriteLocationsDao,
) {
    /** Saves a place at the end of the list. */
    suspend fun addNewFavouriteLocation(address: Address) {
        locationsDao.addAtEnd(address.toEntity())
    }

    suspend fun getFavouriteLocations() = locationsDao.getFavouriteLocations()

    fun observeFavouriteLocations() = locationsDao.observeFavouriteLocations()

    suspend fun deleteFavouriteLocation(id: Int) = locationsDao.deleteFavouriteLocation(id)

    /** Puts back a place removed by [deleteFavouriteLocation], with its id, name and position. */
    suspend fun restoreFavouriteLocation(place: FavouriteLocationsEntity) = locationsDao.addFavouriteLocations(place)

    suspend fun renameFavouriteLocation(id: Int, name: String) = locationsDao.renameFavouriteLocation(id, name)

    /** Saves the list order: [ids] from first to last. */
    suspend fun reorderFavouriteLocations(ids: List<Int>) = locationsDao.setOrder(ids)
}
