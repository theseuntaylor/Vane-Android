package com.theseuntaylor.vane.core.navigation

import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
data class DetailedForecast(
    val location: String,
    val longitude: String,
    val latitude: String
)

@Serializable
object AddFavouriteLocation
