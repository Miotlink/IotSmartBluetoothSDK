package com.miotlink.ble.service;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.miotlink.ble.Ble;
import com.miotlink.ble.callback.BleWriteCallback;

import com.miotlink.ble.listener.OTAListener;
import com.miotlink.ble.model.BleModelDevice;

import com.miotlink.ble.utils.ByteUtils;
import com.miotlink.protocol.BluetoothProtocol;
import com.miotlink.protocol.BluetoothProtocolImpl;

import java.io.File;

class BleDeviceOTAImpl extends BleWriteCallback<BleModelDevice> {


    private OTAThread otaThread=null;

    private BleModelDevice bleModelDevice=null;

    private int index=0;

    private boolean isRunning=false;

    private boolean isConnect=false;


    private OTAListener otaListener=null;

    private Ble<BleModelDevice> ble=null;

    private File file=null;

    private String macCode;



    public BleDeviceOTAImpl(){

    }




    @Override
    public void onWriteSuccess(BleModelDevice device, BluetoothGattCharacteristic characteristic) {

    }




    class OTAThread extends Thread{
        private BleModelDevice bleModelDevice=null;

        public void setBleModelDevice(BleModelDevice bleModelDevice) {
            this.bleModelDevice = bleModelDevice;
        }
        @Override
        public void run() {
            super.run();
            try {
                byte[] data = ByteUtils.toByteArray(file);
                final int packLength = 128;

                index=0;
                int length = data.length;
                isRunning=true;
                int availableLength = length;
                while (index < length){
                    if (isRunning){
                        if (!isConnect){
                            if (otaListener!=null){
                                otaListener.firmwareOtaListener(bleModelDevice!=null?bleModelDevice.getMacAddress():macCode, 4);
                            }
                            try {
                                isRunning=false;
                                if (otaThread!=null){
                                    otaThread.interrupt();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                        int onePackLength = packLength;
                        onePackLength = (availableLength >= packLength ? packLength : availableLength);
                        byte[] txBuffer = new byte[onePackLength];
                        Log.e("index", "index="+index);
                        for (int i=0; i<onePackLength; i++){
                            if(index < length){
                                txBuffer[i] = data[index++];
                            }
                        }
                        availableLength-=onePackLength;
                        if (bleModelDevice != null) {
                            if (otaListener!=null){
                                otaListener.progress(bleModelDevice.getMacAddress(), index * (360-(100*100/360)) / length);
                            }
                            BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                            byte [] fileHexByte=bluetoothProtocol.startOtaByte(index, txBuffer);
                            ble.writeByUuid(bleModelDevice,fileHexByte,
                                    Ble.options().getUuidService(),
                                    Ble.options().getUuidOtaWriteCha(),
                                   BleDeviceOTAImpl.this);
//                            isRunning=false;
                        }else {
                            if (otaListener!=null){
                                otaListener.firmwareOtaListener(bleModelDevice!=null?bleModelDevice.getMacAddress():macCode, 4);
                            }
                            try {
                                isRunning=false;
                                if (otaThread!=null){
                                    otaThread.interrupt();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
