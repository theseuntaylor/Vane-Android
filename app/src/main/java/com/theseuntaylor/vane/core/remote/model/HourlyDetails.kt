package com.theseuntaylor.vane.core.remote.model

import com.google.gson.annotations.SerializedName

// TODO: add more hourly units and hourly values to show in the detailed weather forecast screen
data class HourlyUnits(
    @SerializedName("time") val time: String,
    @SerializedName("temperature_2m") val temperature_2m: String,
    @SerializedName("relative_humidity_2m") val relative_humidity_2m: String,
    @SerializedName("wind_speed_10m") val wind_speed_10m: String,
)

data class Hourly(
    @SerializedName("time") var time: List<String> = listOf(
        "2024-08-27T00:00",
        "2024-08-27T01:00",
        "2024-08-27T02:00",
        "2024-08-27T03:00",
        "2024-08-27T04:00",
        "2024-08-27T05:00",
        "2024-08-27T06:00",
        "2024-08-27T07:00",
    ),
    @SerializedName("temperature_2m") var temperature_2m: List<Double> = listOf(
        15.1,
        15.3,
        15.6,
        15.5,
        15.1,
        14.8,
        15.3,
        16.4,
    ),
    @SerializedName("relative_humidity_2m") var relative_humidity_2m: List<Int> = listOf(
        89,
        91,
        91,
        91,
        94,
        95,
        93,
        87,
    ),
    @SerializedName("wind_speed_10m") var wind_speed_10m: List<Double> = listOf(
        0.0,
        8.0,
        8.0,
        25.0,
        18.0,
        10.0,
        5.0,
        3.0,
    ),
)
