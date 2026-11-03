package com.theseuntaylor.vane.feature.favouriteLocations.ui

import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theseuntaylor.vane.core.utils.findAddresses
import com.theseuntaylor.vane.feature.favouriteLocations.domain.FavouritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouriteLocationViewModel @Inject constructor(
    private val geocoder: Geocoder,
    private val useCase: FavouritesUseCase,
) : ViewModel() {

    private val _listOfAddresses = MutableStateFlow<List<Address>>(emptyList())
    val listOfAddresses = _listOfAddresses.asStateFlow()

    private var searchJob: Job? = null

    fun searchLocation(searchedAddress: String) {
        if (!Geocoder.isPresent()) return
        // Each keystroke starts a search; only the latest one may update the results.
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val addresses = geocoder.findAddresses(searchedAddress, 10)
            if (addresses.isNotEmpty()) {
                _listOfAddresses.value = addresses
            }
        }
    }

    fun saveNewFavouriteLocation(address: Address) {
        viewModelScope.launch {
            useCase.invokeSaveNewFavouriteLocation(address)
        }
    }
}
