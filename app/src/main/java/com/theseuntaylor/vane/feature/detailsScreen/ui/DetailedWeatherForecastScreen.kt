package com.theseuntaylor.vane.feature.detailsScreen.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.theseuntaylor.vane.core.components.CenteredLoader
import com.theseuntaylor.vane.core.components.HourlyWeatherForecast
import com.theseuntaylor.vane.core.components.ShowErrorSnackBar
import com.theseuntaylor.vane.core.components.VaneAppBar
import com.theseuntaylor.vane.core.components.WeatherDetailChip
import com.theseuntaylor.vane.core.navigation.DetailedForecast
import com.theseuntaylor.vane.core.utils.returnTime

@RequiresApi(Build.VERSION_CODES.O)
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

    Scaffold(
        topBar = {
            VaneAppBar(
                title = locationName,
                navController = navController,
                navigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.navigateUp()
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = viewModel.uiState.value) {
                is DetailedWeatherForecastUiState.Initial, DetailedWeatherForecastUiState.Loading -> {
                    CenteredLoader()
                }

                is DetailedWeatherForecastUiState.Error -> {
                    ShowErrorSnackBar(
                        message = state.errorMessage, snackbarHostState = snackbarHostState
                    )
                }

                is DetailedWeatherForecastUiState.Success -> {

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        item { Text("Today's Hourly Forecast") }

                        item {
                            HourlyWeatherForecast(
                                state.data.hourly,
                            )
                        }

                        item { Text("Today's Weather Conditions") }

                        item {
                            Row(
                                modifier = modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                WeatherDetailChip(
                                    modifier = modifier
                                        .weight(1f)
                                        .fillMaxHeight(1f),
                                    "UV Index",
                                    state.data.daily.uv_index_max[0].toString(),
                                    state.data.dailyUnits.uv_index_max,
                                    weatherDetailDescription = "A very sunny day that proves God is real"
                                )
                                WeatherDetailChip(
                                    modifier = modifier
                                        .weight(1f)
                                        .fillMaxHeight(1f),
                                    "Precipitation",
                                    state.data.daily.precipitation_sum[0].toString(),
                                    state.data.dailyUnits.precipitation_sum,
                                    weatherDetailDescription = "A very sunny day that proves God is real"
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
/*                        WeatherDetailChip(
                            modifier = modifier
                                .weight(1f)
                                .fillMaxHeight(1f),
                            "Relative Humidity",
                            state.data.daily.precipitation_sum[0].toString(),
                            state.data.dailyUnits.precipitation_sum,
                            weatherDetailDescription = "A very sunny day that proves God is real"
                        )*/
                                WeatherDetailChip(
                                    modifier = modifier
                                        .weight(1f)
                                        .fillMaxHeight(1f),
                                    "Sun Rises At",
                                    state.data.daily.sunrise[0].returnTime(),
                                    "",
                                    weatherDetailDescription = "A very sunny day that proves God is real"
                                )
                                WeatherDetailChip(
                                    modifier = modifier
                                        .weight(1f)
                                        .fillMaxHeight(1f),
                                    "Sun Sets At",
                                    state.data.daily.sunset[0].returnTime(),
                                    "",
                                    weatherDetailDescription = "A very sunny day that proves God is real"
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                WeatherDetailChip(
                                    modifier = modifier
                                        .weight(1f)
                                        .fillMaxHeight(1f),
                                    "Pressure",
                                    state.data.current.pressure_msl.toString(),
                                    state.data.currentUnits.pressure_msl,
                                    weatherDetailDescription = "A very sunny day that proves God is real"
                                )
                                WeatherDetailChip(
                                    modifier = modifier
                                        .weight(1f)
                                        .fillMaxHeight(1f),
                                    "Surface Pressure",
                                    state.data.current.surfacePressure.toString(),
                                    state.data.currentUnits.surfacePressure,
                                    weatherDetailDescription = "A very sunny day that proves God is real"
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                WeatherDetailChip(
                                    modifier = modifier
                                        .weight(1f)
                                        .fillMaxHeight(1f),
                                    "Wind Speed",
                                    state.data.current.wind_speed_10m.toString(),
                                    state.data.currentUnits.wind_speed_10m,
                                    weatherDetailDescription = "A very sunny day that proves God is real"
                                )
                                WeatherDetailChip(
                                    modifier = modifier
                                        .weight(1f)
                                        .fillMaxHeight(1f),
                                    "Wind Direction",
                                    state.data.current.wind_direction_10m.toString(),
                                    state.data.currentUnits.wind_direction_10m,
                                    weatherDetailDescription = "A very sunny day that proves God is real"
                                )
                            }
                        }
                    }
                    // Box of column showing the forecast for the next 7 days [
                    // what day it is - Highest and lowest temp ]
                }
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