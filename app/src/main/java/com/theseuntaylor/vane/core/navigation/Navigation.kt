package com.theseuntaylor.vane.core.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.theseuntaylor.vane.feature.detailsScreen.ui.DetailedWeatherForecastScreen
import com.theseuntaylor.vane.feature.favouriteLocations.ui.AddFavouriteLocationScreen
import com.theseuntaylor.vane.feature.home.ui.HomeScreen

const val homeScreenRoute = "homeScreen"
const val addLocationScreenRoute = "addLocation"
const val detailedForecastScreenRoute = "detailedForecastScreen/{locationId}"

@RequiresApi(Build.VERSION_CODES.O)
fun NavGraphBuilder.detailedForecastScreen(
    modifier: Modifier.Companion,
    navController: NavHostController
) {
    composable(
        route = detailedForecastScreenRoute
    ) {
        DetailedWeatherForecastScreen(
            modifier = modifier,
            navController = navController
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun NavGraphBuilder.addLocationScreen(navController: NavController) {
    composable(
        route = addLocationScreenRoute
    ) {
        AddFavouriteLocationScreen(navController = navController)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun NavGraphBuilder.homeScreen(snackbarHostState: SnackbarHostState, navController: NavController) {
    composable(
        route = homeScreenRoute
    ) {
        HomeScreen(
            snackBarHostState = snackbarHostState,
            navigateToDetailedForecast = { locationId ->
                navController.navigate(
                    Screen.DetailedForecastScreen.createRoute(
                        locationId = locationId
                    )
                )
            }
        )
    }
}
