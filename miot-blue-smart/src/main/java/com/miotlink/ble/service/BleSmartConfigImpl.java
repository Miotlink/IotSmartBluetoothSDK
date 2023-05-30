package com.miotlink.ble.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.bluetooth.sdk.R;
import com.miotlink.ble.Ble;
import com.miotlink.ble.BleLog;
import com.miotlink.ble.callback.BleConnectCallback;
import com.miotlink.ble.callback.BleMtuCallback;
import com.miotlink.ble.callback.BleNotifyCallback;
import com.miotlink.ble.callback.BleWriteCallback;
import com.miotlink.ble.listener.ILinkSmartConfigListener;
import com.miotlink.ble.model.BleModelDevice;
import com.miotlink.ble.model.BluetoothDeviceStore;
import com.miotlink.protocol.BluetoothProtocol;
import com.miotlink.protocol.BluetoothProtocolImpl;
import com.miotlink.utils.HexUtil;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;


class BleSmartConfigImpl extends BleConnectCallback<BleModelDevice> {
    Ble<BleModelDevice> ble=null;
    private MyThread myThread=null;
    private String macCode;
    private byte[] bytes=null;
    private BleModelDevice device=null;

    private String deviceName="";

    private int delayMillis=60;

    private MyGetDeviceInfoThread myGetDeviceInfoThread=null;

    private String errorMessage = "未配网";
    private int errorCode=7001;

    private BleModelDevice bleModelDevice=null;

    private ILinkSmartConfigListener smartConfigListener=null;

    private boolean isOpen=false;

    public void setSmartConfigListener(ILinkSmartConfigListener smartConfigListener) {
        this.smartConfigListener = smartConfigListener;
    }

    public BleSmartConfigImpl(Ble<BleModelDevice> ble, String macCode, byte[] bytes,int delayMillis){
        this.macCode=macCode;
        this.ble=ble;
        this.bytes=bytes;
        this.delayMillis=delayMillis;
        handler.sendEmptyMessageDelayed(1000,delayMillis*1000);
    }

    public void setDeviceInfo(boolean isOpen){
        this.isOpen=isOpen;
    }

    public void setSmartData(final BleModelDevice bleModelDevice){
        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                try {
                    ble.writeByUuid(bleModelDevice, bytes,
                            Ble.options().getUuidService(),
                            Ble.options().getUuidWriteCha(),
                            new BleWriteCallback<BleModelDevice>() {
                                @Override
                                public void onWriteSuccess(BleModelDevice device, BluetoothGattCharacteristic characteristic) {

                                }
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void onDestory(){
        try {
            handler.removeMessages(1000);
            if (myThread!=null){
                myThread.interrupt();
                myThread=null;
            }
            ble.disconnect(bleModelDevice);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionChanged(BleModelDevice device) {
        bleModelDevice=device;
        if (device.isDisconnected()){
            BluetoothDeviceStore.getInstace().removeConnectDevice(macCode);
            errorCode=7010;
            errorMessage="设备与手机断开连接，请确保设备与手机距离靠近。";
        }else if (device.isConnected()){
            BluetoothDeviceStore.getInstace().addConnectDevice(macCode,device);
        }

    }
    @Override
    public void onServicesDiscovered(BleModelDevice device, BluetoothGatt gatt) {
        super.onServicesDiscovered(device, gatt);
        if(device.isConnected()){
            try {
                this.device=device;
                if (myThread!=null){
                    myThread.interrupt();
                    myThread=null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            myThread=new MyThread(device);
            myThread.start();
        }

    }



    class MyThread extends Thread{
        private BleModelDevice device;
        public MyThread(BleModelDevice modelDevice){
            this.device=modelDevice;
        }
        @Override
        public void run() {
            super.run();
            try {
                Thread.sleep(1000);
                ble.setMTU(device.getBleAddress(), 128,bleMtuCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    @Override
    public void onReady(BleModelDevice device) {
        ble.enableNotify(device, true, bleNotifyCallback);
        super.onReady(device);

    }

    BleMtuCallback<BleModelDevice> bleMtuCallback=new BleMtuCallback<BleModelDevice>() {
        @Override
        public void onMtuChanged(final BleModelDevice device, int mtu, int status) {
            if (myThread!=null){
                myThread.interrupt();
                myThread=null;
            }
            if (isOpen){
                myGetDeviceInfoThread=new MyGetDeviceInfoThread();
                myGetDeviceInfoThread.setBleModelDevice(device);
                myGetDeviceInfoThread.start();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ble.writeByUuid(device, bytes,
                            Ble.options().getUuidService(),
                            Ble.options().getUuidWriteCha(),
                            new BleWriteCallback<BleModelDevice>() {
                                @Override
                                public void onWriteSuccess(BleModelDevice device, BluetoothGattCharacteristic characteristic) {

                                }
                            });

                }
            }).start();


        }
    };


    BleNotifyCallback<BleModelDevice> bleNotifyCallback = new BleNotifyCallback<BleModelDevice>() {
        @Override
        public void onChanged(BleModelDevice device, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            if (!TextUtils.isEmpty(uuid.toString())&&TextUtils.equals(Ble.options().getUuidReadCha().toString(),uuid.toString())){
                byte[] value = characteristic.getValue();
                Log.e("hex", HexUtil.encodeHexStr(value));
                if (value==null){
                    return;
                }
                BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                Map<String, Object> decode = bluetoothProtocol.decode(value);
                if (decode!=null){
                    if (decode != null && decode.containsKey("code") && decode.containsKey("value")) {
                        int code = (int) decode.get("code");
                        if (code == 4) {
                            String valueCode = (String) decode.get("value");
                            BleLog.e("onChanged", "Code:" + code + "value:" + valueCode);
                            if (!TextUtils.isEmpty(valueCode)) {
                                if (TextUtils.equals("00", valueCode)) {
                                    errorMessage = "未配网";
                                    errorCode=7001;
                                } else if (TextUtils.equals("01", valueCode)) {
                                    errorMessage = "未配网";
                                    errorCode=7001;
                                }else if (TextUtils.equals("02", valueCode)) {
                                    errorCode=7002;
                                    errorMessage = "";
                                } else if (TextUtils.equals("03", valueCode)) {
                                    errorMessage =ble.getContext().getResources().getString(R.string.ble_device_start_error_message);
                                    errorCode=7003;
                                } else if (TextUtils.equals("0F", valueCode)||TextUtils.equals("0f", valueCode)) {
                                    errorMessage = "配网成功。";
                                    errorCode=7015;
                                    errorMessage="SUCCESS";
                                    BleLog.e("onChanged",errorMessage);
                                    try {
                                        JSONObject jsonObject=new JSONObject();
                                        jsonObject.put("mac",macCode);
                                        if (!TextUtils.isEmpty(deviceName)){
                                            jsonObject.put("deviceId",deviceName);
                                        }
                                        if (smartConfigListener!=null){
                                            smartConfigListener.onLinkSmartConfigListener(errorCode, errorMessage,jsonObject.toString());
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    onDestory();
                                } else if (TextUtils.equals("ff", valueCode)) {
                                    handler.removeMessages(1000);
                                    errorCode=7255;
                                    errorMessage = ble.getContext().getResources().getString(R.string.ble_device_connect_error_message);
                                    try {
                                        if (smartConfigListener!=null){
                                            smartConfigListener.onLinkSmartConfigListener(errorCode, errorMessage,macCode);
                                        }
                                        onDestory();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }else if (code==0x08){
                            byte [] bytesValue=null;
                            int len=0;
                            if (decode.containsKey("byte")){
                                bytesValue=(byte[]) decode.get("byte");
                                len=bytesValue.length;
                                if (bytesValue!=null){
                                    try {
                                        deviceName = new String(bytesValue, "UTF-8");
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                    BleLog.e("deviceName",deviceName);
                                    isOpen=false;
                                    if (myGetDeviceInfoThread!=null){
                                        myGetDeviceInfoThread.interrupt();
                                        myGetDeviceInfoThread=null;
                                    }
                                }
                            }
                        }
                    }
                }

            }

        }
        @Override
        public void onNotifySuccess(final BleModelDevice device) {
            super.onNotifySuccess(device);
        }
    };


    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1000) {
                try {
                    try {
                        handler.removeMessages(1000);
                        if (smartConfigListener!=null){
                            smartConfigListener.onLinkSmartConfigTimeOut(errorCode, errorMessage);
                        }
                        onDestory();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    class  MyGetDeviceInfoThread extends Thread{
        BleModelDevice modelDevice=null;
        public void setBleModelDevice(BleModelDevice bleModelDevice) {
            this.modelDevice = bleModelDevice;
        }
        @Override
        public void run() {
            super.run();
            while (isOpen){
                try {
                    Thread.sleep(1000);
                    BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                    byte[] bytes = bluetoothProtocol.getDeviceInfo();
                    Log.e("UART", "DeviceName"+HexUtil.encodeHexStr(bytes));
                    ble.writeByUuid(modelDevice, bytes,
                            Ble.options().getUuidService(),
                            Ble.options().getUuidWriteCha(),
                            new BleWriteCallback<BleModelDevice>() {
                                @Override
                                public void onWriteSuccess(BleModelDevice device, BluetoothGattCharacteristic characteristic) {

                                }
                            });

                } catch (Exception e) {

                }
            }
        }
    }

}
