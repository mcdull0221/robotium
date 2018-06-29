package com.nurotron.ble_ui;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;

import android.nfc.Tag;
import android.os.Handler;

import android.os.Build;

import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.nurotron.ble_ui.GAIA_Library.GaiaCommand;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.nurotron.ble_ui.menu.SlidingMenu;

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import com.nurotron.ble_ui.GAIA_Library.Gaia;


public class MainActivity extends AppCompatActivity implements LocationListener {

    private Intent gattServiceIntent;
    private ComponentName myService;

    private final static String TAG = MainActivity.class.getSimpleName();

    //shared preference stored key
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String DEVICE_FOUND = "DEVICE_FOUND";
    public static final String BATTERY_RECEIVED = "BATTERY_RECEIVED";
    public static final String CURRENT_PROGRAM = "CURRENT_PROGRAM";
    public static final String CURRENT_VOLUME = "CURRENT_VOLUME";
    public static final String LAST_KNOWN_LAT = "LAST_KNOWN_LAT";
    public static final String LAST_KNOWN_LON = "LAST_KNOWN_LON";
    public static final String CURRENT_MODE = "CURRENT_MODE";
    public static final String CURRENT_INPUT = "CURRENT_INPUT";
    public static final String CURRENT_CTONE = "CURRENT_CTONE";
    public static final String CURRENT_NR = "CURRENT_NR";

    //Chinese
    public static final String PROGRAM1_ZH = "PROGRAM1_ZH";
    public static final String PROGRAM2_ZH = "PROGRAM2_ZH";
    public static final String PROGRAM3_ZH = "PROGRAM3_ZH";
    public static final String PROGRAM4_ZH = "PROGRAM4_ZH";
    //English
    public static final String PROGRAM1_EN = "PROGRAM1_EN";
    public static final String PROGRAM2_EN = "PROGRAM2_EN";
    public static final String PROGRAM3_EN = "PROGRAM3_EN";
    public static final String PROGRAM4_EN = "PROGRAM4_EN";
    //Spanish
    public static final String PROGRAM1_ES = "PROGRAM1_ES";
    public static final String PROGRAM2_ES = "PROGRAM2_ES";
    public static final String PROGRAM3_ES = "PROGRAM3_ES";
    public static final String PROGRAM4_ES = "PROGRAM4_ES";

    public static String PROGRAM1;
    public static String PROGRAM2;
    public static String PROGRAM3;
    public static String PROGRAM4;


    //Current status string, pass to StatusActivity
    public static boolean DEVICE_STATUS = false;
    public static boolean IMPLANT_STATUS = false;

    //number of connected device, left and right
    public static int numberOfDevice = 1;

    public static boolean success = false;


    View inflatedView;
    public PresenterListener listener;
    ActionToolbarPresenter actionToolbarPresenter;

    public BluetoothLeService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_APP_SETTINGS = 2;
    LocationManager locationManager;


    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected;

    //notification received data
    private byte receivedBattery;
    private byte[] receiveData;
    private int receivedRSSI;



    //command code
    byte[] volume = new byte[1];
    public byte pgm[] = new byte[1];
    byte[] set = new byte[1];
    private int type = 0;


    KeyValueStore sharedPrefs;


    ImageView img_page1, img_page2;
    SlidingMenu menu;

    VolumeTab v; // = new VolumeTab();
    PreprocessingTab prep;

    //Notification
    Timer batteryTimer = new Timer("Battery Timer");
    private boolean batteryLow = false;

    Timer notificationTimer = new Timer("Notification Timer");
    private int ntfCount = 0;
    private int retry = 0;
    private boolean isNotified = false;
    private boolean isRetry = false;

    private Vector<AlertDialog> dialogs = new Vector<AlertDialog>();




    ImageView actionToolbarIcon;
    boolean doubleBackToExitPressedOnce = false;

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }


           // BluetoothLeService.requestID = 0;
            //BluetoothLeService.isConnectedByService = false;
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLeService.getBattery();
            //mBluetoothLeService.connect(mDeviceAddress);
            if(mBluetoothLeService != null){
                if(BluetoothLeService.mConnectionState == BluetoothLeService.STATE_CONNECTED){
                    mConnected = true;
                    getCurrentStatus();
                    Toast.makeText(getApplicationContext(), getString(R.string.device_connected), Toast.LENGTH_SHORT).show();

                }
                else if (BluetoothLeService.mConnectionState == BluetoothLeService.STATE_DISCONNECTED) {
                    mBluetoothLeService.connect(mDeviceAddress);
                    Toast.makeText(getApplicationContext(), getString(R.string.wait), Toast.LENGTH_LONG).show();

                    Log.v(TAG, "------------------------------connect--------------------------------");
                }

            }

            // v.lockButton(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device, result of read operation.
    // ACTION_NOTIFICATION: received data from the device notification.
    // ACTION_RSSI_READ: received RSSI from the device.
    // ACTION_BATTERY_AVAILABLE: received battery value from device.
    public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;

                Toast.makeText(getApplicationContext(), getString(R.string.device_connected), Toast.LENGTH_SHORT).show();

                DEVICE_STATUS = true;
                waitfor(500);
                inflatedView.setAlpha(1);
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                closeDialogs();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                sharedPrefs.putInt(BATTERY_RECEIVED, 0);
                actionToolbarPresenter.setBattery(0);
                alertConnection();
                batteryTimer.cancel();
                
              //  mBluetoothLeService.disconnect();
                DEVICE_STATUS = false;
                if(v!= null && prep!= null) {
                    v.lockButton(true);
                    prep.lockRadioButton(true);
                }

                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //////       displayGattServices(mBluetoothLeService.getSupportedGattServices());
                getCurrentStatus();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                receiveData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                //        checkChangeResult(type);
                // displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

                Log.e("READ data available", "Data length:" + receiveData.length);

            } else if (BluetoothLeService.ACTION_RSSI_READ.equals(action)) {
                receivedRSSI = intent.getIntExtra(BluetoothLeService.RSSI_DATA, 0);
                Log.v("RSSI", "Rssi received " + receivedRSSI);
                //mLocateActivity.setmRssi(receivedRSSI);

            } else if (BluetoothLeService.ACTION_BATTERY_AVAILABLE.equals(action)) {
                receivedBattery = intent.getByteExtra(BluetoothLeService.BATTERY_DATA, (byte) 0);
                if (receivedBattery > 20)
                    batteryLow = false;
                //comment to disable battery low alert updated by reading characteristic
//                if (receivedBattery <= 20 && !batteryLow) {
//                    alertLowBattery();
//                    batteryLow = true;
//                }
                sharedPrefs.putInt(BATTERY_RECEIVED, receivedBattery);
                actionToolbarPresenter.setBattery(sharedPrefs.getInt(BATTERY_RECEIVED, 0));
//                mBatteryFrame.updateBattery(receivedBattery);
            } else if (BluetoothLeService.ACTION_NOTIFICATION.equals(action)) {
                inflatedView.setAlpha(1);
                receiveData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);

                getACKStatus();
                isNotified = true;
                v.lockButton(false);
                prep.lockRadioButton(false);
                Log.e("NOTIFICATION", "Data length:" + receiveData.length);
            }
            else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.e(TAG, "Bluetooth off");
                        batteryTimer.cancel();
                        onResume();
                        break;


                }

            }

        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        sharedPrefs = new KeyValueStore.SharedPrefs(getApplicationContext());
        configureLanguage();

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();


        DEVICE_STATUS = false;
        IMPLANT_STATUS = false;

        /* B2 */

        gattServiceIntent = new Intent(this, BluetoothLeService.class);

        if(!isMyServiceRunning(BluetoothLeService.class)) {
            Log.d(TAG, "onStartService() called onCreate");
            myService = startService(gattServiceIntent);
            Toast.makeText(this, getString(R.string.wait), Toast.LENGTH_LONG).show();

        }

        inflatedView = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(inflatedView);

        actionToolbarIcon = (ImageView) findViewById(R.id.action_toolbar_icon);

        configureSlidingMenu();
        configureViewPresenters(inflatedView);
        configureStatusBarBackground();


        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);

        // use sharePref.putString(<key>, <String>) to store a value to persistent storage like this:
        //sharedPrefs.putString(EXTRAS_DEVICE_ADDRESS, "aa:bb:cc:dd:ee:ff");
        //sharedPrefs.putBool(DEVICE_FOUND, false);
        // to find out if a device is stored in sharedPrefs, use:
        if (sharedPrefs.getBool(DEVICE_FOUND)) {
            // happy! please ble_connect to it
            mDeviceAddress = sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS);
            //    mBluetoothLeService.connect(mDeviceAddress);
            //sharedPrefs.putBool(DEVICE_FOUND, true);
        } else {
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            // scan them! When connected, do not forget to perform:
            sharedPrefs.putBool(DEVICE_FOUND, false);

            Toast.makeText(this, getString(R.string.wait), Toast.LENGTH_LONG).show();
            // see KeyValueStore.java for other function. reset() can be useful later.
            // you can also use sharePref to store battery level (so you don't need to send an intent.
            // LeyValueStore.java is thread-safe guaranteed. You can store battery level in your ble service and periodically access it from MainActivity.
        }

        img_page1 = (ImageView) findViewById(R.id.img_page1);
        img_page2 = (ImageView) findViewById(R.id.img_page2);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.


        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                //currentPosition = position;
                //seekBar.setOnSeekBarChangeListener(painLevelHandler);
                switch (position) {
                    case 0:
                        img_page1.setImageResource(R.drawable.dot_24);
                        img_page2.setImageResource(R.drawable.dot);
                        break;

                    case 1:
                        img_page1.setImageResource(R.drawable.dot);
                        img_page2.setImageResource(R.drawable.dot_24);
                        break;

                    default:
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


//        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        v = new VolumeTab();
        prep = new PreprocessingTab();

        actionToolbarPresenter.setBattery(sharedPrefs.getInt(BATTERY_RECEIVED, 0));


        setTypefaceToAll(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);


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
//        actionToolbarPresenter.setBattery(sharedPrefs.getInt(BATTERY_RECEIVED, 0));
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


    ////Alert: show alert dialog when battery low, implant abnormal, lost connection, notification not received

    public void alertConnection() {
        AlertDialog connectionAlert = new AlertDialog.Builder(this).create();
        connectionAlert.setCanceledOnTouchOutside(false);
        // Setting Dialog Title
        connectionAlert.setTitle(getString(R.string.menu_connect));

        // Setting Dialog Message
        connectionAlert.setMessage(getString(R.string.retry));

        // Setting OK Button
//        connectionAlert.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.menu_disconnect), new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                //mBluetoothLeService.close();
//                waitfor(500);
//                mBluetoothLeService.connect(mDeviceAddress);
//                dialog.cancel();
//            }
//        });
        connectionAlert.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        // Showing Alert Message
        dialogs.add(connectionAlert);
        connectionAlert.show();
        inflatedView.setAlpha(0.5f);

    }

    public void alertLowBattery() {
        AlertDialog batteryAlert = new AlertDialog.Builder(this).create();

        // Setting Dialog Title
        batteryAlert.setTitle(getString(R.string.alert));

        // Setting Dialog Message
        batteryAlert.setMessage(getString(R.string.charge));

        // Setting OK Button
        batteryAlert.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        dialogs.add(batteryAlert);
        batteryAlert.show();

    }

    public void alertImplantAbnormal(){

        AlertDialog implantAlert = new AlertDialog.Builder(this).create();

        // Setting Dialog Title
        implantAlert.setTitle(getString(R.string.alert));

        // Setting Dialog Message
        implantAlert.setMessage(getString(R.string.implant_alert));

        // Setting OK Button
        implantAlert.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        implantAlert.setCanceledOnTouchOutside(false);
        // Showing Alert Message
        dialogs.add(implantAlert);
        implantAlert.show();

    }
    private void DSPNotification(GaiaCommand notification) throws IOException {
        //ACK payload, length = 2
        byte[] payload = new byte[2];
        payload[0] = 0;
        if( notification.getPayload()[0] == (byte) 0x81 ) {
            //ack
            payload[1] = (byte) 0x81;
            byte[] ackFrame = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.ACK_DSP_NOTIFICATION, payload);

            if (writeCharacteristic(ackFrame))
                Log.d("NOTIFICATION", "ACK sent");
            else
                Log.d("NOTIFICATION", "ACK cannot be sent");

            //response
            closeDialogs();
            alertImplantAbnormal();

        }
        else if (notification.getPayload()[0] == (byte) 0x82){
            //ack
            payload[1] = (byte) 0x82;
            byte[] ackFrame = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.ACK_DSP_NOTIFICATION, payload);
            if (writeCharacteristic(ackFrame))
                Log.d("NOTIFICATION", "ACK sent");
            else
                Log.d("NOTIFICATION", "ACK cannot be sent");

            //response
            closeDialogs();
            Toast.makeText(getApplicationContext(), R.string.implant_connected, Toast.LENGTH_SHORT).show();
            Log.d("NOTIFICATION", "implant connected");
            waitfor(500);
            getCurrentStatus();
        }
        else if (notification.getPayload()[0] == (byte) 0x83){
            //ack
            payload[1] = (byte) 0x83;
            byte[] ackFrame = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.ACK_DSP_NOTIFICATION, payload);
            if (writeCharacteristic(ackFrame))
                Log.d("NOTIFICATION", "ACK sent");
            else
                Log.d("NOTIFICATION", "ACK cannot be sent");

            //response
            waitfor(500);
            getCurrentStatus();
            Log.d("NOTIFICATION", "DSP value update");
        }
        else if(notification.getPayload()[0] == (byte) 0x84){
            //ack
            payload[1] = (byte) 0x84;
            byte[] ackFrame = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.ACK_DSP_NOTIFICATION, payload);

            if (writeCharacteristic(ackFrame))
                Log.d("NOTIFICATION", "ACK sent");
            else
                Log.d("NOTIFICATION", "ACK cannot be sent");

            //response
            closeDialogs();
            alertLowBattery();
            Log.d("NOTIFICATION", "battery low alert");
        }
        else
            Log.d("NOTIFICATION", "notification payload: " + notification.getPayload()[0]);

    }

    private void notificationAlert(){
        AlertDialog notification = new AlertDialog.Builder(this).create();

        // Setting Dialog Title
        notification.setTitle(getString(R.string.alert));

        // Setting Dialog Message
        notification.setMessage(getString(R.string.notification_alert));

        // Setting OK Button
        notification.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        notification.setCanceledOnTouchOutside(false);
        // Showing Alert Message
        dialogs.add(notification);

        if(!MainActivity.this.isFinishing())
        {
            //show dialog
            notification.show();
        }

    }
    public void closeDialogs() {
        System.out.println(">>>>>>>>>>> AlertDialog size = " + dialogs.size());
        Iterator<AlertDialog> iter = dialogs.iterator();
        while(iter.hasNext()){
            AlertDialog dialog = iter.next();
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            iter.remove();
        }
    }

    ////Lifecycle functions
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
//            if(mBluetoothLeService != null)
//                mBluetoothLeService.disconnect();
            finishAffinity();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.double_press, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }


    @Override
    protected void onResume() {

        super.onResume();
        //show impression of disabled buttons
        inflatedView.setAlpha(0.5f);

        checkPermission();

        System.out.println("VerticalSlidebarExample onResume() called");

        if (gattServiceIntent == null) {
            gattServiceIntent = new Intent(this, BluetoothLeService.class);
        }
        if(!isMyServiceRunning(BluetoothLeService.class)) {
            Log.d(TAG, "onStartService() called");
            myService = startService(gattServiceIntent);
            Toast.makeText(this, getString(R.string.wait), Toast.LENGTH_LONG).show();

        }
        /* B2 */
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
////
//        if (mServiceConnection != null) {
//            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//            System.out.println("onResume() bind service calles");
//        }
        //if (mBluetoothLeService != null)
            //mBluetoothLeService.connect(mDeviceAddress);
        configureDayNight();

        if(DeviceScanActivity.isFirstTime) {
            pairBluetoothRequest();
            DeviceScanActivity.isFirstTime = false;
        }

    }


    private void pairBluetoothRequest(){
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        if(!mBluetoothAdapter.getBondedDevices().contains(device)){
            final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.bluetooth_pair));
            builder.setPositiveButton(getString(R.string.goto_bluetooth), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Intent intentOpenBluetoothSettings = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                    startActivity(intentOpenBluetoothSettings);
                }
            });
            builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        this.batteryTimer.cancel();
        Log.e(TAG, "Timer is canceled onPause.");
//        if (mBluetoothLeService != null)
//            mBluetoothLeService.disconnect();
        unbindService(mServiceConnection);

       // mBluetoothLeService = null;
        closeDialogs();
    }

    @Override
    public void onStart() {
        super.onStart();
//        locationManager = (LocationManager) getSystemService(this.LOCATION_SERVICE);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//            ActivityCompat.requestPermissions(MainActivity.this,
//                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE},
//                    REQUEST_APP_SETTINGS);
//
//            return;
//        }
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, this);

    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
//        if(requestCode == REQUEST_APP_SETTINGS && grantResults.equals(PackageManager.PERMISSION_GRANTED))
//            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//
//
//    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unbindService(mServiceConnection);
//        if (mBluetoothLeService != null)
//            mBluetoothLeService.close();
        mBluetoothLeService = null;
        batteryTimer.cancel();
        Log.e(TAG, "Timer is canceled onDestroy.");
        closeDialogs();
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
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
        // location service

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE},
                    REQUEST_APP_SETTINGS);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_RSSI_READ);
        intentFilter.addAction(BluetoothLeService.ACTION_BATTERY_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_NOTIFICATION);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return intentFilter;
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }


    @Override
    public void onStop() {
        super.onStop();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d(TAG,"Service is running");

                return true;
            }
        }
        Log.d(TAG,"Service has stopped");

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // TODO Auto-generated method stub

        super.onActivityResult(requestCode, resultCode, data);
        //if (requestCode == setting)
            //checkSettings();
        //if (requestCode == locating)
            //rssiTimer.cancel();
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            // TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            //textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if (position == 0) {

                v.setCustomerListener(new VolumeTab.OnSeekbarSelectedListener() {

                    @Override
                    public void onSeekbarSelected(int vol) {
                        try {
                            System.out.println(">>>>>>>>>>>>>>>>Volume clicked");
                            if(change_vol(vol))
                                v.lockButton(true);
                            else
                                v.lockButton(false);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onButtonSelected(int program) {
                        try {
                            System.out.println(">>>>>>>>>>>>>>>>Program clicked");
                            if(change_program(program))
                                v.lockButton(true);
                            else
                                v.lockButton(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onBluetoothButtonSelected(int option) {
                        if(option == 0)
                            mBluetoothLeService.startBT(0);
                        else
                            mBluetoothLeService.startBT(1);
                    }
                });
                return v;
            }
            if (position == 1){

                prep.setCustomerListener(new PreprocessingTab.OnButtonChangeListener(){

                    @Override
                    public void onButtonChange(String setting, int value) {
                        try {
                            change_pre(setting, value);
                            prep.lockRadioButton(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onProgramNameChanged(int program) {
                        v.changeProgramName();
                        v.setCurrentProgram(program);
                    }

                });
                return prep;
            }

            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 4 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Volume";
                case 1:
                    return "Pre-processing";

            }
            return null;
        }
    }

    public boolean readCharacteristic(){
        if (mBluetoothLeService != null && mConnected) {
            if (mBluetoothLeService.readCustomCharacteristic()) {
                Log.v(TAG, "Successfully read.");
                return true;
            }
        }
        return false;
    }

    public boolean writeCharacteristic(byte[] vol) {

        if (mBluetoothLeService != null && mConnected) {
            if (mBluetoothLeService.writeCustomCharacteristic(vol)) {
                Log.v(TAG, "Successfully write byte to device.");
                return true;
            }
        }
        return false;
    }

    public void enableNotify() {
        if (mBluetoothLeService != null && mConnected) {

            mBluetoothLeService.setCustomeNotification(true);
            //displayData("Notification Enabled!");
          //  Toast.makeText(getApplicationContext(), "Notification Enabled.", Toast.LENGTH_SHORT).show();
        }
        isNotified = false;
        //// TODO: 10/19/2017 setup timer
        if(!isRetry)
            setNotificationTimer();

    }

    public void disableNotify() {
        if (mBluetoothLeService != null && mConnected) {

            mBluetoothLeService.setCustomeNotification(false);
            // displayData("Notification Disabled!");
            Toast.makeText(getApplicationContext(), "Notification Disabled.", Toast.LENGTH_SHORT).show();
        }
    }

//    @Override
//    public boolean onKeyDown(int keycode, KeyEvent event){
//        if(keycode == KeyEvent.KEYCODE_VOLUME_DOWN){
//            v.mute.performClick();
//            return true;
//        }
//        else if(keycode == KeyEvent.KEYCODE_VOLUME_UP){
//            v.add_vol.performClick();
//            return true;
//        }
//        else
//            return super.onKeyDown(keycode,event);
//    }

    public boolean change_vol(int progress) throws IOException {
        int retry = 0;
        type = 1;   //volume

        volume[0] = (byte) progress;

        byte[] volFrame = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.COMMAND_SET_VOLUME,volume);

        Log.d("VOLUME","Set to " + progress + "; Frame length: " + volFrame.length);

        if(DeviceScanActivity.isDemo){
            Toast.makeText(getApplicationContext(), "Volume changed.", Toast.LENGTH_SHORT).show();
            return true;
        }
        while(retry < 3 && !writeCharacteristic(volFrame)){
            retry ++;
        }
        if (retry < 3) {
            enableNotify();            //waitfor(5000);
            // readCharacteristic();
            return true;
        }
        else{

            Toast.makeText(getApplicationContext(), getString(R.string.service_not_found), Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    public boolean change_program(int position) throws IOException {
        int retry = 0;
        type = 2;       //program
        int program = position + 1;
        pgm[0] = (byte) program;

        byte[] pgmFrame = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.COMMAND_SET_MAP,pgm);

        Log.d("PROGRAM","Set to " + program + "; Frame length: " + pgmFrame.length);

        if(DeviceScanActivity.isDemo){
            Toast.makeText(getApplicationContext(), "Program changed.", Toast.LENGTH_SHORT).show();
            return true;
        }

        while(retry < 3 && !writeCharacteristic(pgmFrame)){
            retry ++;
            Log.e(TAG,"write to device failed. retry = " + retry);
        }
        if (retry < 3) {
            Log.d("RESULT", pgmFrame[0]+ " " +pgmFrame[1] +" " + pgmFrame[2] +" " + pgmFrame[3] +" " + pgmFrame[4]);
            enableNotify();
            return true;
            //waitfor(5000);
            // readCharacteristic();
        }
        else{
            Toast.makeText(getApplicationContext(),  getString(R.string.service_not_found), Toast.LENGTH_SHORT).show();
            v.setCurrentProgram(sharedPrefs.getInt(CURRENT_PROGRAM, 0));
            v.lockButton(false);
            return false;
        }
//        if (mBluetoothLeService.readSuccess == false)
//            Toast.makeText(getApplicationContext(), "Failed to change the program.", Toast.LENGTH_SHORT).show();

    }


    public void getACKStatus() {
        if (receiveData != null && receiveData.length > 0) {

            GaiaCommand ack = new GaiaCommand(receiveData, true);
            if (ack.getCommandId() == Gaia.COMMAND_EVENT_NOTIFICATION) {
                try {
                    DSPNotification(ack);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(ack.getCommandId() == Gaia.ACK_TURN_BT){
                Log.v("ACK", "Turn on/off bluetooth command received.");
            }
            else {
                if (ack.getCustomStatusCode() == 0) {
                    byte[] gaiaPayload = ack.getPayload();
                    if (gaiaPayload.length > 7)
                        checkChangeResult(gaiaPayload, ack.getCommandId());
                    else{
                        v.setVolumeBar(sharedPrefs.getInt(CURRENT_VOLUME,0));
                        v.setCurrentProgram(sharedPrefs.getInt(CURRENT_PROGRAM, 0));

                        Toast.makeText(getApplicationContext(), getString(R.string.operation_failed), Toast.LENGTH_SHORT).show();
                        Log.e("ACK", "Payload incomplete");
                    }
                } else {
                    //error
                    v.setVolumeBar(sharedPrefs.getInt(CURRENT_VOLUME,0));
                    v.setCurrentProgram(sharedPrefs.getInt(CURRENT_PROGRAM, 0));

                    Toast.makeText(getApplicationContext(), getString(R.string.operation_failed), Toast.LENGTH_SHORT).show();
                    Log.e("ACK", "status code error");
                }

            }
        } else {
          //  v.setVolumeBar(sharedPrefs.getInt(CURRENT_VOLUME,0));
          //  v.setCurrentProgram(sharedPrefs.getInt(CURRENT_PROGRAM, 0));

            Toast.makeText(getApplicationContext(), getString(R.string.operation_failed), Toast.LENGTH_SHORT).show();
            Log.e("ACK", "ACK null value");
        }
    }




    public void checkChangeResult(byte[] ackPayload, int commandID){


        Log.d("RESULT", ""+ackPayload[0] + " " + ackPayload[1] + " " + ackPayload[2] + " " + ackPayload[3] + " "+ ackPayload[4] + " "+ ackPayload[5] + " "+ ackPayload[6] + " "+ ackPayload[7] + " "+ ackPayload[8] + " ");


        int dStatus = byte2bit(ackPayload[8], 7);

        int bStatus = byte2bit(ackPayload[8],5);

        Log.d("RESULT", "implant: " + dStatus + "battery: " + bStatus);
        if(bStatus == 1){
            closeDialogs();
            alertLowBattery();
            v.setVolumeBar(sharedPrefs.getInt(CURRENT_VOLUME,0));
            v.setCurrentProgram(sharedPrefs.getInt(CURRENT_PROGRAM, 0));
            return;
        }
        if(ackPayload[2] == 0){

            getCurrentStatus();
            return;
        }

        if (dStatus == 0)
            IMPLANT_STATUS = true;
        else {
            IMPLANT_STATUS = false;
            closeDialogs();
            alertImplantAbnormal();
            v.setVolumeBar(sharedPrefs.getInt(CURRENT_VOLUME,0));
            v.setCurrentProgram(sharedPrefs.getInt(CURRENT_PROGRAM, 0));

            //Toast.makeText(getApplicationContext(),getString(R.string.implant_alert),Toast.LENGTH_LONG).show();
            return;
        }

        switch (type) {
            //initialization
            case 0: {

                sharedPrefs.putInt(CURRENT_PROGRAM, ackPayload[2]);
                v.setCurrentProgram(ackPayload[2]);
                sharedPrefs.putInt(CURRENT_VOLUME, ackPayload[3]);
                v.setVolumeBar(ackPayload[3]);
                sharedPrefs.putInt(CURRENT_INPUT,ackPayload[4]);
                prep.setInputSource(ackPayload[4]);
                sharedPrefs.putInt(CURRENT_MODE, ackPayload[5]);
                prep.setMode(ackPayload[5]);
                sharedPrefs.putInt(CURRENT_CTONE,ackPayload[6]);
                prep.setCtone(ackPayload[6]);
                sharedPrefs.putInt(CURRENT_NR, ackPayload[7]);
                prep.setNR(ackPayload[7]);
                prep.setProgramName(ackPayload[2]);
                v.lockButton(false);
                prep.lockRadioButton(false);
                break;
            }
            //volume
            case 1: {
                if (ackPayload[3] == volume[0]) {
                    //   mBatteryFrame.updateImplantStatus(gaiaReceived);
                    sharedPrefs.putInt(CURRENT_VOLUME, ackPayload[3]);
                    v.setVolumeBar(ackPayload[3]);
                   // Toast.makeText(getApplicationContext(), "Successfully changed volume to " + ackPayload[3], Toast.LENGTH_SHORT).show();
                }
                else {
                    v.setVolumeBar(sharedPrefs.getInt(CURRENT_VOLUME,0));
                    Toast.makeText(getApplicationContext(), getString(R.string.unable_volume) + " " + volume[0], Toast.LENGTH_SHORT).show();
                }
                break;
            }
            //program
            case 2: {

//                    p.setPgmByte(receiveData);
                if (ackPayload[2] == pgm[0]) {

//                    v.setCurrentProgram(ackPayload[2]);
                    sharedPrefs.putInt(CURRENT_PROGRAM, ackPayload[2]);

                    sharedPrefs.putInt(CURRENT_VOLUME, ackPayload[3]);
                    v.setVolumeBar(ackPayload[3]);

                    sharedPrefs.putInt(CURRENT_INPUT,ackPayload[4]);
                    prep.setInputSource(ackPayload[4]);
                    sharedPrefs.putInt(CURRENT_MODE, ackPayload[5]);
                    prep.setMode(ackPayload[5]);
                    sharedPrefs.putInt(CURRENT_CTONE,ackPayload[6]);
                    prep.setCtone(ackPayload[6]);
                    sharedPrefs.putInt(CURRENT_NR, ackPayload[7]);
                    prep.setNR(ackPayload[7]);
                    prep.setProgramName(ackPayload[2]);

                    success = true;
                    //Toast.makeText(getApplicationContext(), "Successfully changed program to " + (ackPayload[2]), Toast.LENGTH_SHORT).show();
                }
                else {
                    success = false;
//                    if(sharedPrefs.getInt(CURRENT_PROGRAM, 0) != 0)
//                        v.setCurrentProgram(sharedPrefs.getInt(CURRENT_PROGRAM, 0));
//                    else
//                        v.setCurrentProgram(ackPayload[2]);

                    getCurrentStatus();

                    Toast.makeText(getApplicationContext(), getString(R.string.unable_program) + " " + (pgm[0]), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            //preprocessing
            case 3:{
                switch(commandID){
                    case Gaia.ACK_SET_MODE:{
                        if(ackPayload[5] == set[0])
                            //Toast.makeText(getApplicationContext(), "Successfully changed mode.", Toast.LENGTH_SHORT).show();
                            Log.d("Preprocessing", "mode changed");
                        else {

                            Toast.makeText(getApplicationContext(), getString(R.string.unable_mode), Toast.LENGTH_SHORT).show();
                            getCurrentStatus();
                        }
                        break;
                    }
                    case Gaia.ACK_SET_CTONE:{
                        if(ackPayload[6] == set[0])
                            Log.d("Preprocessing", "ctone changed");
                        else {
                            Toast.makeText(getApplicationContext(), getString(R.string.unable_ctone), Toast.LENGTH_SHORT).show();
                            getCurrentStatus();
                        }
                        break;
                    }
                    case Gaia.ACK_SET_INPUT_SOURCE:{
                        if(ackPayload[4] == set[0]){
                            Log.d("Preprocessing", "Input source changed");
                            sharedPrefs.putInt(CURRENT_INPUT,ackPayload[4]);
                            prep.setInputSource(ackPayload[4]);

                        }
                        else {
                            Toast.makeText(getApplicationContext(), getString(R.string.unable_input), Toast.LENGTH_SHORT).show();
                            getCurrentStatus();
                        }
                        break;
                    }
                    case Gaia.ACK_SET_NOISE_REDUCTION:{
                        if(ackPayload[7] == set[0])
                            Log.d("Preprocessing", "Noise Reduction changed");
                        else {
                            Toast.makeText(getApplicationContext(), getString(R.string.unable_noise), Toast.LENGTH_SHORT).show();
                            getCurrentStatus();
                        }
                        break;
                    }
                    default: break;
                }
            }
            default: break;
        }

    }

    private int byte2bit(byte b, int position){

        String s = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
        char[] c = s.toCharArray();
        System.out.println(s); // 10000001

        //return c[position];
        return Character.getNumericValue(s.toCharArray()[position]);
    }


    public void change_pre(String setting, int value) throws IOException {
        int retry = 0;
        type = 3;

        int commandId = 0;

        switch(setting){
            case "mode": {
                commandId = Gaia.COMMAND_SET_MODE;
                if(value == 0)
                    set[0] = (byte)2;
                else
                    set[0] = (byte)0;
                break;
            }

            case "ctone": {
                commandId = Gaia.COMMAND_SET_CTONE;
                set[0] = (byte) value;
                break;
            }
            case "noise": {
                commandId = Gaia.COMMAND_SET_NOISE_REDUCTION;
                set[0] = (byte) value;
                break;
            }
            case "input": {
                commandId = Gaia.COMMAND_SET_INPUT_SOURCE;
                if (value == 0)
                    set[0] = (byte) 0x0E;
                else if (value == 1)
                    set[0] = (byte) 0x73;
                else if (value == 2)
                    set[0] = (byte) 0x33;
                else if (value == 3)
                    set[0] = (byte) 0x0F;

                break;

            }
            case "ratio":{
                //// TODO: 10/12/2017 set input ratio
                commandId = Gaia.COMMAND_SET_INPUT_SOURCE;
                int currentInput = sharedPrefs.getInt(CURRENT_INPUT, 0);
                if(currentInput == 0) {
                    Log.e(TAG,"No input source detected.");
                    break;
                }
                if( currentInput >= (byte)0x31 && currentInput <= (byte)0x37){
                    if (value == 0)
                        set[0] = (byte) 0x31;
                    else if (value == 1)
                        set[0] = (byte) 0x33;
                    else if (value == 2)
                        set[0] = (byte) 0x35;
                    else if (value == 3)
                        set[0] = (byte) 0x37;
                }
                else if (currentInput >= (byte) 0x71 && currentInput <= (byte) 0x77){
                    if (value == 0)
                        set[0] = (byte) 0x71;
                    else if (value == 1)
                        set[0] = (byte) 0x73;
                    else if (value == 2)
                        set[0] = (byte) 0x75;
                    else if (value == 3)
                        set[0] = (byte) 0x77;
                }

                break;
            }

        }

        byte[] setFrame = Gaia.commandGATT(Gaia.VENDOR_CSR, commandId,set);
        Log.d("INPUT RATIO", " " + set[0]);
        while(retry < 3 && !writeCharacteristic(setFrame)){
            retry ++;
        }
        if (retry < 3) {
            enableNotify();
        }
        else
            Toast.makeText(getApplicationContext(),  getString(R.string.service_not_found), Toast.LENGTH_SHORT).show();

    }

    private void checkSettings(){
        SharedPreferences myPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = myPref.edit();
        if (myPref.getBoolean("unpair",false)){
            editor.putBoolean("unpair",false);
            editor.apply();
            finish();
        }
        if(!DeviceScanActivity.isDemo){
            if (myPref.getBoolean("notification",false)){
                enableNotify();
            }
            else
                disableNotify();

        }
        else
            return;
    }

    private void checkBattery(){

        batteryTimer = new Timer("Battery Timer");
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(mBluetoothLeService != null)
                    if(!mBluetoothLeService.getBattery())
                        cancel();

            }
        };
        //batteryTimer.scheduleAtFixedRate(task, 0, 300000);
        batteryTimer.scheduleAtFixedRate(task, 0, 30000);

    }

    private void getCurrentStatus() {
      //  Toast.makeText(this,getString(R.string.detect_status),Toast.LENGTH_SHORT).show();
        type = 0;
        byte[] init = new byte[0];
        try {
            init = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_DSP_STATUS,null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (writeCharacteristic(init)) {
            waitfor(100);
            enableNotify();
            //readCharacteristic();
        }
        waitfor(100);
        checkBattery();
    }


    private void waitfor(int ms){
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



//    private void setNotificationTimer(){
//        ntfCount = 0;
//        isNotified = true;
//        notificationTimer = new Timer();
//        TimerTask task = new TimerTask() {
//            @Override
//            public void run() {
//                ntfCount ++;
//                if(ntfCount > 10) {
//                    isNotified = false;
//                    cancel();
//                }
//            }
//        };
//        notificationTimer.scheduleAtFixedRate(task, 0, 100);
//    }

    private void setNotificationTimer(){
        System.out.println("----------------->Notification timer: start");
        retry = 0;
        ntfCount = 0;
        notificationTimer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                ntfCount ++;
                System.out.println("----------------->Notification timer" + ntfCount);
                if(isNotified) {
                    System.out.println("----------------->Notification timer: notification received");
                    cancel();
                }
                else if(ntfCount > 10 && !isNotified) {
                    System.out.println("----------------->Notification timer: notification not received");
                    if(retry == 5) {
                        isRetry = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), getString(R.string.status_failed), Toast.LENGTH_SHORT).show();
                                //// TODO: 10/19/2017 alert dialog
                                notificationAlert();
                                v.lockButton(false);
                                prep.lockRadioButton(false);
                            }
                        });
                        cancel();
                    }
                    else {
                        isRetry = true;
                        getCurrentStatus();
                        retry ++;
                        ntfCount = 0;

                    }

                }
            }
        };
        notificationTimer.scheduleAtFixedRate(task, 0, 100);
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

    public void setTypefaceToAll(Activity activity)
    {

        View view = activity.findViewById(android.R.id.content).getRootView();
        setTypefaceToAll(view);
    }

    public void setTypefaceToAll(View view)
    {
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            int count = g.getChildCount();
            for (int i = 0; i < count; i++)
                setTypefaceToAll(g.getChildAt(i));
        } else if (view instanceof TextView ) {
            TextView tv = (TextView) view;
            Typeface typeface = TypefaceUtil.get(this);
            tv.setTypeface(typeface);
            //tv.setTextSize(size);
        } else if (view instanceof EditText) {
            EditText et = (EditText) view;
            Typeface typeface = TypefaceUtil.get(this);
            et.setTypeface(typeface);
            //et.setTextSize(size);
        } else if (view instanceof RadioButton) {
            RadioButton rb = (RadioButton) view;
            Typeface typeface = TypefaceUtil.get(this);
            rb.setTypeface(typeface);
            //rb.setTextSize(size);
        }
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
            actionToolbarIcon.setImageResource(R.drawable.menu_night);
        } else {
            actionToolbarIcon.setImageResource(R.drawable.menu);
        }
    }



    public void configureLanguage() {
        String languageToLoad; // your language
        if (sharedPrefs.getString(SettingsActivity.LANGUAGE)==null || sharedPrefs.getString(SettingsActivity.LANGUAGE).isEmpty()) {
            languageToLoad = "zh";

        }
        else
            languageToLoad = sharedPrefs.getString(SettingsActivity.LANGUAGE);

        switch(languageToLoad){
            case "zh":
                PROGRAM1 = PROGRAM1_ZH;
                PROGRAM2 = PROGRAM2_ZH;
                PROGRAM3 = PROGRAM3_ZH;
                PROGRAM4 = PROGRAM4_ZH;
                break;
            case "es":
                PROGRAM1 = PROGRAM1_ES;
                PROGRAM2 = PROGRAM2_ES;
                PROGRAM3 = PROGRAM3_ES;
                PROGRAM4 = PROGRAM4_ES;
                break;
            default:
                PROGRAM1 = PROGRAM1_EN;
                PROGRAM2 = PROGRAM2_EN;
                PROGRAM3 = PROGRAM3_EN;
                PROGRAM4 = PROGRAM4_EN;
                break;

        }


        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
    }

    @Override public void onLocationChanged(Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();

        sharedPrefs.putFloat(LAST_KNOWN_LAT, (float) latitude);
        sharedPrefs.putFloat(LAST_KNOWN_LON, (float) longitude);
    }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {
        // no-op. Override in implementation.
    }

    @Override public void onProviderEnabled(String provider) {
        // no-op. Override in implementation.
    }

    @Override public void onProviderDisabled(String provider) {
        // no-op. Override in implementation.
    }
}
