package com.theseuntaylor.vane.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.components.CityCard
import com.theseuntaylor.vane.core.components.SectionMessage
import com.theseuntaylor.vane.core.components.TopLoader
import com.theseuntaylor.vane.feature.detailsScreen.ui.weatherDetailsItems

/** Home tab: the weather where the user is, in detail. Saved places are on the Favourites tab. */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = stringResource(R.string.your_current_location),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        when (val state = uiState) {
            is HomeUiState.Initial, HomeUiState.Loading -> item { TopLoader() }

            is HomeUiState.Success -> {
                // The details are already below, so the card doesn't open another screen.
                item { CityCard(name = state.data.currentLocation, uiModel = state.data) }
                weatherDetailsItems(state.forecast)
            }

            is HomeUiState.Error -> item {
                SectionMessage(message = state.errorMessage, onRetry = viewModel::loadCurrentLocationForecast)
            }
        }
    }
}
