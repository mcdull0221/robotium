package com.nurotron.ble_ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.Window;
import android.view.WindowManager;

import java.util.Locale;

import static com.nurotron.ble_ui.MainActivity.DEVICE_FOUND;

/**
 * Created by tyeh on 4/26/17.
 */

public class SplashActivity extends AppCompatActivity {


    KeyValueStore sharedPrefs;

    private Handler delayHandler = new Handler();
    private final Runnable delayRunnable = new Runnable() {
        @Override public void run() {
            if(!sharedPrefs.getBool(DEVICE_FOUND)) {
                Intent intent = new Intent(getBaseContext(), DeviceScanActivity.class );
                startActivity(intent);
                finish();
            }
            else{
                Intent intent = new Intent(getBaseContext(),MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = new KeyValueStore.SharedPrefs(getApplicationContext());
        configureLanguage();
        configureNightMode();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int color = ContextCompat.getColor(this, R.color.background);
            window.setStatusBarColor(color);

        }
        delayHandler.postDelayed(delayRunnable, 1100);

    }

    public void configureLanguage() {
        String languageToLoad; // your language
        if (sharedPrefs.getString(SettingsActivity.LANGUAGE)==null || sharedPrefs.getString(SettingsActivity.LANGUAGE).isEmpty()) languageToLoad="zh";
        else languageToLoad = sharedPrefs.getString(SettingsActivity.LANGUAGE);

        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }

    public void configureNightMode() {
        String nightMode = sharedPrefs.getString(SettingsActivity.NIGHTMODE);
        boolean nightModeEnabled;
        if (nightMode == null) nightModeEnabled = false;
        else {
            if (nightMode.equals("disable")) nightModeEnabled = false;
            else nightModeEnabled = true;
        }
        if (nightModeEnabled) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}