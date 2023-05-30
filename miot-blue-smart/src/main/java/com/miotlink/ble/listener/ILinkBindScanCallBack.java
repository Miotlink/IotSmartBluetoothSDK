package com.miotlink.ble.listener;

import com.miotlink.ble.model.BleModelDevice;

@Deprecated
public interface ILinkBindScanCallBack {

    public void onHasBindScanDevice(BleModelDevice bleModelDevice)throws Exception;
}
