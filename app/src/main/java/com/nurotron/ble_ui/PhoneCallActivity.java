package com.nurotron.ble_ui;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Nurotron on 10/27/2017.
 */

public class PhoneCallActivity extends Activity {


    public static final String ACTION_TO_SERVICE = "Turn on BT";
    public static final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 100;
    public static final String OUT_GOING_CALL = "android.intent.action.NEW_OUTGOING_CALL";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sendBroadcast();
        final Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:0123456789"));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE);
        } else {
            //You already have permission
            try {
                startActivity(intent);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        finish();


    }

    private void sendBroadcast() {
        Intent new_intent = new Intent();
        new_intent.setAction(ACTION_TO_SERVICE);
        sendBroadcast(new_intent);
        Toast.makeText(this, "Connecting to Enduro...", Toast.LENGTH_LONG).show();

    }
}
