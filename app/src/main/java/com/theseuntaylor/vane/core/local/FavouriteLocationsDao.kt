package com.theseuntaylor.vane.core.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteLocationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavouriteLocations(newLocation: FavouriteLocationsEntity)

    /** Adds a place at the end of the list (or moves it there if it's already saved). */
    @Transaction
    suspend fun addAtEnd(newLocation: FavouriteLocationsEntity) {
        addFavouriteLocations(newLocation.copy(position = nextPosition()))
    }

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM favouriteLocationsEntity")
    suspend fun nextPosition(): Int

    @Query("SELECT * FROM favouriteLocationsEntity ORDER BY position, id")
    suspend fun getFavouriteLocations(): List<FavouriteLocationsEntity>

    /** Emits the favourites now and again whenever the table changes, in the user's order. */
    @Query("SELECT * FROM favouriteLocationsEntity ORDER BY position, id")
    fun observeFavouriteLocations(): Flow<List<FavouriteLocationsEntity>>

    @Query("SELECT * FROM favouriteLocationsEntity WHERE id = :id")
    suspend fun getSingleFavouriteLocation(id: Int): FavouriteLocationsEntity

    @Query("DELETE FROM favouriteLocationsEntity WHERE id = :id")
    suspend fun deleteFavouriteLocation(id: Int)

    @Query("UPDATE favouriteLocationsEntity SET name = :name WHERE id = :id")
    suspend fun renameFavouriteLocation(id: Int, name: String)

    @Query("UPDATE favouriteLocationsEntity SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Int, position: Int)

    /** Saves the list order: [ids] from first to last. */
    @Transaction
    suspend fun setOrder(ids: List<Int>) {
        ids.forEachIndexed { position, id -> setPosition(id, position) }
    }
}
