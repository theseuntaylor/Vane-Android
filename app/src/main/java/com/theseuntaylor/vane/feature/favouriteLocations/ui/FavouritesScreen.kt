package com.theseuntaylor.vane.feature.favouriteLocations.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.components.CityCard
import com.theseuntaylor.vane.core.components.SectionMessage
import com.theseuntaylor.vane.core.components.TopLoader
import com.theseuntaylor.vane.core.navigation.DetailedForecast

/** Favourites tab: saved places, each opening its full forecast, and the button to add more. */
@Composable
fun FavouritesScreen(
    viewModel: FavouritesViewModel,
    onAddPlace: () -> Unit,
    onOpenPlace: (DetailedForecast) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Room under the last card for the "Add place" button.
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.favourite_locations),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            when (val state = uiState) {
                is FavouritesUiState.Initial, FavouritesUiState.Loading -> item { TopLoader() }

                is FavouritesUiState.Empty -> item { SectionMessage(message = R.string.favourites_empty) }

                is FavouritesUiState.Error -> item {
                    SectionMessage(message = state.errorMessage, onRetry = viewModel::loadFavourites)
                }

                is FavouritesUiState.Success -> items(state.places, key = { (place, _) -> place.id }) { (place, forecast) ->
                    CityCard(
                        name = place.name,
                        uiModel = forecast,
                        onCardItemClicked = {
                            onOpenPlace(
                                DetailedForecast(
                                    location = place.name,
                                    latitude = forecast.location.first.toString(),
                                    longitude = forecast.location.second.toString(),
                                )
                            )
                        },
                    )
                }
            }
        }

        val addPlace = stringResource(R.string.add_place)
        ExtendedFloatingActionButton(
            onClick = onAddPlace,
            icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
            text = { Text(addPlace) },
            // The button's label text doesn't reach the accessibility tree on its own (uiautomator
            // showed an unlabelled button), so name the button for TalkBack explicitly.
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .semantics { contentDescription = addPlace },
        )
    }
}
