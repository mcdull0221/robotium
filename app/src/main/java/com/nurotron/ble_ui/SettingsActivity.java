package com.nurotron.ble_ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nurotron.ble_ui.menu.SlidingMenu;

import org.w3c.dom.Text;

import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.nurotron.ble_ui.MainActivity.BATTERY_RECEIVED;
import static com.nurotron.ble_ui.R.id.left;
import static com.nurotron.ble_ui.R.id.left_arrow_image;
import static com.nurotron.ble_ui.R.id.night;

/**
 * Created by Nurotron on 5/16/2017.
 */

public class SettingsActivity extends AppCompatActivity {

    SlidingMenu menu;
    View inflatedView;
    public PresenterListener listener;
    ActionToolbarPresenter actionToolbarPresenter;

    @InjectView(night) Button nightMode;
    @InjectView(R.id.status) Button connectDevice;
    @InjectView(R.id.text_size) Button textSize;
    @InjectView(R.id.backtohome) Button back;
    @InjectView(R.id.troubleshoot) Button troubleshoot;
    @InjectView(R.id.language) Button language;
    @InjectView(R.id.setting_layout) LinearLayout settingLayout;
    @InjectView(R.id.back_home_text) TextView backHomeText;
    @InjectView(R.id.setting_center_text) TextView settingCenterText;
    @InjectView(R.id.action_toolbar_icon) ImageView actionToolbarIcon;
    @InjectView(R.id.left_arrow_image) ImageView leftArrowImage;

    KeyValueStore sharedPrefs;
    public static String LANGUAGE = "LANGUAGE";
    public static String TEXTSIZES = "TEXTSIZES";
    public static String NIGHTMODE = "NIGHTMODE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = new KeyValueStore.SharedPrefs(getApplicationContext());
        configureLanguage(); // configure language needs to happen before layout inflation, otherwise default strings will be used


        inflatedView = getLayoutInflater().inflate(R.layout.settings_tab, null);
        setContentView(inflatedView);

        ButterKnife.inject(this, inflatedView);
        configureSlidingMenu();
        configureViewPresenters(inflatedView);
        configureStatusBarBackground();


        actionToolbarPresenter.setBattery(sharedPrefs.getInt(BATTERY_RECEIVED, 0));

        connectDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, ConnectDeviceActivity.class);
                startActivity(intent);
            }
        });
        textSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, TextSizeActivity.class);
                startActivity(intent);
            }
        });

        language.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, LanguageActivity.class);
                startActivity(intent);
            }
        });

        nightMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, NightModeActivity.class);
                startActivity(intent);
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    @Override
    public void onBackPressed() {
        final Intent intent = getParentActivityIntent();
        finish();
        startActivity(intent);
    }


    @Override protected void onResume() {
        super.onResume();
        setStyles();
        configureDayNight();

    }

    public void configureDayNight() {
        //settingLayout.setBackgroundColor(getResources().getColor(R.color.background, null));

        String nightMode = sharedPrefs.getString(SettingsActivity.NIGHTMODE);
        boolean nightModeEnabled;
        if (nightMode == null) nightModeEnabled = false;
        else {
            if (nightMode.equals("disable")) nightModeEnabled = false;
            else nightModeEnabled = true;
        }
        if (nightModeEnabled) {
            settingLayout.setBackgroundColor(Color.parseColor("#37393d"));
            backHomeText.setTextColor(Color.parseColor("#cfcfcf"));
            settingCenterText.setTextColor(Color.parseColor("#cfcfcf"));
            actionToolbarIcon.setImageResource(R.drawable.menu_night);
            leftArrowImage.setImageResource(R.drawable.left_arrow_night);
        } else {
            settingLayout.setBackgroundColor(Color.parseColor("#ffffff"));
            backHomeText.setTextColor(Color.parseColor("#131313"));
            settingCenterText.setTextColor(Color.parseColor("#131313"));
            actionToolbarIcon.setImageResource(R.drawable.menu);
            leftArrowImage.setImageResource(R.drawable.left_arrow);
        }
    }

    public void configureLanguage() {
        String languageToLoad; // your language
        if (sharedPrefs.getString(LANGUAGE)==null || sharedPrefs.getString(LANGUAGE).isEmpty()) languageToLoad="zh";
        else languageToLoad = sharedPrefs.getString(LANGUAGE);

        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }

    public void configureStatusBarBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //window.setStatusBarColor(getColor(R.color.background));
            int color = ContextCompat.getColor(this, R.color.header);
            window.setStatusBarColor(color);
        }
    }


    /**
     * Configure View Presenters with the provided inflated view.
     * Set up any necessary event listeners.
     *
     * @param inflatedView
     */
    private void configureViewPresenters(View inflatedView) {
        // Instantiate the Event Listener
        listener = new PresenterListener(this);
        actionToolbarPresenter = new ActionToolbarPresenter(inflatedView);
        actionToolbarPresenter.setListener(listener);
    }


    /**
     * Presenter Listener Contains the actions that should take place when
     * a UI action is taken.
     *
     * ActionToolbarPresenter.Listener interface is called when icon or activity label are clicked.
     * ActivityToolbarPresenter.Lister interface is called when the call/text items are clicked.
     */
    class PresenterListener implements ActionToolbarPresenter.Listener {

        Context context;
        IntentUtility intentUtility;

        public PresenterListener(Context context) {
            this.context = context;
            this.intentUtility = new IntentUtility(context);
        }


        @Override public void onIconClicked() {
            menu.showMenu();
        }

    }


    private void configureSlidingMenu() {
        menu = new SlidingMenu(this);
        menu.setMode(SlidingMenu.LEFT);
        menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        menu.setShadowWidthRes(R.dimen.shadow_width);
        menu.setShadowDrawable(R.drawable.shadow);
        menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        menu.setFadeDegree(0.35f);
        menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        menu.setMenu(R.layout.menu);
    }

    private void setStyles() {
        float sp = 25.0f;
        if (sharedPrefs.getString(SettingsActivity.TEXTSIZES) != null && sharedPrefs.getString(SettingsActivity.TEXTSIZES).equals("small")) sp = sp * 0.8f;
        else if (sharedPrefs.getString(SettingsActivity.TEXTSIZES) != null && sharedPrefs.getString(SettingsActivity.TEXTSIZES).equals("large")) sp = sp * 1.2f;
        float size = sp * getResources().getDisplayMetrics().scaledDensity;
        Typeface typeface = TypefaceUtil.get(this);
        connectDevice.setTypeface(typeface);
        connectDevice.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        textSize.setTypeface(typeface);
        textSize.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        language.setTypeface(typeface);
        language.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        troubleshoot.setTypeface(typeface);
        troubleshoot.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        back.setTypeface(typeface);
        back.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        nightMode.setTypeface(typeface);
        nightMode.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

}
