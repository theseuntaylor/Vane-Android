package com.theseuntaylor.vane.feature.alerts.domain

import com.theseuntaylor.vane.core.remote.model.PrecipitationNowcast
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

enum class PrecipitationKind { RAIN, SNOW, THUNDERSTORM }

data class UpcomingPrecipitation(val kind: PrecipitationKind, val startsAtMillis: Long)

/** What a check of the 15-minute forecast found. */
sealed interface NowcastResult {
    data class Upcoming(val precipitation: UpcomingPrecipitation) : NowcastResult
    data object AlreadyFalling : NowcastResult
    data object Dry : NowcastResult
}

internal val SLOT_MILLIS = TimeUnit.MINUTES.toMillis(15)
internal val LOOKAHEAD_MILLIS = TimeUnit.MINUTES.toMillis(90)

/**
 * Finds the first rain, snow or thunderstorm that starts within [LOOKAHEAD_MILLIS] of [nowMillis].
 * Nothing is upcoming while it is already falling: the user can see that for themselves.
 */
fun PrecipitationNowcast.findUpcomingPrecipitation(nowMillis: Long): NowcastResult {
    val slots = minutely15
    for (i in slots.time.indices) {
        val startMillis = slots.time[i].toEpochMillis(utcOffsetSeconds) ?: continue
        if (startMillis + SLOT_MILLIS <= nowMillis) continue // already over
        if (startMillis > nowMillis + LOOKAHEAD_MILLIS) break
        val kind = precipitationKind(
            weatherCode = slots.weatherCode.getOrNull(i),
            snowfall = slots.snowfall.getOrNull(i),
        ) ?: continue
        // A wet slot that has already started means it is raining or snowing now.
        return if (startMillis <= nowMillis) NowcastResult.AlreadyFalling
        else NowcastResult.Upcoming(UpcomingPrecipitation(kind, startMillis))
    }
    return NowcastResult.Dry
}

/** Rain, snow or a thunderstorm by WMO code. Drizzle (51–57) and fog don't count. */
internal fun precipitationKind(weatherCode: Int?, snowfall: Double?): PrecipitationKind? = when (weatherCode) {
    in 95..99 -> PrecipitationKind.THUNDERSTORM
    in 71..77, 85, 86 -> PrecipitationKind.SNOW
    // Rain with snow measured in the same slot (sleet) is reported as snow.
    in 61..67, in 80..82 -> if ((snowfall ?: 0.0) > 0.0) PrecipitationKind.SNOW else PrecipitationKind.RAIN
    else -> null
}

/** Open-Meteo times are local ISO times ("2026-11-11T14:15") at the given UTC offset. */
private fun String.toEpochMillis(utcOffsetSeconds: Int): Long? = try {
    LocalDateTime.parse(this).toInstant(ZoneOffset.ofTotalSeconds(utcOffsetSeconds)).toEpochMilli()
} catch (e: DateTimeParseException) {
    null
}
