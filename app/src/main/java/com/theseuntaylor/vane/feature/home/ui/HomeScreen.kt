package com.theseuntaylor.vane.feature.home.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.theseuntaylor.vane.core.components.CityCard
import com.theseuntaylor.vane.core.components.Loader

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
) {
    val uiState = viewModel.uiState

    when (val state = uiState.collectAsState().value) {
        is HomeUiState.Loading -> {
            Loader()
        }

        is HomeUiState.Success -> {
            val data = state.data

            val cities = listOf("London", "Birmingham", "Manchester", "Anfield")

            LazyColumn {
                itemsIndexed(cities) { index, city ->
                    CityCard(
                        name = "$city $index",
                        // hourly = data.hourly, current = data.current
                    )
                }
            }

        }

        // Show error message and show a retry button.
        is HomeUiState.Error -> {

        }

        // I
        else -> {

        }
    }
}