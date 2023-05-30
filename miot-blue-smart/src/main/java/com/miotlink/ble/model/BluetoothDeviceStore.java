package com.miotlink.ble.model;



import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BluetoothDeviceStore {

    private static BluetoothDeviceStore instace=null;

    public static BluetoothDeviceStore getInstace() {

        if (instace==null){
            synchronized (BluetoothDeviceStore.class){
                if (instace==null){
                    instace=new BluetoothDeviceStore();
                }
            }
        }
        return instace;
    }

    private final Map<String, BleModelDevice> mDeviceMap;

    private final Map<String, BleModelDevice> mConnectedDeviceMap;

    public BluetoothDeviceStore() {
        mDeviceMap = new HashMap<>();
        mConnectedDeviceMap=new HashMap<>();
    }

    public void addConnectDevice(String macCode,BleModelDevice device){
        if (!mConnectedDeviceMap.containsKey(macCode)){
            mConnectedDeviceMap.put(macCode,device);
        }
    }

    public void removeConnectDevice(String macCode){
        if (mConnectedDeviceMap.containsKey(macCode)){
            mConnectedDeviceMap.remove(macCode);
        }
    }
    public BleModelDevice getConnectDevice(String macCode){
        if (mConnectedDeviceMap.containsKey(macCode)){
            return mConnectedDeviceMap.get(macCode);
        }
        return null;
    }

    public void addDevice(BleModelDevice device) {

        if (!TextUtils.isEmpty(device.getMacAddress())){
            if (!mDeviceMap.containsKey(device.getMacAddress())) {
                mDeviceMap.put(device.getMacAddress(), device);
            }
        }else {
            if (!mDeviceMap.containsKey(device.getBleAddress())) {
                mDeviceMap.put(device.getBleAddress(), device);
            }
        }
    }
    public void addDevice(String macCode,BleModelDevice device) {
        if (!mDeviceMap.containsKey(macCode)) {
            mDeviceMap.put(macCode, device);
        }
    }

    public void addScanDevice(String macCode,BleModelDevice device){
        if (mDeviceMap!=null&&!mDeviceMap.containsKey(macCode)){
            mDeviceMap.put(macCode, device);
        }
    }
    public void remove(String macCode){
        if (mDeviceMap!=null&&mDeviceMap.containsKey(macCode)){
            mDeviceMap.remove(macCode);
        }
    }


    public BleModelDevice getBleModelDevice(String macCode){
        if (TextUtils.isEmpty(macCode)){
            return null;
        }
         if (mDeviceMap!=null&&mDeviceMap.containsKey(macCode)){
            return mDeviceMap.get(macCode);
        }
        return null;
    }

    public void removeDevice(String deviceMac) {

        if (mDeviceMap.containsKey(deviceMac)) {
            mDeviceMap.remove(deviceMac);
        }
    }

    public void clear() {
        mDeviceMap.clear();
        mConnectedDeviceMap.clear();
    }

    public Map<String, BleModelDevice> getDeviceMap() {
        return mDeviceMap;
    }
    public BleModelDevice getDevice(String macCode) {
        if (mDeviceMap!=null&&mDeviceMap.containsKey(macCode)){
            return mDeviceMap.get(macCode);
        }
        return null;
    }


    /**
     * 是否存在设备
     * @param address
     * @return
     */
    public boolean isHasDevice(String address){
        if (mDeviceMap!=null&&mDeviceMap.containsKey(address)){
            return true;
        }
        return false;
    }

    public List<BleModelDevice> getDeviceList() {
        final List<BleModelDevice> methodResult = new ArrayList<>(mDeviceMap.values());
        return methodResult;
    }

    @Override
    public String toString() {
        return "BluetoothLeDeviceStore{" +
                "DeviceList=" + getDeviceList() +
                '}';
    }
}
