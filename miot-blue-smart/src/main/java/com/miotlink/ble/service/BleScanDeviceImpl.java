package com.miotlink.ble.service;

import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.RequiresApi;

import com.miotlink.ble.BleLog;
import com.miotlink.ble.callback.BleScanCallback;
import com.miotlink.ble.listener.ILinkBlueScanCallBack;
import com.miotlink.ble.model.BleModelDevice;
import com.miotlink.ble.model.BluetoothDeviceStore;
import com.miotlink.ble.model.ScanRecord;
import com.miotlink.utils.HexUtil;

class BleScanDeviceImpl extends BleScanCallback<BleModelDevice> {

    private static String TAG =BleScanDeviceImpl.class.getName();



    private ILinkBlueScanCallBack mILinkBlueScanCallBack=null;
    public BleScanDeviceImpl(){
        BluetoothDeviceStore.getInstace().clear();
    }

    public void setmILinkBlueScanCallBack(ILinkBlueScanCallBack mILinkBlueScanCallBack) {
        this.mILinkBlueScanCallBack = mILinkBlueScanCallBack;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onLeScan(BleModelDevice device, int rssi, byte[] scanRecord) {
        ScanRecord scanRecords = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanRecords = ScanRecord.parseFromBytes(scanRecord);
        }
        device.setRssi(rssi);
        String hexScanRecord = "";
        try {
            if (scanRecords != null) {
                device.setScanRecord(scanRecords);
                SparseArray<byte[]> manufacturerSpecificData = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    manufacturerSpecificData = scanRecords.getManufacturerSpecificData();
                }
                if (manufacturerSpecificData != null && manufacturerSpecificData.size() > 0) {
                    byte[] bytes = manufacturerSpecificData.get(26470);
                    if (bytes != null && bytes.length > 0) {
                        hexScanRecord = HexUtil.encodeHexStr(bytes);
                        BleLog.e(TAG,hexScanRecord);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        //CMB
        if (!TextUtils.isEmpty(hexScanRecord)
                || !TextUtils.isEmpty(device.getBleName())
                && device.getBleName().startsWith("Hi-Huawei")
                ||!TextUtils.isEmpty(device.getBleName())
                && device.getBleName().startsWith("HI-Huawei")
                ||!TextUtils.isEmpty(device.getBleName())
                && device.getBleName().startsWith("CMB")) {
            if (TextUtils.isEmpty(device.getMacAddress())) {
                device.setMacAddress(device.getBleAddress());
            }
            if (!BluetoothDeviceStore.getInstace().isHasDevice(device.getMacAddress())) {
                BluetoothDeviceStore.getInstace().addDevice(device.getMacAddress(), device);
               if (mILinkBlueScanCallBack!=null){
                   try {
                       mILinkBlueScanCallBack.onScanDevice(device);
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
               }
            }

        }

    }

}
