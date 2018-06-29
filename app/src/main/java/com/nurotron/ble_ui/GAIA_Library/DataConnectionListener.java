package com.nurotron.ble_ui.GAIA_Library;

/**
 * Created by TongXinyu on 16/2/26.
 */
public interface DataConnectionListener {
    /**
     * Callback for when data starts or stops being sent or received on the GaiaLink.
     * @param isDataTransferInProgress True if data is being sent or received on the link.
     */
    public void update(boolean isDataTransferInProgress);
}
