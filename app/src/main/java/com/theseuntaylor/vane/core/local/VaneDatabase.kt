package com.theseuntaylor.vane.core.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavouriteLocationsEntity::class],
    version = 1,
    // Schemas are written to app/schemas and committed, so future versions can be migrated and tested.
    exportSchema = true
)
abstract class VaneDatabase : RoomDatabase() {
    abstract fun favouriteLocationsDao(): FavouriteLocationsDao
}