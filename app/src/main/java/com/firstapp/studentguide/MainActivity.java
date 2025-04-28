package com.firstapp.studentguide;

import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private boolean returningFromLocationSettings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button suggestPlanButton = findViewById(R.id.open_suggest_plan_button);
        ImageView settingsButton = findViewById(R.id.settings_button);
        Button openAIButton = findViewById(R.id.open_ai_button);
        Button openMapsButton = findViewById(R.id.open_maps_button);
        Button openTranslationButton = findViewById(R.id.open_translation_button);

        openMapsButton.setOnClickListener(v -> checkLocationAndOpenMap());

        suggestPlanButton.setOnClickListener(v -> startActivity(new Intent(this, DailyPlanActivity.class)));
        settingsButton.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        openAIButton.setOnClickListener(v -> startActivity(new Intent(this, ChatbotActivity.class)));
        openTranslationButton.setOnClickListener(v -> startActivity(new Intent(this, TranslationActivity.class)));
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

    private void checkLocationAndOpenMap() {
        if (!isLocationEnabled()) {
            showLocationSettingsDialog();
        } else {
            openGoogleMaps();
        }
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

    private void openGoogleMaps() {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps"));
        mapIntent.setPackage("com.google.android.apps.maps");
        mapIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        try {
            startActivity(mapIntent);
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com")));
        }
    }
}