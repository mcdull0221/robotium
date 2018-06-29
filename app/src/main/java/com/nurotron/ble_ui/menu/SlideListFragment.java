package com.nurotron.ble_ui.menu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nurotron.ble_ui.AboutActivity;
import com.nurotron.ble_ui.CNFinderActivity;
import com.nurotron.ble_ui.FinderActivity;
import com.nurotron.ble_ui.KeyValueStore;
import com.nurotron.ble_ui.MainActivity;
import com.nurotron.ble_ui.R;
import com.nurotron.ble_ui.SettingsActivity;
import com.nurotron.ble_ui.StatusActivity;
import com.nurotron.ble_ui.UsageActivity;

import java.util.Locale;

import butterknife.InjectView;

import static android.R.attr.configure;

/**
 * Created on 9/06/15.
 */
public class SlideListFragment extends ListFragment {

    private final int TITLE = 0;
    private final int HOME = 1;
    private final int SETTINGS = 2;
    private final int STATUS = 3;
    private final int FINDER = 4;
    private final int LOG = 5;
    private final int ABOUT = 6;
    ListView listView;
    KeyValueStore sharedPrefs;
    double CHINESE_RADIUS = 2700; // distance of kilometer to center of gravity of China (if China is a circle)

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        sharedPrefs = new KeyValueStore.SharedPrefs(getContext());
        configureLanguage();
        return inflater.inflate(R.layout.list, null);
    }


    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SampleAdapter adapter = new SampleAdapter(getActivity());
        adapter.add(new MenuItem(getResources().getString(R.string.action_home), R.drawable.home, HOME));
        adapter.add(new MenuItem(getResources().getString(R.string.action_settings), R.drawable.settings, SETTINGS));
        adapter.add(new MenuItem(getResources().getString(R.string.action_status), R.drawable.status, STATUS));
        adapter.add(new MenuItem(getResources().getString(R.string.action_finder), R.drawable.finder, FINDER));
        adapter.add(new MenuItem(getResources().getString(R.string.action_user_log), R.drawable.userlog, LOG));
        adapter.add(new MenuItem(getResources().getString(R.string.action_about), R.drawable.about, ABOUT));
        setListAdapter(adapter);
        View view = View.inflate(getContext(), R.layout.menu_header, null);
        listView = getListView();
        listView.addHeaderView(view);
    }


    @Override
    public void onListItemClick(ListView listView, View v, int position, long id) {
        Intent chosenIntent = null;
        switch (position) {
            case TITLE:
                break;
            case HOME:
                chosenIntent = new Intent(getActivity(), MainActivity.class);
                break;
            case SETTINGS:
                chosenIntent = new Intent(getActivity(), SettingsActivity.class);
                break;
            case STATUS:
                chosenIntent = new Intent(getActivity(), StatusActivity.class);
                break;
            case FINDER:
                if (distanceToCenterOfChina((double)(sharedPrefs.getFloat(MainActivity.LAST_KNOWN_LAT, 33.6846f)),
                                            (double)(sharedPrefs.getFloat(MainActivity.LAST_KNOWN_LON, 117.8265f))) > CHINESE_RADIUS) {
                    chosenIntent = new Intent(getActivity(), FinderActivity.class);  // simulation mode
                } else {
                    chosenIntent = new Intent(getActivity(), CNFinderActivity.class);
                }
                break;
            case LOG:
                chosenIntent = new Intent(getActivity(), UsageActivity.class);
                break;
            case ABOUT:
                chosenIntent = new Intent(getActivity(), AboutActivity.class);
                break;

            default:
                if (isVisible()) {
                    Toast.makeText(getActivity(), "Action Not Defined", Toast.LENGTH_SHORT).show();
                }
        }

        if (chosenIntent != null) {

            //clear all previous activities and put current on the top
      //      chosenIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//            chosenIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            chosenIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(chosenIntent);

        }
    }



    private class MenuItem {
        private int activityIndex;
        public String tag;
        public int iconRes;


        public MenuItem(String tag, int iconRes, int activityIndex) {
            this.tag = tag;
            this.iconRes = iconRes;
            this.activityIndex = activityIndex;
        }
    }


    public class SampleAdapter extends ArrayAdapter<MenuItem> {

        public SampleAdapter(Context context) {
            super(context, 0);
        }


        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row, null);
            }
            /*
            if (position == 0) {
                int topPadding = (int) (getResources().getDisplayMetrics().density * 50);
                convertView.setPadding(0, topPadding, 0, 0);
            }
            */

            ImageView icon = (ImageView) convertView.findViewById(R.id.row_icon);
            icon.setImageResource(getItem(position).iconRes);

            TextView title = (TextView) convertView.findViewById(R.id.row_title);
            title.setText(getItem(position).tag);

            return convertView;
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
        getContext().getResources().updateConfiguration(config,
                getContext().getResources().getDisplayMetrics());
    }

    private double distanceToCenterOfChina(double lat1, double lon1) {
        // China center of gravity: 36.244273 (lat), 103.183594 (long)
        double lat2 = 36.244273;
        double lon2 = 103.183594;
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }
}