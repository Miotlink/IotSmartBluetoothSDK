package com.miotlink.ble.service;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.miotlink.ble.Ble;
import com.miotlink.ble.BleLog;
import com.miotlink.ble.callback.BleStatusCallback;

import com.miotlink.ble.listener.ILinkBlueScanCallBack;
import com.miotlink.ble.listener.ILinkConnectCallback;
import com.miotlink.ble.listener.ILinkSmartConfigListener;
import com.miotlink.ble.listener.OTAListener;
import com.miotlink.ble.listener.SmartListener;
import com.miotlink.ble.listener.SmartNotifyListener;
import com.miotlink.ble.model.BleFactory;
import com.miotlink.ble.model.BleModelDevice;
import com.miotlink.ble.model.BluetoothDeviceStore;
import com.miotlink.ble.utils.Utils;
import com.miotlink.ble.utils.UuidUtils;
import com.miotlink.protocol.BluetoothProtocol;
import com.miotlink.protocol.BluetoothProtocolImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothSmartImpl implements ISmart{
    private static final String TAG = BluetoothSmartImpl.class.getName();
    private Ble<BleModelDevice> ble = null;
    private SmartListener mSmartListener = null;
    private Context mContext = null;
    public static boolean Debug = false;
    private boolean isOpen;
    private ILinkBlueScanCallBack mILinkBlueScanCallBack = null;
    private ILinkSmartConfigListener mILinkSmartConfigListener = null;
    private BluetoothDeviceStore bluetoothDeviceStore = new BluetoothDeviceStore();

    private List<SmartNotifyListener> listeners=new ArrayList<>();

    private String macCode="";
    private BluetoothSmartImpl(){

    }
    BleFactory bleFactory = new BleFactory<BleModelDevice>() {
        @Override
        public BleModelDevice create(String address, String name) {
            return new BleModelDevice(address, name);
        }
    };


    @Override
    public void init(Context mContext, SmartListener smartListener) throws Exception {
        this.mContext = mContext;
        this.mSmartListener = smartListener;
        Ble.options().setLogBleEnable(Debug)//设置是否输出打印蓝牙日志
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
                .create(mContext, new Ble.InitCallback() {
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
                });
    }

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
        BleLog.e(TAG, "setDeviceInfo:"+isOpen);
        this.isOpen=isOpen;
    }

    @Override
    public void setServiceUUID(String serviceUuId, String readUuid, String writeUuid) throws Exception {
        Ble.options().setUuidService(UUID.fromString(UuidUtils.uuid16To128(serviceUuId)))//设置主服务的uuid
                .setUuidWriteCha(UUID.fromString(UuidUtils.uuid16To128(writeUuid)))//设置可写特征的uuid
                .setUuidReadCha(UUID.fromString(UuidUtils.uuid16To128(readUuid)))//设置可读特征的uuid （选填）
                .setUuidNotifyCha(UUID.fromString(UuidUtils.uuid16To128(readUuid)));//设置可通知特征的uuid （
    }
    @Override
    public void onScan(ILinkBlueScanCallBack mILinkBlueScanCallBack) throws Exception {
        if (ble == null) {
            ble = Ble.getInstance();
        }
        if (!ble.isSupportBle(mContext)) {
            throw new Exception("该手机暂不支持蓝牙设备");
        }
        if (!ble.isBleEnable()) {
            throw new Exception("蓝牙访问权限未打开");
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Utils.isGpsOpen(mContext)){
            throw new Exception("Android 操作系统8.1以上未打开定位服务");
        }else if (!Utils.isPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)&&
                !Utils.isPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)){
            throw new Exception("Android 操作系统8.0以上未打开定位权限");
        }

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

    BleScanDeviceImpl scanCallback=new BleScanDeviceImpl() {
        @Override
        protected void onScanDeviceReceiver(BleModelDevice device) throws Exception {
            if (mILinkBlueScanCallBack!=null){
                mILinkBlueScanCallBack.onScanDevice(device);
            }
        }
    };

    @Override
    public void sendUartData(String mac, byte[] data, SmartNotifyListener smartNotifyListener) throws Exception {

    }

    @Override
    public void disConnectAll() throws Exception {
        if (ble.getConnectedDevices()!=null){
            ble.disconnectAll();
        }
    }

    @Override
    public BleModelDevice getBleModelDevice(String macCode) {
        return null;
    }

    @Override
    public BleModelDevice getScanBleModelDevice(String macCode) {
        return null;
    }

    @Override
    public BleModelDevice getScanBindDevice(String macCode) {
        return null;
    }

    @Override
    public void deleteDevice(String macCode) {

    }




    @Override
    public void openBluetooth() {

    }

    @Override
    public void onScanStop() throws Exception {

    }

    @Override
    public void onConnect(String macCode, ILinkConnectCallback mLinkConnectCallback) throws Exception {



    }



    @Override
    public void setNotifyListener(SmartNotifyListener notifyListener) {

        if (listeners!=null&&!listeners.contains(notifyListener)){
            listeners.add(notifyListener);
        }
    }

    @Override
    public void unRegirster(SmartNotifyListener notifyListener) {
        if (listeners!=null&&listeners.contains(notifyListener)){
            listeners.remove(notifyListener);
        }
    }

    @Override
    public void onStartSmartConfig(String macCode, String ssid, String password, int delayMillis, ILinkSmartConfigListener mILinkSmartConfigListener) throws Exception {

        BleModelDevice  bleModelDevice =null;
        if (bluetoothDeviceStore.getDeviceMap().containsKey(macCode)) {
            bleModelDevice = bluetoothDeviceStore.getDeviceMap().get(macCode);
        }else {
            bleModelDevice=ble.getBleDevice(macCode);
        }

        BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
        byte[] bytes =null;
        if (TextUtils.equals("0", ssid)&&
                TextUtils.equals("0", password)){
            bytes=bluetoothProtocol.bleSmartConfig();
        }else {
            bytes=bluetoothProtocol.SmartConfigEncode(ssid, password);
        }
        ble.connect(bleModelDevice, new BleSmartConfigImpl(ble,macCode,bytes) {
            @Override
            protected void onSmartConfigReceiver(String macCode, int errorCode, String errorMessage) throws Exception {

            }
        });

    }

    @Override
    public void onAwsStartSmartConfig(String macCode, String awsNetworkInfo, int delayMillis, ILinkSmartConfigListener mILinkSmartConfigListener1) throws Exception {
        if (ble == null) {
            ble = Ble.getInstance();
        }
        if (delayMillis<60){
            delayMillis=60;
        }
        this.macCode=macCode;

        this.mILinkSmartConfigListener = mILinkSmartConfigListener1;
        BleModelDevice bleModelDevice=null;
        if (bluetoothDeviceStore.getDeviceMap().containsKey(macCode)) {
            bleModelDevice = bluetoothDeviceStore.getDeviceMap().get(macCode);
        }else {
            bleModelDevice=ble.getBleDevice(macCode);
        }
        handler.sendEmptyMessageDelayed(1000, delayMillis*1000);
        ble.connect(bleModelDevice, new AwsSmartConfigImpl(ble,macCode, awsNetworkInfo) {
            @Override
            protected void onReceiver(BleModelDevice device,String macCode, int awsErrorCode, String message, String data)throws Exception {
                Log.e("onReceiver", "macCode:"+macCode+"  errorCode:"+awsErrorCode+"message:"+message);
                if (awsErrorCode==65){
                    ble.disconnect(device);
//                    hasConnectBleDeviceStore.romove(macCode);
                    bluetoothDeviceStore.clear();
                    handler.removeMessages(1000);
//                    errorMessage = "配网成功。";
//                    errorCode=7015;
                    if (mILinkSmartConfigListener!=null)
                        mILinkSmartConfigListener.onLinkSmartConfigListener(7015, "errorMessage", macCode);
                    return;
                }
//                errorCode=awsErrorCode;
//                errorMessage=message;

            }
        });
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1000) {
                try {
                    if (mILinkSmartConfigListener!=null){
                        mILinkSmartConfigListener.onLinkSmartConfigTimeOut(1000,"errorMessage");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void sendUart(String mac, byte[] data) throws Exception {

    }

    @Override
    public void onDisConnect(String macCode) throws Exception {

    }

    @Override
    public void disConnected(BleModelDevice modelDevice) throws Exception {

    }

    @Override
    public void onDestory() throws Exception {

    }

    @Override
    public void getDeviceVersion(String macCode) throws Exception {

    }

    @Override
    public void startOtaFile(String macCode, File file) throws Exception {

    }

    @Override
    public boolean startOta(String macCode, File file, OTAListener otaListener) throws Exception {
        return false;
    }

    @Override
    public void stopOta(String macCode) throws Exception {

    }

    @Override
    public void unBindPu(String macCode, int kindId, int modelId) throws Exception {

    }


}
