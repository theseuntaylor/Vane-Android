package com.theseuntaylor.vane.feature.home.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theseuntaylor.vane.core.components.CityCard
import com.theseuntaylor.vane.core.components.Loader
import com.theseuntaylor.vane.core.components.ShowErrorSnackBar
import com.theseuntaylor.vane.core.utils.hasLocationPermission
import com.theseuntaylor.vane.feature.home.data.model.LatAndLong

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    snackBarHostState: SnackbarHostState,
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
    Column {
        Text(
            text = "Your Current Location", style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        )
        when (val state = uiState.collectAsState().value) {

            is HomeUiState.Initial -> {}

            is HomeUiState.Loading -> {
                Loader()
            }

            is HomeUiState.Success -> {
                val data = state.data

                CityCard(
                    name = data.currentLocation, uiModel = data
                )
            }

            // Show retry button.
            is HomeUiState.Error -> {
                ShowErrorSnackBar(
                    message = state.errorMessage, snackbarHostState = snackBarHostState
                )
            }
        }
    }

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

}