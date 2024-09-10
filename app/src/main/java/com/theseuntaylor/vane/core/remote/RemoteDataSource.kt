package com.theseuntaylor.vane.core.remote

import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RemoteDataSource {
    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        // 1 by default. change to 7 for you know what.
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,wind_speed_10m",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,daylight_duration,sunshine_duration,uv_index_max,uv_index_clear_sky_max,precipitation_sum,rain_sum,showers_sum,snowfall_sum,precipitation_hours,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,shortwave_radiation_sum,et0_fao_evapotranspiration",
        @Query("timezone") timezone: String = "auto",
    ): WeatherForecastResponse

    @GET("v1/forecast")
    suspend fun getFavouriteLocationsWeatherForecast(
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String,
        @Query("forecast_days") forecastDays: Int = 1,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,wind_speed_10m",
        @Query("timezone") timezone: String = "auto",
    ): List<WeatherForecastResponse>

}