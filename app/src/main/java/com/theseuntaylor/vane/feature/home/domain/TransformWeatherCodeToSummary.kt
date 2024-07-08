package com.theseuntaylor.vane.feature.home.domain

import com.theseuntaylor.vane.core.local.WeatherCodeDao
import javax.inject.Inject

//
class TransformWeatherCodeToSummary @Inject constructor(
    private val database: WeatherCodeDao,
)