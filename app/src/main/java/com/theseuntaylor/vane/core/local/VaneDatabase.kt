package com.theseuntaylor.vane.core.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavouriteLocationsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VaneDatabase : RoomDatabase() {
    abstract fun favouriteLocationsDao(): FavouriteLocationsDao
}