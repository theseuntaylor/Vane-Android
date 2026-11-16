package com.theseuntaylor.vane.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.theseuntaylor.vane.R
import kotlinx.serialization.Serializable

/** Home tab: the weather where the user is. */
@Serializable
object Home

/** Favourites tab: saved places. */
@Serializable
object Favourites

/** Settings tab. */
@Serializable
object Settings

@Serializable
data class DetailedForecast(
    val location: String,
    val longitude: String,
    val latitude: String
)

@Serializable
object AddFavouriteLocation

@Serializable
object Licences

/** The bottom navigation bar's tabs, in order. */
enum class TopLevelDestination(
    val route: Any,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    HOME(Home, R.drawable.ic_home, R.string.tab_home),
    FAVOURITES(Favourites, R.drawable.ic_favourite, R.string.tab_favourites),
    SETTINGS(Settings, R.drawable.ic_settings, R.string.tab_settings),
}
