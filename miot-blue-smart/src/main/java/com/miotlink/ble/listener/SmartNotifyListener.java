package com.miotlink.ble.listener;

public interface SmartNotifyListener {


    /**
     * 监听设备断开
     * @param state
     */
    public void notifyState(int state);

    public void notifyVersion(String macCode, long version);

    public void notifyUartData(String macCode, String hexUartData);

    public void deletePu(String macCode, int version);


    public void notifyProductInfo(String macCode,String data);

    public void notifySetState(int type,String macCode,int errorCode);

}
