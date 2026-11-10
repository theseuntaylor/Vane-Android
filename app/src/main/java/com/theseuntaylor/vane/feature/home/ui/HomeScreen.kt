package com.theseuntaylor.vane.feature.home.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.components.CityCard
import com.theseuntaylor.vane.core.components.TopLoader
import com.theseuntaylor.vane.core.navigation.DetailedForecast

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    navigateToDetailedForecast: (DetailedForecast) -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val favouritesUiState by viewModel.favouriteUiState.collectAsStateWithLifecycle()
    val favouriteLocations by viewModel.favouriteLocations.collectAsStateWithLifecycle()

    Column {
        Text(
            text = stringResource(R.string.your_current_location),
            style = MaterialTheme.typography.titleLarge,
        )

        when (val state = uiState) {

            is HomeUiState.Initial, HomeUiState.Loading -> {
                TopLoader()
            }

            is HomeUiState.Success -> {
                val data = state.data

                CityCard(
                    name = data.currentLocation, uiModel = data, onCardItemClicked = {
                        navigateToDetailedForecast(
                            DetailedForecast(
                                latitude = data.location.first.toString(),
                                longitude = data.location.second.toString(),
                                location = data.currentLocation
                            )
                        )
                    }
                )
            }

            is HomeUiState.Error -> {
                SectionMessage(message = state.errorMessage, onRetry = viewModel::loadCurrentLocationForecast)
            }
        }

        Spacer(modifier = modifier.height(16.dp))

        Text(
            text = stringResource(R.string.favourite_locations),
            style = MaterialTheme.typography.titleLarge,
        )

        when (val state = favouritesUiState) {

            is FavouritesUiState.Initial, FavouritesUiState.Loading -> {
                TopLoader()
            }

            is FavouritesUiState.Empty -> {
                SectionMessage(message = R.string.favourites_empty)
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
                SectionMessage(message = state.errorMessage, onRetry = viewModel::loadFavourites)
            }
        }
    }
}

/** A message in place of a section's content, with a "Try again" button when [onRetry] is given. */
@Composable
private fun SectionMessage(@StringRes message: Int, onRetry: (() -> Unit)? = null) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.try_again))
            }
        }
    }
}
