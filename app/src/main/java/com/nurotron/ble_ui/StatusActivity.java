package com.nurotron.ble_ui;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.nurotron.ble_ui.GAIA_Library.Gaia;
import com.nurotron.ble_ui.GAIA_Library.GaiaCommand;
import com.nurotron.ble_ui.menu.SlidingMenu;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.nurotron.ble_ui.MainActivity.BATTERY_RECEIVED;
import static com.nurotron.ble_ui.MainActivity.CURRENT_PROGRAM;
import static com.nurotron.ble_ui.MainActivity.CURRENT_VOLUME;
import static com.nurotron.ble_ui.MainActivity.DEVICE_FOUND;
import static com.nurotron.ble_ui.MainActivity.EXTRAS_DEVICE_ADDRESS;

/**
 * Created by Nurotron on 5/16/2017.
 */

public class StatusActivity extends AppCompatActivity {


    SlidingMenu menu;
    View inflatedView;
    public PresenterListener listener;
    ActionToolbarPresenter actionToolbarPresenter;

    KeyValueStore sharedPrefs;


    @InjectView(R.id.program_status) TextView mProgram;
    @InjectView(R.id.implant_status) TextView mImplant;
    @InjectView(R.id.device_status) TextView mDevice;
    @InjectView(R.id.volume_status) TextView mVolume;
    @InjectView(R.id.backtohome)  Button back;
    @InjectView(R.id.status_TextView) TextView mTextView;
    @InjectView(R.id.device_text) TextView mDeviceText;
    @InjectView(R.id.implant_text) TextView mImplantText;
    @InjectView(R.id.program_text) TextView mProgramText;
    @InjectView(R.id.volume_text) TextView mVolText;
    @InjectView(R.id.audio_text) TextView mAudio;
    @InjectView(R.id.audio_status) Switch audioSwitch;

    @InjectView(R.id.action_toolbar_icon) ImageView actionToolbarIcon;
    @InjectView(R.id.left_arrow_image) ImageView leftArrowImage;

    public BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    private boolean mConnected = false;
    private final static String TAG = StatusActivity.class.getSimpleName();
    private boolean isInitialize = false;
    private Vector<AlertDialog> dialogs = new Vector<AlertDialog>();

    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            System.out.println("--------------> enter onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            //BluetoothLeService.requestID = 0;
            //BluetoothLeService.isConnectedByService = false;
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLeService.connect(mDeviceAddress);

            if(mBluetoothLeService != null){
                if(BluetoothLeService.mConnectionState == BluetoothLeService.STATE_CONNECTED) {
                    mBluetoothLeService.setCustomeNotification(true);
                    getCurrentStatus();

                }
                else if (BluetoothLeService.mConnectionState == BluetoothLeService.STATE_DISCONNECTED) {
                    mBluetoothLeService.connect(mDeviceAddress);
                    Log.v(TAG, "------------------------------connect--------------------------------");
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

        sharedPrefs = new KeyValueStore.SharedPrefs(getApplicationContext());
        //configureLanguage();
        inflatedView = getLayoutInflater().inflate(R.layout.status_tab, null);
        setContentView(inflatedView);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        ButterKnife.inject(this, inflatedView);
        configureSlidingMenu();
        configureViewPresenters(inflatedView);
        configureStatusBarBackground();


        final Intent intent = getIntent();
        actionToolbarPresenter.setBattery(sharedPrefs.getInt(BATTERY_RECEIVED, 0));
        if (sharedPrefs.getBool(DEVICE_FOUND))
            mDeviceAddress = sharedPrefs.getString(EXTRAS_DEVICE_ADDRESS);
        else
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        //setStyles();
//        if (MainActivity.DEVICE_STATUS)
//            mDevice.setText(getString(R.string.connected));
//        else
//            mDevice.setText(getString(R.string.disconnected));
//        if (MainActivity.IMPLANT_STATUS)
//            mImplant.setText(getString(R.string.connected));
//        else
//            mImplant.setText(getString(R.string.disconnected));
//        mProgram.setText(Integer.toString(sharedPrefs.getInt(CURRENT_PROGRAM,0)));
//        mVolume.setText(Integer.toString(sharedPrefs.getInt(CURRENT_VOLUME,0)) +" "+ getString(R.string.volume_text));
        mDevice.setText(getString(R.string.disconnected));
        mImplant.setText(getString(R.string.disconnected));
        mProgram.setText("N/A");
        mVolume.setText("N/A");

        audioSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isInitialize) {
                    if (isChecked) {
                        mBluetoothLeService.startBT(1);
                        alertPairBT();

                    }
                    else
                        mBluetoothLeService.startBT(0);
                }
                else
                    isInitialize = false;
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


    }



    private boolean isDeviceBonded() {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mDeviceAddress);
        //return BluetoothAdapter.getDefaultAdapter().getBondedDevices().contains(device);
        return device.getBondState() == BluetoothDevice.BOND_BONDED;
    }

    public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("-----------------> entering onReceive()");
            final String action = intent.getAction();
                if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                inflatedView.setAlpha(1);
            }else if(BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mDevice.setText(getString(R.string.disconnected));
                mImplant.setText(getString(R.string.disconnected));
                mProgram.setText("N/A");
                mVolume.setText("N/A");

                sharedPrefs.putInt(BATTERY_RECEIVED, 0);
                actionToolbarPresenter.setBattery(0);

                final int state = BluetoothAdapter.getDefaultAdapter().getState();
                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.e(TAG, "Bluetooth off");
                        onBackPressed();
                        break;
                }
                alertConnection();
            }else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //////       displayGattServices(mBluetoothLeService.getSupportedGattServices());
                mBluetoothLeService.getBattery();
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mBluetoothLeService != null && mConnected) {
                    mBluetoothLeService.setCustomeNotification(true);
                }
                getCurrentStatus();
            }  else if (BluetoothLeService.ACTION_NOTIFICATION.equals(action)){
                inflatedView.setAlpha(1);

                byte[] receiveData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                //writeData(receiveData[5]);
                getACKStatus(receiveData);

                Log.e("NOTIFICATION", "Data length:" + receiveData.length);
                Log.v("NOTIFICATION", "data: " + receiveData[0] + " " + receiveData[1] + " " + receiveData[2] + " " + receiveData[3] + " " + receiveData[4]);

            }else if (BluetoothLeService.ACTION_BATTERY_AVAILABLE.equals(action)) {
                    int receivedBattery = intent.getByteExtra(BluetoothLeService.BATTERY_DATA, (byte) 0);

                    sharedPrefs.putInt(BATTERY_RECEIVED, receivedBattery);
                    actionToolbarPresenter.setBattery(sharedPrefs.getInt(BATTERY_RECEIVED, 0));
            }


        }
    };

    @Override
    public void onBackPressed() {
        final Intent intent = getParentActivityIntent();
        finish();
        startActivity(intent);
    }

/* chenxi: this is not necessary. I made everything such that you do not need to do this. Doing too much will break night mode.
    @Override protected void onResume() {
        super.onResume();
        setStyles();
        configureLanguage();

        //configureDayNight();

    }
*/

    @Override protected void onResume() {
        super.onResume();
        setStyles();
        inflatedView.setAlpha(0.5f);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

//        if (mBluetoothLeService != null)
//            mBluetoothLeService.connect(mDeviceAddress);

    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
//        if (mBluetoothLeService != null)
//            //mBluetoothLeService.close();
//            mBluetoothLeService.disconnect();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
      //  unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void getCurrentStatus(){
        //  Toast.makeText(this,getString(R.string.detect_status),Toast.LENGTH_SHORT).show();

        try {
            byte[] init = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_DSP_STATUS,null);
            mBluetoothLeService.writeCustomCharacteristic(init);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            TimeUnit.MILLISECONDS.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isInitialize = true;
        try {
            byte[] checkBT = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.COMMAND_CHECK_BT_STATUS, null);
            mBluetoothLeService.writeCustomCharacteristic(checkBT);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void getACKStatus(byte[] receiveData) {
        mDevice.setText(getString(R.string.connected));

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
            else if(ack.getCommandId() == Gaia.ACK_CHECK_BT){
               // isInitialize = false;
                byte[] gaiaPayload = ack.getPayload();
                if(gaiaPayload[1] == 1){
                    if(isDeviceBonded()) {
                        audioSwitch.setChecked(true);
                        isInitialize = false;
                    }
                    else {
                        alertPairBT();
                        audioSwitch.setChecked(false);
                        isInitialize = false;
                    }
                }
                else {
                    audioSwitch.setChecked(false);
                    isInitialize = false;
                }
            }
            else {
                if (ack.getCustomStatusCode() == 0) {
                    byte[] gaiaPayload = ack.getPayload();
                    if (gaiaPayload.length > 7)
                        checkChangeResult(gaiaPayload, ack.getCommandId());
                    else{
                        mProgram.setText("N/A");
                        mVolume.setText("N/A");
                        mImplant.setText("N/A");
                        Toast.makeText(getApplicationContext(), getString(R.string.status_failed), Toast.LENGTH_SHORT).show();
                        Log.e("ACK", "Payload incomplete");
                    }
                } else {
                    //error
                    mProgram.setText("N/A");
                    mVolume.setText("N/A");
                    mImplant.setText("N/A");
                    Toast.makeText(getApplicationContext(), getString(R.string.status_failed), Toast.LENGTH_SHORT).show();
                    Log.e("ACK", "status code error: " + ack.getCustomStatusCode());
                }

            }
        } else {
            mProgram.setText("N/A");
            mVolume.setText("N/A");
            mImplant.setText("N/A");
            Toast.makeText(getApplicationContext(), getString(R.string.status_failed), Toast.LENGTH_SHORT).show();
            Log.e("ACK", "ACK null value");
        }
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

            if (mBluetoothLeService.writeCustomCharacteristic(ackFrame))
                Log.d("NOTIFICATION", "ACK sent");
            else
                Log.d("NOTIFICATION", "ACK cannot be sent");

            //response
            closeDialogs();
            alertImplantAbnormal();
            mImplant.setText(getString(R.string.disconnected));


        }
        else if (notification.getPayload()[0] == (byte) 0x82){
            //ack
            payload[1] = (byte) 0x82;
            byte[] ackFrame = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.ACK_DSP_NOTIFICATION, payload);
            if (mBluetoothLeService.writeCustomCharacteristic(ackFrame))
                Log.d("NOTIFICATION", "ACK sent");
            else
                Log.d("NOTIFICATION", "ACK cannot be sent");

            //response
            closeDialogs();
            Toast.makeText(getApplicationContext(), R.string.implant_connected, Toast.LENGTH_SHORT).show();
            Log.d("NOTIFICATION", "implant connected");
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            getCurrentStatus();
        }
        else if (notification.getPayload()[0] == (byte) 0x83){
            //ack
            payload[1] = (byte) 0x83;
            byte[] ackFrame = Gaia.commandGATT(Gaia.VENDOR_CSR, Gaia.ACK_DSP_NOTIFICATION, payload);
            if (mBluetoothLeService.writeCustomCharacteristic(ackFrame))
                Log.d("NOTIFICATION", "ACK sent");
            else
                Log.d("NOTIFICATION", "ACK cannot be sent");

            //response
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            getCurrentStatus();
            Log.d("NOTIFICATION", "DSP value update");
        }
        else
            Log.d("NOTIFICATION", "notification payload: " + notification.getPayload()[0]);

    }
    public void alertConnection() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setCanceledOnTouchOutside(false);
        // Setting Dialog Title
        alertDialog.setTitle(getString(R.string.menu_connect));

        // Setting Dialog Message
        alertDialog.setMessage(getString(R.string.retry));

        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        // Showing Alert Message
        dialogs.add(alertDialog);
        alertDialog.show();
        //inflatedView.setAlpha(0.5f);

    }

    public void alertPairBT(){
        if(!isDeviceBonded()) {
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
                    audioSwitch.setChecked(false);
                    mBluetoothLeService.startBT(0);

                }
            });
            builder.show();
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


    public void checkChangeResult(byte[] ackPayload, int commandID) {

        Log.d("RESULT", "" + ackPayload[0] + " " + ackPayload[1] + " " + ackPayload[2] + " " + ackPayload[3] + " " + ackPayload[4] + " " + ackPayload[5] + " " + ackPayload[6] + " " + ackPayload[7] + " " + ackPayload[8] + " ");


        int dStatus = byte2bit(ackPayload[8], 7);
        if (dStatus == 0)
            mImplant.setText(getString(R.string.connected));
        else
            mImplant.setText(getString(R.string.disconnected));

        if(ackPayload[2] == 0){

            getCurrentStatus();
            return;
        }
        sharedPrefs.putInt(CURRENT_PROGRAM, ackPayload[2]);
        sharedPrefs.putInt(CURRENT_VOLUME, ackPayload[3]);
        mProgram.setText(Integer.toString(sharedPrefs.getInt(CURRENT_PROGRAM,0)));
        mVolume.setText(Integer.toString(sharedPrefs.getInt(CURRENT_VOLUME,0)) +" "+ getString(R.string.volume_text));
    }

    private int byte2bit(byte b, int position){

        String s = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
        char[] c = s.toCharArray();
        //return c[position];
        return Character.getNumericValue(s.toCharArray()[position]);
//        System.out.println(s1); // 10000001
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_NOTIFICATION);
        intentFilter.addAction(BluetoothLeService.ACTION_BATTERY_AVAILABLE);
        return intentFilter;
    }





    //set display data styles
    private void setStyles() {
        float sp = 25.0f;
        if (sharedPrefs.getString(SettingsActivity.TEXTSIZES) != null && sharedPrefs.getString(SettingsActivity.TEXTSIZES).equals("small")) sp = sp * 0.8f;
        else if (sharedPrefs.getString(SettingsActivity.TEXTSIZES) != null && sharedPrefs.getString(SettingsActivity.TEXTSIZES).equals("large")) sp = sp * 1.2f;
        float size = sp * getResources().getDisplayMetrics().scaledDensity;
        Typeface typeface = TypefaceUtil.get(this);
        mTextView.setTypeface(typeface);
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mDeviceText.setTypeface(typeface);
        mDeviceText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mImplantText.setTypeface(typeface);
        mImplantText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mProgramText.setTypeface(typeface);
        mProgramText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mVolText.setTypeface(typeface);
        mVolText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mDevice.setTypeface(typeface);
        mDevice.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mProgram.setTypeface(typeface);
        mProgram.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mVolume.setTypeface(typeface);
        mVolume.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mImplant.setTypeface(typeface);
        mImplant.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        mAudio.setTypeface(typeface);
        mAudio.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        back.setTypeface(typeface);
        back.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);

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
}
