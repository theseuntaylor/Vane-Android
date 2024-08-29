package com.theseuntaylor.vane.core.navigation

import android.util.Log
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(
    val route: String,
    val navArguments: List<NamedNavArgument> = emptyList()
) {

    data object Home : Screen(
        route = homeScreenRoute
    )

    data object DetailedForecastScreen : Screen(
        route = detailedForecastScreenRoute,
        navArguments = listOf(navArgument("locationId") {
            type = NavType.IntType
        })
    ) {
        fun createRoute(locationId: Int): String {
            Log.e("Location ID", locationId.toString())
            return "detailedForecastScreen/${locationId}"
        }
    }

    data object AddFavouriteLocation : Screen(
        route = addLocationScreenRoute,
    )
}