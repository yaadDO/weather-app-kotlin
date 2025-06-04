package com.once.weatherez.data.remote

import com.once.weatherez.data.remote.model.WeatherResponse
import retrofit2.http.Query
import retrofit2.http.GET

interface WeatherApiService {
    @GET("weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String
    ): WeatherResponse  // Changed from WeatherRepository to WeatherResponse
}