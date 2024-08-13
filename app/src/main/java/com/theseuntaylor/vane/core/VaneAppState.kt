package com.theseuntaylor.vane.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.currentBackStackEntryAsState

enum class TopLevelDestinations(
    val destinationRouteName: String,
) {
    HOME(
        destinationRouteName = "home"
    )
}

@Stable
class VaneAppState(private val navController: NavController) {
    private val topLevelDestinations: List<TopLevelDestinations> = TopLevelDestinations.entries

    private val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val shouldShowFAB: Boolean
        @Composable get() = currentDestination?.route in topLevelDestinations.map {
            it.destinationRouteName
        }

}

