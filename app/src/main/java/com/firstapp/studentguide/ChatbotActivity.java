package com.firstapp.studentguide;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatbotActivity extends AppCompatActivity {
    private EditText chatInput;
    private LinearLayout chatHistoryContainer;
    private ScrollView chatHistory;
    private GenerativeModelFutures modelFutures;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.chatbot_activity);

        chatInput = findViewById(R.id.chat_input);
        ImageView sendButton = findViewById(R.id.send_button);
        chatHistoryContainer = findViewById(R.id.chat_history_container);
        chatHistory = findViewById(R.id.chat_history);
        ImageView homeButton = findViewById(R.id.home_button);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.GONE);

        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChatbotActivity.this, MainActivity.class);
            startActivity(intent);
        });

        String apiKey = BuildConfig.API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            Toast.makeText(this, "API Key missing", Toast.LENGTH_SHORT).show();
            return;
        }

        GenerativeModel model = new GenerativeModel("gemini-2.0-flash", apiKey);
        modelFutures = GenerativeModelFutures.from(model);

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String userMessage = chatInput.getText().toString().trim();
        if (userMessage.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        addUserMessage(userMessage);

        progressBar.setVisibility(View.VISIBLE);

        Content content = new Content.Builder().addText(userMessage).build();
        ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);

        Futures.addCallback(response, new FutureCallback<>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String botResponse = result.getText();
                runOnUiThread(() -> {
                    addBotMessage(botResponse);

                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure(@NonNull @NotNull Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(ChatbotActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        }, executor);

        chatInput.setText("");
    }

    private void addUserMessage(String message) {
        View userBubble = LayoutInflater.from(this).inflate(R.layout.user_message_bubble, chatHistoryContainer, false);
        TextView userMessageText = userBubble.findViewById(R.id.user_message_text);
        userMessageText.setText(message);
        userMessageText.setTextColor(ContextCompat.getColor(this, R.color.black));
        chatHistoryContainer.addView(userBubble);

        scrollToBottom();
    }

    private void addBotMessage(String message) {
        View botBubble = LayoutInflater.from(this).inflate(R.layout.bot_message_bubble, chatHistoryContainer, false);
        TextView botMessageText = botBubble.findViewById(R.id.bot_message_text);
        botMessageText.setText(message);

        chatHistoryContainer.addView(botBubble);

        scrollToBottom();
    }

    private void scrollToBottom() {
        chatHistory.post(() -> chatHistory.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
