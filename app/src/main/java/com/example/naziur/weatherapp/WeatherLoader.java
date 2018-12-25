package com.example.naziur.weatherapp;

import android.content.AsyncTaskLoader;
import android.content.Context;

import java.util.List;
public class WeatherLoader extends AsyncTaskLoader<List<Weather>> {

    private String mUrl;

    public WeatherLoader (Context context, String url) {
        super(context);
        mUrl = url;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public List<Weather> loadInBackground() {
        if (mUrl == null) return null;
        List<Weather> allWeathers = QueryUtils.fetchWeatherData(mUrl);
        return allWeathers;
    }

}
