package com.theseuntaylor.vane.core.remote.model

import com.google.gson.annotations.SerializedName

data class DailyUnits(
    @SerializedName("time")
    val time: String = "iso8601",
    @SerializedName("weather_code")
    val weatherCode: String = "wmo code",
    @SerializedName("temperature_2m_max")
    val temperature_2m_max: String = "°C",
    @SerializedName("temperature_2m_min")
    val temperature_2m_min: String = "°C",
    @SerializedName("apparent_temperature_max")
    val apparent_temperature_max: String = "°C",
    @SerializedName("apparent_temperature_min")
    val apparent_temperature_min: String = "°C",
    @SerializedName("sunrise")
    val sunrise: String = "iso8601",
    @SerializedName("sunset")
    val sunset: String = "iso8601",
    @SerializedName("daylight_duration")
    val daylightDuration: String = "s",
    @SerializedName("sunshine_duration")
    val sunshineDuration: String = "s",
    @SerializedName("uv_index_max")
    val uv_index_max: String = "",
    @SerializedName("uv_index_clear_sky_max")
    val uv_index_clear_sky_max: String = "",
    @SerializedName("precipitation_sum")
    val precipitation_sum: String = "mm",
    @SerializedName("rain_sum")
    val rain_sum: String = "mm",
    @SerializedName("showers_sum")
    val showers_sum: String = "mm",
    @SerializedName("snowfall_sum")
    val snowfall_sum: String = "cm",
    @SerializedName("precipitation_hours")
    val precipitation_hours: String = "h",
    @SerializedName("precipitation_probability_max")
    val precipitation_probability_max: String = "%",
    @SerializedName("wind_speed_10m_max")
    val wind_speed_10m_max: String = "km/h",
    @SerializedName("wind_gusts_10m_max")
    val wind_gusts_10m_max: String = "km/h",
    @SerializedName("wind_direction_10m_dominant")
    val wind_direction_10m_dominant: String = "°",
    @SerializedName("shortwave_radiation_sum")
    val shortwave_radiation_sum: String = "MJ/m²",
    @SerializedName("et0_fao_evapotranspiration")
    val et0_fao_evapotranspiration: String = "mm"
)

data class Daily(
    @SerializedName("time")
    val time: List<String>,
    @SerializedName("weather_code")
    val weather_code: List<Int>,
    @SerializedName("temperature_2m_max")
    val temperature_2m_max: List<Double>,
    @SerializedName("temperature_2m_min")
    val temperature_2m_min: List<Double>,
    @SerializedName("apparent_temperature_max")
    val apparent_temperature_max: List<Double>,
    @SerializedName("apparent_temperature_min")
    val apparent_temperature_min: List<Double>,
    @SerializedName("sunrise")
    val sunrise: List<String>,
    @SerializedName("sunset")
    val sunset: List<String>,
    @SerializedName("daylight_duration")
    val daylight_duration: List<Double>,
    @SerializedName("sunshine_duration")
    val sunshine_duration: List<Double>,
    @SerializedName("uv_index_max")
    val uv_index_max: List<Double>,
    @SerializedName("uv_index_clear_sky_max")
    val uv_index_clear_sky_max: List<Double>,
    @SerializedName("precipitation_sum")
    val precipitation_sum: List<Double>,
    @SerializedName("rain_sum")
    val rain_sum: List<Double>,
    @SerializedName("showers_sum")
    val showers_sum: List<Double>,
    @SerializedName("snowfall_sum")
    val snowfall_sum: List<Double>,
    @SerializedName("precipitation_hours")
    val precipitation_hours: List<Double>,
    @SerializedName("precipitation_probability_max")
    val precipitation_probability_max: List<Int>,
    @SerializedName("wind_speed_10m_max")
    val wind_speed_10m_max: List<Double>,
    @SerializedName("wind_gusts_10m_max")
    val wind_gusts_10m_max: List<Double>,
    @SerializedName("wind_direction_10m_dominant")
    val wind_direction_10m_dominant: List<Double>,
    @SerializedName("shortwave_radiation_sum")
    val shortwave_radiation_sum: List<Double>,
    @SerializedName("et0_fao_evapotranspiration")
    val et0_fao_evapotranspiration: List<Double>,
)