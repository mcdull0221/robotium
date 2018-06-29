package com.nurotron.ble_ui;

/**
 * Created by tyeh on 5/7/17.
 */

import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * IntentUtility contains helper methods that to identify intents
 * or other not so common operations.
 */
public class IntentUtility {

    Context context;

    public IntentUtility(Context context) {
        this.context = context;
    }


    /**
     * Searches for an intent given a type string and a user-facing label.
     *
     * @param type - eg. tel:, sms:, or mailto:
     * @param label - User-facing label displayed to ask user for default application.
     */
    public void startIntentFor(String type, String label) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(type));
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resInfo = pm.queryIntentActivities(intent, 0);
        if (resInfo.size() > 0) {
            ResolveInfo ri = resInfo.get(0);
            Intent intentChooser = pm.getLaunchIntentForPackage(ri.activityInfo.packageName);
            Intent openInChooser = Intent.createChooser(intentChooser, "Choose Your Default "+ label +" App");
            List<LabeledIntent> intentList = new ArrayList<>();
            for (int i = 1; i < resInfo.size(); i++) {
                ri = resInfo.get(i);
                String packageName = ri.activityInfo.packageName;
                Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                intentList.add(new LabeledIntent(launchIntent, packageName, ri.loadLabel(pm), ri.icon));
            }
            LabeledIntent[] extraIntents = intentList.toArray(new LabeledIntent[intentList.size()]);
            openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
            context.startActivity(openInChooser);
        }
    }
}
