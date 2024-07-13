package com.theseuntaylor.vane.core.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "favouriteLocationsEntity"
)

data class FavouriteLocationsEntity(
    @PrimaryKey val id: Int,
    var name: String,
    var longitude: Double,
    var latitude: Double,
    var isFavourite: Boolean = true,
)