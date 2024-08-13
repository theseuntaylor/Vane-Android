package com.theseuntaylor.vane.core.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavouriteLocationsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavouriteLocations(newLocation: FavouriteLocationsEntity)

    @Query("SELECT * FROM favouriteLocationsEntity")
    suspend fun getFavouriteLocations(): List<FavouriteLocationsEntity>

    @Query("SELECT * FROM favouriteLocationsEntity WHERE id = :id")
    suspend fun getSingleFavouriteLocation(id: Int): FavouriteLocationsEntity
}