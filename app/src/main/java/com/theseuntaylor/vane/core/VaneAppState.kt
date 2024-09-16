package com.theseuntaylor.vane.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.theseuntaylor.vane.core.navigation.Home

@Stable
class VaneAppState(private val navController: NavController) {

    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val shouldShowFAB: Boolean
        @Composable get() = currentDestination?.hasRoute<Home>() == true
}

