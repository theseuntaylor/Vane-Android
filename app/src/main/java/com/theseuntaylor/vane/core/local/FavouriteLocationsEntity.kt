package com.theseuntaylor.vane.core.local

import android.location.Address
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "favouriteLocationsEntity"
)

data class FavouriteLocationsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var name: String,
    var longitude: Double,
    var latitude: Double,
    var isFavourite: Boolean = true,
)

fun Address.toEntity() = FavouriteLocationsEntity(
    name = featureName,
    latitude = latitude,
    longitude = longitude,
    isFavourite = true
)