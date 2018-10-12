package com.aning.coolweather.db

import org.litepal.crud.LitePalSupport

/**
 * 省份
 */
data class Province(
        var id: Int = 0,
        var provinceName: String,
        var provinceCode: Int
) : LitePalSupport()