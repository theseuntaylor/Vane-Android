package com.theseuntaylor.vane.feature.detailsScreen.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
                        message = state.errorMessage, snackbarHostState = snackbarHostState
                    )
                }

                is DetailedWeatherForecastUiState.Success -> {

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        item { Text(stringResource(R.string.today_s_hourly_forecast)) }

                        item {
                            HourlyWeatherForecast(
                                state.data.hourly,
                            )
                        }

                        item { Text(stringResource(R.string.today_s_weather_conditions)) }

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
                                        weatherDetailDescription = "A measure of ultraviolet radiation from the sun." + " Higher values indicate a greater risk of skin damage from sun exposure (e.g., 0-2 is low risk, 8+ is very high risk)."
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
                                        weatherDetailDescription = "Any form of water, such as rain, snow, sleet, or hail, that falls from the sky. Usually measured in millimeters (mm)."
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
                                        weatherDetailDescription = "The local time when the sun first appears above the horizon, marking the start of daylight."
                                    )
                                }

                                Box {
                                    WeatherDetailChip(
                                        modifier = modifier.fillMaxSize(),
                                        stringResource(R.string.sun_sets_at),
                                        state.data.daily.sunset[0].returnTime(),
                                        "",
                                        weatherDetailDescription = "The local time when the sun drops below the horizon, marking the start of nighttime."
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
                                        weatherDetailDescription = "The weight of the atmosphere exerted at a given point. Normal atmospheric pressure at sea level is about 1013 hPa."
                                    )
                                }

                                Box {
                                    WeatherDetailChip(
                                        modifier = modifier.fillMaxSize(),
                                        stringResource(R.string.surface_pressure),
                                        state.data.current.surfacePressure.toString(),
                                        state.data.currentUnits.surfacePressure,
                                        weatherDetailDescription = "Atmospheric pressure measured specifically at ground level, which can differ from sea level pressure due to altitude."
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
                                        weatherDetailDescription = " The rate at which air moves, measured in kilometers per hour (km/h) or meters per second (m/s)."
                                    )
                                }

                                Box {
                                    WeatherDetailChip(
                                        modifier = modifier.fillMaxWidth(),
                                        stringResource(R.string.wind_direction),
                                        state.data.current.wind_direction_10m.toString(),
                                        state.data.currentUnits.wind_direction_10m,
                                        weatherDetailDescription = " The direction from which the wind is blowing, usually measured in degrees (0° = North, 90° = East, 180° = South, 270° = West)."
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