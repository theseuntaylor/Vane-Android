package com.theseuntaylor.vane.core.navigation

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
        navArguments = listOf(
            navArgument("locationName") {
                type = NavType.StringType
            },
            navArgument("longitude") {
                type = NavType.StringType
            },
            navArgument("latitude") {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(locationName: String, longitude: String, latitude: String): String {
            return "detailedForecastScreen/$locationName/$longitude/$latitude"
        }
    }

    data object AddFavouriteLocation : Screen(
        route = addLocationScreenRoute,
    )
}