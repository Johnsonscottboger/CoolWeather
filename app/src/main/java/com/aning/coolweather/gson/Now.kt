package com.aning.coolweather.gson

import com.google.gson.annotations.SerializedName

data class Now(
        @SerializedName("tmp")
        var temperature: String = "--",

        @SerializedName("cond")
        var more: More = More()
) {
    data class More(
            @SerializedName("txt")
            var info: String = "--"
    )
}