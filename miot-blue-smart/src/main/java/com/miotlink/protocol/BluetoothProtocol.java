package com.miotlink.protocol;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface BluetoothProtocol {

    /**
     *配网
     * @param ssid
     * @param password
     * @return
     */
    public byte[] SmartConfigEncode(String ssid, String password);

    public List<byte[]> awsSmartConfigEnCode(String mac, int mtu, String awsNetWorkInfo)throws Exception;

    public byte[] HeartbeatEncode();

    public byte[] bleSmartConfig();

    public byte[] getDeviceInfo();

    public byte[] hexEncode(byte[] hex);

    public byte[] getVersion();

    public byte[] unbindPu(int kindId, int modelId);

    public byte[] startOta(File file)throws Exception;

    public byte [] startOtaByte(int index, byte[] bytes)throws Exception;

    public Map<String,Object> decode(byte[] bytes);

    public List<byte[]> getUartData(String macCode, int mtu, String uart)throws Exception;

    public List<byte[]> getUartData(String macCode, int mtu, byte[] uart)throws Exception;

    public List<byte[]> getDeviceInfo(String macCode, int mtu, String uart)throws Exception;

    public List<byte[]> startOta(String macCode, int mtu, String uart)throws Exception;
}
