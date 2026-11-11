package com.theseuntaylor.vane.core.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteLocationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavouriteLocations(newLocation: FavouriteLocationsEntity)

    @Query("SELECT * FROM favouriteLocationsEntity")
    suspend fun getFavouriteLocations(): List<FavouriteLocationsEntity>

    /** Emits the favourites now and again whenever the table changes. */
    @Query("SELECT * FROM favouriteLocationsEntity")
    fun observeFavouriteLocations(): Flow<List<FavouriteLocationsEntity>>

    @Query("SELECT * FROM favouriteLocationsEntity WHERE id = :id")
    suspend fun getSingleFavouriteLocation(id: Int): FavouriteLocationsEntity
}