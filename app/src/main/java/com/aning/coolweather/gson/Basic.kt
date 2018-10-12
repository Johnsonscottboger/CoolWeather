package com.aning.coolweather.gson

import com.google.gson.annotations.SerializedName

data class Basic(
        @SerializedName("city")
        var cityName: String = "--",

        @SerializedName("id")
        var weatherId: String = "--",

        @SerializedName("update")
        var update: Update = Update()
) {
    data class Update(
            @SerializedName("loc")
            var updateTime: String = "--"
    )
}