package com.theseuntaylor.vane.core.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavouriteLocationsEntity::class],
    version = 2,
    // Schemas are written to app/schemas and committed, so future versions can be migrated and tested.
    exportSchema = true
)
abstract class VaneDatabase : RoomDatabase() {
    abstract fun favouriteLocationsDao(): FavouriteLocationsDao
}

/** Version 2 adds the favourites' order. Existing places keep the order they were added in. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE favouriteLocationsEntity ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE favouriteLocationsEntity SET position = id")
    }
}
