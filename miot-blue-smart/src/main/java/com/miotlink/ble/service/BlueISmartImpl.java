package com.miotlink.ble.service;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.bluetooth.sdk.R;
import com.miotlink.ble.Ble;
import com.miotlink.ble.BleLog;
import com.miotlink.ble.callback.BleConnectCallback;
import com.miotlink.ble.callback.BleMtuCallback;
import com.miotlink.ble.callback.BleNotifyCallback;
import com.miotlink.ble.callback.BleScanCallback;
import com.miotlink.ble.callback.BleStatusCallback;
import com.miotlink.ble.callback.BleWriteCallback;
import com.miotlink.ble.listener.ILinkBindScanCallBack;
import com.miotlink.ble.listener.ILinkBlueScanCallBack;
import com.miotlink.ble.listener.ILinkConnectCallback;
import com.miotlink.ble.listener.ILinkSmartConfigListener;
import com.miotlink.ble.listener.OTAListener;
import com.miotlink.ble.listener.SmartListener;
import com.miotlink.ble.listener.SmartNotifyDeviceConnectListener;
import com.miotlink.ble.listener.SmartNotifyListener;
import com.miotlink.ble.listener.SmartNotifyOTAListener;
import com.miotlink.ble.listener.SmartNotifyUartDataListener;
import com.miotlink.ble.model.BleFactory;
import com.miotlink.ble.model.BleModelDevice;
import com.miotlink.ble.model.BluetoothDeviceStore;
import com.miotlink.ble.model.HasConnectBleDeviceStore;
import com.miotlink.ble.model.ScanRecord;
import com.miotlink.ble.utils.ByteUtils;
import com.miotlink.ble.utils.Utils;
import com.miotlink.ble.utils.UuidUtils;
import com.miotlink.protocol.BluetoothProtocol;
import com.miotlink.protocol.BluetoothProtocolImpl;
import com.miotlink.utils.HexUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Deprecated
public class BlueISmartImpl extends BleWriteCallback<BleModelDevice> implements ISmart {
    private static final String filter_name = "MLink";
    private Ble<BleModelDevice> ble = null;
    private SmartListener mSmartListener = null;
    private Context mContext = null;
    public static boolean Debug = false;
    private BluetoothDeviceStore bluetoothDeviceStore = new BluetoothDeviceStore();
    private BluetoothDeviceStore hasAddDeviceStores = new BluetoothDeviceStore();


    private HasConnectBleDeviceStore hasConnectBleDeviceStore=new HasConnectBleDeviceStore();
    private String ssid = "";
    private String password = "";
    private ILinkBlueScanCallBack mILinkBlueScanCallBack = null;
    private ILinkSmartConfigListener mILinkSmartConfigListener = null;
    private String errorMessage = "";
    private int errorCode=7001;
    private ILinkConnectCallback mLinkConnectCallback=null;
    private  MyThread myThread=null;
    private OTAThread otaThread=null;
    private boolean isConnect=false;
    private ILinkBindScanCallBack mILinkBindScanCallBack=null;

    private List<SmartNotifyListener> notifyListeners=new ArrayList<>();
    @Override
    public void init(Context mContext, SmartListener mSmartListener) throws Exception {
        this.mContext = mContext;
        this.mSmartListener = mSmartListener;

        BleFactory bleFactory = new BleFactory<BleModelDevice>() {
            @Override
            public BleModelDevice create(String address, String name) {
                return new BleModelDevice(address, name);
            }
        };
        Ble.options()
                .setLogBleEnable(Debug)//设置是否输出打印蓝牙日志
                .setThrowBleException(true)//设置是否抛出蓝牙异常
                .setLogTAG("Mlink_BLE")//设置全局蓝牙操作日志TAG
                .setAutoConnect(true)//设置是否自动连接
                .setIgnoreRepeat(false)//设置是否过滤扫描到的设备(已扫描到的不会再次扫描)
                .setConnectFailedRetryCount(3)//连接异常时（如蓝牙协议栈错误）,重新连接次数
                .setConnectTimeout(10 * 1000)//设置连接超时时长
                .setScanPeriod(24*60 * 60 * 1000)//设置扫描时长
                .setParseScanData(true)
                .setMaxConnectNum(7)//最大连接数量
                .setUuidService(UUID.fromString(UuidUtils.uuid16To128("6600")))//设置主服务的uuid
                .setUuidWriteCha(UUID.fromString(UuidUtils.uuid16To128("6602")))//设置可写特征的uuid
                .setUuidReadCha(UUID.fromString(UuidUtils.uuid16To128("6601")))//设置可读特征的uuid （选填）
                .setUuidNotifyCha(UUID.fromString(UuidUtils.uuid16To128("6601")))//设置可通知特征的uuid （选填，库中默认已匹配可通知特征的uuid）
                .setUuidOtaWriteCha(UUID.fromString(UuidUtils.uuid16To128("6603")))
                .setFactory(bleFactory)
                .create(mContext, initCallback);
    }
    Ble.InitCallback initCallback = new Ble.InitCallback() {

        @Override
        public void success() {
            if (mSmartListener != null) {
                try {
                    mSmartListener.onSmartListener(1, "init success", null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void failed(int failedCode) {

            if (mSmartListener != null) {
                try {
                    mSmartListener.onSmartListener(-1, "init failed", null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };


    @Override
    public int checkAuthority() {
        if (!Ble.getInstance().isSupportBle(mContext)) {
            return 2;
        }
        if (!Ble.getInstance().isBleEnable()) {
            return 3;
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Utils.isGpsOpen(mContext)){
            return 4;
        }else if (!Utils.isPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)&&
                !Utils.isPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)){
            return 5;
        }
        return 1;

    }

    @Override
    public void setDeviceInfo(boolean isOpen) {

    }

    @Override
    public void sendUartData(String mac, byte[] data) throws Exception {

    }

    @Override
    public void setServiceUUID(String serviceUuId, String readUuid, String writeUuid) throws Exception {
        Ble.options().setUuidService(UUID.fromString(UuidUtils.uuid16To128(serviceUuId)))//设置主服务的uuid
                .setUuidWriteCha(UUID.fromString(UuidUtils.uuid16To128(writeUuid)))//设置可写特征的uuid
                .setUuidReadCha(UUID.fromString(UuidUtils.uuid16To128(readUuid)))
                .setUuidOtaWriteCha(UUID.fromString(UuidUtils.uuid16To128("6603")))
                //设置可读特征的uuid （选填）
                .setUuidNotifyCha(UUID.fromString(UuidUtils.uuid16To128(readUuid)));//设置可通知特征的uuid （
    }

    @Override
    public void sendUartData(String mac, String data, SmartNotifyListener smartNotifyListener) throws Exception {

    }



    @Override
    public void deleteDevice(String macCode) {
        if(bluetoothDeviceStore.getDeviceMap().containsKey(macCode)){
            bluetoothDeviceStore.removeDevice(macCode);
        };
    }



    @Override
    public void onScan(final ILinkBlueScanCallBack mILinkBlueScanCallBack) throws Exception {
        if (ble == null) {
            ble = Ble.getInstance();
        }
        bluetoothDeviceStore.clear();

        this.mILinkBlueScanCallBack = mILinkBlueScanCallBack;
        ble.setBleStatusCallback(new BleStatusCallback() {
            @Override
            public void onBluetoothStatusChanged(boolean isOn) {
                if (isOn) {
                    ble.startScan(scanCallback);
                }else {
                    ble.stopScan();
                }
            }
        });
        if (!ble.isBleEnable()) {
            return;
        }
        ble.startScan(scanCallback);
    }

//    @Override
//    public void onScan(ILinkBindScanCallBack mILinkBindScanCallBack) throws Exception {
//        if (ble == null) {
//            ble = Ble.getInstance();
//        }
//        bluetoothDeviceStore.clear();
//        this.mILinkBindScanCallBack = mILinkBindScanCallBack;
//        ble.setBleStatusCallback(new BleStatusCallback() {
//            @Override
//            public void onBluetoothStatusChanged(boolean isOn) {
//                if (isOn) {
//                    ble.startScan(scanCallback);
//                }else {
//                    ble.stopScan();
//                }
//            }
//        });
//        if (!ble.isBleEnable()) {
//            return;
//        }
//        ble.startScan(scanCallback);
//    }



    BleScanCallback<BleModelDevice> scanCallback=new BleScanCallback<BleModelDevice>() {
        @Override
        public void onLeScan(BleModelDevice device, int rssi, byte[] scanRecord) {
            ScanRecord scanRecords = ScanRecord.parseFromBytes(scanRecord);
            String hexScanRecord="";
            try {
                if (scanRecords!=null){
                    SparseArray<byte[]> manufacturerSpecificData = scanRecords.getManufacturerSpecificData();
                    if (manufacturerSpecificData!=null&&manufacturerSpecificData.size()>0){
                        byte[] bytes = manufacturerSpecificData.get(26470);
                        if (bytes!=null&&bytes.length>0){
                            hexScanRecord=HexUtil.encodeHexStr(bytes);
                        }
                    }
                }
//                Log.e("bleScan", hexScanRecord);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!TextUtils.isEmpty(hexScanRecord) ||!TextUtils.isEmpty(device.getBleName())
                    &&device.getBleName().startsWith("Hi-Huawei-Mars")) {
                device.setScanRecord(ScanRecord.parseFromBytes(scanRecord));
                if (TextUtils.isEmpty(device.getMacAddress())){
                    device.setMacAddress(device.getBleAddress());
                }
                if (device.getMark()==0){
                    if (!hasAddDeviceStores.isHasDevice(device.getMacAddress())){
                        hasAddDeviceStores.addDevice(device.getMacAddress(),device);
                        if (mILinkBindScanCallBack!=null){
                            try {
                                mILinkBindScanCallBack.onHasBindScanDevice(device);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return;
                }
                if (hasAddDeviceStores.isHasDevice(device.getMacAddress())){
                    hasAddDeviceStores.remove(device.getMacAddress());
                    if (mILinkBindScanCallBack!=null){
                        try {
                            mILinkBindScanCallBack.onHasBindScanDevice(device);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (bluetoothDeviceStore.isHasDevice(device.getMacAddress())){
                    return;
                }
                try {
                    bluetoothDeviceStore.addDevice(device.getMacAddress(),device);
                    if (hasAddDeviceStores.isHasDevice(device.getMacAddress())){
                        hasAddDeviceStores.remove(device.getMacAddress());
                    }
                    if (mILinkBlueScanCallBack!=null){
                        mILinkBlueScanCallBack.onScanDevice(device);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };



    @Override
    public void openBluetooth() {
        if (ble == null) {
            ble = Ble.getInstance();
        }
        ble.turnOnBlueToothNo();
    }

    @Override
    public void onScanStop() throws Exception {
        if (ble == null) {
            ble = Ble.getInstance();
        }
        ble.stopScan();
        if (mILinkBindScanCallBack!=null){
            mILinkBindScanCallBack=null;
        }
        if (mILinkBlueScanCallBack!=null){
            mILinkBlueScanCallBack=null;
        }
    }

    private OTAListener otaListener=null;



    BleConnectCallback<BleModelDevice> connectCallback=new BleConnectCallback<BleModelDevice>() {
        @Override
        public void onConnectionChanged(BleModelDevice device) {
            try {
                if (device.isConnected()) {
                    isConnect=true;
                } else if (device.isConnecting()) {
                    if (mLinkConnectCallback!=null){
                        mLinkConnectCallback.onConnected(2,device);
                    }
                }else if (device.isDisconnected()){
                    if (mLinkConnectCallback!=null){
                        mLinkConnectCallback.onConnected(0,device);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onReady(BleModelDevice device) {
            super.onReady(device);
        }

        @Override
        public void onConnectFailed(BleModelDevice device, int errorCode) {
            super.onConnectFailed(device, errorCode);
        }

        @Override
        public void onServicesDiscovered(BleModelDevice device, BluetoothGatt gatt) {
            super.onServicesDiscovered(device, gatt);
            ble.setMTU(device.getBleAddress(), 256, new BleMtuCallback<BleModelDevice>() {
                @Override
                public void onMtuChanged(BleModelDevice device, int mtu, int status) {
                    if (BluetoothGatt.GATT_SUCCESS == status && 256 == mtu) {
                        if (mLinkConnectCallback!=null){
                            try {
                                mLinkConnectCallback.onConnected(2,device);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
    };

    @Override
    public void onConnect(final String macCode, final ILinkConnectCallback mLinkConnectCback) throws Exception {
        if (ble == null) {
            ble = Ble.getInstance();
        }

        this.mLinkConnectCallback=mLinkConnectCback;
        Log.e("onConnect", macCode);
        ble.connect(macCode, new BleConnectCallback<BleModelDevice>() {
            @Override
            public void onConnectionChanged(BleModelDevice device) {
                try {
                    if (device.isConnected()) {
                        hasConnectBleDeviceStore.addOrUpdateConnectDevice(macCode,device);
                        isConnect=true;
                    } else if (device.isDisconnected()) {
                        isConnect=false;
                        hasConnectBleDeviceStore.romove(macCode);
                        if (mLinkConnectCback!=null){
                            mLinkConnectCallback.onConnected(0,device);
                        }

                    }else if (device.isConnecting()){
                        if (mLinkConnectCback!=null){
                            mLinkConnectCallback.onConnected(2,device);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            @Override
            public void onServicesDiscovered(final BleModelDevice device, BluetoothGatt gatt) {
                super.onServicesDiscovered(device, gatt);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            ble.setMTU(device.getBleAddress(), 256, new BleMtuCallback<BleModelDevice>() {
                                @Override
                                public void onMtuChanged(BleModelDevice device, int mtu, int status) {
                                    if (mLinkConnectCallback!=null){
                                        try {
                                            mLinkConnectCallback.onConnected(1,device);
                                            BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                                            byte[] bytes = bluetoothProtocol.getVersion();
                                            ble.writeByUuid(device, bytes,
                                                    Ble.options().getUuidService(),
                                                    Ble.options().getUuidWriteCha(),
                                                    BlueISmartImpl.this);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }

                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }

            @Override
            public void onReady(BleModelDevice device) {
                if (ble != null) {
                    ble.enableNotify(device, true, bleNotifyCallback);
                }
                super.onReady(device);
            }
        });

    }

    @Override
    public void connect(String macCode) throws Exception {

    }

    @Override
    public void setSmartNotifyDeviceConnectListener(SmartNotifyDeviceConnectListener smartNotifyDeviceConnectListener) {

    }

    @Override
    public void setSmartNotifyUartData(SmartNotifyUartDataListener notifyUartDataListener) {

    }

    @Override
    public void setSmartNotifyOnlineOTAListener(SmartNotifyOTAListener notifyOnlineOTAListener) {

    }

    @Override
    public void setNotifyListener(SmartNotifyListener notifyListener) {
        if (notifyListeners!=null&&!notifyListeners.contains(notifyListener)){
            notifyListeners.add(notifyListener);
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1000) {
                try {
                    if (mILinkSmartConfigListener!=null){
                        mILinkSmartConfigListener.onLinkSmartConfigTimeOut(errorCode,errorMessage);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private String smartMac="";
    @Override
    public void onStartSmartConfig(String macCode, String ssid, String password, int delayMillis,ILinkSmartConfigListener mILinkSmartConfigListener) throws Exception {
        if (ble == null) {
            ble = Ble.getInstance();
        }
        if (delayMillis<60){
            delayMillis=60;
        }
        smartMac=macCode;
        handler.sendEmptyMessageDelayed(1000, delayMillis*1000);
        this.ssid = ssid;
        this.password = password;
        this.mILinkSmartConfigListener = mILinkSmartConfigListener;
        BleModelDevice bleModelDevice=null;
        if (bluetoothDeviceStore.getDeviceMap().containsKey(macCode)) {
            bleModelDevice = bluetoothDeviceStore.getDeviceMap().get(macCode);
            bleModelDevice.setAutoConnecting(false);

        }else {
            bleModelDevice=ble.getBleDevice(smartMac);
        }
        ble.connect(bleModelDevice, bleModelDeviceCallback);
    }

    @Override
    public void onAwsStartSmartConfig(String macCode, String  awsNetworkInfo, int delayMillis, ILinkSmartConfigListener mILinkSmartConfigListener1) throws Exception {
        if (ble == null) {
            ble = Ble.getInstance();
        }
        if (delayMillis<60){
            delayMillis=60;
        }
        smartMac=macCode;
        handler.sendEmptyMessageDelayed(1000, delayMillis*1000);
        this.mILinkSmartConfigListener = mILinkSmartConfigListener1;
        BleModelDevice bleModelDevice=null;
        if (bluetoothDeviceStore.getDeviceMap().containsKey(macCode)) {
            bleModelDevice = bluetoothDeviceStore.getDeviceMap().get(macCode);
        }else {
            bleModelDevice=ble.getBleDevice(smartMac);
        }
        AwsSmartConfigImpl awsSmartConfig = new AwsSmartConfigImpl(ble, smartMac, awsNetworkInfo,delayMillis);
        ble.connect(bleModelDevice,awsSmartConfig );

    }

    @Override
    public void onStopSmartConfig(String macCode) {

    }

    @Override
    public void getBleDeviceInfo(String macCode) {

    }

    @Override
    public void startOtaOnline(String macCode, String data) throws Exception {

    }

    BleConnectCallback<BleModelDevice> bleModelDeviceCallback = new BleConnectCallback<BleModelDevice>() {
        @Override
        public void onConnectionChanged(BleModelDevice device) {
            if (device.isDisconnected()){

                errorMessage="设备与手机断开连接，请确保设备与手机距离靠近。";
            }else if (device.isConnected()){
                hasConnectBleDeviceStore.addOrUpdateConnectDevice(smartMac,device);
            }
        }

        @Override
        public void onReady(BleModelDevice device) {
            if (ble != null) {
                ble.enableNotify(device, true, bleNotifyCallback);
            }
            super.onReady(device);
        }

        @Override
        public void onServicesDiscovered(final BleModelDevice device, BluetoothGatt gatt) {
            if (device.isConnected()) {
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
            super.onServicesDiscovered(device, gatt);
        }
    };

    @Override
    public void unRegirster(SmartNotifyListener notifyListener) {
        if (notifyListeners!=null&&notifyListeners.contains(notifyListener)){
            notifyListeners.remove(notifyListener);
        }
    }

    BleNotifyCallback<BleModelDevice> bleNotifyCallback = new BleNotifyCallback<BleModelDevice>() {
        @Override
        public void onChanged(BleModelDevice device, BluetoothGattCharacteristic characteristic) {
            try {
                UUID uuid = characteristic.getUuid();
                if (uuid.equals(Ble.options().getUuidReadCha())) {
                    byte[] value = characteristic.getValue();
                    if (value != null) {
                        Log.e("hex", HexUtil.encodeHexStr(value));
                        BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                        Map<String, Object> decode = bluetoothProtocol.decode(value);
                        if (decode != null && decode.containsKey("code") && decode.containsKey("value")) {
//                            Log.e("error", decode.toString());
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
                                        errorMessage = mContext.getResources().getString(R.string.ble_device_start_net_error_message);
                                        errorCode=7001;
                                    }else if (TextUtils.equals("02", valueCode)) {
                                        errorMessage = mContext.getResources().getString(R.string.ble_device_start_connting_error_message);
                                        errorCode=7002;
                                    } else if (TextUtils.equals("03", valueCode)) {
                                        errorCode=7003;
                                        errorMessage = mContext.getResources().getString(R.string.ble_device_start_error_message);
                                    } else if (TextUtils.equals("0f", valueCode)) {
                                        handler.removeMessages(1000);
                                        errorMessage = "配网成功。";
                                        errorCode=7015;
                                        mILinkSmartConfigListener.onLinkSmartConfigListener(errorCode, errorMessage, device.getMacAddress());
                                    } else if (TextUtils.equals("ff", valueCode)) {
                                        handler.removeMessages(1000);
                                        errorCode=7255;
                                        errorMessage = mContext.getResources().getString(R.string.ble_device_connect_error_message);
                                        mILinkSmartConfigListener.onLinkSmartConfigListener(errorCode, errorMessage, device.getMacAddress());
                                    }
                                }
                            }else if (code ==0x06){
                                String valueCode = (String) decode.get("value");
                                BleLog.e("onChanged", "Code:" + code + "value:" + valueCode);
                                if (notifyListeners!=null&&notifyListeners.size()>0){
                                    for(SmartNotifyListener notifyListener:notifyListeners){
                                        notifyListener.notifyUartData(device.getBleAddress(),valueCode);
                                    }
                                }
                            }else if (code==0x09){
                                if (notifyListeners!=null&&notifyListeners.size()>0){
                                    for(SmartNotifyListener notifyListener:notifyListeners){
                                        long valueCode = (long) decode.get("value");
                                        notifyListener.notifyVersion(device.getBleAddress(),valueCode);
                                    }
                                }

                            }else if (code==0x0A){
                                if (otaListener!=null){
                                    int valueCode = (int) decode.get("value");
                                    BleLog.e("otaCheck", "Code:" + code + "value:" + valueCode);
                                    otaListener.checkOtaStateListener(device.getBleAddress(),valueCode);
                                    if (valueCode==1){
                                        if (otaThread!=null){
                                            otaThread.interrupt();
                                            isRunning=false;
                                            otaThread=null;
                                        }
                                        isRunning=true;
                                        otaThread=new OTAThread();
                                        otaThread. setBleModelDevice(device);
                                        otaThread.start();
                                    }
                                }
                            }else if (code==0x0B){
                                int valueCode = (int) decode.get("value");
                                if (valueCode==1){
                                }else if (valueCode==1){
                                    isRunning=true;
                                    index-=128;
                                }
                                else if (valueCode==3||valueCode==4){
                                    if (otaListener!=null){
                                        BleLog.e("onChanged", "Code:" + code + "value:" + valueCode);
                                        otaListener.firmwareOtaListener(device.getBleAddress(),valueCode);
                                    }
                                }
                            }else if (code==0x0C){
                                if (notifyListeners!=null&&notifyListeners.size()>0){
                                    for(SmartNotifyListener notifyListener:notifyListeners){
                                        int valueCode = (int) decode.get("value");
                                        BleLog.e("onChanged", "Code:" + code + "value:" + valueCode);
                                        notifyListener.deletePu(device.getBleAddress(),valueCode);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
    };

    @Override
    public void sendUart(String macCode, byte[] data) throws Exception {
        if (ble == null) {
            ble=Ble.getInstance();
        }
        BleModelDevice bleModelDevice=(BleModelDevice) Ble.getInstance().getBleDevice(macCode);
        if (bleModelDevice==null){
            bleModelDevice = hasConnectBleDeviceStore.getConnectDevice(macCode);
        }
        if (bleModelDevice != null) {
            BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
            byte[] bytes = bluetoothProtocol.hexEncode(data);
            BleLog.e("uart", HexUtil.encodeHexStr(bytes));
            ble.writeByUuid(bleModelDevice, bytes,
                    Ble.options().getUuidService(),
                    Ble.options().getUuidWriteCha(),
                    BlueISmartImpl.this);
        }

    }

    @Override
    public void sendUart(String mac, String data) throws Exception {

    }

    @Override
    public void onDisConnect(String macCode) throws Exception {
        mILinkSmartConfigListener=null;
        handler.removeMessages(1000);
        if (ble == null) {
            ble=Ble.getInstance();
        }
        try {
            if (!TextUtils.isEmpty(macCode)) {
                BleModelDevice bleDevice = hasConnectBleDeviceStore.getConnectDevice(macCode);
                if (bleDevice != null) {
                    Log.e("disConnected", "断开连接");
                    ble.disconnect(bleDevice);
                    hasConnectBleDeviceStore.romove(macCode);
                    bluetoothDeviceStore.clear();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onDestory() throws Exception {
        if (ble != null) {
            ble.released();
            hasConnectBleDeviceStore.clear();
        }
    }

    @Override
    public void disConnectAll() throws Exception {

    }

    @Override
    public void getDeviceVersion(String macCode) throws Exception {
        if (ble == null) {
            ble=Ble.getInstance();
        }
        BleModelDevice bleModelDevice=(BleModelDevice) Ble.getInstance().getBleDevice(macCode);
        if (bleModelDevice==null){
            bleModelDevice=hasConnectBleDeviceStore.getConnectDevice(macCode);
        }
        if (bleModelDevice != null) {
            BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
            byte[] bytes = bluetoothProtocol.getVersion();
            ble.writeByUuid(bleModelDevice, bytes,
                    Ble.options().getUuidService(),
                    Ble.options().getUuidWriteCha(),
                    BlueISmartImpl.this);
        }
    }
    @Override
    public void startOtaFile(String macCode,File file) throws Exception {


    }

    @Override
    public boolean startOta(String macCode, File file,OTAListener listener) throws Exception {
        this.otaListener=listener;
        this.file=file;
        if (ble == null) {
            ble=Ble.getInstance();
        }
        smartMac=macCode;
        BleModelDevice bleModelDevice=(BleModelDevice) Ble.getInstance().getBleDevice(macCode);
        if (bleModelDevice==null){
            bleModelDevice=hasConnectBleDeviceStore.getConnectDevice(macCode);
        }
        if (bleModelDevice != null) {
            BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
            byte[] bytes = bluetoothProtocol.startOta(file);
            ble.writeByUuid(bleModelDevice, bytes, Ble.options().getUuidService(), Ble.options().getUuidWriteCha(),
                    BlueISmartImpl.this);

            return true;

        }
        return false;
    }

    @Override
    public void onWriteSuccess(BleModelDevice device, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void unBindPu(String macCode, int kindId, int modelId) throws Exception {
        if (ble == null) {
            ble=Ble.getInstance();
        }

        BleModelDevice bleModelDevice=(BleModelDevice) Ble.getInstance().getBleDevice(macCode);
        if (bleModelDevice==null){
            bleModelDevice = hasConnectBleDeviceStore.getConnectDevice(macCode);
        }
        if (bleModelDevice != null) {
            if (bleModelDevice.isConnected()){
                BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                byte[] bytes = bluetoothProtocol.unbindPu(kindId,modelId);
                ble.writeByUuid(bleModelDevice, bytes,
                        Ble.options().getUuidService(),
                        Ble.options().getUuidWriteCha(),
                        BlueISmartImpl.this);
            }
        }
    }

    private boolean isRunning=false;

    private int count=0;

    private File file=null;


    private int index = 0;


    @Override
    public void stopOta(String macCode) throws Exception {
        if (isRunning) {
            isRunning=false;
        }
        if (otaThread!=null){
            otaThread.interrupt();
            otaThread=null;
        }
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
                                otaListener.firmwareOtaListener(bleModelDevice!=null?bleModelDevice.getMacAddress():smartMac, 4);
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
                                    BlueISmartImpl.this);
//                            isRunning=false;
                        }else {
                            if (otaListener!=null){
                                otaListener.firmwareOtaListener(bleModelDevice!=null?bleModelDevice.getMacAddress():smartMac, 4);
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
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ble.setMTU(device.getBleAddress(), 128, new BleMtuCallback<BleModelDevice>() {
                @Override
                public void onMtuChanged(final BleModelDevice device, int mtu, int status) {
                    BleLog.e("onMtuChanged",device.getBleAddress()+"MTU"+status);
                    BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                    byte[] bytes =null;
                    if (TextUtils.equals("0", ssid)&&
                            TextUtils.equals("0", password)){
                        bytes=bluetoothProtocol.bleSmartConfig();
                    }else {
                        bytes=bluetoothProtocol.SmartConfigEncode(ssid, password);
                    }
                    if (bytes != null) {
                        BleLog.e("onConnectionChanged", HexUtil.encodeHexStr(bytes));
                        ble.writeByUuid(device, bytes,
                                Ble.options().getUuidService(),
                                Ble.options().getUuidWriteCha(),
                                BlueISmartImpl.this);
                    }
                }
            });
        }
    }
 }
