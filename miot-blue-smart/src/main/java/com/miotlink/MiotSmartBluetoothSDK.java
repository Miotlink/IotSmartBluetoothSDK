package com.miotlink;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.miotlink.ble.listener.ILinkBlueScanCallBack;
import com.miotlink.ble.listener.ILinkConnectCallback;
import com.miotlink.ble.listener.ILinkSmartConfigListener;
import com.miotlink.ble.listener.SmartListener;
import com.miotlink.ble.listener.SmartNotifyCMBListener;
import com.miotlink.ble.listener.SmartNotifyDeviceConnectListener;
import com.miotlink.ble.listener.SmartNotifyListener;
import com.miotlink.ble.listener.SmartNotifyOTAListener;
import com.miotlink.ble.listener.SmartNotifyUartDataListener;
import com.miotlink.ble.service.BlueISmartImpl;
import com.miotlink.ble.service.ISmart;
import com.miotlink.ble.service.SmartBluetoothImpl;
import com.miotlink.utils.IBluetooth;

import org.json.JSONObject;

public class MiotSmartBluetoothSDK {


    private static volatile MiotSmartBluetoothSDK instance = null;


    public static synchronized MiotSmartBluetoothSDK getInstance() {

        if (instance == null) {
            synchronized (MiotSmartBluetoothSDK.class) {
                if (instance == null) {
                    instance = new MiotSmartBluetoothSDK();
                }
            }
        }
        return instance;
    }





    private ISmart iSmart=null;

    public void setDeviceInfo(boolean isOpen){

        if (iSmart!=null){
            iSmart.setDeviceInfo(isOpen);
        }
    }

    /**
     * 打印数据
     * @param isDebug
     */
    public static void setDebug(boolean isDebug){
        SmartBluetoothImpl.Debug=isDebug;
        BlueISmartImpl.Debug=isDebug;
    }

    /**
     * 设置UUID  参数
     * @param serverUuid
     * @param writeUuid
     * @param readUuid
     * @param notifyUuid
     */
    public static void setServerUUID(String serverUuid,String writeUuid,String readUuid,String notifyUuid){
        if (!TextUtils.isEmpty(serverUuid)){
            IBluetooth.SERVER_UUID =serverUuid;
        }
        if (!TextUtils.isEmpty(writeUuid)){
            IBluetooth.SERVER_WRITE_UUID=writeUuid;
        }
        if (!TextUtils.isEmpty(readUuid)){
            IBluetooth.SERVER_READ_UUID=readUuid;
        }
        if (!TextUtils.isEmpty(notifyUuid)){
            IBluetooth.SERVER_NOTIFY_UUID=notifyUuid;
        }
    }

    public static void setBleFilterName(String name){
        IBluetooth.FILTER_NAME=name;
    }


    /**
     * 初始化参数
     * @param mContext
     * @param smartListener
     * @throws Exception
     */
    public void init(Context mContext,String appKey,SmartListener smartListener)  {
        try {
            if (iSmart==null){
                iSmart=new SmartBluetoothImpl();
            }
            iSmart.init(mContext, smartListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 扫描蓝牙设备
     * @param scanCallBack
     */
    public void startScan(final ILinkBlueScanCallBack scanCallBack) {
        if (iSmart==null){
            iSmart=new SmartBluetoothImpl();
        }
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    iSmart.onScan(scanCallBack);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String macAddress="";

    /**
     *
     * @param macCode  mac地址
     * @param ssid  路由器账户
     * @param passowrd 路由器密码
     * @param delayMillis 设置超时时间 默认60s
     * @param linkSmartConfigListener
     */

    @Deprecated
    public void startSmartConfig(String macCode, String ssid, String passowrd,int delayMillis,ILinkSmartConfigListener linkSmartConfigListener){
        try {
            if (iSmart==null){
                iSmart=new SmartBluetoothImpl();
            }
            this.macAddress=macCode;
            iSmart.onStartSmartConfig(macCode,ssid,passowrd,delayMillis,linkSmartConfigListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param macCode
     * @param ssid
     * @param passowrd
     * @param delayMillis
     * @param linkSmartConfigListener
     */
    public void smartConfig(String macCode, String ssid, String passowrd,int delayMillis,ILinkSmartConfigListener linkSmartConfigListener){
        try {
            if (iSmart==null){
                iSmart=new SmartBluetoothImpl();
            }
           JSONObject awsNetWorkInfo =new JSONObject();
            awsNetWorkInfo.put("Ssid",ssid);
            awsNetWorkInfo.put("Pwd",passowrd);
            awsNetWorkInfo.put("PF",0);
            awsNetWorkInfo.put("Host","");
            awsNetWorkInfo.put("Port",0);
            awsNetWorkInfo.put("PId","");
            awsNetWorkInfo.put("DId","");
            awsNetWorkInfo.put("BindCode","");
            awsNetWorkInfo.put("Token","");
            awsNetWorkInfo.put("ActUrl","");
            awsNetWorkInfo.put("CertUrl","");
            awsNetWorkInfo.put("BindUrl","");
            awsNetWorkInfo.put("OtaUrl","");
            awsNetWorkInfo.put("Res","");
            JSONObject awsNetWorks=new JSONObject();
            if (awsNetWorkInfo!=null){
                awsNetWorks.put("type", "Wifi");
                awsNetWorks.put("data",awsNetWorkInfo);
            }
            this.macAddress=macCode;
            iSmart.onAwsStartSmartConfig(macCode,awsNetWorks.toString(),delayMillis,linkSmartConfigListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onStopSmartConfig(String macAddress){
        try {
            if (iSmart==null){
                iSmart=new SmartBluetoothImpl();
            }
            iSmart.onStopSmartConfig(macAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检查蓝牙权限
     * @return
     * 2 该手机不支持蓝牙
     */
    public int checkPermissions(){
        if(iSmart!=null){
            return iSmart.checkAuthority();
        }
        return 0;
    }

    /**
     * 强制打开蓝牙开关
     */
    public void turnOnBlueToothNo(){
        if (iSmart!=null){
            iSmart.openBluetooth();
        }

    }

    public void regirster(SmartNotifyCMBListener smartNotifyCMBListener){
        if (iSmart!=null){
            iSmart.setSmartNotifyCMBListener(smartNotifyCMBListener);
        }
    }
    public void unRegirster(SmartNotifyCMBListener smartNotifyCMBListener){
        if (iSmart!=null){
            iSmart.unregirster(smartNotifyCMBListener);
        }
    }


    @Deprecated
    public void onConnect(final String macCode, final ILinkConnectCallback linkConnectCallback){
        if (iSmart!=null){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        iSmart.onConnect(macCode,linkConnectCallback);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        }
    }
    public void connect(final String macCode, final SmartNotifyDeviceConnectListener  smartNotifyDeviceConnectListener){
        if (iSmart!=null){

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        iSmart.setSmartNotifyDeviceConnectListener(smartNotifyDeviceConnectListener);
                        iSmart.connect(macCode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        }
    }





    public void startBluetooth(Activity activity, int requestCode) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, requestCode);
    }


    @Deprecated
    public void send(String macCode,String uartData, SmartNotifyListener smartNotifyListener){
        if (iSmart!=null){
            try {

                iSmart.sendUartData(macCode,uartData,smartNotifyListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendUartData(String macCode,byte[] uartData, SmartNotifyUartDataListener smartNotifyListener){
        if (iSmart!=null){
            try {
                iSmart.setSmartNotifyUartData(smartNotifyListener);
                iSmart.sendUartData(macCode,uartData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void getDeviceInfo(String macCode){
        if (iSmart!=null){
            try {
                iSmart.getBleDeviceInfo(macCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 读取数据通知
     * @param notifyUartDataListener
     */
    public void setSmartNotifyUartDataListener(SmartNotifyUartDataListener notifyUartDataListener){
        if (iSmart!=null){
            iSmart.setSmartNotifyUartData(notifyUartDataListener);
        }
    }

    /**
     * 升级OTA通知
     * @param smartNotifyOTAListener
     */
    public void setSmartNotifyOTAListener(SmartNotifyOTAListener smartNotifyOTAListener){
        if (iSmart!=null){
            iSmart.setSmartNotifyOnlineOTAListener(smartNotifyOTAListener);
        }
    }

    public void regirster(SmartNotifyListener smartNotifyListener){
        if (smartNotifyListener!=null){
            if (iSmart!=null){

                iSmart.setNotifyListener(smartNotifyListener);
            }
        }
    }

    public void unRegirster(SmartNotifyListener smartNotifyListener){
        if (iSmart!=null){
            iSmart.unRegirster(smartNotifyListener);
        }
    }
    /**
     * 在线OTA升级
     * @param macCode 设备MAC地址
     * @param otaType
     * @param deviceId
     * @param token
     * @param otaUrl
     * @param crc32
     */
   public void startOtaOnline(String macCode, int otaType, String deviceId, String token, String otaUrl,int crc32){
        if (iSmart!=null){
            try {
                JSONObject awsNetWorkInfo =new JSONObject();
                awsNetWorkInfo.put("OTP",otaType);
                awsNetWorkInfo.put("DId",deviceId);
                awsNetWorkInfo.put("Token",token);
                awsNetWorkInfo.put("OtaUrl",otaUrl);
                awsNetWorkInfo.put("CRC32",crc32);
                JSONObject awsNetWorks=new JSONObject();
                if (awsNetWorkInfo!=null){
                    awsNetWorks.put("type", "Ota");
                    awsNetWorks.put("data",awsNetWorkInfo);
                }
                iSmart.startOtaOnline(macCode,awsNetWorks.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 停止扫描
     */
    public void onStopScan() {
        if (iSmart!=null){
            try {
                iSmart.onScanStop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取产品信息
     * @param macCode
     */
    public void getProduct(String macCode){
        try {
            if (iSmart!=null){
                iSmart.getProductInfo(macCode,0x0d);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取SN CMEI 信息
     * @param macCode
     */
    public void getSnAndCmei(String macCode){
        try {
            if (iSmart!=null){
                iSmart.getProductInfo(macCode,0x0e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取厂家信息
     * @param macCode
     */
    public void getDeviceFactory(String macCode){
        try {
            if (iSmart!=null){
                iSmart.getProductInfo(macCode,0x0f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取MAC
     * @param macCode
     */
    public void getDeviceMacCode(String macCode){
        try {
            if (iSmart!=null){
                iSmart.getProductInfo(macCode,0x23);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 设置产品信息
     * @param macCode
     * @param type
     * @param pToken
     * @param AToken
     */
    public void setProduct(String macCode,String type,String pToken,String AToken){
        try {
            if (iSmart!=null){
                iSmart.setProductInfo(macCode,0x20,type,pToken,AToken);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置SN \CMEI\
     * @param macCode
     * @param sn
     * @param cmei
     * @param date
     */
    public void setSnAndCmei(String macCode,String sn,String cmei,String date){
        try {
            if (iSmart!=null){
                iSmart.setProductInfo(macCode,0x21,sn,cmei,date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void setFactory(String macCode,String factory,String brand,String model,String powered){
        try {
            if (iSmart!=null){
                iSmart.setProductInfo(macCode,0x22,factory,brand,model,powered);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void setMacCode(String macCode,String mac){
        try {
            if (iSmart!=null){
                iSmart.setProductInfo(macCode,0x24,mac);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testPro(String macCode){
        try {
            if (iSmart!=null){
                iSmart.getProductInfo(macCode,0x24);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean disConnect(String macCode){
        if (iSmart!=null){
            try {
                iSmart.onDisConnect(macCode);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
    public void onDestory() {
        if (iSmart!=null){
            try {
                iSmart.onDestory();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
