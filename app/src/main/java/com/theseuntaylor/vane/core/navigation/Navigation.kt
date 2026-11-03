package com.theseuntaylor.vane.core.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.theseuntaylor.vane.feature.detailsScreen.ui.DetailedWeatherForecastScreen
import com.theseuntaylor.vane.feature.favouriteLocations.ui.AddFavouriteLocationScreen
import com.theseuntaylor.vane.feature.home.ui.HomeScreen

fun NavGraphBuilder.detailedForecastScreen(
    modifier: Modifier,
    snackbarHostState: SnackbarHostState,
    navController: NavHostController
) {
    composable<DetailedForecast> {
        val args = it.toRoute<DetailedForecast>()
        DetailedWeatherForecastScreen(
            modifier = modifier,
            navController = navController,
            snackbarHostState = snackbarHostState,
            args = args
        )
    }
}

fun NavGraphBuilder.addLocationScreen(navController: NavController) {
    composable<AddFavouriteLocation> {
        AddFavouriteLocationScreen(navController = navController)
    }
}

fun NavGraphBuilder.homeScreen(
    snackbarHostState: SnackbarHostState,
    navController: NavController
) {
    composable<Home> {
        HomeScreen(
            snackBarHostState = snackbarHostState,
            navigateToDetailedForecast = { favouriteLocation ->
                navController.navigate(
                    DetailedForecast(
                        location = favouriteLocation.location,
                        longitude = favouriteLocation.longitude,
                        latitude = favouriteLocation.latitude
                    )
                )
            }
        )
    }
}
