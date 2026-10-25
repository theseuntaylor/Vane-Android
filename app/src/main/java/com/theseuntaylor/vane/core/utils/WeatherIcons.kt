package com.theseuntaylor.vane.core.utils

import androidx.annotation.DrawableRes
import com.theseuntaylor.vane.R

/** Material Symbols icon for a WMO weather code; shared by the app and the home screen widget. */
@DrawableRes
fun weatherIconRes(weatherCode: Int): Int = when (weatherCode) {
    0, 1 -> R.drawable.ic_weather_clear
    2 -> R.drawable.ic_weather_partly_cloudy
    3 -> R.drawable.ic_weather_cloudy
    45, 48 -> R.drawable.ic_weather_fog
    in 51..67, in 80..82 -> R.drawable.ic_weather_rain
    in 71..77, 85, 86 -> R.drawable.ic_weather_snow
    in 95..99 -> R.drawable.ic_weather_thunderstorm
    else -> R.drawable.ic_weather_unknown
}
