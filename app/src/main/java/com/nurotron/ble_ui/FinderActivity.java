package com.nurotron.ble_ui;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nurotron.ble_ui.GAIA_Library.Gaia;
import com.nurotron.ble_ui.GAIA_Library.GaiaLink;
import com.nurotron.ble_ui.menu.SlidingMenu;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.nurotron.ble_ui.MainActivity.BATTERY_RECEIVED;
import static com.nurotron.ble_ui.MainActivity.DEVICE_FOUND;
import static com.nurotron.ble_ui.MainActivity.EXTRAS_DEVICE_ADDRESS;

/**
 * Created by Nurotron on 6/2/2017.
 */

public class FinderActivity extends AppCompatActivity implements OnMapReadyCallback {
    SlidingMenu menu;
    View inflatedView;
    public PresenterListener listener;
    ActionToolbarPresenter actionToolbarPresenter;
    KeyValueStore sharedPrefs;

    @InjectView(R.id.backtohome)
    Button back;

    @InjectView(R.id.locate_progress)
    ProgressBar locateProgress;
    @InjectView(R.id.further)
    TextView textFurther;
    @InjectView(R.id.closer) TextView textCloser;

    LocationManager locationManager;
    private double longitude = 33.6846, latitude = 117.8265;
    private Location lastLocation;
    MapFragment mapFragment;
    Marker marker;
    private static final int REQUEST_ENABLE_GPS = 1;
    private static final int REQUEST_APP_SETTINGS = 2;


    public int receivedRSSI;
    private int count = 0;
    private int times = 0;
    private int[] filter = {-100, -100, -100};
    Timer rssiTimer = new Timer("RSSI Timer");


    public BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    private boolean mConnected = false;
    private final static String TAG = FinderActivity.class.getSimpleName();

    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            System.out.println("--------------> enter onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            if (mBluetoothLeService != null) {
                if (BluetoothLeService.mConnectionState == BluetoothLeService.STATE_CONNECTED) {
                    mConnected = true;
                    System.out.println("-------------> start checking RSSI");
                    checkRssi();
                    locateProgress.setVisibility(View.VISIBLE);
                    textCloser.setVisibility(View.VISIBLE);
                    textFurther.setVisibility(View.VISIBLE);

                    Toast.makeText(getApplicationContext(), getString(R.string.device_connected), Toast.LENGTH_LONG).show();
                    mBluetoothLeService.setCustomeNotification(true);

                } else if (BluetoothLeService.mConnectionState == BluetoothLeService.STATE_DISCONNECTED) {
                    locateProgress.setVisibility(View.INVISIBLE);
                    textFurther.setVisibility(View.INVISIBLE);
                    textCloser.setVisibility(View.INVISIBLE);

                    alertConnection();
                    mBluetoothLeService.connect(mDeviceAddress);
                }

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflatedView = getLayoutInflater().inflate(R.layout.finder, null);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        locationManager = (LocationManager) getSystemService(this.LOCATION_SERVICE);

//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            checkLocationPermission();
//
//        }
//        else
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, locationListener);


        setContentView(inflatedView);
        ButterKnife.inject(this, inflatedView);
        configureSlidingMenu();
        configureViewPresenters(inflatedView);
        configureStatusBarBackground();
        //mapView.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        //shared preference
        sharedPrefs = new KeyValueStore.SharedPrefs(getApplicationContext());
        actionToolbarPresenter.setBattery(sharedPrefs.getInt(BATTERY_RECEIVED, 0));
        if (sharedPrefs.getBool(DEVICE_FOUND))
            mDeviceAddress = sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS);
        else
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //map
        mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        locateProgress.setMax(100);
        locateProgress.setVisibility(View.INVISIBLE);
        textCloser.setVisibility(View.INVISIBLE);
        textFurther.setVisibility(View.INVISIBLE);

    }

    @Override
    public void onBackPressed() {
        final Intent intent = getParentActivityIntent();
        finish();
        startActivity(intent);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            ActivityCompat.requestPermissions(FinderActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE},
                    REQUEST_APP_SETTINGS);
            return;
        }
        else
            lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(lastLocation!=null) {
            latitude = lastLocation.getLatitude();
            longitude = lastLocation.getLongitude();
        }
        LatLng pos;
        if (latitude != 0 && longitude != 0) {
            pos = new LatLng(latitude, longitude);
        } else {
            pos = new LatLng(33.6846, 117.8265);
        }
        if(marker == null) {

            marker = googleMap.addMarker(new MarkerOptions().position(pos)
                    .title("Marker of device"));
        }
        else {
            marker.remove();
            marker = googleMap.addMarker(new MarkerOptions().position(pos)
                    .title("Marker of device"));

        }
        //googleMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14.6f));
    }



    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();

            mapFragment.getMapAsync(FinderActivity.this);
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
    };

    public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("-----------------> entering onReceive()");
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;

                Toast.makeText(getApplicationContext(), getString(R.string.device_connected), Toast.LENGTH_LONG).show();

                invalidateOptionsMenu();
            }else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                locateProgress.setVisibility(View.INVISIBLE);
                textCloser.setVisibility(View.INVISIBLE);
                textFurther.setVisibility(View.INVISIBLE);
                rssiTimer.cancel();
                alertConnection();

            }else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //////       displayGattServices(mBluetoothLeService.getSupportedGattServices());
                System.out.println("-------------> start checking RSSI");
                checkRssi();
                locateProgress.setVisibility(View.VISIBLE);
                textFurther.setVisibility(View.VISIBLE);
                textCloser.setVisibility(View.VISIBLE);

                if (mBluetoothLeService != null && mConnected) {

                    mBluetoothLeService.setCustomeNotification(true);
                }

            }else if (BluetoothLeService.ACTION_RSSI_READ.equals(action)){
                count ++;
                receivedRSSI = intent.getIntExtra(BluetoothLeService.RSSI_DATA, 0);
                Log.v("ACTION_RSSI_READ", "Rssi received " + receivedRSSI);
                if(count == 10) {
                    count = 0;
                    setmRssi(receivedRSSI);
                }
            }  else if (BluetoothLeService.ACTION_NOTIFICATION.equals(action)){
                byte[] receiveData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                Log.v("ACTION_NOTIFICATION", "Rssi received " + receiveData.length);
                //writeData(receiveData[5]);

                setmRssi(receiveData[5]);

                Log.e("NOTIFICATION", "Data length:" + receiveData.length);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationPermission();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            Log.e(TAG,"No permission");
        else
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, locationListener);

//        if (mServiceConnection != null) {
//            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//        }
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }
    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
        rssiTimer.cancel();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        rssiTimer.cancel();
        mBluetoothLeService = null;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_GPS && resultCode == Activity.RESULT_CANCELED) {
            onBackPressed();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    public void alertConnection(){
        android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this).create();

        // Setting Dialog Title
        alertDialog.setTitle(getString(R.string.menu_connect));

        // Setting Dialog Message
        alertDialog.setMessage(getString(R.string.last_location));


        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE,getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        // Showing Alert Message
        alertDialog.show();

    }

    private void checkLocationPermission(){
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.location_service));
            builder.setMessage(getString(R.string.gps_enable));
            builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, REQUEST_ENABLE_GPS);
                }
            });
            builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    onBackPressed();
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.show();
            return;
        }
    }
    public void setmRssi(int rssi){
        filter[times] = rssi;
        times ++;
        times = times % 3;
        int[] sorted = filter;
        Arrays.sort(sorted);
        rssi = sorted[sorted.length/2];

        if(rssi < -84)
            locateProgress.setProgress(0);
        else if(rssi < -80)
            locateProgress.setProgress(20);
        else if(rssi < -77)
            locateProgress.setProgress(40);
        else if(rssi < -73)
            locateProgress.setProgress(60);
        else if(rssi < -66)
            locateProgress.setProgress(80);
        else
            locateProgress.setProgress(100);
//        locateProgress.setProgress(rssi+90);

    }

    private void checkRssi(){
        Log.d(TAG,"RSSI Timer started");
        rssiTimer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(mBluetoothLeService != null)
                    mBluetoothLeService.readRemoteRssi();
                try {
                    byte[] readRSSI = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_CURRENT_RSSI,null);
                    mBluetoothLeService.writeCustomCharacteristic(readRSSI);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        rssiTimer.scheduleAtFixedRate(task, 0, 500);
    }



    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_RSSI_READ);
        intentFilter.addAction(BluetoothLeService.ACTION_NOTIFICATION);
        return intentFilter;
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
        actionToolbarPresenter.setBattery(29);
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
