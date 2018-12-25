package com.example.naziur.weatherapp;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class QueryUtils {
    private QueryUtils() {

    }

    private static List<Weather> extractWeatherFromJson(String weatherJson){
        if(TextUtils.isEmpty(weatherJson))
            return null;

        List<Weather> weatherList = new ArrayList<>();

        try{
            JSONObject baseJsonResponse = new JSONObject(weatherJson);
            JSONObject forcastWeather = baseJsonResponse.getJSONObject("ForecastWeather");
            JSONArray days = forcastWeather.getJSONArray("Days");
            for(int i = 0; i < days.length(); i++) {
                JSONObject currentDay = days.getJSONObject(i);
                String date = currentDay.getString("date");
                double maxTemp = currentDay.getDouble("temp_max_c");
                double minTemp = currentDay.getDouble("temp_min_c");
                JSONArray timeFrame = currentDay.getJSONArray("Timeframes");
                JSONObject currentTimeFrame = timeFrame.getJSONObject(0);
                String description = currentTimeFrame.getString("wx_desc");
                int descriptionCode = currentTimeFrame.getInt("wx_code");
                String imgUrl = currentTimeFrame.getString("wx_icon");
                double currentTemp = currentTimeFrame.getDouble("temp_c");

                weatherList.add(new Weather(date, minTemp, maxTemp, currentTemp, description, imgUrl, descriptionCode));
            }

        } catch (JSONException e) {
            Log.e("QueryUtils", "Problem parsing the source JSON results", e);
        }

        return weatherList;

    }

    public static List<Weather> fetchWeatherData(String requestUrl){
        List<Weather> weathers = extractWeatherFromJson(getHttpResultString(requestUrl));
        return weathers;
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e("QueryUtils", "Problem building the URL ", e);
        }
        return url;
    }

    private static String createHttpRequest (URL url) throws IOException {
        String jsonResponse = "";
        if (url != null) {
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000);
                urlConnection.setConnectTimeout(15000);
                urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                } else{
                    Log.e("QueryUtils", "Error response code: " + urlConnection.getResponseCode());
                }
            } catch (IOException e) {
                Log.e("QueryUtils", "Problem retrieving the JSON results.", e);
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
                if (inputStream != null) inputStream.close();
            }
        }
        return jsonResponse;
    }

    private static String getHttpResultString (String requestUrl) {
        URL url = createUrl(requestUrl);

        String jsonResponse = null;

        try{
            jsonResponse = createHttpRequest(url);
        } catch (IOException e){
            Log.e("QueryUtils", "Problem making the HTTP request");
        }
        return jsonResponse;
    }

    private static String readFromStream (InputStream inputStream) throws IOException{
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line;
            do {
                line = reader.readLine();
                output.append(line);
            } while (line != null);
        }
        return output.toString();
    }


}
