package com.once.weatherez.util

import android.widget.ImageView
import coil.load

fun ImageView.loadWeatherIcon(iconCode: String) {
    val url = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
    load(url) {
        crossfade(true)
        error(android.R.drawable.ic_menu_report_image)
    }
}