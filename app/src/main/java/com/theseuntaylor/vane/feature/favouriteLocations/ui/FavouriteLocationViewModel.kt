package com.theseuntaylor.vane.feature.favouriteLocations.ui

import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theseuntaylor.vane.feature.favouriteLocations.FavouritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun searchLocation(searchedAddress: String) {
        if (Geocoder.isPresent()) {
            geocoder.getFromLocationName(
                searchedAddress,
                5
            ) { p0 ->
                if (p0.isNotEmpty()) {
                    _listOfAddresses.value = p0
                }
            }
        }
    }

    fun saveNewFavouriteLocation(address: Address) {
        viewModelScope.launch {
            useCase.invokeSaveNewFavouriteLocation(address)
        }
    }
}