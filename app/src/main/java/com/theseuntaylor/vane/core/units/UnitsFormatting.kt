package com.theseuntaylor.vane.core.units

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/** [Units.formatTemperature] with the units from [LocalUnits]. */
@Composable
@ReadOnlyComposable
fun formatTemperature(celsius: Double): String = LocalUnits.current.formatTemperature(celsius)

/** [Units.formatTime] with the units from [LocalUnits] and the device's 12/24-hour setting. */
@Composable
@ReadOnlyComposable
fun formatTime(isoLocalDateTime: String): String =
    LocalUnits.current.formatTime(isoLocalDateTime, DateFormat.is24HourFormat(LocalContext.current))
