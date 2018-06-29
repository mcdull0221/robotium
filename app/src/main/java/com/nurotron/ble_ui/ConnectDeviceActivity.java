package com.nurotron.ble_ui;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nurotron.ble_ui.ActionToolbarPresenter;
import com.nurotron.ble_ui.IntentUtility;
import com.nurotron.ble_ui.KeyValueStore;
import com.nurotron.ble_ui.R;
import com.nurotron.ble_ui.TypefaceUtil;
import com.nurotron.ble_ui.menu.SlidingMenu;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.nurotron.ble_ui.MainActivity.BATTERY_RECEIVED;
import static com.nurotron.ble_ui.MainActivity.DEVICE_FOUND;
import static com.nurotron.ble_ui.MainActivity.EXTRAS_DEVICE_ADDRESS;

/**
 * Created by Nurotron on 6/7/2017.
 */

public class ConnectDeviceActivity extends AppCompatActivity {
    SlidingMenu menu;
    View inflatedView;
    public PresenterListener listener;
    ActionToolbarPresenter actionToolbarPresenter;

    KeyValueStore sharedPrefs;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private LeDeviceListAdapter lastDeviceAdapter;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothScanner;
    private boolean mScanning;

    private boolean isNewDevice = false;

    private Handler mHandler;
    private static final long SCAN_PERIOD = 20000;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_APP_SETTINGS = 2;
    private static final int REQUEST_ENABLE_GPS = 3;


    @InjectView(R.id.device_list_view) ListView mListView;
    @InjectView(R.id.saved_device_view) ListView lastDevice;
    @InjectView(R.id.scan) Button scanAgain;
    @InjectView(R.id.backToSetting) Button back;
    @InjectView(R.id.connect_title) TextView title;
    @InjectView(R.id.guide_text) TextView guide;
    @InjectView(R.id.found_text) TextView found;
    @InjectView(R.id.disconnect) Button disconnectDevice;

    public BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress ="";
    private boolean mConnected = false;
    private final static String TAG = ConnectDeviceActivity.class.getSimpleName();

    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            BluetoothLeService.requestID = 0;
            if(mBluetoothLeService != null){
                if(BluetoothLeService.mConnectionState == BluetoothLeService.STATE_CONNECTED){
                    mConnected = true;
                    if(isNewDevice){
                        DeviceScanActivity.isFirstTime = true;
                        isNewDevice = false;
                    }
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    lastDeviceAdapter = new LeDeviceListAdapter();
                    if(sharedPrefs.getBool(DEVICE_FOUND)) {
                        mDeviceAddress = sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS);
                        BluetoothDevice pairedDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                        lastDeviceAdapter.addDevice(pairedDevice);
                        mLeDeviceListAdapter.addDevice(pairedDevice);
                    }
                    lastDevice.setAdapter(lastDeviceAdapter);
                    Toast.makeText(getApplicationContext(), getString(R.string.device_connected), Toast.LENGTH_LONG).show();
                    disconnectDevice.setVisibility(View.VISIBLE);

                }
                else if (BluetoothLeService.mConnectionState == BluetoothLeService.STATE_DISCONNECTED) {
                    mConnected = false;
                    disconnectDevice.setVisibility(View.INVISIBLE);
                    mBluetoothLeService.disconnect();

                    sharedPrefs.putInt(BATTERY_RECEIVED, 0);
                    actionToolbarPresenter.setBattery(0);

                    mLeDeviceListAdapter.notifyDataSetChanged();
                    lastDeviceAdapter.notifyDataSetChanged();
                }
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                if(isNewDevice){
                    DeviceScanActivity.isFirstTime = true;
                    isNewDevice = false;
                }
                lastDeviceAdapter = new LeDeviceListAdapter();
                if(sharedPrefs.getBool(DEVICE_FOUND)) {
                    mDeviceAddress = sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS);
                    BluetoothDevice pairedDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                    lastDeviceAdapter.addDevice(pairedDevice);
                    mLeDeviceListAdapter.addDevice(pairedDevice);
                }
                lastDevice.setAdapter(lastDeviceAdapter);
                Toast.makeText(getApplicationContext(), getString(R.string.device_connected), Toast.LENGTH_LONG).show();
                mLeDeviceListAdapter.notifyDataSetChanged();
                lastDeviceAdapter.notifyDataSetChanged();
                disconnectDevice.setVisibility(View.VISIBLE);
                mBluetoothLeService.getBattery();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                disconnectDevice.setVisibility(View.INVISIBLE);
                mBluetoothLeService.disconnect();

                sharedPrefs.putInt(BATTERY_RECEIVED, 0);
                actionToolbarPresenter.setBattery(0);

                mLeDeviceListAdapter.notifyDataSetChanged();
                lastDeviceAdapter.notifyDataSetChanged();
               // sharedPrefs.putInt(BATTERY_RECEIVED, 0);
               // actionToolbarPresenter.setBattery(0);
            }
            else if (BluetoothLeService.ACTION_BATTERY_AVAILABLE.equals(action)) {
                int receivedBattery = intent.getByteExtra(BluetoothLeService.BATTERY_DATA, (byte) 0);

                sharedPrefs.putInt(BATTERY_RECEIVED, receivedBattery);
                actionToolbarPresenter.setBattery(sharedPrefs.getInt(BATTERY_RECEIVED, 0));
            }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.e(TAG, "Bluetooth off");
                        finish();
                        break;
                }

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();

        //mListView = (ListView) findViewById(R.id.list);
        inflatedView = getLayoutInflater().inflate(R.layout.connect_device, null);
        setContentView(inflatedView);
        ButterKnife.inject(this, inflatedView);
        configureSlidingMenu();
        configureViewPresenters(inflatedView);
        configureStatusBarBackground();
        sharedPrefs = new KeyValueStore.SharedPrefs(getApplicationContext());

        setStyles();
        actionToolbarPresenter.setBattery(sharedPrefs.getInt(BATTERY_RECEIVED, 0));

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

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        if(!isMyServiceRunning(BluetoothLeService.class)) {
            Log.d(TAG, "onStartService() called onCreate");
            startService(gattServiceIntent);
        }

        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        scanAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(false);
                mLeDeviceListAdapter = new LeDeviceListAdapter();
                //setListAdapter(mLeDeviceListAdapter);
                mListView.setAdapter(mLeDeviceListAdapter);
                lastDeviceAdapter = new LeDeviceListAdapter();
                if(sharedPrefs.getBool(DEVICE_FOUND)) {
                    mDeviceAddress = sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS);
                    BluetoothDevice pairedDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                    lastDeviceAdapter.addDevice(pairedDevice);
                    if(mConnected){
                        mLeDeviceListAdapter.addDevice(pairedDevice);
                    }
                }
                lastDevice.setAdapter(lastDeviceAdapter);

                scanLeDevice(true);
                scanAgain.setText(getString(R.string.search));
            }
        });

        disconnectDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBluetoothLeService != null)
                    mBluetoothLeService.disconnect();
            }
        });
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

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        if(!isMyServiceRunning(BluetoothLeService.class)) {
            Log.d(TAG, "onStartService() called onResume");
            startService(gattServiceIntent);
        }

        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mLeDeviceListAdapter = new LeDeviceListAdapter();

        mListView.setAdapter(mLeDeviceListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                if(!mDeviceAddress.equals(device.getAddress()))
                    isNewDevice = true;
                mDeviceAddress = device.getAddress();
                mBluetoothScanner.stopScan(mScanCallback);

                ////////////////////////////////
                //disconnect previous connected device
                mBluetoothLeService.disconnect();

                mBluetoothLeService.connect(mDeviceAddress);
            }
        });

        lastDeviceAdapter = new LeDeviceListAdapter();
        if(sharedPrefs.getBool(DEVICE_FOUND)) {
            mDeviceAddress = sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS);
            BluetoothDevice pairedDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
            lastDeviceAdapter.addDevice(pairedDevice);
        }
        lastDevice.setAdapter(lastDeviceAdapter);
        lastDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = lastDeviceAdapter.getDevice(position);
                if (device == null) return;
                mDeviceAddress = device.getAddress();
                mBluetoothScanner.stopScan(mScanCallback);
                isNewDevice = false;
                ////////////////////////////////
                //disconnect previous connected device
                mBluetoothLeService.disconnect();

                mBluetoothLeService.connect(mDeviceAddress);
            }
        });
        checkPermission();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
        unregisterReceiver(mGattUpdateReceiver);
        mLeDeviceListAdapter.clear();
        lastDeviceAdapter.clear();
        unbindService(mServiceConnection);

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //  unbindService(mServiceConnection);
        mBluetoothLeService = null;
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
        else{
            mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();

            scanLeDevice(true);

        }
        // location service

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ConnectDeviceActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE},
                    REQUEST_APP_SETTINGS);
        }
        final LocationManager manager = (LocationManager) getSystemService(getApplicationContext().LOCATION_SERVICE);

        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.location_request));
            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, REQUEST_ENABLE_GPS);
                    dialogInterface.cancel();

                }
            });
            builder.show();
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

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothLeService.ACTION_BATTERY_AVAILABLE);
        return intentFilter;
    }

    private void scanLeDevice(final boolean enable) {
        Runnable scanHandler = new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                if(mBluetoothAdapter.isEnabled())
                    mBluetoothScanner.stopScan(mScanCallback);
                scanAgain.setText(getString(R.string.scan_again));
                invalidateOptionsMenu();
            }
        };
        mHandler.removeCallbacks(scanHandler);

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(scanHandler, SCAN_PERIOD);

            mScanning = true;
            mBluetoothScanner.startScan(mScanCallback);
        } else {

            mScanning = false;
            //mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mBluetoothScanner.stopScan(mScanCallback);
        }

        invalidateOptionsMenu();
    }

    private void setStyles() {
        float sp = 25.0f;
        if (sharedPrefs.getString(SettingsActivity.TEXTSIZES) != null && sharedPrefs.getString(SettingsActivity.TEXTSIZES).equals("small")) sp = sp * 0.8f;
        else if (sharedPrefs.getString(SettingsActivity.TEXTSIZES) != null && sharedPrefs.getString(SettingsActivity.TEXTSIZES).equals("large")) sp = sp * 1.2f;
        float size = sp * getResources().getDisplayMetrics().scaledDensity;
        Typeface typeface = TypefaceUtil.get(this);
        scanAgain.setTypeface(typeface);
        scanAgain.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        guide.setTypeface(typeface);
        guide.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        found.setTypeface(typeface);
        found.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        title.setTypeface(typeface);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        disconnectDevice.setTypeface(typeface);
        disconnectDevice.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;



        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = getLayoutInflater();

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
        }

        public boolean setCheckbox(BluetoothDevice device){
            if(mConnected && device.getAddress().equals(mDeviceAddress))
                return true;
            else
                return false;
        }

        public boolean contains(BluetoothDevice device){
            if(mLeDevices.contains(device))
                return true;
            else
                return false;
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
                view = mInflator.inflate(R.layout.row_devices, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.isConnected = (CheckBox) view.findViewById(R.id.is_connected);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);

            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.isConnected.setChecked(setCheckbox(device));

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device,final int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
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
                    //if(mLeDeviceListAdapter.contains(result.getDevice()))
                    if(result.getDevice().getAddress().contains("00:02:5B"))
                        mLeDeviceListAdapter.addDevice(result.getDevice());
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceAddress;
        CheckBox isConnected;
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

}
