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
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.nurotron.ble_ui.GAIA_Library.Gaia;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static com.nurotron.ble_ui.MainActivity.BATTERY_RECEIVED;
import static com.nurotron.ble_ui.MainActivity.DEVICE_FOUND;
import static com.nurotron.ble_ui.MainActivity.EXTRAS_DEVICE_ADDRESS;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    public static int mConnectionState;

    //GAIA service
    private String DATA_UUID = "00001103-D102-11E1-9B23-00025B00A5A5";
    private String SERVICE_UUID = "00001100-D102-11E1-9B23-00025B00A5A5";
    private String RESPONSE_UUID = "00001102-D102-11E1-9B23-00025B00A5A5";
    private String COMMAND_UUID = "00001101-D102-11E1-9B23-00025B00A5A5";


    //battery service
    private String Battery_Service_UUID = "0000180f-0000-1000-8000-00805f9b34fb";
    private String Battery_Level_UUID = "00002a19-0000-1000-8000-00805f9b34fb";


    //read/write callback success
    public boolean readSuccess = false;
    public boolean writeSuccess = false;


    //retry read for 3 times if callback unsuccessful, suppose always write before read???
    private int count = 0;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";


    //RSSI
    public final static String ACTION_RSSI_READ =
            "com.example.bluetooth.le.ACTION_RSSI_READ";
    public final static String RSSI_DATA =
            "com.example.bluetooth.le.RSSI_DATA";

    //Battery
    public final static String ACTION_BATTERY_AVAILABLE =
            "com.example.bluetooth.le.ACTION_BATTERY_AVAILABLE";
    public final static String BATTERY_DATA =
            "com.example.bluetooth.le.BATTERY_DATA";


    //Notification
    public final static String ACTION_NOTIFICATION =
            "com.example.bluetooth.le.ACTION_NOTIFICATION";


    //store paired device
    KeyValueStore sharedPrefs;


    public int SERVICE_RUNNING_NOTIFICATION_ID = 10000;
    public int PHONE_BT_TURNON = 101;
    public int PHONE_BT_TURNOFF = 102;
    public static int requestID = 0;

    private boolean checkingBT = false;
    private boolean isOn = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sharedPrefs = new KeyValueStore.SharedPrefs(getApplicationContext());
//        if (sharedPrefs.getBool(DEVICE_FOUND)) {
//            initialize();
//            connect(sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS));
//            Log.v(TAG,"--------------------------Connect to BLE device in service.");
//        }
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getText(R.string.app_name))
                .setContentText("BLE service is running")
                .setSmallIcon(R.drawable.about)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.app_name))
                .build();

        startForeground(SERVICE_RUNNING_NOTIFICATION_ID, notification);


        initialize();

        Log.d(TAG,"Start service .............");

        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ){
            Intent i = getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage( getBaseContext().getPackageName() );
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
        else
            if(mConnectionState == STATE_DISCONNECTED)
            connect(sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS));

        return START_NOT_STICKY;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BTT-LE", "BLE State_CONNECTED" + " status = " + status);
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;


                sharedPrefs.putString(EXTRAS_DEVICE_ADDRESS, mBluetoothDeviceAddress);
                sharedPrefs.putBool(DEVICE_FOUND, true);
                Log.d("KeyValueStore","stored device" + mBluetoothDeviceAddress);


                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());



            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                Log.i("BTT-LE", "disconnected OK" + " status= " + status);
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;

                //Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);

            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){

            Log.v("CALLBACK","RSSI callback value " + rssi);
            Intent intent = new Intent();
            intent.setAction(ACTION_RSSI_READ);
            intent.putExtra(RSSI_DATA, rssi);
            sendBroadcast(intent);

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                if(requestID == PHONE_BT_TURNON) {
                    checkBT();
                    requestID = 0;
                }
                if(requestID == PHONE_BT_TURNOFF) {
                    startBT(0);
                    requestID = 0;
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.v("CALLBACK","Read callback");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //readSuccess = true;
//                Log.e("CALLBACK", " "+characteristic.getValue()[0]+ " "+characteristic.getValue()[1] + " " + characteristic.getValue()[2] + " " + characteristic.getValue()[3]+"\ndata length:" + characteristic.getValue().length);
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                readSuccess = true;
            }
            else{
                if( count < 3) {
                    count ++;
                    Log.e("COUNT","count = " + count);
                    readCharacteristic(characteristic);
                }
                if(count == 3)
                   // Toast.makeText(getApplicationContext(), "Operation failed!", Toast.LENGTH_SHORT).show();
                    readSuccess = false;
            }

//            else
//                Log.e("CALLBACK","read unsuccessful");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            Log.v("CALLBACK","Write callback");
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.e("CALLBACK","write successful");

                count = 0;
                writeSuccess = true;

//                if(!isOn && isConnectedByService) {
//                    isConnectedByService = false;
//                    disconnect();
//                }
            }

        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if(checkingBT){
                checkingBT = false;
                byte[] bluetooth = characteristic.getValue();
                if(bluetooth[5] == 0) {
                    isOn = true;
                    startBT(1);
                }
            }
            else
                broadcastUpdate(ACTION_NOTIFICATION, characteristic);
            //////
            byte[] noti_data = characteristic.getValue();
            //notification_alert(noti_data);
            Log.v(TAG,"NOTIFICATION DATA: " + noti_data.length + ", content: " + noti_data[0]);

//            if(isConnectedByService) {
//                isConnectedByService = false;
//                disconnect();
//            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);

    }


    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);


        if (Battery_Level_UUID.equals(characteristic.getUuid().toString())) {

            final byte[] battery = characteristic.getValue();
            Log.d(TAG, "Received battery level:" + battery[0]);
            intent.setAction(ACTION_BATTERY_AVAILABLE);
            intent.putExtra(BATTERY_DATA, battery[0]);
            sharedPrefs.putInt(BATTERY_RECEIVED, battery[0]);
        } else {
            final byte[] readData = characteristic.getValue();
            intent.setAction(action);
            intent.putExtra(EXTRA_DATA,readData);
            Log.v("BROADCAST", ""+ readData.length);
        }


        sendBroadcast(intent);


    }

//    private void broadcastUpdate(boolean action){
//        Intent intent = new Intent(action);
//        sendBroadcast(intent);
//    }

    @Override
    public void onCreate(){
        registerReceiver(phoneStateReceiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
        registerReceiver(phoneStateReceiver, new IntentFilter(PhoneCallActivity.ACTION_TO_SERVICE));
        registerReceiver(phoneStateReceiver, new IntentFilter(PhoneCallActivity.OUT_GOING_CALL));
        registerReceiver(phoneStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));


        mConnectionState = STATE_DISCONNECTED;
    }

    @Override
    public void onDestroy(){
        unregisterReceiver(phoneStateReceiver);
        close();
        Log.e(TAG,"BLE service shut down.");
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        //close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        /*
        AdvertiseSettings settings;
        settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();
        */
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;

                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.

        //mBluetoothGatt = device.connectGatt(this, false, mGattCallback, 2);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Method connectGattMethod = null;

                try {
                    connectGattMethod = device.getClass().getMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }

                try {
                    mBluetoothGatt = (BluetoothGatt) connectGattMethod.invoke(device, BluetoothLeService.this, false, mGattCallback, TRANSPORT_LE);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }, 500);

        //refreshDeviceCache(mBluetoothGatt);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        System.out.println("--------------------------> BLE disconnect called");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        System.out.println("==========================> BLE close called");
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);


    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

    }


    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public boolean readCustomCharacteristic(){
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(SERVICE_UUID));
        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
            return false;
        }
        BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(UUID.fromString(RESPONSE_UUID));
        return mBluetoothGatt.readCharacteristic(mReadCharacteristic);

    }


    public boolean writeCustomCharacteristic(byte[] value){
        if(mBluetoothGatt == null){
            Log.e(TAG, "lost connection");
            return false;
        }
        BluetoothGattService mService = mBluetoothGatt.getService(UUID.fromString(SERVICE_UUID));
        if(mService == null){
            Log.e(TAG,"service not found!");
            return false;
        }
        BluetoothGattCharacteristic mCharac = mService.getCharacteristic(UUID.fromString(COMMAND_UUID));
        if(mCharac == null){
            Log.e(TAG,"characteristic not found!");
            return false;
        }
        mCharac.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(mCharac);

        return status;
    }

    public void setCustomeNotification(boolean enable){
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
        }
        BluetoothGattService mCustomService = mBluetoothGatt.getService(UUID.fromString(SERVICE_UUID));

        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
            return;
        }
//        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mNotifyCharacteristic = mCustomService.getCharacteristic(UUID.fromString(RESPONSE_UUID));
        if(enable) {
//            UUID uuid = UUID.fromString("00002902-D102-11E1-9B23-00025B00A5A5");
//            BluetoothGattDescriptor descriptor = mNotifyCharacteristic.getDescriptor(uuid);
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            mBluetoothGatt.writeDescriptor(descriptor);
            //To receive notification in Android is mandatory to set characteristic notification to true
            mBluetoothGatt.setCharacteristicNotification(mNotifyCharacteristic, true);
            //data = "Notification Enabled.\n";
            Log.v(TAG, "Notification enabled!!!!!");
        }
        else
            mBluetoothGatt.setCharacteristicNotification(mNotifyCharacteristic, false);
    }

    public boolean getBattery(){

        if(mBluetoothGatt == null){
            Log.e(TAG, "lost connection");
            return false;
        }
        BluetoothGattService batteryService = mBluetoothGatt.getService(UUID.fromString(Battery_Service_UUID));
        if(batteryService == null) {
            Log.d(TAG, "Battery service not found!");
            return false;
        }

        BluetoothGattCharacteristic batteryLevel = batteryService.getCharacteristic(UUID.fromString(Battery_Level_UUID));
        if(batteryLevel == null) {
            Log.d(TAG, "Battery level not found!");
            return false;
        }
        mBluetoothGatt.readCharacteristic(batteryLevel);
        return true;
    }


    public void readRemoteRssi(){
        if(mBluetoothGatt == null){
            Log.e(TAG, "lost connection");
            return;
        }
        if (!mBluetoothGatt.readRemoteRssi())
            Log.e("RSSI", "false");

    }

    //phone state receiver
    public BroadcastReceiver phoneStateReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.e(TAG, "Bluetooth off");
                        stopSelf();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.v(TAG, "------------------------------>>>connect");
                        if (mConnectionState != STATE_CONNECTED) {
                            connect(sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS));

                        }
                        break;
                }
            }else {
                try {
                    System.out.println("Receiver start");
                    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        //Toast.makeText(context,"Turning on Enduro...",Toast.LENGTH_SHORT).show();
                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        Notification notification = new Notification.Builder(context)
                                .setSmallIcon(R.drawable.about)
                                .setContentText("Turning on Enduro... ")
                                .setPriority(Notification.PRIORITY_HIGH)
                                .build();
                        mNotificationManager.notify(0, notification);
                        sharedPrefs = new KeyValueStore.SharedPrefs(getApplicationContext());

                        if (sharedPrefs.getBool(DEVICE_FOUND)) {
                            initialize();
                            if (mConnectionState == STATE_CONNECTED) {
                                System.out.println("---------------------" + mConnectionState);
                                startBT(1);
                            } else if (mConnectionState == STATE_DISCONNECTED) {
                                connect(sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS));
                                Log.v(TAG, "--------------------------Connect to BLE device in service.");

                                requestID = PHONE_BT_TURNON;
                            }
                        } else
                            Log.d(TAG, "No previous device found!");
                        Log.i("PhoneCall", "ringing state");


                    }
                    if ((state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))) {
                        Log.i("PhoneCall", "offhook state");

                        // Toast.makeText(context,"Received State",Toast.LENGTH_SHORT).show();
                    }
                    if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        //Toast.makeText(context,"Idle State",Toast.LENGTH_SHORT).show();
                        Log.i("PhoneCall", "Idle state");


                        if (mConnectionState == STATE_CONNECTED) {
                            System.out.println("---------------------" + mConnectionState);
//                            if (isOn) {
//                                startBT(0);
//                                isOn = false;
//                            }
                            startBT(0);
                        } else if (mConnectionState == STATE_DISCONNECTED) {
                            initialize();
                            connect(sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS));
                            Log.v(TAG, "--------------------------Connect to BLE device in service.");
                            requestID = PHONE_BT_TURNOFF;
                        }


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (intent.getAction().equals(PhoneCallActivity.OUT_GOING_CALL)) {
                    Log.i("PhoneCall", "outgoing call");
                    if (sharedPrefs != null) {
                        if (sharedPrefs.getBool(DEVICE_FOUND)) {
                            if (mConnectionState == STATE_DISCONNECTED) {
                                initialize();
                                connect(sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS));
                                Log.v(TAG, "--------------------------Connect to BLE device in service.");
                                requestID = PHONE_BT_TURNON;
                            } else if (mConnectionState == STATE_CONNECTED) {
                                checkBT();
                            }
                        }
                    } else
                        Log.d(TAG, "No previous device found!");
                }
            }
        }
    };

    public void checkBT(){
        //check current bluetooth status (on/off)
        try {
            byte[] checkBT = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.COMMAND_CHECK_BT_STATUS, null);
            Log.d(TAG,"Checking BT...");
            if (writeCustomCharacteristic(checkBT)) {
                Log.i(TAG,"Check BT status command sent");
                checkingBT = true;
                setCustomeNotification(true);
                // readCharacteristic();
            }

            else{
                Toast.makeText(getApplicationContext(), "Service not found.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void startBT(int option){

        byte bt[] = new byte[1];
        bt[0] = (byte)option;
        try {
            //power on traditional bluetooth
            byte[] startBT = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.COMMAND_TURNON_BT,bt);
            if(option == 0)
                Log.d(TAG,"Turning off BT...");
            else
                Log.d(TAG,"Turning on BT...");



            if (writeCustomCharacteristic(startBT)) {
                //Log.i(TAG,"Turn on BT command sent");
                setCustomeNotification(true);
                // readCharacteristic();
            }
            else{
                //  Toast.makeText(getApplicationContext(), "Service not found.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
