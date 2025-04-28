package com.firstapp.studentguide;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SignUpActivity extends Activity {

    private EditText fullNameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "SignUpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.signup_activity);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .build();
        db.setFirestoreSettings(settings);

        fullNameEditText = findViewById(R.id.fullname);
        emailEditText = findViewById(R.id.email_signup);
        passwordEditText = findViewById(R.id.password_signup);
        confirmPasswordEditText = findViewById(R.id.confirm_password);
        Button signUpButton = findViewById(R.id.signup_button);

        signUpButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim().toLowerCase();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (!validateInputs(fullName, email, password, confirmPassword)) {
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "User authentication successful: " + user.getUid());
                            createUserDocument(user.getUid(), fullName, email);
                        }
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Registration failed: " + error);
                        showToast("Registration failed: " + error);
                    }
                });
    }

    private void createUserDocument(String userId, String fullName, String email) {
        String firstName = fullName.split("\\s+")[0];
        String username = generateUniqueUsername(firstName);
        String apiKey = "app_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("username", username);
        userData.put("apiKey", apiKey);
        userData.put("createdAt", FieldValue.serverTimestamp());
        userData.put("lastLogin", FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    saveApiKeyLocally(apiKey);
                    Log.d(TAG, "User document created successfully: " + userId);
                    showToast("Account created successfully!");
                    navigateToLogin();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore write failed: " + e.getMessage(), e);
                    showToast("Account created but data save failed. Please login again.");
                    navigateToLogin();
                });
    }

    private String generateUniqueUsername(String firstName) {
        String username = firstName.toLowerCase() + generateRandomNumber();
        checkUsernameUniqueness(username, firstName);
        return username;
    }

    private String generateRandomNumber() {
        int randomNum = (int) (Math.random() * 900) + 100;
        return String.valueOf(randomNum);
    }

    private void checkUsernameUniqueness(String username, String firstName) {
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.getResult().isEmpty()) {
                        String newUsername = generateUniqueUsername(firstName);
                        checkUsernameUniqueness(newUsername, firstName);
                    }
                });
    }


    private boolean validateInputs(String fullName, String email,
                                   String password, String confirmPassword) {
        return validateFullName(fullName) &&
                validateEmail(email) &&
                validatePassword(password, confirmPassword);
    }

    private boolean validateFullName(String fullName) {
        if (fullName.isEmpty()) {
            fullNameEditText.setError("Full name cannot be empty");
            return false;
        }
        if (fullName.split("\\s+").length < 2) {
            fullNameEditText.setError("Enter first and last name");
            return false;
        }
        return true;
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            emailEditText.setError("Email cannot be empty");
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Invalid email format");
            return false;
        }
        return true;
    }

    private boolean validatePassword(String password, String confirmPassword) {
        if (password.isEmpty()) {
            passwordEditText.setError("Password cannot be empty");
            return false;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Minimum 6 characters required");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords don't match");
            return false;
        }
        return true;
    }

    private void saveApiKeyLocally(String apiKey) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putString("apiKey", apiKey).apply();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void navigateToLogin() {
        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
        finish();
    }
}