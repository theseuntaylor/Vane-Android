package com.theseuntaylor.vane.feature.favouriteLocations.data

import android.location.Address
import com.theseuntaylor.vane.core.local.FavouriteLocationsDao
import com.theseuntaylor.vane.core.local.toEntity
import javax.inject.Inject

class FavouriteLocationRepository @Inject constructor(
    private val locationsDao: FavouriteLocationsDao,
) {
    suspend fun addNewFavouriteLocation(address: Address) {
        val newLocation = address.toEntity()
        locationsDao.addFavouriteLocations(newLocation)
    }

    suspend fun getFavouriteLocations() = locationsDao.getFavouriteLocations()

    suspend fun deleteFavouriteLocation() {}

    suspend fun deleteAllFavouriteLocations() {}
}