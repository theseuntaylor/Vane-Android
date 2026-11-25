package com.theseuntaylor.vane.feature.favouriteLocations.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.theseuntaylor.vane.core.local.FavouriteLocationsEntity
import com.theseuntaylor.vane.core.navigation.DetailedForecast
import kotlinx.coroutines.launch

/**
 * Favourites tab: saved places, each opening its full forecast, and the button to add more. Each
 * card's menu renames, moves or deletes the place; a delete can be undone from the snackbar.
 */
@Composable
fun FavouritesScreen(
    viewModel: FavouritesViewModel,
    onAddPlace: () -> Unit,
    onOpenPlace: (DetailedForecast) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // The id of the place being renamed, if any.
    var renamingId by rememberSaveable { mutableStateOf<Int?>(null) }

    fun delete(place: FavouriteLocationsEntity) {
        viewModel.delete(place)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.favourite_removed, place.name),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(place)
        }
    }

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
                    val index = state.places.indexOfFirst { it.first.id == place.id }
                    CityCard(
                        modifier = Modifier.animateItem(),
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
                        actions = {
                            FavouriteMenu(
                                placeName = place.name,
                                canMoveUp = index > 0,
                                canMoveDown = index < state.places.lastIndex,
                                onRename = { renamingId = place.id },
                                onMove = { by -> viewModel.move(place, by) },
                                onDelete = { delete(place) },
                            )
                        },
                    )
                }
            }
        }

        val renaming = (uiState as? FavouritesUiState.Success)?.places?.firstOrNull { it.first.id == renamingId }?.first
        if (renaming != null) {
            RenameFavouriteDialog(
                currentName = renaming.name,
                onRename = { name ->
                    viewModel.rename(renaming, name)
                    renamingId = null
                },
                onDismiss = { renamingId = null },
            )
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
