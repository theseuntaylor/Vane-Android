package com.theseuntaylor.vane.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.theseuntaylor.vane.core.navigation.TopLevelDestination

@Stable
class VaneAppState(val navController: NavHostController) {

    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    /** The tab whose screen is showing, or null on a screen opened from a tab (details, search). */
    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() {
            val destination = currentDestination ?: return null
            return TopLevelDestination.entries.firstOrNull { destination.isRoute(it) }
        }

    /** The bar shows on the three tabs only; screens opened from them are full screen with a back arrow. */
    val shouldShowBottomBar: Boolean
        @Composable get() = currentTopLevelDestination != null

    fun isSelected(destination: NavDestination?, tab: TopLevelDestination): Boolean =
        destination?.hierarchy?.any { it.isRoute(tab) } == true

    /** Switches tab, keeping each tab's own state (scroll position, view model) for when it comes back. */
    fun navigateToTopLevelDestination(tab: TopLevelDestination) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

private fun NavDestination.isRoute(tab: TopLevelDestination): Boolean =
    hasRoute(tab.route::class)
