package com.once.weatherez.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.once.weatherez.data.repository.WeatherRepository
import com.once.weatherez.ui.state.WeatherUiState
import com.once.weatherez.util.LocationClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: WeatherRepository,
    private val locationClient: LocationClient
) : ViewModel() {
    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState: StateFlow<WeatherUiState> = _weatherState
    private val _recentCities = MutableStateFlow<List<String>>(emptyList())
    val recentCities: StateFlow<List<String>> = _recentCities

    fun loadWeather(city: String) {
        _weatherState.value = WeatherUiState.Loading
        viewModelScope.launch {
            try {
                _recentCities.value = listOf(city) + _recentCities.value
                    .filter { it != city }
                    .take(4)

                repository.getWeather(city).collect { weather ->
                    _weatherState.value = WeatherUiState.Success(weather)
                }
            } catch (e: Exception) {
                _weatherState.value = WeatherUiState.Error(e.message ?: "Failed to load weather data")
            }
        }
    }

    fun loadCurrentLocationWeather() {
        _weatherState.value = WeatherUiState.Loading
        viewModelScope.launch {
            try {
                val location = locationClient.getCurrentLocation()
                if (location != null) {
                    // Use coordinates directly (OpenWeatherMap supports lat/lon query)
                    val query = "${location.latitude},${location.longitude}"
                    loadWeather(query)
                } else {
                    _weatherState.value = WeatherUiState.Error("Location not available")
                }
            } catch (e: Exception) {
                _weatherState.value = WeatherUiState.Error("Location error: ${e.message}")
            }
        }
    }
}