package com.theseuntaylor.vane.feature.detailsScreen.ui

import android.location.Geocoder
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DetailedWeatherForecastViewModel @Inject constructor(
    private val geocoder: Geocoder,
) : ViewModel()