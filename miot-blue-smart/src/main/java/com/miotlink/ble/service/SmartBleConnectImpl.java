package com.miotlink.ble.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.miotlink.ble.Ble;
import com.miotlink.ble.callback.BleConnectCallback;
import com.miotlink.ble.callback.BleMtuCallback;
import com.miotlink.ble.callback.BleNotifyCallback;
import com.miotlink.ble.callback.BleWriteCallback;
import com.miotlink.ble.listener.ILinkConnectCallback;
import com.miotlink.ble.listener.SmartNotifyDeviceConnectListener;
import com.miotlink.ble.model.BleModelDevice;
import com.miotlink.ble.model.BluetoothDeviceStore;
import com.miotlink.protocol.BluetoothProtocol;
import com.miotlink.protocol.BluetoothProtocolImpl;

import org.json.JSONObject;

import java.util.List;

class SmartBleConnectImpl extends BleConnectCallback<BleModelDevice> {

    private Ble<BleModelDevice> ble=Ble.getInstance();

    private ILinkConnectCallback linkConnectCallback=null;

    private String macCode="";

    private NofityCallback nofityCallback=null;


    private SmartNotifyDeviceConnectListener smartNotifyDeviceConnectListener=null;

    public void setSmartNotifyDeviceConnectListener(SmartNotifyDeviceConnectListener smartNotifyDeviceConnectListener) {
        this.smartNotifyDeviceConnectListener = smartNotifyDeviceConnectListener;
    }

    public void setNofityCallback(NofityCallback nofityCallback) {
        this.nofityCallback = nofityCallback;
    }

    public void getBleDeviceInfo(String mac){
        this.macCode=mac;
        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                try {
                    JSONObject jsonObject=new JSONObject();
                    jsonObject.put("type","Query");
                    List<byte[]> list = bluetoothProtocol.getDeviceInfo(macCode, 256, jsonObject.toString());
                    BleModelDevice bleModelDevice = BluetoothDeviceStore.getInstace().getConnectDevice(macCode);
                    if (bleModelDevice!=null&&bleModelDevice.isConnected()){
                        if (list!=null&&list.size()>0){
                            for (byte[] bytes:list){
                                ble.writeByUuid(bleModelDevice, bytes,
                                        Ble.options().getUuidService(),
                                        Ble.options().getUuidWriteCha(),
                                        bleWriteCallback);
                                Thread.sleep(300);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void startOta(String mac, final String data){
        this.macCode=mac;
        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                try {
                    List<byte[]> list = bluetoothProtocol.startOta(macCode, 256, data);
                    BleModelDevice bleModelDevice = BluetoothDeviceStore.getInstace().getConnectDevice(macCode);
                    if (bleModelDevice!=null&&bleModelDevice.isConnected()){
                        if (list!=null&&list.size()>0){
                            for (byte[] bytes:list){
                                ble.writeByUuid(bleModelDevice, bytes,
                                        Ble.options().getUuidService(),
                                        Ble.options().getUuidWriteCha(),
                                        bleWriteCallback);
                                Thread.sleep(300);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void send(String mac, final byte[] data){
        this.macCode=mac;
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                    byte[] bytes = bluetoothProtocol.hexEncode(data);

                    BleModelDevice bleModelDevice = BluetoothDeviceStore.getInstace().getConnectDevice(macCode);
                    if (bleModelDevice!=null&&bleModelDevice.isConnected()){
                        ble.writeByUuid(bleModelDevice, bytes,
                                Ble.options().getUuidService(),
                                Ble.options().getUuidWriteCha(),
                                bleWriteCallback);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void sendData(String mac, final byte[] data){
        this.macCode=mac;
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    BleModelDevice bleModelDevice = BluetoothDeviceStore.getInstace().getConnectDevice(macCode);
                    if (bleModelDevice!=null&&bleModelDevice.isConnected()){
                        ble.writeByUuid(bleModelDevice, data,
                                Ble.options().getUuidService(),
                                Ble.options().getUuidWriteCha(),
                                bleWriteCallback);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public void  sendUart(String mac, final byte[] data)throws Exception{
        this.macCode=mac;
        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                try {
                    List<byte[]> list = bluetoothProtocol.getUartData(macCode, 256, data);
                    BleModelDevice bleModelDevice = BluetoothDeviceStore.getInstace().getConnectDevice(macCode);
                    if (bleModelDevice!=null&&bleModelDevice.isConnected()){
                        if (list!=null&&list.size()>0){
                            for (byte[] bytes:list){
                                ble.writeByUuid(bleModelDevice, bytes,
                                        Ble.options().getUuidService(),
                                        Ble.options().getUuidWriteCha(),
                                        bleWriteCallback);
                                Thread.sleep(300);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public void  sendUart(String mac, final String data)throws Exception{
        this.macCode=mac;
        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                try {
                    List<byte[]> list = bluetoothProtocol.getUartData(macCode, 256, data);
                    BleModelDevice bleModelDevice = BluetoothDeviceStore.getInstace().getConnectDevice(macCode);
                    if (bleModelDevice!=null&&bleModelDevice.isConnected()){
                        if (list!=null&&list.size()>0){
                            for (byte[] bytes:list){
                                ble.writeByUuid(bleModelDevice, bytes,
                                        Ble.options().getUuidService(),
                                        Ble.options().getUuidWriteCha(),
                                        bleWriteCallback);
                                Thread.sleep(300);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    BleWriteCallback<BleModelDevice> bleWriteCallback=new BleWriteCallback<BleModelDevice>() {
        @Override
        public void onWriteSuccess(BleModelDevice device, BluetoothGattCharacteristic characteristic) {

        }
    };

    public void setLinkConnectCallback(ILinkConnectCallback linkConnectCallback) {
        this.linkConnectCallback = linkConnectCallback;
    }
    public void setMacCode(String macCode) {
        this.macCode = macCode;
    }
    @Override
    public void onConnectionChanged(BleModelDevice device) {
        try {
            int connectState =100;
            if (device.isConnecting()){
                connectState=101;
            }else if (device.isDisconnected()){
                connectState=103;
                BluetoothDeviceStore.getInstace().removeConnectDevice(macCode);
            }else if (device.isConnected()){
                BluetoothDeviceStore.getInstace().addConnectDevice(macCode,device);
            }
          if (linkConnectCallback!=null){
              linkConnectCallback.onConnected(connectState,device);
          }
          if (smartNotifyDeviceConnectListener!=null){
              smartNotifyDeviceConnectListener.notifyDeviceConnectListener(connectState,device);
          }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    @Override
    public void onReady(BleModelDevice device) {
        ble.enableNotify(device,true,bleNotifyCallback);
        super.onReady(device);

    }

    @Override
    public void onConnectCancel(BleModelDevice device) {
        if (linkConnectCallback!=null){
            try {
                linkConnectCallback.onConnected(104,device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (smartNotifyDeviceConnectListener!=null){
            smartNotifyDeviceConnectListener.notifyDeviceConnectListener(104,device);
        }
        super.onConnectCancel(device);
    }

    @Override
    public void onConnectFailed(BleModelDevice device, int errorCode) {
        if (linkConnectCallback!=null){
            try {
                linkConnectCallback.onConnected(errorCode,device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onConnectFailed(device, errorCode);
    }

    @Override
    public void onServicesDiscovered(final BleModelDevice device, BluetoothGatt gatt) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ble.setMTU(device.getBleAddress(),256,bleMtuCallback);
            }
        }).start();

        super.onServicesDiscovered(device, gatt);

    }
    BleMtuCallback<BleModelDevice> bleMtuCallback=new BleMtuCallback<BleModelDevice>() {
        @Override
        public void onMtuChanged(BleModelDevice device, int mtu, int status) {
            if (linkConnectCallback!=null){
                try {
                    linkConnectCallback.onConnected(200,device);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (smartNotifyDeviceConnectListener!=null){
                smartNotifyDeviceConnectListener.notifyDeviceConnectListener(200,device);
            }
            super.onMtuChanged(device, mtu, status);
        }
    };

    BleNotifyCallback<BleModelDevice> bleNotifyCallback=new BleNotifyCallback<BleModelDevice>() {
        @Override
        public void onChanged(BleModelDevice device, BluetoothGattCharacteristic characteristic) {
            if (nofityCallback!=null){
                try {
                    nofityCallback.onChanged(macCode,device,characteristic);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public interface NofityCallback {
        public void onChanged(String macCode, BleModelDevice device, BluetoothGattCharacteristic characteristic)throws Exception;
    }
}
