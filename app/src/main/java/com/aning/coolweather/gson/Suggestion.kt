package com.aning.coolweather.gson

import com.google.gson.annotations.SerializedName

data class Suggestion(
        @SerializedName("comf")
        var comfort: Comfort = Comfort(),

        @SerializedName("cw")
        var carWash: CarWash = CarWash(),

        @SerializedName("sport")
        var sport: Sport = Sport()
) {
    data class Comfort(
            @SerializedName("txt")
            var info: String = "--"
    )

    data class CarWash(
            @SerializedName("txt")
            var info: String = "--"
    )

    data class Sport(
            @SerializedName("txt")
            var info: String = "--"
    )
}