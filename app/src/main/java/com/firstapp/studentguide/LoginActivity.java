package com.firstapp.studentguide;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class LoginActivity extends Activity {

    private EditText emailEditText, passwordEditText;
    private FirebaseAuth mAuth;
    private CheckBox rememberMeCheckBox;
    private FirebaseFirestore db;
    public static final String SHARED_PREFS = "sharedPrefs";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.login_activity);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        rememberMeCheckBox = findViewById(R.id.remember_me_checkbox);
        rememberMeCheckBox.setPaintFlags(rememberMeCheckBox.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        rememberMeCheckBox.setText("Remember me");
        checkBox();

        emailEditText = findViewById(R.id.email_login);
        passwordEditText = findViewById(R.id.password_login);
        Button loginButton = findViewById(R.id.login_button);
        TextView signUpTextView = findViewById(R.id.signUpTextView);

        loginButton.setOnClickListener(v -> loginUser());

        SpannableString spannableString = new SpannableString("Don't have an account? Sign Up.");



        int start = spannableString.toString().indexOf("Sign Up");
        int end = start + "Sign Up".length();

        int brandRed = ContextCompat.getColor(this, R.color.brand_red);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(brandRed);
                ds.setUnderlineText(true);
                ds.setTypeface(Typeface.DEFAULT_BOLD);
            }
        };

        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        signUpTextView.setText(spannableString);
        signUpTextView.setMovementMethod(LinkMovementMethod.getInstance());    }

    private void checkBox() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String check = sharedPreferences.getString("name", "");
        if (check.equals("true")) {
            Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!validateEmail(email) || !validatePassword(password)) {
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();

                        if (rememberMeCheckBox.isChecked()) {
                            editor.putString("name", "true");
                        } else {
                            editor.putString("name", "");
                        }
                        editor.apply();

                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                        assert user != null;
                        fetchApiKeyFromFirestore(user.getUid());
                    } else {
                        Toast.makeText(LoginActivity.this, "Login Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchApiKeyFromFirestore(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String apiKey = documentSnapshot.getString("apiKey");

                        if (apiKey != null) {
                            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("apiKey", apiKey);
                            editor.commit();


                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "API Key not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "User not found in Firestore", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Error fetching API key: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return false;
        }
        return true;
    }

    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return false;
        }
        return true;
    }
}
