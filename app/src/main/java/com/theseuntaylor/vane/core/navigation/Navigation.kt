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
import com.theseuntaylor.vane.feature.favouriteLocations.ui.FavouritesScreen
import com.theseuntaylor.vane.feature.favouriteLocations.ui.FavouritesViewModel
import com.theseuntaylor.vane.feature.home.ui.HomeScreen
import com.theseuntaylor.vane.feature.settings.ui.LicencesScreen
import com.theseuntaylor.vane.feature.settings.ui.SettingsScreen

fun NavGraphBuilder.homeScreen() {
    composable<Home> {
        HomeScreen()
    }
}

fun NavGraphBuilder.favouritesScreen(
    navController: NavController,
    favouritesViewModel: FavouritesViewModel,
    snackbarHostState: SnackbarHostState,
) {
    composable<Favourites> {
        FavouritesScreen(
            viewModel = favouritesViewModel,
            snackbarHostState = snackbarHostState,
            onAddPlace = { navController.navigate(AddFavouriteLocation) },
            onOpenPlace = { navController.navigate(it) },
        )
    }
}

fun NavGraphBuilder.settingsScreen(navController: NavController) {
    composable<Settings> {
        SettingsScreen(onOpenLicences = { navController.navigate(Licences) })
    }
}

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

fun NavGraphBuilder.licencesScreen(navController: NavController) {
    composable<Licences> {
        LicencesScreen(navController = navController)
    }
}
