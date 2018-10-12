package com.aning.coolweather.util

import android.text.TextUtils
import com.aning.coolweather.db.City
import com.aning.coolweather.db.County
import com.aning.coolweather.db.Province
import com.aning.coolweather.gson.Weather
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception

public object Utility {

    /**
     * 解析和处理服务器返回的省级数据
     * @param response 服务响应的JSON数据
     */
    public fun handleProvinceResponse(response: String): Boolean {
        if (response.isEmpty())
            return false;
        return try {
            val allProvinces = JSONArray(response);
            for (i in 0 until allProvinces.length()) {
                val provinceObject = allProvinces.getJSONObject(i) ?: continue;
                Province(provinceName = provinceObject.getString("name"),
                        provinceCode = provinceObject.getInt("id"))
                        .save();
            }
            true;
        } catch (e: JSONException) {
            e.printStackTrace();
            false;
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
        return try {
            val allCities = JSONArray(response);
            for (i in 0 until allCities.length()) {
                val cityObject = allCities.getJSONObject(i) ?: continue;
                City(cityName = cityObject.getString("name"),
                        cityCode = cityObject.getInt("id"),
                        provinceId = provinceId)
                        .save();
            }
            true;
        } catch (ex: JSONException) {
            ex.printStackTrace();
            false;
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
        return try {
            val allCounties = JSONArray(response);
            for (i in 0 until allCounties.length()) {
                val countyObject = allCounties.getJSONObject(i) ?: continue;
                County(countyName = countyObject.getString("name"),
                        weatherId = countyObject.getString("weather_id"),
                        cityId = cityId)
                        .save();
            }
            true;
        } catch (ex: JSONException) {
            ex.printStackTrace();
            false;
        }
    }

    /**
     * 将响应的JSON数据转换为 Weather 实例
     */
    public fun handleWeatherResponse(response: String): Weather {
        return try {
            val jsonObject = JSONObject(response);
            val jsonArray = jsonObject.getJSONArray("HeWeather");
            val weatherContent = jsonArray.getJSONObject(0).toString();
            Gson().fromJson<Weather>(weatherContent, Weather::class.java);
        } catch (ex: Exception){
            ex.printStackTrace();
            Weather();
        }
    }
}