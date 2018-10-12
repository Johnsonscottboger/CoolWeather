package com.aning.coolweather.util

import android.text.TextUtils
import com.aning.coolweather.db.City
import com.aning.coolweather.db.County
import com.aning.coolweather.db.Province
import org.json.JSONArray
import org.json.JSONException

public object Utility {

    /**
     * 解析和处理服务器返回的省级数据
     * @param response 服务响应的JSON数据
     */
    public fun handleProvinceResponse(response: String): Boolean {
        if (response.isEmpty())
            return false;
        try {
            val allProvinces = JSONArray(response);
            for (i in 0 until allProvinces.length()) {
                val provinceObject = allProvinces.getJSONObject(i) ?: continue;
                Province(provinceName = provinceObject.getString("name"),
                        provinceCode = provinceObject.getInt("id"))
                        .save();
            }
            return true;
        } catch (e: JSONException) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 解析和处理服务器返回的市级数据
     * @param response 服务器响应的JSON数据
     * @param provinceId 省份ID
     */
    public fun handleCityResponse(response: String, provinceId: Int): Boolean {
        if (response.isEmpty())
            return false;
        try {
            val allCities = JSONArray(response);
            for (i in 0 until allCities.length()) {
                val cityObject = allCities.getJSONObject(i) ?: continue;
                City(cityName = cityObject.getString("name"),
                        cityCode = cityObject.getInt("id"),
                        provinceId = provinceId)
                        .save();
            }
            return true;
        } catch (ex: JSONException) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * 解析和处理服务器返回的县级数据
     * @param response 服务器响应的JSON数据
     * @param cityId 市级ID
     */
    public fun handleCountyResponse(response: String, cityId: Int): Boolean {
        if (response.isEmpty())
            return false;
        try {
            val allCounties = JSONArray(response);
            for (i in 0 until allCounties.length()) {
                val countyObject = allCounties.getJSONObject(i) ?: continue;
                County(countyName = countyObject.getString("name"),
                        weatherId = countyObject.getString("weather_id"),
                        cityId = cityId)
                        .save();
            }
            return true;
        } catch (ex: JSONException) {
            ex.printStackTrace();
            return false;
        }
    }
}