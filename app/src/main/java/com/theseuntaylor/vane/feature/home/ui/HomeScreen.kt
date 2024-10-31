package com.theseuntaylor.vane.feature.home.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theseuntaylor.vane.core.components.CenteredLoader
import com.theseuntaylor.vane.core.components.CityCard
import com.theseuntaylor.vane.core.components.ShowErrorSnackBar
import com.theseuntaylor.vane.core.components.TopLoader
import com.theseuntaylor.vane.core.navigation.DetailedForecast

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    snackBarHostState: SnackbarHostState,
    navigateToDetailedForecast: (DetailedForecast) -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val favouritesUiState by viewModel.favouriteUiState.collectAsStateWithLifecycle()
    val favouriteLocations by viewModel.favouriteLocations.collectAsStateWithLifecycle()

    Column {
        Text(
            text = "Your Current Location", style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        )

        when (val state = uiState) {

            is HomeUiState.Initial, HomeUiState.Loading -> {
                TopLoader()
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
                CenteredLoader()
            }

            is FavouritesUiState.Success -> {
                val weatherForecasts = state.data
                LazyColumn {
                    itemsIndexed(weatherForecasts) { index, weatherForecast ->
                        CityCard(
                            name = favouriteLocations[index].name,
                            onCardItemClicked = {
                                navigateToDetailedForecast(
                                    DetailedForecast(
                                        latitude = weatherForecasts[index].location.first.toString(),
                                        longitude = weatherForecasts[index].location.second.toString(),
                                        location = favouriteLocations[index].name
                                    )
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