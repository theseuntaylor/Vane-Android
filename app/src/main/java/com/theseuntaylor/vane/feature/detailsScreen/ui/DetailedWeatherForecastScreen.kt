package com.theseuntaylor.vane.feature.detailsScreen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.components.CenteredLoader
import com.theseuntaylor.vane.core.components.HourlyWeatherForecast
import com.theseuntaylor.vane.core.components.ShowErrorSnackBar
import com.theseuntaylor.vane.core.components.VaneAppBar
import com.theseuntaylor.vane.core.components.WeatherDetailChip
import com.theseuntaylor.vane.core.navigation.DetailedForecast
import com.theseuntaylor.vane.core.utils.returnTime

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

    Scaffold(topBar = {
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
            when (val state = viewModel.uiState.value) {
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

                        item { Text(stringResource(R.string.today_s_hourly_forecast), style = MaterialTheme.typography.titleMedium) }

                        item {
                            HourlyWeatherForecast(
                                state.data.hourly,
                            )
                        }

                        item { Text(stringResource(R.string.today_s_weather_conditions), style = MaterialTheme.typography.titleMedium) }

                        item {
                            Row(
                                modifier = modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = modifier.fillMaxWidth(0.5f)
                                ) {
                                    WeatherDetailChip(
                                        modifier = modifier.fillMaxSize(),
                                        stringResource(R.string.uv_index),
                                        state.data.daily.uv_index_max[0].toString(),
                                        state.data.dailyUnits.uv_index_max,
                                        weatherDetailDescription = stringResource(R.string.uv_index_description)
                                    )
                                }

                                Box(
                                    modifier = modifier
                                ) {
                                    WeatherDetailChip(
                                        modifier = modifier.fillMaxSize(),
                                        stringResource(R.string.precipitation),
                                        state.data.daily.precipitation_sum[0].toString(),
                                        state.data.dailyUnits.precipitation_sum,
                                        weatherDetailDescription = stringResource(R.string.precipitation_description)
                                    )
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = modifier.fillMaxWidth(0.5f)
                                ) {
                                    WeatherDetailChip(
                                        modifier = modifier.fillMaxSize(),
                                        stringResource(R.string.sun_rises_at),
                                        state.data.daily.sunrise[0].returnTime(),
                                        "",
                                        weatherDetailDescription = stringResource(R.string.sunrise_description)
                                    )
                                }

                                Box {
                                    WeatherDetailChip(
                                        modifier = modifier.fillMaxSize(),
                                        stringResource(R.string.sun_sets_at),
                                        state.data.daily.sunset[0].returnTime(),
                                        "",
                                        weatherDetailDescription = stringResource(R.string.sunset_description)
                                    )
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = modifier.fillMaxWidth(0.5f)
                                ) {
                                    WeatherDetailChip(
                                        modifier = modifier.fillMaxSize(),
                                        stringResource(R.string.pressure),
                                        state.data.current.pressure_msl.toString(),
                                        state.data.currentUnits.pressure_msl,
                                        weatherDetailDescription = stringResource(R.string.pressure_description)
                                    )
                                }

                                Box {
                                    WeatherDetailChip(
                                        modifier = modifier.fillMaxSize(),
                                        stringResource(R.string.surface_pressure),
                                        state.data.current.surfacePressure.toString(),
                                        state.data.currentUnits.surfacePressure,
                                        weatherDetailDescription = stringResource(R.string.surface_pressure_description)
                                    )
                                }
                            }
                        }


                        item {
                            Row(
                                modifier = modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = modifier.fillMaxWidth(0.5f)
                                ) {
                                    WeatherDetailChip(
                                        modifier = modifier.fillMaxWidth(),
                                        stringResource(R.string.wind_speed),
                                        state.data.current.wind_speed_10m.toString(),
                                        state.data.currentUnits.wind_speed_10m,
                                        weatherDetailDescription = stringResource(R.string.wind_speed_description)
                                    )
                                }

                                Box {
                                    WeatherDetailChip(
                                        modifier = modifier.fillMaxWidth(),
                                        stringResource(R.string.wind_direction),
                                        state.data.current.wind_direction_10m.toString(),
                                        state.data.currentUnits.wind_direction_10m,
                                        weatherDetailDescription = stringResource(R.string.wind_direction_description)
                                    )
                                }

                            }
                        }
                        // Box of column showing the forecast for the next 7 days [
                        // what day it is - Highest and lowest temp ]
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