package com.firstapp.studentguide;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends Activity {
    private FirebaseAuth mAuth;
    Button btnLogout;
    public static final String SHARED_PREFS = "sharedPrefs";


    TextView emailTextView, userIdTextView;
    ImageView homeButton;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.settings_activity);


        mAuth = FirebaseAuth.getInstance();

        btnLogout = findViewById(R.id.logout_button);
        emailTextView = findViewById(R.id.email_settings);
        userIdTextView = findViewById(R.id.user_id_display);
        homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            startActivity(intent);
        });

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();

            String emailText = getString(R.string.email_display, email);
            SpannableString emailSpannable = new SpannableString(emailText);
            emailSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, emailText.indexOf(":") + 1, 0);
            emailTextView.setText(emailSpannable);

            String userId = user.getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                String usernameText = getString(R.string.username_display, username);
                                SpannableString usernameSpannable = new SpannableString(usernameText);
                                usernameSpannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, usernameText.indexOf(":") + 1, 0);
                                userIdTextView.setText(usernameSpannable);
                            }
                        }
                    });
        }

        btnLogout.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            mAuth.signOut();

            Toast.makeText(SettingsActivity.this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
