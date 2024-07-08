package com.theseuntaylor.vane.core.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "weatherCodeEntity"
)

data class WeatherCodeEntity(
    @PrimaryKey val id: Int,
    var weatherSummary: String,
)