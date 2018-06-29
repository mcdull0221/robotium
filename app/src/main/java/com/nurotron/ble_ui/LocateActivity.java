package com.nurotron.ble_ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nurotron.ble_ui.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Created by Nurotron on 3/16/2017.
 */

public class LocateActivity extends AppCompatActivity {


    public TextView mRssi;

    public int receivedRSSI;

    public ProgressBar mProgressBar;

    private int count = 0;
    private int times = 0;

    public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
          //  peekService(context,);

           if (BluetoothLeService.ACTION_RSSI_READ.equals(action)){
                count ++;
                receivedRSSI = intent.getIntExtra(BluetoothLeService.RSSI_DATA, 0);
                Log.v("RSSI", "Rssi received " + receivedRSSI);
               writeData(receivedRSSI);
               if(count == 10) {
                   count = 0;
                   setmRssi(receivedRSSI);
               }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Locate Device");
        setContentView(R.layout.locate_device);

        mRssi = (TextView)findViewById(R.id.find);
       // mRssi.setText("Read Rssi:");

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar2);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
//             Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        final Intent intent = getParentActivityIntent();
        finish();
        startActivity(intent);
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, updateRssiFilter());


    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);

    }

    private static IntentFilter updateRssiFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_RSSI_READ);
        return intentFilter;
    }

    public void setmRssi(int rssi){
        times ++;
        mRssi.setText(times + "Received RSSI: "+rssi);
        mProgressBar.setProgress(rssi+100);

    }

    private void writeData(int rssi) {

        try {
            // Creates a file in the primary external storage space of the
            // current application.
            // If the file does not exists, it is created.
            File testFile = new File(this.getExternalFilesDir(null), "TestFile.txt");
            if (!testFile.exists())
                testFile.createNewFile();

            // Adds a line to the file
            BufferedWriter writer = new BufferedWriter(new FileWriter(testFile, true /*append*/));
            writer.write(""+ rssi + ", ");
            //writer.write(System.lineSeparator());
            Log.i("WRITE","writing"+rssi);
            writer.close();
            // Refresh the data so it can seen when the device is plugged in a
            // computer. You may have to unplug and replug the device to see the
            // latest changes. This is not necessary if the user should not modify
            // the files.
            MediaScannerConnection.scanFile(this,
                    new String[]{testFile.toString()},
                    null,
                    null);
        } catch (IOException e) {
            Log.e("ReadWriteFile", "Unable to write to the TestFile.txt file.");
        }
    }

}
