package cz.zdenekmacura.weather.android;


import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.util.Log;

import java.io.BufferedReader;

import android.os.AsyncTask;
import android.graphics.drawable.Drawable;

import java.net.URL;

import android.location.Location;
import android.location.LocationManager;

import java.util.*;
import java.lang.reflect.*;
import java.lang.*;
import java.text.*;

public class MainActivity extends ActionBarActivity {

    private Drawable icon;
    private LocationManager locationManager;
    private Hashtable<String, Drawable> iconTable;
    private Class c = R.id.class;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        iconTable = new Hashtable<String, Drawable>();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_forecast:
                weatherForecast();
                return true;
            case R.id.action_weathernow:
                weatherToday();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void weatherToday() {
        findViewById(R.id.weather_today).setVisibility(View.VISIBLE);
        findViewById(R.id.weather_forecast).setVisibility(View.GONE);
        Location location = getCurrentPosition();
        if (location != null) {
            String lon = String.valueOf(location.getLongitude());
            String lat = String.valueOf(location.getLatitude());
            DownloadWebPageTask task = new DownloadWebPageTask();
            task.execute(new String[]{"http://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon});
        }

    }


    public Location getCurrentPosition() {
        this.locationManager = ((LocationManager) getSystemService(Context.LOCATION_SERVICE));
        Location location = this.locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) {
            return location;
        }
        Log.d("activity", "no last known location");
        return null;
    }

    public void weatherForecast() {
        findViewById(R.id.weather_today).setVisibility(View.GONE);
        findViewById(R.id.weather_forecast).setVisibility(View.VISIBLE);
        Location location = getCurrentPosition();
        if (location != null) {
            String lon = String.valueOf(location.getLongitude());
            String lat = String.valueOf(location.getLatitude());
            DownloadWebPageTask task = new DownloadWebPageTask();
            task.execute(new String[]{"http://api.openweathermap.org/data/2.5/forecast/daily?lat=" + lat + "&lon=" + lon + "&cnt=10&mode=json"});
        }
    }


    public void displayWeatherToday(JSONObject jsonObj) {
        try {
            //get name of the country

            JSONObject sys = jsonObj.getJSONObject("sys");
            String country = sys.getString("country");
            String name = jsonObj.getString("name");
            ((TextView) findViewById(R.id.country)).setText(country + ", " + name);
            //get temperature, humidity, pressure and wind

            JSONObject main = jsonObj.getJSONObject("main");
            String temp_string = main.getString("temp");
            String humidity = main.getString("humidity");
            String pressure = main.getString("pressure");
            JSONObject wind = jsonObj.getJSONObject("wind");
            String speed = wind.getString("speed");
            // calculate temperature

            double temp = Float.valueOf(temp_string).doubleValue();
            double temp_zero = 273.15;
            String temp_celsius = String.format("%.2f", new Object[]{Double.valueOf(temp - temp_zero)});
            // get icon file name and weather description

            JSONArray weather = jsonObj.getJSONArray("weather");
            JSONObject iconObj = weather.getJSONObject(0);
            String icon = iconObj.getString("icon");
            String description = iconObj.getString("description");
            //set the text values for the TextViews

            ((TextView) findViewById(R.id.description)).setText(temp_celsius + "°C | " + description);
            ((TextView) findViewById(R.id.humidity)).setText(humidity + "%");
            ((TextView) findViewById(R.id.pressure)).setText(pressure + " hPa");
            ((TextView) findViewById(R.id.wind)).setText(speed + " km/h");
            //download weather icon from the open weather website

            String iconUrl = "http://openweathermap.org/img/w/" + icon + ".png";
            DownloadImageTask task = new DownloadImageTask();
            task.execute(new String[]{iconUrl});

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void displayForecast(JSONObject jsonObj) {
        try {
            //get country and city name and display on the top

            JSONObject city = jsonObj.getJSONObject("city");
            String country = city.getString("country");
            String name = city.getString("name");
            ((TextView) findViewById(R.id.countryforecast)).setText(country + ", " + name);
            //get list of forecast just for next 7 days

            JSONArray list = jsonObj.getJSONArray("list");
            if (list == null) {
                ((TextView) findViewById(R.id.countryforecast)).setText("Sorry, but the data are not now available");
            }
            for (int i = 0; i < 7; i++) {
                //get timestamp

                JSONObject listObj = list.getJSONObject(i);
                String timeStamp = listObj.getString("dt");
                // get weather object

                JSONArray weather = listObj.getJSONArray("weather");
                JSONObject weatherObj = weather.getJSONObject(0);
                //get weather icon file name and download

                String icon = weatherObj.getString("icon");
                String iconUrl = "http://openweathermap.org/img/w/" + icon + ".png?day=" + i;
                DownloadImageTask task = new DownloadImageTask();
                task.execute(new String[]{iconUrl});
                //get temperature and calculate it

                JSONObject tempObj = listObj.getJSONObject("temp");
                String temp_string = tempObj.getString("day");
                double temp = Float.valueOf(temp_string).doubleValue();
                double temp_zero = 273.15;
                String temp_celsius = String.format("%.2f", new Object[]{Double.valueOf(temp - temp_zero)});
                //get description of the weather

                String description = weatherObj.getString("description");
                //get week day from the timestamp

                Date dateTime = new Date(Long.valueOf(timeStamp) * 1000L);
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE", Locale.US);
                String asWeek = dateFormat.format(dateTime);
                //display week day and description of the weather

                try {
                    Field fieldDescription = c.getField("description" + i);
                    Field fieldWeek = c.getField("asWeek" + i);
                    ((TextView) findViewById(fieldDescription.getInt(c))).setText(temp_celsius + "°C | " + description);
                    ((TextView) findViewById(fieldWeek.getInt(c))).setText(asWeek);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void displayIcon(String url) {
        Drawable icon = iconTable.get(url);
        String iconId = "imageIcon";
        if (url.contains("=")) iconId += url.substring(url.indexOf("=") + 1);
        try {
            Field f1 = c.getField(iconId);
            ((ImageView) findViewById(f1.getInt(c))).setImageDrawable(icon);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private class DownloadWebPageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String response = "";
            for (String url : urls) {
                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);
                try {
                    HttpResponse execute = client.execute(httpGet);
                    InputStream content = execute.getEntity().getContent();

                    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                    String s = "";
                    while ((s = buffer.readLine()) != null) {
                        response += s;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObj = new JSONObject(result);
                if (jsonObj.has("sys")) {
                    displayWeatherToday(jsonObj);
                } else {
                    displayForecast(jsonObj);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String url = urls[0];
            try {
                InputStream is = (InputStream) new URL(url).getContent();
                icon = Drawable.createFromStream(is, url);
                iconTable.put(url, icon);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return url;

        }

        @Override
        protected void onPostExecute(String url) {
            displayIcon(url);
        }
    }
}
