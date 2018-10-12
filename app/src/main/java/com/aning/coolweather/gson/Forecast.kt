package com.aning.coolweather.gson

import com.google.gson.annotations.SerializedName

data class Forecast(
        @SerializedName("date")
        var date: String,

        @SerializedName("tmp")
        var temperature: Temperature,

        @SerializedName("cond")
        var more: More
) {
    data class Temperature(
            var min: String,
            var max: String
    )

    data class More(
            @SerializedName("txt_d")
            var info:String
    )
}