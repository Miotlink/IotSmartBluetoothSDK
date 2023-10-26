package com.miotlink.ble.service;

import android.content.Context;

import com.miotlink.ble.listener.ILinkBlueScanCallBack;
import com.miotlink.ble.listener.ILinkConnectCallback;
import com.miotlink.ble.listener.ILinkSmartConfigListener;
import com.miotlink.ble.listener.OTAListener;
import com.miotlink.ble.listener.SmartListener;
import com.miotlink.ble.listener.SmartNotifyDeviceConnectListener;
import com.miotlink.ble.listener.SmartNotifyListener;
import com.miotlink.ble.listener.SmartNotifyOTAListener;
import com.miotlink.ble.listener.SmartNotifyUartDataListener;
import com.miotlink.ble.model.BleModelDevice;

import java.io.File;

public interface ISmart {

    /**
     * 初始化参数信息
     * @param mContext
     * @throws Exception
     */
    public void init(Context mContext, SmartListener mSmartListener)throws Exception;

    public int checkAuthority();

    public void setDeviceInfo(boolean isOpen);

    public void setServiceUUID(String serviceUuId, String readUuid, String writeUuid)throws Exception;

    /**
     * 发送串口数据
     * @param mac
     * @param data
     * @throws Exception
     */
    public void sendUartData(String mac, String data, SmartNotifyListener smartNotifyListener)throws Exception;


    public void sendUartData(String mac, byte[] data)throws Exception;


    public void  deleteDevice(String macCode);
    /**
     * 扫描妙联蓝牙设备
     * @throws Exception
     */
    public void onScan(ILinkBlueScanCallBack mILinkBlueScanCallBack)throws Exception;


    /**
     * 打开蓝牙
     */
    public void openBluetooth();

    /**
     * 停止扫描蓝牙设备
     * @throws Exception
     */
    public void onScanStop()throws Exception;

    /**
     * 根据MAC地址连接蓝牙
     * @param macCode
     * @throws Exception
     */
    public void onConnect(String macCode, ILinkConnectCallback mLinkConnectCallback)throws Exception;

    public void connect(String macCode)throws Exception;

    public void setSmartNotifyDeviceConnectListener(SmartNotifyDeviceConnectListener smartNotifyDeviceConnectListener);

    public void setSmartNotifyUartData(SmartNotifyUartDataListener notifyUartDataListener);

    public void setSmartNotifyOnlineOTAListener(SmartNotifyOTAListener notifyOnlineOTAListener);

    public void setNotifyListener(SmartNotifyListener notifyListener);

    public void unRegirster(SmartNotifyListener notifyListener);
    /**
     * 开启配网信息
     * @param ssid
     * @param password
     * @throws Exception
     */
    public void onStartSmartConfig(String macCode, String ssid, String password, int delayMillis, ILinkSmartConfigListener mILinkSmartConfigListener)throws Exception;



    public void onAwsStartSmartConfig(String macCode, String awsNetworkInfo, int delayMillis, ILinkSmartConfigListener mILinkSmartConfigListener)throws Exception;


    public void onStopSmartConfig(String macCode);

    public void getBleDeviceInfo(String macCode);

    public void startOtaOnline(String macCode, String data)throws Exception;
    /**
     * 发送串口数据
     * @param mac
     * @param data
     * @throws Exception
     */
    public void sendUart(String mac, byte[] data)throws Exception;

    public void sendUart(String mac, String data)throws Exception;

    public void onDisConnect(String macCode)throws Exception;


    public void onDestory()throws Exception;

    public void disConnectAll()throws Exception;

    public void getDeviceVersion(String macCode)throws Exception;

    public void startOtaFile(String macCode, File file)throws Exception;

    public boolean startOta(String macCode, File file, OTAListener otaListener)throws Exception;

    public void stopOta(String macCode)throws Exception;

    public void unBindPu(String macCode, int kindId, int modelId)throws Exception;


    public void getProductInfo(String macCode,int code)throws Exception;

    public void setProductInfo(String macCode,int code,String ... message)throws Exception;
}
