package com.theseuntaylor.vane.core.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherCodeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addWeatherCodes(weatherCodeEntity: WeatherCodeEntity)

    @Query("SELECT * FROM weatherCodeEntity WHERE id = :id")
    suspend fun getWeatherSummary(id: Int): WeatherCodeEntity
}