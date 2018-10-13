package com.aning.coolweather.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.preference.PreferenceManager
import com.aning.coolweather.util.HttpUtil
import com.aning.coolweather.util.Utility
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class AutoUpdateWeatherService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateWeather();
        updateBingPic();
        val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        val interval = 8 * 60 * 60 * 1000;
        val triggerAtTime = SystemClock.elapsedRealtime() + interval;
        val i = Intent(this, AutoUpdateWeatherService::class.java);
        val pendingIntent = PendingIntent.getService(this, 0, i, 0);
        alarmManager.cancel(pendingIntent);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pendingIntent);
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 更新天气信息
     */
    private fun updateWeather() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this);
        val weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            val weather = Utility.handleWeatherResponse(weatherString);
            val weatherId = weather.basic.weatherId;
            val weatherUrl = "http://guolin.tech/api/weather?cityid=$weatherId&key=24fc4a5349284cefb252760c6d26cceb";
            HttpUtil.sendOkHttpRequest(weatherUrl, object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val responseText = response.body()?.string() ?: return;
                    val w = Utility.handleWeatherResponse(responseText);
                    if (w.status == "ok") {
                        val editor = PreferenceManager.getDefaultSharedPreferences(this@AutoUpdateWeatherService)
                                .edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * 更新必应每日一图
     */
    private fun updateBingPic() {
        val requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val bingPic = response.body()?.string() ?: return;
                val editor = PreferenceManager.getDefaultSharedPreferences(this@AutoUpdateWeatherService)
                        .edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace();
            }
        })
    }
}
