package com.aning.coolweather

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.speech.tts.UtteranceProgressListener
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.widget.Toast
import com.aning.coolweather.db.City
import com.aning.coolweather.db.County
import com.aning.coolweather.db.Province
import com.aning.coolweather.util.HttpUtil
import com.aning.coolweather.util.Utility
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.coroutines.experimental.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.Response
import org.litepal.LitePal
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val _progressBar: Dialog by lazy {
        val alertDialogBuilder = AlertDialog.Builder(this)
                .setView(R.layout.progress);
        alertDialogBuilder.create();
    }

    private lateinit var _locationClient: LocationClient;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissionList = java.util.ArrayList<String>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED)
            permissionList.add(Manifest.permission.READ_PHONE_STATE);

        if (permissionList.any()) {
            ActivityCompat.requestPermissions(this, Array(permissionList.size) { index ->
                permissionList[index]
            }, 1)
        } else {
            this.requestLocation();
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.any()) {
                    for (result in grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意所有权限才能使用", Toast.LENGTH_LONG)
                                    .show();
                            this.finish();
                            return;
                        }
                    }
                    this.requestLocation();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_LONG)
                            .show();
                    this.finish();
                }
            }
        }
    }

    private fun requestLocation() {
        this._locationClient = LocationClient(this.applicationContext);
        this._locationClient.registerLocationListener(this::onReceiveLocation);
        this._locationClient.locOption = LocationClientOption().apply {
            this.setIsNeedAddress(true);
        }
        this._locationClient.start();
    }

    private fun showWeatherActivity(weatherId: String) {
        val intent = Intent(this, WeatherActivity::class.java);
        intent.putExtra("weather_id", weatherId);
        this.startActivity(intent);
        this.finish();
    }

    /**
     * 接收到位置信息
     */
    private fun onReceiveLocation(location: BDLocation) {
        val province = location.province;
        val city = location.city;
        val county = location.district;
        if (province == null || city == null || county == null)
            return;
        launch {
            val weatherId = this@MainActivity.getWeatherId(province, city, county);
            if (weatherId != null) {
                launch(Dispatchers.Main) {
                    this@MainActivity.showWeatherActivity(weatherId);
                    Toast.makeText(this@MainActivity, "当前位置:$province·$city·$county", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    /**
     * 根据省,市,县的名称, 获取对应的 weatherId
     * @param provinceName 省份名称
     * @param cityName 城市名称
     * @param countyName 区/县名称
     */
    private suspend fun getWeatherId(provinceName: String, cityName: String, countyName: String): String? {
        val province = queryProvinces().firstOrNull { it ->
            provinceName.contains(it.provinceName) || it.provinceName.contains(provinceName)
        } ?: return null;

        val city = queryCities(province.id, province.provinceCode).firstOrNull { it ->
            cityName.contains(it.cityName) || it.cityName.contains(cityName);
        } ?: return null;

        val county = queryCounties(city.id, province.provinceCode, city.cityCode).firstOrNull { it ->
            countyName.contains(it.countyName) || it.countyName.contains(countyName);
        } ?: return null;

        return county.weatherId;
    }

    /**
     * 请求全国所有省份
     */
    private suspend fun queryProvinces(): List<Province> {
        val result = LitePal.findAll(Province::class.java);
        return if (result.any()) {
            result
        } else {
            val url = "http://guolin.tech/api/china";
            queryFromServer<Province>(url, "province");
            queryProvinces();
        }
    }

    /**
     * 请求指定省份的城市
     * @param provinceId 指定请求的省份id
     * @param provinceCode 指定请求的provinceCode
     */
    private suspend fun queryCities(provinceId: Int, provinceCode: Int): List<City> {
        val result = LitePal.where("provinceid = ?", provinceId.toString())
                .find(City::class.java);
        return if (result.any()) {
            result;
        } else {
            val url = "http://guolin.tech/api/china/$provinceCode";
            queryFromServer<City>(url, "city", provinceId);
        }
    }

    /**
     * 请求指定城市的区/县
     * @param cityId 指定请求的城市id
     * @param provinceCode 指定请求的provinceCode
     * @param cityCode 指定请求的cityCode
     */
    private suspend fun queryCounties(cityId: Int, provinceCode: Int, cityCode: Int): List<County> {
        val result = LitePal.where("cityid = ?", cityId.toString())
                .find(County::class.java);
        return if (result.any()) {
            result;
        } else {
            val url = "http://guolin.tech/api/china/$provinceCode/$cityCode";
            queryFromServer<County>(url, "county", cityId = cityId);
        }
    }

    /**
     * 从服务端请求省市县数据
     * @param url 指定访问的url
     * @param type 指定请求的数据类型:province, city, county
     * @param provinceId 指定请求的省份id
     * @param cityId 指定请求的城市id
     */
    private suspend inline fun <reified T> queryFromServer(url: String, type: String, provinceId: Int = 0, cityId: Int = 0): List<T> {
        this._progressBar.show();
        try {
            val responseTextTask = async {
                HttpUtil.sendOkHttpRequest(url) ?: String();
            };
            val responseText = responseTextTask.await();
            return when (T::class.java) {
                Province::class.java -> {
                    Utility.handleProvinceResponse(responseText)
                    LitePal.findAll(T::class.java);
                }
                City::class.java -> {
                    Utility.handleCityResponse(responseText, provinceId);
                    LitePal.where("provinceid = ?", provinceId.toString())
                            .find(T::class.java);
                }
                County::class.java -> {
                    Utility.handleCountyResponse(responseText, cityId);
                    LitePal.where("cityid = ?", cityId.toString())
                            .find(T::class.java);
                }
                else -> listOf();
            }
        } catch (e: Exception) {
            e.printStackTrace();
            Toast.makeText(this, "加载失败", Toast.LENGTH_SHORT)
                    .show();
            return listOf();
        } finally {
            this._progressBar.dismiss();
        }
    }

    override fun onDestroy() {
        this._locationClient.stop();
        super.onDestroy()
    }
}
