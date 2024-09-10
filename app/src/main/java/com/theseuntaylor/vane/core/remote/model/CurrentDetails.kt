package com.theseuntaylor.vane.core.remote.model

import com.google.gson.annotations.SerializedName

data class CurrentUnits(
    @SerializedName("time") val time: String = "",
    @SerializedName("interval") val interval: String = "",
    @SerializedName("temperature_2m") val temperature_2m: String = "",
    @SerializedName("wind_speed_10m") val wind_speed_10m: String = "",
    @SerializedName("wind_direction_10m") val wind_direction_10m: String = "",
    @SerializedName("wind_gusts_10m") val wind_gusts_10m: String = "",
    @SerializedName("weather_code") val weatherCode: String = "",
    @SerializedName("pressure_msl") val pressure_msl: String = "",
    @SerializedName("surface_pressure") val surfacePressure: String = "",
)

data class Current(
    @SerializedName("time") val time: String = "",
    @SerializedName("interval") val interval: Int = 0,
    @SerializedName("temperature_2m") val temperature_2m: Double = 0.0,
    @SerializedName("wind_speed_10m") val wind_speed_10m: Double = 0.0,
    @SerializedName("weather_code") val weatherCode: Int = 0,
    @SerializedName("pressure_msl") val pressure_msl: Double = 0.0,
    @SerializedName("surface_pressure") val surfacePressure: Double = 0.0,
    @SerializedName("wind_direction_10m") val wind_direction_10m: Double = 0.0,
    @SerializedName("wind_gusts_10m") val wind_gusts_10m: Double = 0.0,
)
