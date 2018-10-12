package com.aning.coolweather.gson

data class AQI(
        val city: AQICity = AQICity()
) {
    data class AQICity(
            var aqi: String = "--",
            var pm25: String = "--"
    )
}