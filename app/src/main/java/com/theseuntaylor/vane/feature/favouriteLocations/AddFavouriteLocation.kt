package com.theseuntaylor.vane.feature.favouriteLocations

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theseuntaylor.vane.core.components.VaneSearchTextField
import com.theseuntaylor.vane.feature.home.ui.HomeViewModel

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AddFavouriteLocation(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {

    var searchedAddress: String by remember { mutableStateOf("") }
    val listOfAddresses = viewModel.listOfAddresses

    Column {
        Text(
            text = "Add Another Location", style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        )
        VaneSearchTextField(
            value = searchedAddress,
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
                itemsIndexed(addresses) { _, address ->
                    Card(
                        modifier = modifier.fillMaxWidth(),
                        onClick = {

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
                                text = "Latitude: ${address.latitude}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "Longitude: ${address.longitude}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}