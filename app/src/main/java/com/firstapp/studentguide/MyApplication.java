package com.firstapp.studentguide;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private int activityCount = 0;
    private boolean isAppInBackground = false;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        activityCount++;
        if (activityCount == 1 && isAppInBackground) {
            Log.d("AppLifecycle", "App moved to foreground.");
            isAppInBackground = false;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        activityCount--;
        if (activityCount == 0) {
            Log.d("AppLifecycle", "App moved to background.");
            isAppInBackground = true;
        }
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (activityCount == 0 && isAppInBackground) {
            Log.d("AppLifecycle", "All activities destroyed, app is about to close.");
            saveConversation();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    private void saveConversation() {
        Log.d("AppLifecycle", "Saving conversation before app closes.");
    }
}
