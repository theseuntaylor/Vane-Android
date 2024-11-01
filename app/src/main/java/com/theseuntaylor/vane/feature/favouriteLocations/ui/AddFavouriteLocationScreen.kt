package com.theseuntaylor.vane.feature.favouriteLocations.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.theseuntaylor.vane.R
import com.theseuntaylor.vane.core.components.VaneAppBar
import com.theseuntaylor.vane.core.components.VaneSearchTextField

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AddFavouriteLocationScreen(
    modifier: Modifier = Modifier,
    viewModel: FavouriteLocationViewModel = hiltViewModel(),
    navController: NavController,
) {
    Scaffold(
        topBar = {
            VaneAppBar(
                title = "Add a New Location",
                navController = navController,
                navigateBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.navigateUp()
                    }
                }
            )
        }
    ) { paddingValues ->

        val focusRequester = remember { FocusRequester() }
        var searchedAddress: String by remember { mutableStateOf("") }
        val listOfAddresses = viewModel.listOfAddresses

        Column(modifier = modifier.padding(paddingValues)) {
            Text(
                text = stringResource(R.string.add_another_location), style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
            Box(modifier = Modifier.height(10.dp))
            VaneSearchTextField(
                value = searchedAddress,
                modifier = modifier.focusRequester(
                    focusRequester = focusRequester
                ),
                onValueChanged = { newSearchedAddress ->
                    searchedAddress = newSearchedAddress
                    viewModel.searchLocation(searchedAddress = searchedAddress)
                }
            )
            Box(modifier = Modifier.height(14.dp))

            val addresses = listOfAddresses.collectAsState().value
            if (addresses.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.search_results), style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                    }
                    itemsIndexed(addresses) { _, address ->
                        Card(
                            modifier = modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.saveNewFavouriteLocation(address = address)
                            }
                        ) {
                            Column(
                                modifier = modifier.padding(10.dp)
                            ) {
                                Text(
                                    text = "${address.getAddressLine(0)}, ${address.countryCode}",
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.latitude, address.latitude),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = stringResource(R.string.longitude, address.longitude),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}