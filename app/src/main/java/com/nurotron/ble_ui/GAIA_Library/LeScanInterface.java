package com.nurotron.ble_ui.GAIA_Library;

import android.bluetooth.BluetoothDevice;

/**
 * Created by TongXinyu on 16/4/20.
 */
public interface LeScanInterface {

    public void DeviceFound(final BluetoothDevice device, final int rssi, final byte[] scanRecord);

    public static class Null implements LeScanInterface{
        @Override
        public void DeviceFound(final BluetoothDevice device, final int rssi, final byte[] scanRecord){};
    }


}
