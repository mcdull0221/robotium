/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nurotron.ble_ui;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nurotron.ble_ui.R;
import com.nurotron.ble_ui.menu.SlidingMenu;

import java.util.ArrayList;
import java.util.HashMap;

import static com.nurotron.ble_ui.MainActivity.EXTRAS_DEVICE_ADDRESS;
import static com.nurotron.ble_ui.MainActivity.DEVICE_FOUND;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothScanner;
    private boolean mScanning;
    private Handler mHandler;

    private SwipeRefreshLayout swipeContainer;

    private TextView scanText;


    private static final int REQUEST_ENABLE_BT = 1;
    ////// Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 30000;
    private static final int REQUEST_APP_SETTINGS = 2;
    private static final int REQUEST_ENABLE_GPS = 3;


    private Button mDemo;
    public static boolean isDemo = false;
    public static boolean isFirstTime = false;
    private Handler handler;

    AlertDialog alertDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getActionBar().setTitle(R.string.title_devices);
        setContentView(R.layout.start_screen);


        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        mDemo = (Button) findViewById(R.id.demo);
        mDemo.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(DeviceScanActivity.this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, "Demo device");
                intent.putExtra(EXTRAS_DEVICE_ADDRESS, "0");
                if (mScanning) {
                   // mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mBluetoothScanner.stopScan(mScanCallback);
                    mScanning = false;
                }
                isDemo = true;
                startActivity(intent);
            }
        });


        scanText = (TextView) findViewById(R.id.scan_text);
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){

            @Override
            public void onRefresh() {

                scanLeDevice(false);
                mLeDeviceListAdapter = new LeDeviceListAdapter();
                setListAdapter(mLeDeviceListAdapter);
                scanLeDevice(true);

                swipeContainer.setRefreshing(false);
            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //super.onCreateOptionsMenu(menu);
        new MenuInflater(getApplication()).inflate(R.menu.main, menu);
        //getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);

        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);

        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;

//        mLeDeviceListAdapter.clear();
//        scanLeDevice(true);
//        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //checkPermission();
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        checkPermission();

        swipeContainer.setRefreshing(false);

    }

    private void checkPermission(){
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        else {
            // Initializes list view adapter.
            mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();
            scanLeDevice(true);
        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DeviceScanActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE},
                    REQUEST_APP_SETTINGS);
        }
        final LocationManager manager = (LocationManager) getSystemService(getApplicationContext().LOCATION_SERVICE);

        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.location_request));
            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, REQUEST_ENABLE_GPS);
                    alertDialog.dismiss();

                }
            });
            alertDialog = builder.show();
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        else if(requestCode == REQUEST_ENABLE_BT && requestCode == Activity.RESULT_OK){

            mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();

            scanLeDevice(true);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
///////////////
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.pair_to_device) + device.getName() + "?")
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        // startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, Uri.parse("package:" + getPackageName())));
                        dialog.cancel();
                        isFirstTime = true;
                        final Intent intent = new Intent(DeviceScanActivity.this,MainActivity.class);
                        intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, device.getName());
                        intent.putExtra(EXTRAS_DEVICE_ADDRESS, device.getAddress());
                        if (mScanning) {
                            //mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            mBluetoothScanner.stopScan(mScanCallback);
                            mScanning = false;
                        }
                        //startActivity(intent);
                        handler = new Handler();
                        final Runnable r = new Runnable() {
                            public void run() {
                                //Intent i = new Intent(DeviceScanActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        };
                        handler.postDelayed(r, 100);

                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();

                    }
                });
        final android.app.AlertDialog alert = builder.create();
        alert.show();

//        final Intent intent = new Intent(this, MainActivity.class);
//        intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, device.getName());
//        intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
//        if (mScanning) {
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            mScanning = false;
//        }
//        finish();
//        startActivity(intent);


    }

    private void scanLeDevice(final boolean enable) {
        Runnable scanHandler = new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                // mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mBluetoothScanner.stopScan(mScanCallback);
                scanText.setText(R.string.refresh);
                invalidateOptionsMenu();
            }
        };
        mHandler.removeCallbacks(scanHandler);

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(scanHandler, SCAN_PERIOD);

            mScanning = true;
           // mBluetoothAdapter.startLeScan(mLeScanCallback);
            mBluetoothScanner.startScan(mScanCallback);
            scanText.setText(R.string.search);
        } else {

            mScanning = false;
            //mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mBluetoothScanner.stopScan(mScanCallback);
            scanText.setText(R.string.refresh);
        }

        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;
        private HashMap<String, Integer> mRssi;

 //       discoveredDevicesRSSI.put(device.getName(), String.valueOf(rssi));
///////////////
        public int getRssi(String address){
            return mRssi.get(address);
        }
        public void setRssi(String address, int rssi){
            this.mRssi.put(address, rssi);
        }


        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mRssi = new HashMap<String, Integer>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
            mRssi.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
               // viewHolder.deviceRssi = (TextView) view.findViewById(R.id.rssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();

            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            byte rssival = (byte) getRssi(device.getAddress());
//            viewHolder.deviceRssi.setText(String.valueOf(rssival));

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    mLeDeviceListAdapter.setRssi(device.getAddress(), rssi);
                    System.out.print(scanRecord.toString());
                }
            });
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(result.getDevice().getAddress().contains("00:02:5B"))
                        mLeDeviceListAdapter.addDevice(result.getDevice());
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    mLeDeviceListAdapter.setRssi(result.getDevice().getAddress(),result.getRssi());
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
    }
}