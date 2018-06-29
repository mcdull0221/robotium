package com.nurotron.ble_ui;

/**
 * Created by tyeh on 5/7/17.
 */

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.text.Layout;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.nurotron.ble_ui.TypefaceUtil;

import java.util.WeakHashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Action Toolbar Presenter configures the view for the actionbar at the top of
 * the scene.
 */
public class ActionToolbarPresenter {

    Listener listener = new SimpleListener();
    View view;
    @InjectView(R.id.action_toolbar_icon) ImageView icon;
    @InjectView(R.id.action_toolbar_battery) ImageView battery;
    @InjectView(R.id.nu_action_bar) View container;
    @InjectView(R.id.action_toolbar_battery) ImageView batteryIcon;
    @InjectView(R.id.action_toolbar_battery_text) TextView batteryText;


    public ActionToolbarPresenter(View view) {
        this.view = view;
        ButterKnife.inject(this, view);
        setStyles();
    }

    private void setStyles() {
        Typeface typeface = TypefaceUtil.get(view.getContext());
        //title.setTypeface(typeface);
    }


    /**
     * Transformation that will be applied to the title value when it is set.
     * @param value value that should be transformed
     * @return transformed value
     */
    private String titleTransformation(String value) {
        if (value.equalsIgnoreCase("idle")) return "";
        else return value.toUpperCase().trim();
    }


    public void setBattery(int percentage) {
        batteryText.setText("" + percentage + "%");
        if(percentage == 0) {
//            batteryIcon.setImageResource(R.drawable.battery_0);没有图片报错
            batteryText.setText("");
        }
        else if (percentage <= 10) {
            batteryIcon.setImageResource(R.drawable.battery_10);
     //       batteryText.setText("10%");
        }
        else if (percentage <= 30) {
            batteryIcon.setImageResource(R.drawable.battery_30);
     //       batteryText.setText("30%");
        }
        else if (percentage <=50) {
            batteryIcon.setImageResource(R.drawable.battery_50);
      //      batteryText.setText("50%");
        }
        else if (percentage <=70) {
            batteryIcon.setImageResource(R.drawable.battery_70);
      //      batteryText.setText("70%");
        }
        else {
            batteryIcon.setImageResource(R.drawable.battery_100);
     //       batteryText.setText("100%");
        }
    }

    /**
     * Set the icon resource for the action toolbar.
     * @param iconRes
     */
    public void setIcon(@DrawableRes int iconRes) {
        if (iconRes == 0) {
            icon.setVisibility(View.GONE);
        } else {
            icon.setImageResource(iconRes);
            icon.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.action_toolbar_icon) void onIconClicked() {
        listener.onIconClicked();
    }


    /**
     * Set the listener that will be called when an action on the UI is performed.
     * @param listener implementation of interface.
     */
    public void setListener(Listener listener) {
        if (listener == null) {
            listener = new SimpleListener();
        }
        this.listener = listener;
    }


    public interface Listener {
        void onIconClicked();
    }


    public static class SimpleListener implements Listener {
        @Override public void onIconClicked() {}
    }
}
