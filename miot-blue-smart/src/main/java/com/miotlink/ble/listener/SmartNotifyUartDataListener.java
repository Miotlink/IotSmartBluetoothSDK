package com.miotlink.ble.listener;

public interface SmartNotifyUartDataListener {

    public void notifyUartData(String macCode, String uart);

    public void notifyDeviceWifiInfo(String macCode, String uart);

    public void notifyFail(int errorCode, String errorMessage);
}
