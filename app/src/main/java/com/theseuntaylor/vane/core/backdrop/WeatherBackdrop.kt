package com.theseuntaylor.vane.core.backdrop

import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import kotlin.math.abs

/** The kind of sky the background shows, from the WMO weather code. */
enum class Sky { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, RAIN, SNOW, STORM }

/** The light the background shows. Dawn and dusk are the 45 minutes either side of sunrise and sunset. */
enum class TimeOfDay { DAY, DAWN, DUSK, NIGHT }

/** What the screen's background shows: the place's sky now, in its light now. */
data class WeatherBackdrop(val sky: Sky, val timeOfDay: TimeOfDay) {
    /** e.g. "backdrop-rain-night", for E2E checks; not read out by TalkBack. */
    val tag: String get() = "backdrop-${sky.name.lowercase().replace('_', '-')}-${timeOfDay.name.lowercase()}"
}

fun skyFor(weatherCode: Int): Sky = when (weatherCode) {
    0, 1 -> Sky.CLEAR
    2 -> Sky.PARTLY_CLOUDY
    45, 48 -> Sky.FOG
    in 51..67, in 80..82 -> Sky.RAIN
    in 71..77, 85, 86 -> Sky.SNOW
    in 95..99 -> Sky.STORM
    else -> Sky.CLOUDY
}

/**
 * The light at [now], all times local to the place as Open-Meteo gives them ("2026-11-25T07:40").
 * [isDay] decides when sunrise or sunset can't be read.
 */
fun timeOfDayAt(now: String, isDay: Boolean, sunrise: String?, sunset: String?): TimeOfDay {
    val time = parse(now) ?: return if (isDay) TimeOfDay.DAY else TimeOfDay.NIGHT
    fun near(event: String?) = parse(event)?.let { abs(Duration.between(it, time).toMinutes()) <= TWILIGHT_MINUTES } == true
    return when {
        near(sunrise) -> TimeOfDay.DAWN
        near(sunset) -> TimeOfDay.DUSK
        isDay -> TimeOfDay.DAY
        else -> TimeOfDay.NIGHT
    }
}

/** The backdrop for a forecast's current conditions. */
fun WeatherForecastResponse.backdrop(): WeatherBackdrop = WeatherBackdrop(
    sky = skyFor(current.weatherCode),
    timeOfDay = timeOfDayAt(current.time, current.isDay == 1, firstDaily { sunrise }, firstDaily { sunset }),
)

// Gson leaves `daily` and its lists null when the request didn't ask for them, despite the types.
private fun WeatherForecastResponse.firstDaily(times: com.theseuntaylor.vane.core.remote.model.Daily.() -> List<String>?): String? =
    runCatching { daily.times()?.firstOrNull() }.getOrNull()

private fun parse(time: String?): LocalDateTime? = try {
    time?.let(LocalDateTime::parse)
} catch (e: DateTimeParseException) {
    null
}

private const val TWILIGHT_MINUTES = 45L
