package com.theseuntaylor.vane.feature.widget.data

import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import kotlinx.serialization.Serializable
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * The last forecast the widget can show without the app running or a network connection.
 */
@Serializable
data class WidgetWeatherSnapshot(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val weatherCode: Int,
    val high: Double,
    val low: Double,
    val upcomingHours: List<HourlyTemperature>,
    val updatedAtMillis: Long,
)

/** A saved favourite's current conditions, shown as a row in the tall widget. */
@Serializable
data class FavouriteWeather(
    val name: String,
    val temperature: Double,
    val weatherCode: Int,
    // Null in data saved before these were added, or when the forecast had no daily values.
    val high: Double? = null,
    val low: Double? = null,
)

@Serializable
data class HourlyTemperature(
    val hour: String,
    val temperature: Double,
    // -1 in data saved before hourly codes were requested.
    val weatherCode: Int = -1,
)

internal const val UPCOMING_HOURS = 5

fun WeatherForecastResponse.toWidgetSnapshot(locationName: String): WidgetWeatherSnapshot {
    // Open-Meteo times are ISO local times ("2024-08-27T14:15"), so they compare as strings.
    val currentHour = current.time.take(13)
    val upcoming = hourly.time.indices
        .filter { hourly.time[it].take(13) > currentHour }
        .take(UPCOMING_HOURS)
        .map {
            HourlyTemperature(
                hour = hourly.time[it].substring(11, 16),
                temperature = hourly.temperature_2m[it],
                weatherCode = hourly.weatherCode.getOrElse(it) { -1 },
            )
        }

    return WidgetWeatherSnapshot(
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        temperature = current.temperature_2m,
        weatherCode = current.weatherCode,
        high = daily.temperature_2m_max.firstOrNull() ?: hourly.temperature_2m.max(),
        low = daily.temperature_2m_min.firstOrNull() ?: hourly.temperature_2m.min(),
        upcomingHours = upcoming,
        updatedAtMillis = observedAtMillis(),
    )
}

/**
 * When the forecast was observed, not when it was read: offline, OkHttp can serve a cached
 * response that is hours old, and the widget's "Updated" label must say so.
 */
private fun WeatherForecastResponse.observedAtMillis(): Long {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return try {
        val localAsUtc = format.parse(current.time)?.time ?: return System.currentTimeMillis()
        localAsUtc - utcOffset_seconds * 1000L
    } catch (e: ParseException) {
        System.currentTimeMillis()
    }
}

/** Today's high and low, or null when the response carries no daily values. */
fun WeatherForecastResponse.todayHighAndLow(): Pair<Double, Double>? {
    // Gson leaves `daily` and its lists null when the request didn't ask for them.
    @Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
    val today = daily ?: return null
    val high = today.temperature_2m_max?.firstOrNull() ?: return null
    val low = today.temperature_2m_min?.firstOrNull() ?: return null
    return high to low
}
