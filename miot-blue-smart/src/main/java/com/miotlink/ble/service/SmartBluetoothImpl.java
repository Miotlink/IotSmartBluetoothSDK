package com.miotlink.ble.service;

import android.Manifest;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.bluetooth.sdk.R;
import com.miotlink.MiotSmartBluetoothSDK;
import com.miotlink.ble.Ble;
import com.miotlink.ble.BleLog;
import com.miotlink.ble.callback.BleStatusCallback;
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
import com.miotlink.ble.utils.AesEncryptUtil;
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

public class SmartBluetoothImpl implements ISmart, SmartBleConnectImpl.NofityCallback {
    private static final String TAG = SmartBluetoothImpl.class.getName();
    private Ble<BleModelDevice> ble = null;
    private SmartListener mSmartListener = null;
    private Context mContext = null;
    public static boolean Debug = false;
    private boolean isOpen;
    private ILinkBlueScanCallBack mILinkBlueScanCallBack = null;
    private ILinkSmartConfigListener mILinkSmartConfigListener = null;
    private BluetoothDeviceStore bluetoothDeviceStore = new BluetoothDeviceStore();
    private List<SmartNotifyListener> notifyListeners = new ArrayList<>();
    private BleSmartConfigImpl smartConfig = null;
    private AwsSmartConfigImpl awsSmartConfig = null;
    private SmartBleConnectImpl bleConnect = null;
    private String macCode = "";
    private SmartNotifyUartDataListener notifyUartDataListener;
    private SmartNotifyOTAListener smartNotifyOTAListener = null;
    private ILinkConnectCallback mLinkConnectCallback = null;
    private SmartNotifyDeviceConnectListener smartNotifyDeviceConnectListener = null;

    public SmartBluetoothImpl() {

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
                .setScanPeriod(24 * 60 * 60 * 1000)//设置扫描时长
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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Utils.isGpsOpen(mContext)) {
            return 4;
        } else if (!Utils.isPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) &&
                !Utils.isPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
            return 5;
        }
        return 1;
    }

    @Override
    public void setDeviceInfo(boolean isOpen) {
        BleLog.e(TAG, "setDeviceInfo:" + isOpen);
        this.isOpen = isOpen;
    }

    @Override
    public void setServiceUUID(String serviceUuId, String readUuid, String writeUuid) throws Exception {
        Ble.options().setUuidService(UUID.fromString(UuidUtils.uuid16To128(serviceUuId)))//设置主服务的uuid
                .setUuidWriteCha(UUID.fromString(UuidUtils.uuid16To128(writeUuid)))//设置可写特征的uuid
                .setUuidReadCha(UUID.fromString(UuidUtils.uuid16To128(readUuid)))//设置可读特征的uuid （选填）
                .setUuidNotifyCha(UUID.fromString(UuidUtils.uuid16To128(readUuid)));//设置可通知特征的uuid （
    }

    @Override
    public void onScan(final ILinkBlueScanCallBack iLinkBlueScanCallBack) throws Exception {
        if (ble == null) {
            ble = Ble.getInstance();
        }
        if (!ble.isSupportBle(mContext)) {
            throw new Exception("该手机暂不支持蓝牙设备");
        }
        if (!ble.isBleEnable()) {
            throw new Exception("蓝牙访问权限未打开");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Utils.isGpsOpen(mContext)) {
            throw new Exception("Android 操作系统8.1以上未打开定位服务");
        } else if (!Utils.isPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) &&
                !Utils.isPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)) {
            throw new Exception("Android 操作系统8.0以上未打开定位权限");
        }

        this.mILinkBlueScanCallBack = iLinkBlueScanCallBack;
        final BleScanDeviceImpl scanCallback = new BleScanDeviceImpl();
        scanCallback.setmILinkBlueScanCallBack(this.mILinkBlueScanCallBack);
        ble.setBleStatusCallback(new BleStatusCallback() {
            @Override
            public void onBluetoothStatusChanged(boolean isOn) {
                if (isOn) {
                    ble.startScan(scanCallback);
                } else {
                    ble.stopScan();
                }
            }
        });
        if (!ble.isBleEnable()) {
            return;
        }

        ble.startScan(scanCallback);
    }


    @Override
    public void sendUartData(String mac, String data, SmartNotifyListener smartNotifyListener) throws Exception {

        if (bleConnect != null) {
            bleConnect.sendUart(mac, data);
        }


    }

    @Override
    public void sendUartData(String mac, byte[] data) throws Exception {

        if (bleConnect != null) {
            bleConnect.sendUart(mac, data);
        }
    }

    @Override
    public void disConnectAll() throws Exception {
        if (ble.getConnectedDevices() != null) {
            ble.disconnectAll();
        }
    }


    @Override
    public void deleteDevice(String macCode) {

    }

    @Override
    public void openBluetooth() {

    }

    @Override
    public void onScanStop() throws Exception {
        ble.stopScan();
    }

    @Override
    public void onConnect(String macCode, ILinkConnectCallback mLinkConnectCallback) throws Exception {
        this.macCode = macCode;
        BleModelDevice bleModelDevice = BluetoothDeviceStore.getInstace().getDevice(macCode);
        if (bleModelDevice == null) {
            bleModelDevice = ble.getBleDevice(macCode);
        }
       if (!TextUtils.isEmpty(bleModelDevice.getBleName())&&bleModelDevice.getBleName().startsWith("CMD")){
           Ble.options().setUuidService(UUID.fromString(UuidUtils.uuid16To128("D459")))//设置主服务的uuid
                   .setUuidWriteCha(UUID.fromString(UuidUtils.uuid16To128("0015")))//设置可写特征的uuid
                   .setUuidReadCha(UUID.fromString(UuidUtils.uuid16To128("6602")))
                   .setUuidOtaWriteCha(UUID.fromString(UuidUtils.uuid16To128("6603")))//设置可读特征的uuid （选填）
                   .setUuidNotifyCha(UUID.fromString(UuidUtils.uuid16To128("0014")));//设置可通知特征的uuid （
       }
        if (bleModelDevice == null) {
            throw new Exception(macCode + "  device  is not found");
        }
        if (bleModelDevice != null) {
            bleConnect = new SmartBleConnectImpl();
            bleConnect.setLinkConnectCallback(mLinkConnectCallback);
            bleConnect.setNofityCallback(this);
            bleConnect.setMacCode(macCode);
            ble.connect(bleModelDevice, bleConnect);
        }
    }

    @Override
    public void connect(String macCode) throws Exception {
        this.macCode = macCode;
        BleModelDevice bleModelDevice = BluetoothDeviceStore.getInstace().getDevice(macCode);
        if (bleModelDevice == null) {
            bleModelDevice = ble.getBleDevice(macCode);
        }
        if (bleModelDevice == null) {
            throw new Exception(macCode + "  device  is not found");
        }
        if (bleModelDevice != null) {
            bleConnect = new SmartBleConnectImpl();
            bleConnect.setSmartNotifyDeviceConnectListener(smartNotifyDeviceConnectListener);
            bleConnect.setNofityCallback(this);
            bleConnect.setMacCode(macCode);
            ble.connect(bleModelDevice, bleConnect);
        }
    }

    @Override
    public void setSmartNotifyUartData(SmartNotifyUartDataListener notifyUartData) {
        this.notifyUartDataListener = notifyUartData;
    }

    @Override
    public void setSmartNotifyOnlineOTAListener(SmartNotifyOTAListener smartNotifyOnlineOTAListener) {
        this.smartNotifyOTAListener = smartNotifyOnlineOTAListener;
    }

    @Override
    public void setSmartNotifyDeviceConnectListener(SmartNotifyDeviceConnectListener smartNotifyDeviceConnectListener) {
        this.smartNotifyDeviceConnectListener = smartNotifyDeviceConnectListener;
    }

    @Override
    public void onChanged(String macCode, BleModelDevice device, BluetoothGattCharacteristic characteristic) throws Exception {
        UUID uuid = characteristic.getUuid();
        if (uuid.equals(Ble.options().getUuidReadCha())) {
            byte[] value = characteristic.getValue();
            String timeHex = "";
            String key = "";
            if (value != null) {
                BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
                Map<String, Object> decode = bluetoothProtocol.decode(value);
                if (decode != null && decode.containsKey("code")) {
                    int code = (int) decode.get("code");
                    JSONObject jsonObject = new JSONObject();
                    switch (code) {

                        case 0x06:
                            String values = (String) decode.get("value");
                            if (notifyListeners != null && notifyListeners.size() > 0) {
                                for (SmartNotifyListener notifyListener : notifyListeners) {
                                    notifyListener.notifyUartData(device.getBleAddress(), values);
                                }
                            }
                            break;
                        case 0x09:
                            if (notifyListeners != null && notifyListeners.size() > 0) {
                                for (SmartNotifyListener notifyListener : notifyListeners) {
                                    long valuel = (long) decode.get("value");
                                    notifyListener.notifyVersion(device.getBleAddress(), valuel);
                                }
                            }
                            break;

                        case 0x19:
                            if (decode.containsKey("time")) {
                                timeHex = decode.get("time").toString();

                            }
                            if (!TextUtils.isEmpty(macCode)) {
                                key = macCode.replaceAll(":", "").toUpperCase();
                            }
                            key += timeHex.toUpperCase();
                            String s = AesEncryptUtil.desEncrypt(key, (byte[]) decode.get("byte"));
                            if (notifyUartDataListener != null) {
                                notifyUartDataListener.notifyDeviceWifiInfo(macCode, s);
                            }
                            break;
                        case 0x15:
                            if (decode.containsKey("time")) {
                                timeHex = decode.get("time").toString();
                            }
                            if (!TextUtils.isEmpty(macCode)) {
                                key = macCode.replaceAll(":", "").toUpperCase();
                            }
                            key += timeHex.toUpperCase();
                            byte[] bytes = AesEncryptUtil.decode(key, (byte[]) decode.get("byte"));
                            Log.e("TAG", HexUtil.encodeHexStr(bytes));
                            if (notifyUartDataListener != null) {
                                notifyUartDataListener.notifyUartData(macCode, HexUtil.encodeHexStr(bytes));
                            }
                            break;
                        case 0x17:
                            if (decode.containsKey("time")) {
                                timeHex = decode.get("time").toString();
                            }
                            if (!TextUtils.isEmpty(macCode)) {
                                key = macCode.replaceAll(":", "").toUpperCase();
                            }
                            key += timeHex.toUpperCase();
                            String data = AesEncryptUtil.desEncrypt(key, (byte[]) decode.get("byte"));
                            if (smartNotifyOTAListener != null) {
                                smartNotifyOTAListener.notifyOtaData(data);
                            }
                            break;
                        case 0x0d:
                            jsonObject.put("code","product");
                            jsonObject.put("data",decode);
                            if (notifyListeners!=null&&notifyListeners.size()>0) {
                                for (SmartNotifyListener notifyListener : notifyListeners) {
                                    notifyListener.notifyProductInfo(device.getBleAddress(), jsonObject.toJSONString());
                                }
                            }
                            break;
                        case 0x0e:
                            jsonObject.put("code", "sn_cmei");
                            jsonObject.put("data", decode);
                            if (notifyListeners != null && notifyListeners.size() > 0) {
                                for (SmartNotifyListener notifyListener : notifyListeners) {
                                    notifyListener.notifyProductInfo(device.getBleAddress(), jsonObject.toJSONString());
                                }
                            }
                            break;
                        case 0x0f:
                            jsonObject.put("code", "factory");
                            jsonObject.put("data", decode);
                            if (notifyListeners != null && notifyListeners.size() > 0) {
                                for (SmartNotifyListener notifyListener : notifyListeners) {
                                    notifyListener.notifyProductInfo(device.getBleAddress(), jsonObject.toJSONString());
                                }
                            }
                            break;
                        case 0x23:
                            jsonObject.put("code", "mac");
                            jsonObject.put("data", decode);
                            if (notifyListeners != null && notifyListeners.size() > 0) {
                                for (SmartNotifyListener notifyListener : notifyListeners) {
                                    notifyListener.notifyProductInfo(device.getBleAddress(), jsonObject.toJSONString());
                                }
                            }
                            break;
                        case 0x20:
                        case 0x21:
                        case 0x22:
                        case 0x24:
                        case 0x25:
                            if (notifyListeners != null && notifyListeners.size() > 0) {
                                for (SmartNotifyListener notifyListener : notifyListeners) {
                                    notifyListener.notifySetState(code, device.getBleAddress(), (int) decode.get("value"));
                                }
                            }
                            break;
                        case 0x26:
                            jsonObject.put("code", "productTest");
                            jsonObject.put("data", decode);
                            if (notifyListeners != null && notifyListeners.size() > 0) {
                                for (SmartNotifyListener notifyListener : notifyListeners) {
                                    notifyListener.notifyProductInfo(device.getBleAddress(), jsonObject.toJSONString());
                                }
                            }
                            break;
                    }
                }
            }
        }

    }


    @Override
    public void setNotifyListener(SmartNotifyListener notifyListener) {
        if (notifyListeners != null && !notifyListeners.contains(notifyListener)) {
            notifyListeners.add(notifyListener);
        }
    }

    @Override
    public void unRegirster(SmartNotifyListener notifyListener) {
        if (notifyListeners != null && notifyListeners.contains(notifyListener)) {
            notifyListeners.remove(notifyListener);
        }
    }

    @Override
    public void onStartSmartConfig(String macCode, String ssid, String password,
                                   int delayMillis, ILinkSmartConfigListener mILinkSmartConfigListener) throws Exception {
        BleModelDevice bleModelDevice = BluetoothDeviceStore.getInstace().getDevice(macCode);
        if (bleModelDevice == null) {
            bleModelDevice = ble.getBleDevice(macCode);
        }
        if (bleModelDevice == null) {
            throw new Exception(macCode + "  device  is not found");
        }
        BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
        byte[] bytes = null;
        if (TextUtils.equals("0", ssid) &&
                TextUtils.equals("0", password)) {
            bytes = bluetoothProtocol.bleSmartConfig();
        } else {
            bytes = bluetoothProtocol.SmartConfigEncode(ssid, password);
        }
        smartConfig = new BleSmartConfigImpl(ble, macCode, bytes, delayMillis);
        smartConfig.setSmartConfigListener(mILinkSmartConfigListener);
        BleModelDevice connectDevice = BluetoothDeviceStore.getInstace().getConnectDevice(macCode);
        if (connectDevice != null) {
            smartConfig.setSmartData(connectDevice);
        }
        ble.connect(bleModelDevice, smartConfig);

    }

    @Override
    public void onAwsStartSmartConfig(String macCode, String awsNetworkInfo,
                                      int delayMillis, ILinkSmartConfigListener mILinkSmartConfigListener1) throws Exception {
        if (ble == null) {
            ble = Ble.getInstance();
        }
        if (delayMillis < 60) {
            delayMillis = 60;
        }
        this.macCode = macCode;

        this.mILinkSmartConfigListener = mILinkSmartConfigListener1;
        BleModelDevice bleModelDevice = BluetoothDeviceStore.getInstace().getDevice(macCode);
        if (bleModelDevice == null) {
            bleModelDevice = ble.getBleDevice(macCode);
        }
        if (bleModelDevice == null) {
            throw new Exception(macCode + "  device  is not found");
        }
        awsSmartConfig = new AwsSmartConfigImpl(ble, macCode, awsNetworkInfo, delayMillis);
        awsSmartConfig.setSmartConfigListener(mILinkSmartConfigListener);

        BleModelDevice connectDevice = BluetoothDeviceStore.getInstace().getConnectDevice(macCode);
        if (connectDevice != null) {
            awsSmartConfig.setSmartData(connectDevice);
            return;
        }
        ble.connect(bleModelDevice, awsSmartConfig);
    }

    @Override
    public void onStopSmartConfig(String macCode) {
        if (smartConfig != null) {
            smartConfig.onDestory();
        }
        if (awsSmartConfig != null) {
            awsSmartConfig.onDestory();
        }
        BleModelDevice connectDevice = BluetoothDeviceStore.getInstace().getConnectDevice(macCode);
        if (connectDevice != null) {
            ble.disconnect(connectDevice);
            BluetoothDeviceStore.getInstace().removeConnectDevice(macCode);
        }

    }

    @Override
    public void getBleDeviceInfo(String macCode) {
        if (bleConnect != null) {
            bleConnect.getBleDeviceInfo(macCode);
        }
    }

    @Override
    public void startOtaOnline(String macCode, String data) throws Exception {
        if (bleConnect != null) {
            bleConnect.startOta(macCode, data);
        }
    }

    @Override
    public void sendUart(String mac, byte[] data) throws Exception {

        if (bleConnect != null) {
            bleConnect.send(mac, data);
        }
    }

    @Override
    public void sendUart(String mac, String data) throws Exception {
        if (bleConnect != null) {
            bleConnect.sendUart(mac, data);
        }
    }

    @Override
    public void onDisConnect(String macCode) throws Exception {
        BleModelDevice connectDevice = BluetoothDeviceStore.getInstace().getConnectDevice(macCode);
        if (connectDevice != null) {
            ble.disconnect(connectDevice);
            BluetoothDeviceStore.getInstace().removeDevice(macCode);
        }
    }


    @Override
    public void onDestory() throws Exception {
        if (ble != null) {
            ble.released();
            BluetoothDeviceStore.getInstace().clear();
        }
    }

    @Override
    public void getDeviceVersion(String macCode) throws Exception {

    }

    @Override
    public void startOtaFile(String macCode, File file) throws Exception {

    }

    @Override
    public boolean startOta(String macCode, File file, OTAListener otaListener) throws
            Exception {
        return false;
    }

    @Override
    public void stopOta(String macCode) throws Exception {

    }

    @Override
    public void unBindPu(String macCode, int kindId, int modelId) throws Exception {

    }

    @Override
    public void getProductInfo(String macCode, int code) throws Exception {
        if (bleConnect != null) {
            BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
            byte[] bytes = bluetoothProtocol.getProductInfos(code);
            bleConnect.sendUart(macCode, bytes);
        }

    }

    @Override
    public void setProductInfo(String macCode, int code, String... message) throws Exception {
        if (bleConnect != null) {
            BluetoothProtocol bluetoothProtocol = new BluetoothProtocolImpl();
            byte[] bytes = bluetoothProtocol.setProductInfos(code, message);
            bleConnect.sendUart(macCode, bytes);
        }
    }

}
