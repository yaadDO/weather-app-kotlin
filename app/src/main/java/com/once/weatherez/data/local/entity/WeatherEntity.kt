package com.once.weatherez.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather")
data class WeatherEntity(
    @PrimaryKey val city: String,
    val temperature: Double,
    val humidity: Int,
    val condition: String,
    val icon: String,
    val lastUpdated: Long
)
