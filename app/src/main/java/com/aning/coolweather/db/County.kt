package com.aning.coolweather.db

import org.litepal.crud.LitePalSupport

/**
 * 县, 区域
 */
data class County(
        var id: Int = 0,
        var countyName: String,
        var weatherId: String,
        var cityId: Int
) : LitePalSupport()