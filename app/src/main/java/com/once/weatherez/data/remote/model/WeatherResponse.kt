package com.once.weatherez.data.remote.model

data class WeatherResponse(
    val name: String,
    val main: Main,
    val weather: List<Weather>,
    val dt: Long
)

data class Main(
    val temp: Double,
    val humidity: Int
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)