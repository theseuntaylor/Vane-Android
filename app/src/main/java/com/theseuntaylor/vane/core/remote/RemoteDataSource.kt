package com.theseuntaylor.vane.core.remote

import com.theseuntaylor.vane.core.remote.model.WeatherForecastResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RemoteDataSource {
    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double = 51.487,
        @Query("longitude") longitude: Double = 0.026,
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("current") current: String = "temperature_2m,wind_speed_10m",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,wind_speed_10m",
    ): WeatherForecastResponse

}