package com.theseuntaylor.vane.feature.home.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theseuntaylor.vane.core.components.CityCard
import com.theseuntaylor.vane.core.components.Loader
import com.theseuntaylor.vane.core.components.ShowErrorSnackBar
import com.theseuntaylor.vane.core.components.VaneSearchTextField
import com.theseuntaylor.vane.feature.home.data.model.LatAndLong

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    snackBarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    var currentAddress = ""
    var location: LatAndLong

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted: Boolean ->
                if (isGranted) {
                    getCurrentLocation(context) { lat, long ->
                        location = LatAndLong(lat, long)
                    }
                }
            })

    val uiState = viewModel.uiState
    Column {

        VaneSearchTextField(
            value = viewModel.searchedLocation,
            onValueChanged = viewModel::searchLocation
        )
        Box(modifier = Modifier.height(14.dp))
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

                val cities = listOf("London")

                CityCard(
                    name = currentAddress, uiModel = data
                    // hourly = data.hourly, current = data.current
                )


            }

            // Show error message and show a retry button.
            is HomeUiState.Error -> {
                ShowErrorSnackBar(
                    message = state.errorMessage, snackbarHostState = snackBarHostState
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            getCurrentLocation(context) { lat, long ->
                location = LatAndLong(lat, long)

                viewModel.getWeatherForecastForCurrentLocation(
                    longitude = location.longitude,
                    latitude = location.latitude
                )
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

}