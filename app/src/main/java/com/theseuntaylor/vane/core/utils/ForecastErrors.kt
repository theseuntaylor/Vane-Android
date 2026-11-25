package com.theseuntaylor.vane.core.utils

import androidx.annotation.StringRes
import com.theseuntaylor.vane.R
import retrofit2.HttpException
import java.io.IOException

/**
 * What to tell the user when a forecast request fails, so they know whether trying again later
 * or connecting to the internet will help.
 */
@StringRes
fun Throwable.forecastErrorMessage(): Int = when {
    // No connection, a timeout, or a dropped connection.
    this is IOException -> R.string.error_offline
    // Offline, OkHttp answers 504 when it has no saved forecast to serve (only-if-cached).
    this is HttpException && code() == HTTP_GATEWAY_TIMEOUT -> R.string.error_offline
    this is HttpException -> R.string.error_forecast_service
    // Anything else, e.g. a response that couldn't be read.
    else -> R.string.error_loading_forecasts
}

private const val HTTP_GATEWAY_TIMEOUT = 504
