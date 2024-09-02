package com.theseuntaylor.vane.feature.detailsScreen.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.theseuntaylor.vane.core.components.HourlyWeatherForecast
import com.theseuntaylor.vane.core.components.WeatherDetailChip
import com.theseuntaylor.vane.core.remote.model.Hourly
import com.theseuntaylor.vane.feature.home.ui.HomeViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DetailedWeatherForecastScreen(
    modifier: Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavHostController
) {

    val longitude = navController.currentBackStackEntry?.arguments?.getString("longitude")
    val latitude = navController.currentBackStackEntry?.arguments?.getString("latitude")
    val locationName =
        navController.currentBackStackEntry?.arguments?.getString("locationName") ?: ""

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        item { Text(longitude.toString()) }
        //TODO remember to retrieve the name of the city
        item { Text("Today's Hourly Forecast") }

        item { HourlyWeatherForecast(Hourly()) }

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
                    "24",
                    "°C",
                    weatherDetailDescription = "A very sunny day that proves God is real"
                )
                WeatherDetailChip(
                    modifier = modifier
                        .weight(1f)
                        .fillMaxHeight(1f),
                    "Precipitation",
                    "24",
                    "°C",
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
                    "Relative Humidity",
                    "24",
                    "°C",
                    weatherDetailDescription = "A very sunny day that proves God is real"
                )
                WeatherDetailChip(
                    modifier = modifier
                        .weight(1f)
                        .fillMaxHeight(1f),
                    "Temperature",
                    "24",
                    "°C",
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
                    "24",
                    "°C",
                    weatherDetailDescription = "A very sunny day that proves God is real"
                )
                WeatherDetailChip(
                    modifier = modifier
                        .weight(1f)
                        .fillMaxHeight(1f),
                    "Surface Pressure",
                    "24",
                    "°C",
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
                    "24",
                    "°C",
                    weatherDetailDescription = "A very sunny day that proves God is real"
                )
                WeatherDetailChip(
                    modifier = modifier
                        .weight(1f)
                        .fillMaxHeight(1f),
                    "Wind Direction",
                    "24",
                    "°C",
                    weatherDetailDescription = "A very sunny day that proves God is real"
                )
            }
        }
    }

    // Box of column showing the forecast for the next 7 days [
    // what day it is - Highest and lowest temp ]

    LaunchedEffect(key1 = longitude, key2 = latitude) {
        if (longitude != null && latitude != null) {
            viewModel.getWeatherForecastForCurrentLocation(
                longitude.toDouble(),
                latitude.toDouble(),
                currentLocation = locationName
            )
            // make call to db, to return the longitude and latitude, which we can use to make calls to get detailed information
        } else {
            //show error for not being able to get location ID (which should not be possible btw):]
        }
    }

}