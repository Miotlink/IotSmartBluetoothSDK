package com.miotlink.ble.listener;

public interface ILinkSmartConfigListener {

    /**
     * 7001 代表未配网
     * 7002 代表配网中
     * 7010  代表设备与手机断开连接
     * 7003 代表已连上路由器
     * 7015 代表 配网成功
     * 7255 代表配网失败
     * 7011 代表设备配网超时
     * @param error
     * @param errorMessage
     * @param data
     * @throws Exception
     */
    public void onLinkSmartConfigListener(int error, String errorMessage, String data)throws Exception;


    public void onLinkSmartConfigTimeOut(int errorCode, String errorMessage)throws Exception;
}
