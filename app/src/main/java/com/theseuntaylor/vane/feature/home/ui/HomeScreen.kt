package com.theseuntaylor.vane.feature.home.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theseuntaylor.vane.core.components.CityCard
import com.theseuntaylor.vane.core.components.Loader
import com.theseuntaylor.vane.core.components.ShowErrorSnackBar
import com.theseuntaylor.vane.core.utils.hasLocationPermission
import com.theseuntaylor.vane.feature.home.data.model.LatAndLong

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    snackBarHostState: SnackbarHostState,
    navigateToDetailedForecast: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var location: LatAndLong

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted: Boolean ->
                if (isGranted) {
                    viewModel.getCurrentLocation { lat, long, currentLocation ->
                        location = LatAndLong(lat, long)

                        viewModel.getWeatherForecastForCurrentLocation(
                            longitude = location.longitude,
                            latitude = location.latitude,
                            currentLocation = currentLocation
                        )
                    }
                }
            })

    val uiState = viewModel.uiState

    val favouritesUiState by viewModel.favouriteUiState.collectAsStateWithLifecycle()
    val favouriteLocations by viewModel.favouriteLocations.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            viewModel.getCurrentLocation { lat, long, currentLocation ->
                location = LatAndLong(lat, long)

                viewModel.getWeatherForecastForCurrentLocation(
                    longitude = location.longitude,
                    latitude = location.latitude,
                    currentLocation = currentLocation
                )
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(key1 = favouriteLocations, key2 = Unit) {
        if (favouriteLocations.isEmpty()) {
            viewModel.getFavouriteLocations()
        }
    }

    Column {

        Text(
            text = "Your Current Location", style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        )

        when (val state = uiState.collectAsState().value) {

            is HomeUiState.Initial, HomeUiState.Loading -> {
                Loader()
            }

            is HomeUiState.Success -> {
                val data = state.data

                CityCard(
                    name = data.currentLocation, uiModel = data
                )

                Spacer(modifier = modifier.height(10.dp))
            }

            is HomeUiState.Error -> {
                ShowErrorSnackBar(
                    message = state.errorMessage, snackbarHostState = snackBarHostState
                )
            }
        }

        if (favouriteLocations.isNotEmpty()) Text(
            text = "Favourite Locations", style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        )

        when (val state = favouritesUiState) {

            is FavouritesUiState.Initial, FavouritesUiState.Loading -> {
                Loader()
            }

            is FavouritesUiState.Success -> {
                val weatherForecasts = state.data
                LazyColumn {
                    itemsIndexed(weatherForecasts) { index, weatherForecast ->
                        CityCard(
                            name = favouriteLocations[index].name,
                            // navigate to details screen and pass the data
                            onCardItemClicked = {
                                navigateToDetailedForecast(
                                    weatherForecasts[index].location.first.toString(),
                                    weatherForecasts[index].location.second.toString(),
                                    favouriteLocations[index].name
                                )
                            },
                            uiModel = weatherForecast
                        )
                    }
                }
            }

            is FavouritesUiState.Error -> {
                ShowErrorSnackBar(
                    message = state.errorMessage, snackbarHostState = snackBarHostState
                )
            }

        }

    }
}