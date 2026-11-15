package com.theseuntaylor.vane.core.remote.model

import com.google.gson.annotations.SerializedName

/** Open-Meteo's 15-minute forecast, used for rain and snow alerts. */
data class PrecipitationNowcast(
    @SerializedName("utc_offset_seconds")
    val utcOffsetSeconds: Int,
    @SerializedName("minutely_15")
    val minutely15: Minutely15,
)

/** Parallel lists, one entry per 15-minute slot. Times are local to the forecast's location. */
data class Minutely15(
    @SerializedName("time")
    val time: List<String>,
    @SerializedName("weather_code")
    val weatherCode: List<Int?>,
    @SerializedName("snowfall")
    val snowfall: List<Double?>,
)
