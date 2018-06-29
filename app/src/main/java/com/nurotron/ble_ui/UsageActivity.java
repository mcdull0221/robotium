package com.nurotron.ble_ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.nurotron.ble_ui.menu.SlidingMenu;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.nurotron.ble_ui.MainActivity.BATTERY_RECEIVED;

/**
 * Created by tyeh on 5/25/17.
 */

public class UsageActivity extends AppCompatActivity {
    SlidingMenu menu;
    View inflatedView;
    public PresenterListener listener;
    ActionToolbarPresenter actionToolbarPresenter;
    KeyValueStore sharedPrefs;

    @InjectView(R.id.progress_bar_1) ProgressBar p1;
    @InjectView(R.id.progress_bar_2) ProgressBar p2;
    @InjectView(R.id.progress_bar_3) ProgressBar p3;
    @InjectView(R.id.progress_bar_4) ProgressBar p4;
    @InjectView(R.id.p1_text) TextView p1Text;
    @InjectView(R.id.p2_text) TextView p2Text;
    @InjectView(R.id.p3_text) TextView p3Text;
    @InjectView(R.id.p4_text) TextView p4Text;
    @InjectView(R.id.total_hour) TextView totalHourText;
    double p1Hour = 10;
    double p2Hour = 20;
    double p3Hour = 15.5;
    double p4Hour = 20;

    private GRadioGroup optionsRB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inflatedView = getLayoutInflater().inflate(R.layout.user_log, null);
        sharedPrefs = new KeyValueStore.SharedPrefs(getApplicationContext());

        setContentView(inflatedView);
        ButterKnife.inject(this, inflatedView);
        configureSlidingMenu();
        configureViewPresenters(inflatedView);
        configureStatusBarBackground();
        configureProgress();
        //mProgressBar = (ProgressBar) findViewById(R.id.progressBar2);
        optionsRB = new GRadioGroup(inflatedView, R.id.radio_allow, R.id.radio_out);

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
        actionToolbarPresenter.setBattery(sharedPrefs.getInt(BATTERY_RECEIVED, 0));    }

    private void configureProgress() {
        double totalHour = p1Hour + p2Hour + p3Hour + p4Hour;
        double p1Rotate = -90, p2Rotate, p3Rotate, p4Rotate;
        double p1Progress, p2Progress, p3Progress, p4Progress;
        p1Progress = (p1Hour/totalHour) * 360;
        p2Rotate = p1Rotate + p1Progress;
        p2Progress = (p2Hour/totalHour) * 360;
        p3Rotate = p2Rotate + p2Progress;
        p3Progress = (p3Hour/totalHour) * 360;
        p4Rotate = p3Rotate + p3Progress;
        p4Progress = (p4Hour/totalHour) * 360;
        p1.setRotation((float) (p1Rotate));
        p1.setProgress((int) p1Progress);
        p2.setRotation((float) p2Rotate);
        p2.setProgress((int) p2Progress);
        p3.setRotation((float) p3Rotate);
        p3.setProgress((int) p3Progress);
        p4.setRotation((float)p4Rotate);
        p4.setProgress((int) p4Progress);
        p1Text.setText(String.valueOf(p1Hour) + " " + getString(R.string.hours));
        p2Text.setText(String.valueOf(p2Hour) + " " + getString(R.string.hours));
        p3Text.setText(String.valueOf(p3Hour) + " " + getString(R.string.hours));
        p4Text.setText(String.valueOf(p4Hour) + " " + getString(R.string.hours));
        totalHourText.setText(String.valueOf(totalHour) + " " + getString(R.string.hours));
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
            String nightMode = sharedPrefs.getString(SettingsActivity.NIGHTMODE);
            if (nightMode==null) nightMode="disable";
            int radioCount=0;
            for (int radioButtonID : radiosIDs) {
                RadioButton rb = (RadioButton)activity.findViewById(radioButtonID);
                if (rb != null) {
                    this.radios.add(rb);
                    rb.setTypeface(TypefaceUtil.get(getApplicationContext()));
                    rb.setOnClickListener(onClick);
                }
                rb.setChecked(false);
                if (radioCount==0 && nightMode.equals("enable")) rb.setChecked(true);
                else if (radioCount==1 && nightMode.equals("disable")) rb.setChecked(true);
                radioCount++;
            }
        }

        /**
         * This occurs every time when one of RadioButtons is clicked,
         * and deselects all others in the group.
         */
        View.OnClickListener onClick = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = getIntent();

                RadioButton rb = (RadioButton) v;

                String lanLabel = rb.getText().toString();
                switch (lanLabel) {
                    case "Enable":
                    case "打开":
                    case "habilitar":
                        System.out.println("night mode: Enable");
                        sharedPrefs.putString(SettingsActivity.NIGHTMODE, "enable");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        finish();

                        startActivity(intent);
                        break;
                    case "Disable":
                    case "关闭":
                    case "inhabilitar":
                        System.out.println("night mode: Disable");
                        sharedPrefs.putString(SettingsActivity.NIGHTMODE, "disable");
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        finish();

                        startActivity(intent);
                        break;
                    default:
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

}
