package com.once.weatherez.data.repository

import com.once.weatherez.data.local.WeatherDao
import com.once.weatherez.data.local.entity.WeatherEntity
import com.once.weatherez.data.remote.ApiClient
import com.once.weatherez.data.remote.WeatherApiService
import com.once.weatherez.data.remote.model.WeatherResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit

class WeatherRepository(
    private val api: WeatherApiService,
    private val dao: WeatherDao
) {
    fun getWeather(city: String): Flow<WeatherEntity> = flow {
        dao.getWeather(city).collect { cachedData ->
            if (shouldFetchFreshData(cachedData)) {
                try {
                    // CORRECT: Getting WeatherResponse from API
                    val response = api.getWeather(city, apiKey = ApiClient.API_KEY)
                    val entity = mapToWeatherEntity(city, response)
                    dao.insert(entity)
                    emit(entity)
                } catch (e: Exception) {
                    cachedData?.let { emit(it) } ?: throw e
                }
            } else {
                cachedData?.let { emit(it) }
            }
        }
    }

    private fun shouldFetchFreshData(cachedData: WeatherEntity?): Boolean {
        if (cachedData == null) return true

        val currentTime = System.currentTimeMillis()
        val cacheDuration = TimeUnit.MINUTES.toMillis(10) // 10 minutes cache
        return (currentTime - cachedData.lastUpdated) > cacheDuration
    }

    private fun mapToWeatherEntity(city: String, response: WeatherResponse): WeatherEntity {
        return WeatherEntity(
            city = city,
            temperature = response.main.temp,
            humidity = response.main.humidity,
            condition = response.weather.firstOrNull()?.main ?: "",
            icon = response.weather.firstOrNull()?.icon ?: "",
            lastUpdated = System.currentTimeMillis()
        )
    }
}