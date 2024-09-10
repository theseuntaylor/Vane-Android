package com.theseuntaylor.vane.core.remote.model

import com.google.gson.annotations.SerializedName


data class WeatherForecastResponse(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("generationtime_ms")
    val generationTime_ms: Double,
    @SerializedName("utc_offset_seconds")
    val utcOffset_seconds: Int,
    @SerializedName("timezone")
    val timezone: String = "",
    @SerializedName("timezone_abbreviation")
    val timezoneAbbreviation: String = "",
    @SerializedName("elevation")
    val elevation: Double,
    @SerializedName("current_units")
    val currentUnits: CurrentUnits,
    @SerializedName("current")
    val current: Current,
    @SerializedName("hourly_units")
    val hourlyUnits: HourlyUnits,
    @SerializedName("hourly")
    val hourly: Hourly,
    @SerializedName("daily_units")
    val dailyUnits: DailyUnits,
    @SerializedName("daily")
    val daily: Daily,
    val currentLocation: String = ""
)