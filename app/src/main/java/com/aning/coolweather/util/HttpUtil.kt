package com.aning.coolweather.util

import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request

public class HttpUtil {
    companion object {
        public fun sendOkHttpRequest(url: String, callback: Callback) {
            val client = OkHttpClient();
            val request = Request.Builder()
                    .url(url)
                    .build();
            client.newCall(request)
                    .enqueue(callback);
        }

        public fun sendOkHttpRequest(url: String): String? {
            val client = OkHttpClient();
            val request = Request.Builder()
                    .url(url)
                    .build();
            val response = client.newCall(request)
                    .execute();
            return response.body()?.string();
        }
    }
}