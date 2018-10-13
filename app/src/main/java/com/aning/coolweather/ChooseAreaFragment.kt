package com.aning.coolweather

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.aning.coolweather.db.City
import com.aning.coolweather.db.County
import com.aning.coolweather.db.Province
import com.aning.coolweather.util.HttpUtil
import com.aning.coolweather.util.Utility
import kotlinx.android.synthetic.main.choose_area.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.litepal.LitePal
import org.litepal.crud.LitePalSupport
import java.io.IOException

public class ChooseAreaFragment : Fragment() {

    private val _progressBar: Dialog by lazy {
        val alertDialogBuilder = AlertDialog.Builder(context)
                .setView(R.layout.progress);
        alertDialogBuilder.create();
    }

    private lateinit var _titleText: TextView;

    private lateinit var _backButton: Button;

    private lateinit var _listView: ListView;

    private lateinit var _adapter: ArrayAdapter<String>;

    private var _dataList: ArrayList<String> = ArrayList();

    private var _currentLevel = 0;

    private lateinit var _provinceList: List<Province>;

    private lateinit var _cityList: List<City>;

    private lateinit var _countyList: List<County>;

    private lateinit var _selectedProvince: Province;

    private lateinit var _selectedCity: City;

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.choose_area, container, false);
        this._listView = view.findViewById(R.id.list_view);
        this._titleText = view.findViewById(R.id.title_text);
        this._backButton = view.findViewById(R.id.back_button);

        this._adapter = ArrayAdapter(this.context!!, android.R.layout.simple_list_item_1, this._dataList);
        this._listView.adapter = this._adapter;
        return view;
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState);
        this._listView.setOnItemClickListener { _, _, position, _ ->
            when (this._currentLevel) {
                LEVEL_PROVINCE -> {
                    this._selectedProvince = this._provinceList.get(position);
                    queryCities();
                }
                LEVEL_CITY -> {
                    this._selectedCity = this._cityList.get(position);
                    queryCounties();
                }

                LEVEL_COUNTY -> {
                    val weatherId = this._countyList[position].weatherId;
                    when (activity) {
                        is MainActivity -> {
                            val intent = Intent(activity, WeatherActivity::class.java);
                            intent.putExtra("weather_id", weatherId);
                            this.startActivity(intent);
                            activity?.finish();
                        }
                        is WeatherActivity -> {
                            (activity as WeatherActivity).switchCounty(weatherId);
                        }
                    }
                }
            }
        }

        this._backButton.setOnClickListener { _ ->
            when (this._currentLevel) {
                LEVEL_COUNTY -> queryCities();
                LEVEL_CITY -> queryProvinces();
            }
        }
        queryProvinces();
    }

    /**
     * 查询全国所有的省, 优先从数据库查询, 如果没有查询到再去服务器上查询
     */
    private fun queryProvinces() {
        this._titleText.text = "中国";
        this._backButton.visibility = View.GONE;
        this._provinceList = LitePal.findAll(Province::class.java);
        if (this._provinceList.any()) {
            this._dataList.clear();
            for (province in this._provinceList) {
                this._dataList.add(province.provinceName);
            }
            this._adapter.notifyDataSetChanged();
            this._listView.setSelection(0);
            this._currentLevel = LEVEL_PROVINCE;
        } else {
            val url = "http://guolin.tech/api/china";
            queryFromServer(url, "province");
        }
    }

    /**
     * 查询选中省内所有的市, 优先从数据库查询, 如果没有查询到再去服务器上查询
     */
    private fun queryCities() {
        this._titleText.text = this._selectedProvince.provinceName;
        this._backButton.visibility = View.VISIBLE;
        this._cityList = LitePal.where("provinceid = ?", this._selectedProvince.id.toString())
                .find(City::class.java);
        if (this._cityList.any()) {
            this._dataList.clear();
            for (city in this._cityList) {
                this._dataList.add(city.cityName);
            }
            this._adapter.notifyDataSetChanged();
            this._listView.setSelection(0);
            this._currentLevel = LEVEL_CITY;
        } else {
            val provinceCode = this._selectedProvince.provinceCode;
            val url = "http://guolin.tech/api/china/$provinceCode";
            queryFromServer(url, "city");
        }
    }

    /**
     * 查询选中市内所有的县或区, 优先从数据库中查询, 如果没有查询到再去服务器上查询
     */
    private fun queryCounties() {
        this._titleText.text = this._selectedCity.cityName;
        this._backButton.visibility = View.VISIBLE;
        this._countyList = LitePal.where("cityid = ?", this._selectedCity.id.toString())
                .find(County::class.java);
        if (this._countyList.any()) {
            this._dataList.clear();
            for (county in this._countyList) {
                this._dataList.add(county.countyName);
            }
            this._adapter.notifyDataSetChanged();
            this._listView.setSelection(0);
            this._currentLevel = LEVEL_COUNTY;
        } else {
            val provinceCode = this._selectedProvince.provinceCode;
            val cityCode = this._selectedCity.cityCode;
            val url = "http://guolin.tech/api/china/$provinceCode/$cityCode";
            queryFromServer(url, "county");
        }
    }

    private fun queryFromServer(url: String, type: String) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(url, object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseText = response.body()?.string();
                val result = when (type) {
                    "province" -> Utility.handleProvinceResponse(responseText!!);
                    "city" -> Utility.handleCityResponse(responseText!!, _selectedProvince.id);
                    "county" -> Utility.handleCountyResponse(responseText!!, _selectedCity.id);
                    else -> false;
                }
                if (result) {
                    activity?.runOnUiThread {
                        closeProgressDialog();
                        when (type) {
                            "province" -> queryProvinces();
                            "city" -> queryCities();
                            "county" -> queryCounties();
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace();
                activity?.runOnUiThread {
                    closeProgressDialog();
                    Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private fun showProgressDialog() {
        this._progressBar.show();
    }

    private fun closeProgressDialog() {
        this._progressBar.dismiss();
    }

    companion object {
        public const val LEVEL_PROVINCE = 0;

        public const val LEVEL_CITY = 1;

        public const val LEVEL_COUNTY = 2;
    }
}