package com.example.crystalweather;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private String apiKey = "503062500168723a2cf9ebde05cf59ea";
    private String defaultZIP = "08852";

    ImageView mainWeatherImage;
    TextView mainTemperature;
    TextView mainHighTemp;
    TextView mainLowTemp;
    TextView cityName;
    TextView mainWeatherConditions;
    TextView mainFeelsLike;
    TextView quote;
    EditText zipCodeText;
    Button submitZipCode;
    LinearLayout zipCodeLayout;
    LinearLayout hourWeatherContainer;
    TextView windSpeed;
    TextView precipitation;
    TextView humidity;
    TextView cloudiness;
    TextView sunriseTime;
    TextView sunriseLabel;
    TextView sunsetTime;
    TextView sunsetLabel;
    HashMap<String,Integer> icons;
    HashMap<String, String[]> quotes;
    LayoutInflater layoutInflater;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        mainWeatherImage = findViewById(R.id.mainWeatherImage);
        mainTemperature = findViewById(R.id.mainTemperature);
        mainHighTemp = findViewById(R.id.mainHighTemp);
        mainLowTemp = findViewById(R.id.mainLowTemp);
        cityName = findViewById(R.id.cityName);
        mainWeatherConditions = findViewById(R.id.mainWeatherConditions);
        hourWeatherContainer = findViewById(R.id.hourWeatherContainer);
        mainFeelsLike = findViewById(R.id.mainFeelsLike);
        quote = findViewById(R.id.quote);
        zipCodeText = findViewById(R.id.zipCode);
        submitZipCode = findViewById(R.id.submitZip);
        zipCodeLayout = findViewById(R.id.zipCodeLayout);
        windSpeed = findViewById(R.id.windSpeed);
        precipitation = findViewById(R.id.rainAmount);
        humidity = findViewById(R.id.humidityAmount);
        cloudiness = findViewById(R.id.cloudiness);
        sunriseTime = findViewById(R.id.sunrise);
        sunriseLabel = findViewById(R.id.sunriseSub);
        sunsetTime = findViewById(R.id.sunset);
        sunsetLabel = findViewById(R.id.sunsetSub);

        layoutInflater = LayoutInflater.from(this);

        // Hide the action bars
        getSupportActionBar().hide();

        // Sets up icons with OpenWeather icon keys
        icons =new HashMap<String,Integer>();
        icons.put("01d", R.drawable.icon_01d);
        icons.put("01n", R.drawable.icon_01n);
        icons.put("02d", R.drawable.icon_02d);
        icons.put("02n", R.drawable.icon_02n);
        icons.put("03d", R.drawable.icon_03d);
        icons.put("03n", R.drawable.icon_03n);
        icons.put("04d", R.drawable.icon_04d);
        icons.put("04n", R.drawable.icon_04n);
        icons.put("09d", R.drawable.icon_09d);
        icons.put("09n", R.drawable.icon_09n);
        icons.put("10d", R.drawable.icon_10d);
        icons.put("10n", R.drawable.icon_10n);
        icons.put("11d", R.drawable.icon_11d);
        icons.put("11n", R.drawable.icon_11n);
        icons.put("13d", R.drawable.icon_13d);
        icons.put("13n", R.drawable.icon_13n);
        icons.put("50d", R.drawable.icon_50d);
        icons.put("50n", R.drawable.icon_50n);

        quotes = new HashMap<String, String[]>();
        quotes.put("Thunderstorm", new String[]{
                "Hopefully there is no rainbow",
                "Looks like Zues is depressed"
        });

        quotes.put("Drizzle", new String[]{
                "Rain but worse"
        });

        quotes.put("Rain", new String[]{
                "Let the rain wash away your sins"
        });

        quotes.put("Snow", new String[]{
                "Snow = Happiness, not good"
        });

        quotes.put("Atmosphere", new String[]{
                "The air will suffocate you, I'm not even lying"
        });

        quotes.put("Clear", new String[]{
                "We need more clouds",
                "It's too bright out there"
        });

        quotes.put("Clouds", new String[]{
                "The best weather, dark and gloomy",
                "It's dark out there"
        });

        // Execute AsyncThread that gets and sets weather conditions
        AsyncThread asyncThread = new AsyncThread();
        asyncThread.execute(defaultZIP);

        zipCodeText.setHint(defaultZIP);
        submitZipCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zipCodeLayout.setVisibility(View.GONE);
                AsyncThread asyncThread = new AsyncThread();
                asyncThread.execute(zipCodeText.getText().toString());
                hideKeyboard();
                zipCodeText.setHint(zipCodeText.getText().toString());
                zipCodeText.setText("");
            }
        });

        cityName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(zipCodeLayout.getVisibility() == View.VISIBLE){
                    zipCodeLayout.setVisibility(View.GONE);
                    hideKeyboard();
                }
                else{
                    zipCodeLayout.setVisibility(View.VISIBLE);
                    showKeyboard();
                }
            }
        });


    }

    public void hideKeyboard(){
        View view = zipCodeText;
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void showKeyboard(){
        View view = zipCodeText;
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    public static String capitalize(String phrase){
        String words[] = phrase.split("\\s+");
        String capitalized = "";
        for(String word:words){
            String firstLetter = word.substring(0,1).toUpperCase();
            String restOfWord = word.substring(1);
            capitalized += firstLetter + restOfWord + " ";
        }
        return capitalized.trim();
    }


    public class AsyncThread extends AsyncTask<String, Void, Void> {
        // Current weatherData
        JSONObject currentWeatherData;
        JSONObject hourlyWeatherData;

        @Override
        protected Void doInBackground(String... strings) {
            String zip = strings[0];

            try {
                URL weatherDataURL = new URL("https://api.openweathermap.org/data/2.5/weather?zip=" + zip +"&appid=" + apiKey + "&units=imperial");
                currentWeatherData = getAPIInfo(weatherDataURL);

                URL hourlyWeatherURL = new URL("https://api.openweathermap.org/data/2.5/forecast?zip=" + zip +"&appid=" + apiKey + "&units=imperial");
                hourlyWeatherData = getAPIInfo(hourlyWeatherURL);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            return null;
        }

        // Take in URL and returns JSONObject with information
        private JSONObject getAPIInfo(URL url){
            try{
                URLConnection urlConnection = url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while( (line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                return new JSONObject(stringBuilder.toString());

            }
            catch(Exception e){
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Invalid Zip Code or Error Retrieving Data", Toast.LENGTH_SHORT).show();
                    }
                });
                return null;
            }
        }

        // Run after AsyncTask finishes
        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            try{
                ///////////////////// CURRENT WEATHER /////////////////////
                String timezone = hourlyWeatherData.getJSONObject("city").get("timezone").toString();
                int timezoneOffset = currentWeatherData.getInt("timezone");
                DateFormat format = new SimpleDateFormat("h a");
                format.setTimeZone(TimeZone.getTimeZone("Etc/GMT"));

                DateFormat formatTimeOnly = new SimpleDateFormat("h:mm");
                formatTimeOnly.setTimeZone(TimeZone.getTimeZone("Etc/GMT"));

                DateFormat formatMarkerOnly = new SimpleDateFormat("a");
                formatMarkerOnly.setTimeZone(TimeZone.getTimeZone("Etc/GMT"));

                // Get the current temperature, convert from K to F and then set the text
                Double temp = Double.valueOf(currentWeatherData.getJSONObject("main").get("temp").toString());
                mainTemperature.setText((int) Math.round(temp) + "°");

                // Get the minimum temperature, convert from K to F and then set the text
                Double minTemp = Double.valueOf(currentWeatherData.getJSONObject("main").get("temp_min").toString());
                mainLowTemp.setText("L:" + (int) Math.round(minTemp) + "°");

                // Get the maximum temperature, convert from K to F and then set the text
                Double maxTemp = Double.valueOf(currentWeatherData.getJSONObject("main").get("temp_max").toString());
                mainHighTemp.setText("H:" + (int) Math.round(maxTemp) + "°");

                // Get the icon ID from OpenWeather and then get the corresponding image from "icons" hashmap
                String imageID = currentWeatherData.getJSONArray("weather").getJSONObject(0).get("icon").toString();
                mainWeatherImage.setImageResource(icons.get(imageID));

                // Get the icon ID from OpenWeather and then get the corresponding image from "icons" hashmap
                String weatherCondition = currentWeatherData.getJSONArray("weather").getJSONObject(0).get("description").toString();
                mainWeatherConditions.setText(capitalize(weatherCondition));

                Double feelsLike = Double.valueOf(currentWeatherData.getJSONObject("main").get("feels_like").toString());
                mainFeelsLike.setText("Feels Like " + (int)Math.round(feelsLike) + "°");

                String city = currentWeatherData.get("name").toString();
                cityName.setText(city);

                String mainCondition = currentWeatherData.getJSONArray("weather").getJSONObject(0).get("main").toString();
                quote.setText("\"" + quotes.get(mainCondition)[(int) Math.floor(Math.random() * quotes.get(mainCondition).length)] + "\"");

                Double windSpeedValue = currentWeatherData.getJSONObject("wind").getDouble("speed");
                windSpeed.setText((int)Math.round(windSpeedValue) + "");

                if(currentWeatherData.has("rain") && currentWeatherData.getJSONObject("rain").has("1h")){
                    Double precipitiationAmount = currentWeatherData.getJSONObject("rain").getDouble("1h");
                    precipitation.setText(Math.round( (precipitiationAmount / 25.4) * 100)/100.0 + "");
                }
                else{
                    precipitation.setText("0 in");
                }

                Double humidityAmount = currentWeatherData.getJSONObject("main").getDouble("humidity");
                humidity.setText((int)Math.round(humidityAmount) + "");

                Double cloudAmount = currentWeatherData.getJSONObject("clouds").getDouble("all");
                cloudiness.setText((int)Math.round(cloudAmount) + "");

                int sunset = currentWeatherData.getJSONObject("sys").getInt("sunset") + timezoneOffset;
                int sunrise = currentWeatherData.getJSONObject("sys").getInt("sunrise") + timezoneOffset;
                //time = format.format(new Date(epochTime*1000)).toString();
                sunriseTime.setText(formatTimeOnly.format(new Date(sunrise*1000)).toString());
                sunriseLabel.setText(formatMarkerOnly.format(new Date(sunrise*1000)).toString());
                sunsetTime.setText(formatTimeOnly.format(new Date(sunset*1000)).toString());
                sunsetLabel.setText(formatMarkerOnly.format(new Date(sunset*1000)).toString());



                hourWeatherContainer.removeAllViews();

                LinearLayout currentWeatherLayout = (LinearLayout) layoutInflater.inflate(R.layout.hourweather, null);
                ((TextView)currentWeatherLayout.findViewWithTag("time")).setText("Now");
                ((TextView)currentWeatherLayout.findViewWithTag("highTemp")).setText("H:" + (int) Math.round(maxTemp) + "°");
                ((TextView)currentWeatherLayout.findViewWithTag("lowTemp")).setText("L:" + (int) Math.round(minTemp) + "°");
                ((ImageView)currentWeatherLayout.findViewWithTag("image")).setImageResource(icons.get(imageID));
                hourWeatherContainer.addView(currentWeatherLayout);

                ///////////////////// HOURLY WEATHER /////////////////////

                JSONArray hourlyArray = hourlyWeatherData.getJSONArray("list");



                int count = 10;
                for(int i = 0; i<hourlyArray.length(); i++){
                    if(i > count)
                        break;
                    JSONObject currentData = hourlyArray.getJSONObject(i);
                    Double hourlyMaxTemp = Double.valueOf(currentData.getJSONObject("main").get("temp_max").toString());
                    Double hourlyMinTemp = Double.valueOf(currentData.getJSONObject("main").get("temp_min").toString());
                    String hourlyImage = currentData.getJSONArray("weather").getJSONObject(0).get("icon").toString();
                    String time = currentData.get("dt").toString();
                    long epochTime = Integer.valueOf(time) + Integer.valueOf(timezone);
                    time = format.format(new Date(epochTime*1000)).toString();

                    LinearLayout hourlyWeatherLayout = (LinearLayout) layoutInflater.inflate(R.layout.hourweather, null);
                    ((TextView)hourlyWeatherLayout.findViewWithTag("time")).setText(time);
                    ((TextView)hourlyWeatherLayout.findViewWithTag("highTemp")).setText("H:" + (int) Math.round(hourlyMaxTemp) + "°");
                    ((TextView)hourlyWeatherLayout.findViewWithTag("lowTemp")).setText("L:" + (int) Math.round(hourlyMinTemp) + "°");
                    ((ImageView)hourlyWeatherLayout.findViewWithTag("image")).setImageResource(icons.get(hourlyImage));
                    hourWeatherContainer.addView(hourlyWeatherLayout);
                }


            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}