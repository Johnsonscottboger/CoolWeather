package com.aning.coolweather

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.aning.coolweather.gson.Weather
import com.aning.coolweather.util.HttpUtil
import com.aning.coolweather.util.Utility
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_weather.*
import kotlinx.android.synthetic.main.now.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class WeatherActivity : AppCompatActivity() {

    private lateinit var _weatherLayout: ScrollView;
    private lateinit var _titleCity: TextView;
    private lateinit var _titleUpdateTime: TextView;
    private lateinit var _degreeText: TextView;
    private lateinit var _weatherInfoText: TextView;
    private lateinit var _forecastLayout: LinearLayout;
    private lateinit var _aqiText: TextView;
    private lateinit var _pm25Text: TextView;
    private lateinit var _comfortText: TextView;
    private lateinit var _carWashText: TextView;
    private lateinit var _sportText: TextView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 21) {
            val decorView = this.window.decorView;
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            this.window.statusBarColor = Color.TRANSPARENT;
        }
        setContentView(R.layout.activity_weather);

        this._weatherLayout = this.findViewById(R.id.weather_layout);
        this._titleCity = this.findViewById(R.id.title_city);
        this._titleUpdateTime = this.findViewById(R.id.title_update_time);
        this._degreeText = this.findViewById(R.id.degree_text);
        this._weatherInfoText = this.findViewById(R.id.weather_info_text);
        this._forecastLayout = this.findViewById(R.id.forecast_layout);
        this._aqiText = this.findViewById(R.id.aqi_text);
        this._pm25Text = this.findViewById(R.id.pm25_text);
        this._comfortText = this.findViewById(R.id.comfort_text);
        this._carWashText = this.findViewById(R.id.car_wash_text);
        this._sportText = this.findViewById(R.id.sport_text);

        val prefs = PreferenceManager.getDefaultSharedPreferences(this);
        val weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            //已经有缓存
            val weather = Utility.handleWeatherResponse(weatherString);
            this.showWeatherInfo(weather);
        } else {
            //没有缓存, 前往服务器查询
            val weatherId = this.intent.getStringExtra("weather_id");
            this._weatherLayout.visibility = View.INVISIBLE;
            this.requestWeather(weatherId);
        }

        val bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this)
                    .load(bingPic)
                    .into(this.bing_pic_img);
        } else {
            loadBingPic();
        }
    }


    /**
     * 根据天气 id 请求城市天气信息
     */
    private fun requestWeather(weatherId: String) {
        val weatherUrl = "http://guolin.tech/api/weather?cityid=$weatherId&key=24fc4a5349284cefb252760c6d26cceb";
        HttpUtil.sendOkHttpRequest(weatherUrl, object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body()?.string();
                if (responseText.isNullOrEmpty())
                    return;
                else {
                    val weather = Utility.handleWeatherResponse(responseText!!);
                    runOnUiThread {
                        if (weather.status.equals("ok")) {
                            val editor = PreferenceManager.getDefaultSharedPreferences(this@WeatherActivity)
                                    .edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(this@WeatherActivity, "获取天气数据失败", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace();
                runOnUiThread {
                    Toast.makeText(this@WeatherActivity, "获取天气数据失败", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

        loadBingPic();
    }

    /**
     * 处理并显示 Weather 实例中的数据
     */
    private fun showWeatherInfo(weather: Weather) {
        val cityName = weather.basic.cityName;
        val updateTime = weather.basic.update.updateTime.split(" ")[1];
        val degree = "${weather.now.temperature}℃";
        val weatherInfo = weather.now.more.info;

        this._titleCity.text = cityName;
        this._titleUpdateTime.text = updateTime;
        this._degreeText.text = degree;
        this._weatherInfoText.text = weatherInfo;
        this._forecastLayout.removeAllViews();
        for (forecast in weather.forecastList) {
            val view = LayoutInflater.from(this)
                    .inflate(R.layout.forecast_item, this._forecastLayout, false);
            view.findViewById<TextView>(R.id.date_text).text = forecast.date;
            view.findViewById<TextView>(R.id.info_text).text = forecast.more.info;
            view.findViewById<TextView>(R.id.max_text).text = forecast.temperature.max;
            view.findViewById<TextView>(R.id.min_text).text = forecast.temperature.min;
            this._forecastLayout.addView(view);
        }

        this._aqiText.text = weather.aqi.city.aqi;
        this._pm25Text.text = weather.aqi.city.pm25;

        this._comfortText.text = "舒适度: ${weather.suggestion.comfort.info}";
        this._carWashText.text = "洗车指数: ${weather.suggestion.carWash.info}";
        this._sportText.text = "运动建议: ${weather.suggestion.sport.info}";
        this._weatherLayout.visibility = View.VISIBLE;
    }

    /**
     * 加载必应每日一图
     */
    private fun loadBingPic() {
        val requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val bingPic = response.body()?.string();
                val editor = PreferenceManager.getDefaultSharedPreferences(this@WeatherActivity)
                        .edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread {
                    Glide.with(this@WeatherActivity)
                            .load(bingPic)
                            .into(this@WeatherActivity.bing_pic_img);
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace();
            }
        })
    }
}
