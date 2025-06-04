package com.once.weatherez.ui.state

import com.once.weatherez.data.local.entity.WeatherEntity

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val weather: WeatherEntity) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}