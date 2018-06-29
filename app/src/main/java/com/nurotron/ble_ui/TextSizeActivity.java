package com.nurotron.ble_ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.nurotron.ble_ui.menu.SlidingMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.nurotron.ble_ui.MainActivity.BATTERY_RECEIVED;

/**
 * Created by Nurotron on 6/29/2017.
 */

public class TextSizeActivity extends AppCompatActivity {
    SlidingMenu menu;
    View inflatedView;
    public PresenterListener listener;
    ActionToolbarPresenter actionToolbarPresenter;

    KeyValueStore sharedPrefs;

    @InjectView(R.id.backToSetting) Button back;
    private GRadioGroup text;

    private final static String TAG = TextSizeActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = new KeyValueStore.SharedPrefs(getApplicationContext());

        //configureLanguage(); // configure language needs to happen before layout inflation, otherwise default strings will be used

        //mListView = (ListView) findViewById(R.id.list);
        inflatedView = getLayoutInflater().inflate(R.layout.text_size, null);
        setContentView(inflatedView);
        ButterKnife.inject(this, inflatedView);
        configureSlidingMenu();
        configureViewPresenters(inflatedView);
        configureStatusBarBackground();
        sharedPrefs = new KeyValueStore.SharedPrefs(getApplicationContext());

        actionToolbarPresenter.setBattery(sharedPrefs.getInt(BATTERY_RECEIVED, 0));


        text = new GRadioGroup(inflatedView, R.id.small_text, R.id.mid_text, R.id.large_text);


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

    //Menu
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



    public class GRadioGroup {

        List<RadioButton> radios = new ArrayList<RadioButton>();
        String name;


        /**
         * Constructor, which allows you to pass number of RadioButton instances,
         * making a group.
         *
         * @param radios
         *            One RadioButton or more.
         */
        public GRadioGroup(RadioButton... radios) {
            super();

            for (RadioButton rb : radios) {
                this.radios.add(rb);
                rb.setOnClickListener(onClick);
            }
        }

        /**
         * Constructor, which allows you to pass number of RadioButtons
         * represented by resource IDs, making a group.
         *
         * @param activity
         *            Current View (or Activity) to which those RadioButtons
         *            belong.
         * @param radiosIDs
         *            One RadioButton or more.
         */
        public GRadioGroup(View activity, int... radiosIDs) {
            super();
            String textSize = sharedPrefs.getString(SettingsActivity.TEXTSIZES);
            if (textSize==null) textSize="medium";
            int radioCount=0;
            for (int radioButtonID : radiosIDs) {
                RadioButton rb = (RadioButton)activity.findViewById(radioButtonID);
                if (rb != null) {
                    this.radios.add(rb);
                    rb.setTypeface(TypefaceUtil.get(getApplicationContext()));
                    rb.setOnClickListener(onClick);
                }
                rb.setChecked(false);
                if (radioCount==0 && textSize.equals("small")) rb.setChecked(true);
                else if (radioCount==1 && textSize.equals("medium")) rb.setChecked(true);
                else if (radioCount==2 && textSize.equals("large")) rb.setChecked(true);
                radioCount++;
            }
        }

        /**
         * This occurs everytime when one of RadioButtons is clicked,
         * and deselects all others in the group.
         */
        View.OnClickListener onClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                RadioButton rb = (RadioButton) v;

                String lanLabel = rb.getText().toString();
                switch (lanLabel) {
                    case "Small":
                    case "小":
                    case "Pequeña":
                        System.out.println("text size: small");
                        sharedPrefs.putString(SettingsActivity.TEXTSIZES, "small");
                        break;
                    case "Medium":
                    case "中":
                    case "Medio":
                        System.out.println("text size: medium");
                        sharedPrefs.putString(SettingsActivity.TEXTSIZES, "medium");
                        break;
                    default:
                        System.out.println("text size: large");
                        sharedPrefs.putString(SettingsActivity.TEXTSIZES, "large");
                        break;
                }

                rb.setChecked(true);
                for (RadioButton rg: radios){
                    if (!rg.equals(rb) && rg.isChecked())
                        rg.setChecked(false);
                }
            }

        };


        public RadioButton getRB(View activity, int radioId){
            RadioButton rb = (RadioButton)activity.findViewById(radioId);
            return rb;
        }

        public void setName(String name){
            this.name = name;
        }


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

}
