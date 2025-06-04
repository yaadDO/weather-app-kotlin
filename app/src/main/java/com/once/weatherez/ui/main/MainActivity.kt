package com.once.weatherez.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.once.weatherez.R
import com.once.weatherez.data.local.WeatherDatabase
import com.once.weatherez.data.local.entity.WeatherEntity
import com.once.weatherez.data.remote.ApiClient
import com.once.weatherez.data.repository.WeatherRepository
import com.once.weatherez.databinding.ActivityMainBinding
import com.once.weatherez.ui.state.WeatherUiState
import com.once.weatherez.util.LocationClient
import com.once.weatherez.util.loadWeatherIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private val locationClient by lazy { LocationClient(this) }
    private val viewModel: MainViewModel by viewModels {
        val database = WeatherDatabase.getDatabase(this)
        val dao = database.weatherDao()
        val repository = WeatherRepository(ApiClient.weatherApi, dao)
        MainViewModelFactory(repository, locationClient)
    }

    // Location permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.loadCurrentLocationWeather()
        } else {
            Toast.makeText(
                this,
                "Location permission denied. Using default city.",
                Toast.LENGTH_SHORT
            ).show()
            viewModel.loadWeather("London")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set dark mode as default
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu) // Make sure this resource exists

        drawerLayout = binding.drawerLayout
        binding.navView.setNavigationItemSelectedListener(this)

        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        setupUI()
        setupCityAutocomplete()
        observeViewModel()
        checkLocationPermission()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Already home
            }
            R.id.nav_settings -> {
                val settingsFragment = SettingsFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, settingsFragment) // Use correct ID
                    .addToBackStack(null)
                    .commit()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupUI() {
        binding.btnSearch.setOnClickListener {
            val city = binding.actvCity.text.toString().trim()
            if (city.isNotEmpty()) {
                viewModel.loadWeather(city)
            } else {
                Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCurrentLocation.setOnClickListener {
            requestLocationPermission()
        }
    }

    private fun setupCityAutocomplete() {
        val cities = resources.getStringArray(R.array.available_cities).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)

        lifecycleScope.launchWhenStarted {
            viewModel.recentCities.collect { recentCities ->
                val allCities = (recentCities + cities).distinct()
                adapter.clear()
                adapter.addAll(allCities) // Removed cast, should work without it
            }
        }

        binding.actvCity.setAdapter(adapter)

        binding.actvCity.setOnItemClickListener { _, _, position, _ ->
            val selectedCity = adapter.getItem(position) ?: return@setOnItemClickListener
            viewModel.loadWeather(selectedCity.toString())
        }

        binding.actvCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val city = binding.actvCity.text.toString().trim()
                if (city.isNotEmpty()) {
                    viewModel.loadWeather(city)
                    return@setOnEditorActionListener true
                }
            }
            false
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.weatherState.collect { state ->
                when (state) {
                    is WeatherUiState.Loading -> showLoading()
                    is WeatherUiState.Success -> showWeather(state.weather)
                    is WeatherUiState.Error -> showError(state.message)
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.weatherContainer.visibility = View.GONE
        binding.tvError.visibility = View.GONE
    }

    private fun showWeather(weather: WeatherEntity) {
        binding.progressBar.visibility = View.GONE
        binding.weatherContainer.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE

        binding.tvCity.text = weather.city
        binding.tvTemp.text = getString(R.string.temperature_format, weather.temperature)
        binding.tvCondition.text = weather.condition
        binding.tvHumidity.text = getString(R.string.humidity_format, weather.humidity)

        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.tvLastUpdated.text = getString(
            R.string.last_updated_format,
            dateFormat.format(Date(weather.lastUpdated))
        )

        binding.ivIcon.loadWeatherIcon(weather.icon)
        binding.actvCity.setText(weather.city)
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.weatherContainer.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message
    }

    private fun checkLocationPermission() {
        if (hasLocationPermission()) {
            // Already have permission
        } else {
            // Start with default city
            viewModel.loadWeather("London")
        }
    }

    private fun requestLocationPermission() {
        if (hasLocationPermission()) {
            viewModel.loadCurrentLocationWeather()
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionRationale()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Needed")
            .setMessage("This app needs location permission to show weather for your current location")
            .setPositiveButton("OK") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
}

class MainViewModelFactory(
    private val repository: WeatherRepository,
    private val locationClient: LocationClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, locationClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}