package com.miotlink.ble.listener;

public interface SmartNotifyCMBListener {

    public void onReceiver(String macCode, String data)throws Exception;

    public void notify(String macCode, int funCode, int errorCode)throws Exception;
}
