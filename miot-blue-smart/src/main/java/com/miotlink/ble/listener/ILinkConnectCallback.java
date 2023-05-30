package com.miotlink.ble.listener;

import com.miotlink.ble.model.BleModelDevice;
@Deprecated
public interface ILinkConnectCallback {
    public void onConnected(int code, BleModelDevice bleModelDevice)throws Exception;
}
