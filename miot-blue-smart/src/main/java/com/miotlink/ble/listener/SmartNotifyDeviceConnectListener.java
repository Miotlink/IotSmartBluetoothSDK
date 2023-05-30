package com.miotlink.ble.listener;

import com.miotlink.ble.model.BleModelDevice;

public interface SmartNotifyDeviceConnectListener {

    /**
     *
     * @param status
     * 100 未连接
     * 200 已连接
     * 101 连接中
     * 102 连接异常
     * 103 断开连接
     *
     * @param bleModelDevice
     */
    public void notifyDeviceConnectListener(int status, BleModelDevice bleModelDevice);
}
