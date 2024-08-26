package com.theseuntaylor.vane.core.remote

import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RemoteDataSource {
    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,wind_speed_10m",
        @Query("timezone") timezone: String = "auto",
    ): WeatherForecastResponse

    @GET("v1/forecast")
    suspend fun getFavouriteLocationsWeatherForecast(
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String,
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,wind_speed_10m",
        @Query("timezone") timezone: String = "auto",
    ): List<WeatherForecastResponse>

}