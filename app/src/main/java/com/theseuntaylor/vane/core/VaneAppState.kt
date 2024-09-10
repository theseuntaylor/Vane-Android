package com.theseuntaylor.vane.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.theseuntaylor.vane.core.navigation.Screen

enum class TopLevelDestinations(
    val destinationRouteName: String,
) {
    HOME(
        destinationRouteName = Screen.Home.route
    )
}

enum class ChildrenDestinations(
    val destinationRouteName: String,
) {
    ADD_LOCATION(
        destinationRouteName = Screen.AddFavouriteLocation.route
    )
}

@Stable
class VaneAppState(private val navController: NavController) {

    private val topLevelDestinations: List<TopLevelDestinations> = TopLevelDestinations.entries
    private val childrenDestinations: List<ChildrenDestinations> = ChildrenDestinations.entries

    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val shouldShowFAB: Boolean
        @Composable get() = currentDestination?.route in topLevelDestinations.map {
            it.destinationRouteName
        }

    val shouldShowActionBar: Boolean
        @Composable get() = currentDestination?.route in childrenDestinations.map {
            it.destinationRouteName
        }

}

