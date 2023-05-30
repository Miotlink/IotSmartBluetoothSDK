package com.miotlink.ble.model;





import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HasConnectBleDeviceStore {


    public static HasConnectBleDeviceStore instance =null;

    public static synchronized HasConnectBleDeviceStore getInstance() {
        if (instance==null){
            synchronized (HasConnectBleDeviceStore.class){
                if (instance==null){
                    instance=new HasConnectBleDeviceStore();
                }
            }
        }
        return instance;
    }

    private Map<String, BleModelDevice> mDeviceMap;

    public HasConnectBleDeviceStore() {
        mDeviceMap = new HashMap<>();
    }

    public void addOrUpdateConnectDevice(String macCode,BleModelDevice device) {
        if (mDeviceMap!=null&&!mDeviceMap.containsKey(macCode)){
            mDeviceMap.put(macCode, device);
        }else {
            mDeviceMap.remove(macCode);
            mDeviceMap.put(macCode, device);
        }

    }

    public void romove(String macCode){
        if (mDeviceMap!=null&&mDeviceMap.containsKey(macCode)){
            mDeviceMap.remove(macCode);
        }
    }

    public BleModelDevice getConnectDevice(String macCode){
        if (mDeviceMap!=null&&mDeviceMap.containsKey(macCode)){
            return mDeviceMap.get(macCode);
        }
        return null;
    }



    public void clear() {
        mDeviceMap.clear();
    }

    public Map<String, BleModelDevice> getDeviceConnect() {
        return mDeviceMap;
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

    public List<BleModelDevice> getConnectDeviceList() {
        final List<BleModelDevice> methodResult = new ArrayList<>(mDeviceMap.values());
        return methodResult;
    }

    @Override
    public String toString() {
        return "BluetoothLeDeviceStore{" +
                "DeviceList=" + getConnectDeviceList() +
                '}';
    }
}
