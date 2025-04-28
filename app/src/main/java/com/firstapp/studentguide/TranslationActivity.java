package com.firstapp.studentguide;


import android.content.Intent;

import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

//google mlkit api--------------------------------------------------------------------------------

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.common.model.DownloadConditions;
//----------------------------------------------------------------------------------------------


import java.util.ArrayList;

import java.util.List;
import java.util.Locale;

import java.util.Set;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;

public class TranslationActivity extends AppCompatActivity {

    private EditText userInputEditText;
    private TextView translatedTextView;
    private TextToSpeech tts;
    private Translator translator;
    private Spinner sourceLangSpinner, targetLangSpinner;
    private ProgressBar progressBar;
    private Set<Locale> availableTtsLanguages;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.translation_activity);

        initializeViews();
        initializeTextToSpeech();
    }

    private void initializeViews() {
        userInputEditText = findViewById(R.id.userInputEditText);
        translatedTextView = findViewById(R.id.translatedTextView);
        ImageView micButton = findViewById(R.id.micButton);
        ImageView speakButton = findViewById(R.id.speakButton);
        Button translateButton = findViewById(R.id.translateButton);
        sourceLangSpinner = findViewById(R.id.sourceLangSpinner);
        targetLangSpinner = findViewById(R.id.targetLangSpinner);
        progressBar = findViewById(R.id.progressBar);
        ImageView settings_button = findViewById(R.id.settings_button);
        ImageView home_button = findViewById(R.id.home_button);

        home_button.setOnClickListener(v -> {
                    Intent intent = new Intent(TranslationActivity.this, MainActivity.class);
                    startActivity(intent);
                });

        settings_button.setOnClickListener(v -> {
                    Intent intent = new Intent(TranslationActivity.this, SettingsActivity.class);
                    startActivity(intent);
                });

        translateButton.setOnClickListener(v -> handleTranslation());
        speakButton.setOnClickListener(v -> speakTranslation());


        // speech to text ----------------------------------------------------------------------------

        micButton.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                startVoiceInput();
            }
        });
//------------------------------------------------------------------------------------------------

    }

    private void setupLanguageSpinners() {
        List<String> languageCodes = TranslateLanguage.getAllLanguages();
        List<LanguageItem> languageItems = new ArrayList<>();

        for (String code : languageCodes) {
            Locale locale = new Locale(code);
            boolean isAvailable = availableTtsLanguages != null &&
                    availableTtsLanguages.contains(locale);
            languageItems.add(new LanguageItem(code, locale.getDisplayLanguage(), isAvailable));
        }

        languageItems.sort((o1, o2) -> o1.displayName.compareToIgnoreCase(o2.displayName));

        ArrayAdapter<LanguageItem> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                languageItems
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                LanguageItem item = getItem(position);
                assert item != null;
                if (!item.isAvailable) {
                    textView.setTextColor(ContextCompat.getColor(TranslationActivity.this, android.R.color.darker_gray));
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sourceLangSpinner.setAdapter(adapter);
        targetLangSpinner.setAdapter(adapter);

        sourceLangSpinner.post(() -> setDefaultLanguageSelection(sourceLangSpinner, "en"));
        targetLangSpinner.post(() -> setDefaultLanguageSelection(targetLangSpinner, "fr"));
    }

    private void setDefaultLanguageSelection(Spinner spinner, String languageCode) {
        SpinnerAdapter adapter = spinner.getAdapter();
        if (adapter instanceof ArrayAdapter) {
            ArrayAdapter<?> arrayAdapter = (ArrayAdapter<?>) adapter;
            for (int i = 0; i < arrayAdapter.getCount(); i++) {
                Object item = arrayAdapter.getItem(i);
                if (item instanceof LanguageItem && ((LanguageItem) item).code.equals(languageCode)) {
                    spinner.setSelection(i);
                    return;
                }
            }
        }
        spinner.setSelection(0);
    }

    // text to speech----------------------------------------------------------------------------
    private void initializeTextToSpeech() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                availableTtsLanguages = tts.getAvailableLanguages();
                setupLanguageSpinners();
            } else {
                Log.e("TTS", "Initialization failed");
                showToast("Text-to-speech initialization failed");
            }
        });
    }
//-------------------------------------------------------------------------------------------------
    private void handleTranslation() {
        String textToTranslate = userInputEditText.getText().toString().trim();
        if (textToTranslate.isEmpty()) {
            showToast("Please enter text");
            return;
        }

        LanguageItem sourceLangItem = (LanguageItem) sourceLangSpinner.getSelectedItem();
        LanguageItem targetLangItem = (LanguageItem) targetLangSpinner.getSelectedItem();

        showLoading(true);
        initializeTranslator(sourceLangItem.code, targetLangItem.code, textToTranslate);
    }

    private void initializeTranslator(String sourceLang, String targetLang, String textToTranslate) {
        if (translator != null) {
            translator.close();
        }

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build();

        translator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> performTranslation(textToTranslate))
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e("Translation", "Model download failed", e);
                    showToast("Failed to download language model");
                });
    }

    private void performTranslation(String textToTranslate) {
        showLoading(true);
        translator.translate(textToTranslate)
                .addOnSuccessListener(translatedText -> {
                    showLoading(false);
                    translatedTextView.setText(translatedText);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e("Translation", "Translation failed", e);
                    showToast("Translation failed");
                });
    }

    private void speakTranslation() {
        String text = translatedTextView.getText().toString().trim();
        if (text.isEmpty()) {
            showToast("Nothing to speak");
            return;
        }

        LanguageItem targetLangItem = (LanguageItem) targetLangSpinner.getSelectedItem();
        Locale targetLocale = new Locale(targetLangItem.code);

        int languageResult = tts.setLanguage(targetLocale);

        if (isLanguageSupported(languageResult)) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Locale baseLocale = new Locale(targetLocale.getLanguage());
            int baseResult = tts.setLanguage(baseLocale);

            if (isLanguageSupported(baseResult)) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                showInstallGuide(targetLocale);
            }
        }
    }

    private boolean isLanguageSupported(int resultCode) {
        return resultCode == TextToSpeech.LANG_AVAILABLE ||
                resultCode == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                resultCode == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
    }

    private void showLoading(boolean isLoading) {
        runOnUiThread(() -> progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE));
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (translator != null) {
            translator.close();
        }
        super.onDestroy();
    }

    private static class LanguageItem {
        String code;
        String displayName;
        boolean isAvailable;

        LanguageItem(String code, String displayName, boolean isAvailable) {
            this.code = code;
            this.displayName = displayName;
            this.isAvailable = isAvailable;
        }

        @NonNull
        @Override
        public String toString() {
            return displayName ;
        }
    }

    private void installTtsLanguage(Locale locale) {
        try {
            Intent installIntent = new Intent();
            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            startActivity(installIntent);
            showToast("Please install voice data for " + locale.getDisplayLanguage());
        } catch (Exception e) {
            Log.e("TTS", "Failed to launch TTS installer", e);
            showToast("Couldn't open voice installer");
        }
    }

    private void showInstallGuide(Locale locale) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Voice Package Required")
                .setMessage(String.format("%s voice not installed. Would you like to install it?", locale.getDisplayLanguage()))
                .setPositiveButton("Install", (dialog, which) -> installTtsLanguage(locale))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            boolean permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (permissionToRecordAccepted) {
                startVoiceInput();
            } else {
                showToast("Microphone permission denied");
            }
        }
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        String selectedLang = ((LanguageItem) sourceLangSpinner.getSelectedItem()).code;

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showToast("Speech recognition not available");
            return;
        }

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLang);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            speechRecognitionLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            showToast("Speech recognition not supported");
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && resultCode == RESULT_OK) {
            ArrayList<String> result = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                userInputEditText.setText(result.get(0));
            }
        }
    }

    private final ActivityResultLauncher<Intent> speechRecognitionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> results = result.getData().getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS);
                    if (results != null && !results.isEmpty()) {
                        userInputEditText.setText(results.get(0));
                    }
                }
            });
}