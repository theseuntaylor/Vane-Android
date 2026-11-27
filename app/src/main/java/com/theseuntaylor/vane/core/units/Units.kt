package com.theseuntaylor.vane.core.units

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToInt

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

enum class WindSpeedUnit { KILOMETRES_PER_HOUR, MILES_PER_HOUR, METRES_PER_SECOND }

enum class PrecipitationUnit { MILLIMETRES, INCHES }

/** 12- or 24-hour clock; [SYSTEM] follows the device's setting. */
enum class ClockFormat { SYSTEM, TWELVE_HOUR, TWENTY_FOUR_HOUR }

/**
 * How the user wants weather shown, chosen in Settings. Forecasts are kept in Open-Meteo's
 * metric units (°C, km/h, mm) and converted only when shown, so a change applies everywhere at
 * once without fetching again.
 */
data class Units(
    val temperature: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windSpeed: WindSpeedUnit = WindSpeedUnit.KILOMETRES_PER_HOUR,
    val precipitation: PrecipitationUnit = PrecipitationUnit.MILLIMETRES,
    val clock: ClockFormat = ClockFormat.SYSTEM,
)

/** The user's units for everything drawn below it (the app and the widget each provide it). */
val LocalUnits = staticCompositionLocalOf { Units() }

/** Whole degrees with a degree sign, e.g. "48°". The unit letter is left out, as on most weather apps. */
fun Units.formatTemperature(celsius: Double): String = "${temperatureValue(celsius)}°"

/** Whole degrees in the chosen unit, for strings that add the degree sign themselves. */
fun Units.temperatureValue(celsius: Double): Int = when (temperature) {
    TemperatureUnit.CELSIUS -> celsius
    TemperatureUnit.FAHRENHEIT -> celsius * 9 / 5 + 32
}.roundToInt()

/** A wind speed given in km/h, as a number in the chosen unit (one decimal) and its label. */
fun Units.windSpeedValue(kilometresPerHour: Double): Pair<String, String> = when (windSpeed) {
    WindSpeedUnit.KILOMETRES_PER_HOUR -> oneDecimal(kilometresPerHour) to "km/h"
    WindSpeedUnit.MILES_PER_HOUR -> oneDecimal(kilometresPerHour / KILOMETRES_PER_MILE) to "mph"
    WindSpeedUnit.METRES_PER_SECOND -> oneDecimal(kilometresPerHour / 3.6) to "m/s"
}

/** A precipitation amount given in mm, as a number in the chosen unit and its label. */
fun Units.precipitationValue(millimetres: Double): Pair<String, String> = when (precipitation) {
    PrecipitationUnit.MILLIMETRES -> oneDecimal(millimetres) to "mm"
    // An inch is 25.4 mm, so two decimals keep light rain from showing as 0.0.
    PrecipitationUnit.INCHES -> String.format(Locale.getDefault(), "%.2f", millimetres / MILLIMETRES_PER_INCH) to "in"
}

/**
 * The time of an Open-Meteo local timestamp ("2026-11-16T14:30"), e.g. "14:30" or "2:30 PM".
 * [deviceUses24Hour] is the device's setting, used when the clock follows the system.
 */
fun Units.formatTime(isoLocalDateTime: String, deviceUses24Hour: Boolean, locale: Locale = Locale.getDefault()): String {
    val time = try {
        LocalDateTime.parse(isoLocalDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalTime()
    } catch (e: DateTimeParseException) {
        return isoLocalDateTime
    }
    return formatTime(time, deviceUses24Hour, locale)
}

/**
 * A clock time saved as "HH:mm" (the widget's upcoming hours), formatted like [formatTime]. With
 * [short], a 12-hour time on the hour leaves out the minutes ("2 PM"), so it fits a narrow slot.
 */
fun Units.formatClockTime(
    hourMinute: String,
    deviceUses24Hour: Boolean,
    locale: Locale = Locale.getDefault(),
    short: Boolean = false,
): String {
    val time = try {
        LocalTime.parse(hourMinute)
    } catch (e: DateTimeParseException) {
        return hourMinute
    }
    return formatTime(time, deviceUses24Hour, locale, short)
}

private fun Units.formatTime(time: LocalTime, deviceUses24Hour: Boolean, locale: Locale, short: Boolean = false): String {
    val twentyFourHour = when (clock) {
        ClockFormat.SYSTEM -> deviceUses24Hour
        ClockFormat.TWELVE_HOUR -> false
        ClockFormat.TWENTY_FOUR_HOUR -> true
    }
    val pattern = when {
        twentyFourHour -> "HH:mm"
        short && time.minute == 0 -> "h a"
        else -> "h:mm a"
    }
    return time.format(DateTimeFormatter.ofPattern(pattern, locale))
}

private fun oneDecimal(value: Double) = String.format(Locale.getDefault(), "%.1f", value)

private const val KILOMETRES_PER_MILE = 1.609344
private const val MILLIMETRES_PER_INCH = 25.4
