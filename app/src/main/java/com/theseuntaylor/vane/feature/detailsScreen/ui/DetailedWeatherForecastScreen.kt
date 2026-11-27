package com.theseuntaylor.vane.feature.detailsScreen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.theseuntaylor.vane.core.backdrop.ShowWeatherBackdrop
import com.theseuntaylor.vane.core.backdrop.backdrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.theseuntaylor.vane.core.components.CenteredLoader
import com.theseuntaylor.vane.core.components.ShowErrorSnackBar
import com.theseuntaylor.vane.core.components.VaneAppBar
import com.theseuntaylor.vane.core.navigation.DetailedForecast

@Composable
fun DetailedWeatherForecastScreen(
    modifier: Modifier,
    viewModel: DetailedWeatherForecastViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    navController: NavHostController,
    args: DetailedForecast
) {

    val longitude = args.longitude
    val latitude = args.latitude
    val locationName = args.location
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // The place's sky and light, behind the screen.
    ShowWeatherBackdrop((uiState as? DetailedWeatherForecastUiState.Success)?.data?.backdrop())

    // Transparent, so the backdrop the app draws shows through.
    Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground, topBar = {
        VaneAppBar(title = locationName, navController = navController, navigateBack = {
            if (navController.previousBackStackEntry != null) {
                navController.navigateUp()
            }
        })
    }) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DetailedWeatherForecastUiState.Initial, DetailedWeatherForecastUiState.Loading -> {
                    CenteredLoader()
                }

                is DetailedWeatherForecastUiState.Error -> {
                    ShowErrorSnackBar(
                        message = stringResource(state.errorMessage), snackbarHostState = snackbarHostState
                    )
                }

                is DetailedWeatherForecastUiState.Success -> {

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        weatherDetailsItems(state.data)
                    }
                }
            }

            LaunchedEffect(key1 = longitude, key2 = latitude) {
                viewModel.getWeatherForecastForLocation(
                    longitude = longitude.toDouble(),
                    latitude = latitude.toDouble(),
                    forecastDays = 7
                )
            }

        }
    }
}