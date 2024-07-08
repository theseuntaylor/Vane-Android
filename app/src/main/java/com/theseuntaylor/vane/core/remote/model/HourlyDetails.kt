package com.theseuntaylor.vane.core.remote.model

import com.google.gson.annotations.SerializedName

data class HourlyUnits(
    @SerializedName("time") val time: String,
    @SerializedName("temperature_2m") val temperature_2m: String,
    @SerializedName("relative_humidity_2m") val relative_humidity_2m: String,
    @SerializedName("wind_speed_10m") val wind_speed_10m: String,
)

data class Hourly(
    @SerializedName("time") var time: List<String> = emptyList(),
    @SerializedName("temperature_2m") var temperature_2m: List<Double> = emptyList(),
    @SerializedName("relative_humidity_2m") var relative_humidity_2m: List<Int> = emptyList(),
    @SerializedName("wind_speed_10m") var wind_speed_10m: List<Double> = emptyList(),
)
