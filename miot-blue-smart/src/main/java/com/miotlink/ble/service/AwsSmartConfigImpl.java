package com.miotlink.ble.service;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.miotlink.ble.Ble;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

class AwsSmartConfigImpl extends BleConnectCallback<BleModelDevice> {
   private MyThread myThread=null;
   private String macCode;
   private String awsNetworkInfoStr;
    Ble<BleModelDevice> ble=null;
    private ILinkSmartConfigListener smartConfigListener=null;
    private String errorMessage = "配网超时";
    private int errorCode=7011;
    private BleModelDevice bleModelDevice=null;
    private int delayMillis=0;
   public AwsSmartConfigImpl(Ble<BleModelDevice> ble, String macCode, String awsNetworkInfoStr,int delayMillis){
       this.macCode=macCode;
       this.delayMillis=delayMillis;
       this.awsNetworkInfoStr=awsNetworkInfoStr;
       this.ble=ble;
       handler.sendEmptyMessageDelayed(1000,delayMillis*1000);
   }


   public void setSmartData(final BleModelDevice bleModelDevice){
       new Thread(new Runnable() {
           @Override
           public void run() {
               BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
               try {
                   List<byte[]> list = bluetoothProtocol.awsSmartConfigEnCode(macCode, 256, awsNetworkInfoStr);
                   if (list!=null&&list.size()>0){
                       for (byte[] bytes:list){
                           ble.writeByUuid(bleModelDevice, bytes,
                                   Ble.options().getUuidService(),
                                   Ble.options().getUuidWriteCha(),
                                   bleWriteCallback);
                           Thread.sleep(300);
                       }
                   }
               } catch (Exception e) {
                   e.printStackTrace();
               }
           }
       }).start();
   }


    public void setSmartConfigListener(ILinkSmartConfigListener smartConfigListener) {
        this.smartConfigListener = smartConfigListener;
    }

    @Override
    public void onConnectionChanged(BleModelDevice device) {
       bleModelDevice=device;
        if (device.isDisconnected()){
            errorCode=7010;
            errorMessage="设备与手机断开连接，请确保设备与手机距离靠近。";

        }
        if (device.isConnected()){
            BluetoothDeviceStore.getInstace().addConnectDevice(macCode,device);
        }
    }

    @Override
    public void onServicesDiscovered(BleModelDevice device, BluetoothGatt gatt) {
        super.onServicesDiscovered(device, gatt);
        if(device.isConnected()){
            try {
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
    public void onDestory(){
        try {
            handler.removeMessages(1000);
            if (myThread!=null){
                myThread.interrupt();
                myThread=null;
            }
            if (bleModelDevice!=null){
                ble.disconnect(bleModelDevice);
            }
            BluetoothDeviceStore.getInstace().removeConnectDevice(macCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onReady(BleModelDevice device) {
        ble.enableNotify(device, true, bleNotifyCallback);
        super.onReady(device);

    }
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
                        if (code==0x13){
                            if (decode.containsKey("byte")) {
                              byte[]  bytesValue = (byte[]) decode.get("byte");
                                if (bytesValue!=null) {
                                    try {
                                        String message = new String(bytesValue, "UTF-8");
                                        if (!TextUtils.isEmpty(message)){
                                            Log.e("Bluetooth", message);
                                            JSONObject jsonObject=new JSONObject(message);
                                            if (jsonObject!=null){
                                                if (!jsonObject.isNull("type")&&TextUtils.equals("WifiReply",jsonObject.getString("type"))){
                                                   if (!jsonObject.isNull("data")){
                                                       JSONObject jsonData=new JSONObject(jsonObject.getString("data"));
                                                       if (jsonData!=null){
                                                           errorCode=jsonData.getInt("Code");
                                                           errorMessage= jsonData.getString("Describe");
                                                       }
                                                       handler.removeMessages(1000);

                                                   }
                                                }
                                                if (smartConfigListener!=null){
                                                    smartConfigListener.onLinkSmartConfigListener(7015,errorMessage,message);
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
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
                ble.setMTU(device.getBleAddress(), 256,bleMtuCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
    BleMtuCallback<BleModelDevice> bleMtuCallback=new BleMtuCallback<BleModelDevice>() {
        @Override
        public void onMtuChanged(final BleModelDevice device, int mtu, int status) {

           new Thread(new Runnable() {
               @Override
               public void run() {
                   BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                   try {
                       List<byte[]> list = bluetoothProtocol.awsSmartConfigEnCode(macCode, 256, awsNetworkInfoStr);
                       if (list!=null&&list.size()>0){
                           for (byte[] bytes:list){
                               ble.writeByUuid(device, bytes,
                                       Ble.options().getUuidService(),
                                       Ble.options().getUuidWriteCha(),
                                       bleWriteCallback);
                               Thread.sleep(300);
                           }
                       }
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
               }
           }).start();


        }
    };
    BleWriteCallback<BleModelDevice> bleWriteCallback=new BleWriteCallback<BleModelDevice>() {
        @Override
        public void onWriteSuccess(BleModelDevice device, BluetoothGattCharacteristic characteristic) {

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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

}
