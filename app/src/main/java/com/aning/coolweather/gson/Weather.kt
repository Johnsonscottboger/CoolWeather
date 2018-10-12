package com.aning.coolweather.gson

import com.google.gson.annotations.SerializedName

data class Weather(
        var status: String = "--",

        var basic: Basic = Basic(),

        var aqi: AQI = AQI(),

        var now: Now = Now(),

        var suggestion: Suggestion = Suggestion(),

        @SerializedName("daily_forecast")
        var forecastList: MutableList<Forecast> = mutableListOf()
) {
}