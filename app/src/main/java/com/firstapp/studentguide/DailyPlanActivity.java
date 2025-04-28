package com.firstapp.studentguide;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DailyPlanActivity extends AppCompatActivity {

    private Spinner spinnerCountry, spinnerCity;
    private CheckBox checkGeneral, checkCulture, checkNature, checkFoodie, checkShopping, checkNightlife;
    private TimePicker timePickerFrom, timePickerTo;
    private Button buttonGeneratePlan, buttonClearPlan, buttonModifyPlan, buttonSeeLocations;
    private TextView textViewGeneratedPlan;
    private ProgressBar progressBar;

    private static final String REST_COUNTRIES_API_URL = "https://restcountries.com/v3.1/all";

    private final Map<String, String> countryCodeMap = new HashMap<>();
    private GenerativeModelFutures modelFutures;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private LocationManager locationManager;
    private boolean returningFromLocationSettings = false;
    private ImageView settingsButton, homeButton;
    private LinearLayout planIntro;

    private OkHttpClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dailyplan_activity);

        Log.d("DailyPlanActivity", "onCreate called");
        initializeViews();
        setupGeminiModel();
        setupButtonListeners();
        spinnerCountry.setEnabled(false);
        spinnerCity.setEnabled(false);
        spinnerCity.setAdapter(null);


        try {
            client = OkHttpHelper.getClient();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            Log.e("OkHttp", "Error creating OkHttpClient with TLS 1.3", e);
            Toast.makeText(this, "Error initializing network client", Toast.LENGTH_LONG).show();
            return;
        }

        loadCountries();


        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dailyplan), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (returningFromLocationSettings) {
                    if (isLocationEnabled()) {
                        openGoogleMaps();
                    }
                    returningFromLocationSettings = false;
                } else {
                    if (isTaskRoot()) {
                        showExitConfirmation();
                    } else {
                        finish();
                    }
                }
            }
        });
    }

    private void initializeViews() {
        spinnerCountry = findViewById(R.id.spinnerCountry);
        spinnerCity = findViewById(R.id.spinnerCity);
        checkGeneral = findViewById(R.id.checkGeneral);
        checkCulture = findViewById(R.id.checkCulture);
        checkNature = findViewById(R.id.checkNature);
        checkFoodie = findViewById(R.id.checkFoodie);
        checkShopping = findViewById(R.id.checkShopping);
        checkNightlife = findViewById(R.id.checkNightlife);
        timePickerFrom = findViewById(R.id.timePickerFrom);
        timePickerTo = findViewById(R.id.timePickerTo);
        buttonGeneratePlan = findViewById(R.id.buttonGeneratePlan);
        buttonClearPlan = findViewById(R.id.buttonClearPlan);
        buttonModifyPlan = findViewById(R.id.buttonModifyPlan);
        textViewGeneratedPlan = findViewById(R.id.textViewGeneratedPlan);
        progressBar = findViewById(R.id.progressBar);
        settingsButton = findViewById(R.id.settings_button);
        homeButton = findViewById(R.id.home_button);
        timePickerFrom.setIs24HourView(true);
        timePickerTo.setIs24HourView(true);
        buttonSeeLocations = findViewById(R.id.buttonSeeLocations);
        planIntro= findViewById(R.id.plan_intro);
    }

//setup gemini api -----------------------------------------------------------------------------
    private void setupGeminiModel() {
        String apiKey = BuildConfig.API_KEY;
        GenerativeModel model = new GenerativeModel("gemini-2.0-flash", apiKey);
        modelFutures = GenerativeModelFutures.from(model);
    }
//------------------------------------------------------------------------------------
    private void setupButtonListeners() {
        buttonGeneratePlan.setOnClickListener(v -> generatePlan());
        buttonClearPlan.setOnClickListener(v -> clearPlan());
        buttonModifyPlan.setOnClickListener(v -> modifyPlan());
        buttonSeeLocations.setOnClickListener(v -> displayLocationLinks());
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(DailyPlanActivity.this, MainActivity.class);
            startActivity(intent);
        });
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(DailyPlanActivity.this, SettingsActivity.class);
            startActivity(intent);
        });




    }

    @SuppressLint("SetTextI18n")
    private void displayLocationLinks() {
        if (!isLocationEnabled()) {
            showLocationSettingsDialog();
        } else {
            openGoogleMaps();
        }
    }


    private void loadCountries() {
        showProgress(true);
        Request request = new Request.Builder()
                .url(REST_COUNTRIES_API_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showError("Couldn't load countries. Please reopen the app");
                    Log.e("API_ERROR", "Country load error", e);
                    showProgress(false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                assert response.body() != null;
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONArray countries = new JSONArray(responseBody);
                        ArrayList<String> countryList = new ArrayList<>();
                        countryList.add("Select a Country");

                        ArrayList<String> countryNames = new ArrayList<>();
                        for (int i = 0; i < countries.length(); i++) {
                            JSONObject country = countries.getJSONObject(i);
                            JSONObject name = country.getJSONObject("name");
                            String countryName = name.getString("common");
                            if (!countryName.equals("Select a Country")) {
                                countryNames.add(countryName);
                            }
                            if (country.has("cca2")) {
                                countryCodeMap.put(countryName, country.getString("cca2"));
                            }
                        }
                        Collections.sort(countryNames);
                        countryList.addAll(countryNames);
                        setupSpinner(spinnerCountry, countryList);
                        spinnerCountry.setEnabled(true);

                        spinnerCountry.setSelection(0, false);

                        spinnerCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                String selectedCountry = parent.getItemAtPosition(position).toString();

                                if (position == 0 || selectedCountry.equals("Select a Country")) {
                                    spinnerCity.setAdapter(null);
                                    return;
                                }

                                if (countryCodeMap.containsKey(selectedCountry)) {
                                    loadCitiesWithOkHttp(selectedCountry); // Always call this to refresh cities
                                    spinnerCity.setEnabled(true);
                                } else {
                                    Log.w("SPINNER_FLOW", "No country code found for: " + selectedCountry);
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                                spinnerCity.setAdapter(null);
                                Log.d("SPINNER_FLOW", "Nothing selected in country spinner");
                            }
                        });

                    } catch (JSONException e) {
                        showError("Error parsing countries");
                        Log.e("API_ERROR", "Country parsing error", e);
                    } finally {
                        showProgress(false);
                    }
                });
            }
        });
    }




    private void loadCitiesWithOkHttp(String countryName) {
        showProgress(true);
        String countryCode = countryCodeMap.get(countryName);
        if (countryCode == null || countryCode.isEmpty()) {
            showError("Country code not available");
            Log.e("LOAD_CITIES", "Missing code for: " + countryName);
            showProgress(false);
            return;
        }

        String url = String.format(BuildConfig.CITIES_URL, countryCode);
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showError("Couldn't load cities. Please reopen the app");
                    Log.e("API_ERROR", "City load error", e);
                    Toast.makeText(DailyPlanActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                assert response.body() != null;
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    try {
                        // 🔵 Step 1: Clear spinner immediately while waiting for parsing
                        ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(DailyPlanActivity.this, android.R.layout.simple_spinner_item, new ArrayList<>());
                        emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerCity.setAdapter(emptyAdapter);
                        spinnerCity.setEnabled(false);

                        // 🔵 Step 2: Now parse the response
                        if (responseBody == null || responseBody.isEmpty()) {
                            showError("Empty response from server");
                            return;
                        }

                        JSONObject json = new JSONObject(responseBody);

                        if (json.has("status")) {
                            JSONObject status = json.getJSONObject("status");
                            String errorMsg = status.getString("message");
                            throw new JSONException("GeoNames error: " + errorMsg);
                        }

                        JSONArray cities = json.getJSONArray("geonames");
                        ArrayList<String> cityList = new ArrayList<>();

                        for (int i = 0; i < cities.length(); i++) {
                            cityList.add(cities.getJSONObject(i).getString("name"));
                        }

                        // 🔵 Step 3: Fill the spinner if we have cities
                        if (!cityList.isEmpty()) {
                            Log.d("CITY_LOAD", "Before setupSpinner: " + cityList.size() + " cities");
                            Collections.sort(cityList);
                            setupSpinner(spinnerCity, cityList);
                            spinnerCity.setEnabled(true);
                            Log.d("CITY_LOAD", "After setupSpinner");
                        } else {
                            showError("No cities found for this country.");
                            spinnerCity.setAdapter(null);
                            spinnerCity.setEnabled(false);
                        }

                    } catch (JSONException e) {
                        showError("City data error");
                        Log.e("API_ERROR", "City parsing error", e);
                    } finally {
                        showProgress(false);
                    }
                });
            }
        });
    }




    private void generatePlan() {
        if (spinnerCountry.getSelectedItem() == null || spinnerCity.getSelectedItem() == null) {
            showError("Please select both a country and city");
            return;
        }

        if (!isValidTimeRange()) {
            showError("End time must be after start time");
            return;
        }

        String country = spinnerCountry.getSelectedItem().toString();
        String city = spinnerCity.getSelectedItem().toString();
        String interests = getSelectedInterests();

        int fromHour = timePickerFrom.getHour();
        int fromMinute = timePickerFrom.getMinute();
        int toHour = timePickerTo.getHour();
        int toMinute = timePickerTo.getMinute();

        @SuppressLint("DefaultLocale") String prompt =
                "Generate EXACTLY 3-4 itinerary items for " + city + ", " + country + ".\n" +
                        "FORMAT RULES:\n" +
                        "1. Each line must start with '• ' then follwed by an emoji for the interest than followed by time\n" +
                        "2. Time format: HH:MM in 24-hour style\n" +
                        "3. After time, write 'Visit ' followed by a real location \n" +
                        "4. Example:\n" +
                        "• emoji 09:00 Visit Colosseum\n" +
                        "• emoji 11:30 Visit Trevi Fountain\n\n" +
                        "TRIP DETAILS:\n" +
                        "- Interests: " + interests + "\n" +
                        "- Time: " + String.format("%02d:%02d", fromHour, fromMinute) +
                        " to " + String.format("%02d:%02d", toHour, toMinute) + "\n" +
                        "- Pace: Relaxed (include travel time)\n\n" +
                        "IMPORTANT: ONLY OUTPUT THE 3-4 ITEMS, NOTHING ELSE! and make sure to use the word Visit only not other words like lunch or scroll or something and after the word Visit i want only the location name";

        showProgress(true);

        //prompt request---------------------------------------------------------------------------
        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);
//------------------------------------------------------------------------------------------------------
        Futures.addCallback(response, new FutureCallback<>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    try {
                        planIntro.setVisibility(View.VISIBLE);
                        String rawResponse = result.getText();
                        String cleanedResponse = cleanGeminiResponse(rawResponse);
                        textViewGeneratedPlan.setText(cleanedResponse);
                        textViewGeneratedPlan.setVisibility(View.VISIBLE);
                        buttonModifyPlan.setVisibility(View.VISIBLE);
                        buttonSeeLocations.setVisibility(View.VISIBLE);
                        buttonClearPlan.setVisibility(View.VISIBLE);
                        assert rawResponse != null;

                    } catch (Exception e) {
                        showError("Failed to parse response");
                        Log.e("Gemini", "Parsing error", e);
                    }
                    showProgress(false);
                });
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                runOnUiThread(() -> {
                    showError("API Error: " + t.getMessage());
                    showProgress(false);
                    Log.e("Gemini", "API call failed", t);
                });
            }
        }, executor);
    }

    private String cleanGeminiResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isEmpty()) {
            return "No valid itinerary generated. Please try again.";
        }

        StringBuilder cleaned = new StringBuilder();
        String[] lines = rawResponse.split("\n");

        for (String line : lines) {
            if (line.matches("^•\\s\\d{2}:\\d{2}\\sVisit\\s.+")) {
                cleaned.append(line).append("\n");
            }
        }

        if (cleaned.length() == 0) {
            Log.w("Gemini", "Unexpected response format:\n" + rawResponse);
            return  rawResponse;
        }

        return cleaned.toString().trim();
    }

    private boolean isValidTimeRange() {
        int fromHour = timePickerFrom.getHour();
        int fromMin = timePickerFrom.getMinute();
        int toHour = timePickerTo.getHour();
        int toMin = timePickerTo.getMinute();
        return (toHour > fromHour) || (toHour == fromHour && toMin > fromMin);
    }

    private String getSelectedInterests() {
        if (!checkCulture.isChecked() && !checkNature.isChecked() &&
                !checkFoodie.isChecked() && !checkShopping.isChecked() &&
                !checkNightlife.isChecked()) {
            checkGeneral.setChecked(true);
            return "General sightseeing (landmarks, popular attractions)";
        }

        StringBuilder interests = new StringBuilder();
        if (checkGeneral.isChecked()) interests.append("General attractions, ");
        if (checkCulture.isChecked()) interests.append("Culture, ");
        if (checkNature.isChecked()) interests.append("Nature, ");
        if (checkFoodie.isChecked()) interests.append("Food, ");
        if (checkShopping.isChecked()) interests.append("Shopping, ");
        if (checkNightlife.isChecked()) interests.append("Nightlife, ");

        return interests.substring(0, interests.length() - 2);
    }

    private void setupSpinner(Spinner spinner, ArrayList<String> data) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                data
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        spinnerCountry.setEnabled(!show);
        spinnerCity.setEnabled(!show);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @SuppressLint("SetTextI18n")
    private void clearPlan() {
        textViewGeneratedPlan.setText("");
        textViewGeneratedPlan.setVisibility(View.GONE);
        planIntro.setVisibility(View.GONE);
        buttonModifyPlan.setVisibility(View.GONE);
        buttonSeeLocations.setVisibility(View.GONE);
        buttonClearPlan.setVisibility(View.GONE);
    }

    private void modifyPlan() {
        if (textViewGeneratedPlan.getText().toString().equals("Your generated plan will appear here.")) {
            showError("No plan to modify. Please generate a plan first.");
            return;
        }
        generatePlan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (returningFromLocationSettings && isLocationEnabled()) {
            openGoogleMaps();
            returningFromLocationSettings = false;
        }
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (d, w) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    private void showLocationSettingsDialog() {
        returningFromLocationSettings = true;
        new AlertDialog.Builder(this)
                .setTitle("Location Services Disabled")
                .setMessage("Enable location services to use this feature")
                .setPositiveButton("Settings", (d, w) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Cancel", (d, w) ->
                        returningFromLocationSettings = false)
                .setOnCancelListener(d ->
                        returningFromLocationSettings = false)
                .show();
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void openGoogleMaps() {
        if (textViewGeneratedPlan.getText().toString().isEmpty() ||
                textViewGeneratedPlan.getText().toString().equals("Your generated plan will appear here.")) {
            showError("Please generate a plan first");
            return;
        }

        String rawResponse = textViewGeneratedPlan.getText().toString();
        ArrayList<String> locations = extractLocations(rawResponse);
        String country = spinnerCountry.getSelectedItem() != null ? spinnerCountry.getSelectedItem().toString() : "";
        String city = spinnerCity.getSelectedItem() != null ? spinnerCity.getSelectedItem().toString() : "";

        if (locations.isEmpty()) {
            showError("No locations found in the plan");
            return;
        }

        try {
            StringBuilder mapsUrl = new StringBuilder("https://www.google.com/maps/dir/");

            if (!city.isEmpty() && !country.isEmpty()) {
                mapsUrl.append(Uri.encode(city + ", " + country)).append("/");
            }

            for (String location : locations) {
                mapsUrl.append(Uri.encode(location + ", " + city + ", " + country)).append("/");
            }

            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl.toString()));
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl.toString())));
            }
        } catch (Exception e) {
            showError("Error opening maps");
            Log.e("Maps", "Error opening maps", e);
        }
    }

    private ArrayList<String> extractLocations(String rawResponse) {
        ArrayList<String> locations = new ArrayList<>();
        if (rawResponse == null || rawResponse.isEmpty()) {
            return locations;
        }

        String[] lines = rawResponse.split("\n");
        Log.d("LocationExtraction", "Raw response lines: " + lines.length);

        for (String line : lines) {
            if (line.contains("Visit")) {
                String location = line.substring(line.indexOf("Visit") + 5).trim();
                if (!location.isEmpty()) {
                    locations.add(location);
                    Log.d("LocationExtraction", "Found location: " + location);
                }
            }
        }

        Log.d("LocationExtraction", "Total locations found: " + locations.size());
        return locations;
    }

}