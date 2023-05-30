package com.miotlink.protocol;

import android.text.TextUtils;
import android.util.Log;

import com.miotlink.ble.utils.AesEncryptUtil;
import com.miotlink.ble.utils.ByteUtils;
import com.miotlink.ble.utils.PacketUtils;
import com.miotlink.utils.BlueTools;
import com.miotlink.utils.BluetoothConsts;
import com.miotlink.utils.HexUtil;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

public class BluetoothProtocolImpl implements BluetoothProtocol {


    @Override
    public byte[] SmartConfigEncode(String ssid, String password) {
        BluetoothMessage bluetoothMessage=new BluetoothMessage();
        BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(3, 2);
        if (blueMessageBody!=null){
            if (!TextUtils.isEmpty(ssid)){
                blueMessageBody.addPropertys(ssid.getBytes().length, ssid);
            }else {
                blueMessageBody.addPropertys(0, 0x00);
            }
            if (!TextUtils.isEmpty(password)){
                blueMessageBody.addPropertys(password.getBytes().length, password);
            }else {
                blueMessageBody.addPropertys(0, 0x00);
            }
            bluetoothMessage.encode();
            return bluetoothMessage.getmBytes();
        }

        return new byte[0];
    }

    @Override
    public List<byte[]> awsSmartConfigEnCode(String mac,int mtu, String awsNetWorkInfo)throws Exception {
        String key="";
        List<byte[]> list=new ArrayList<>();
        int timeId=(int)System.currentTimeMillis() %65536;


        byte[] buff = BlueTools.Int16ToBytes(timeId);


        String timeIdHex = HexUtil.encodeHexStr(buff);

        if (!TextUtils.isEmpty(mac)){
            key=mac.replaceAll(":", "").toUpperCase();
        }
        key+=timeIdHex.toUpperCase();
        byte[] encrypt = AesEncryptUtil.encrypt(key,awsNetWorkInfo);
        List<byte[]> networkInfos= PacketUtils.getPackets(mtu-12, encrypt);
        if (networkInfos!=null){
            if (networkInfos.size()==1){
                byte[] bytes = networkInfos.get(0);
                BluetoothMessage bluetoothMessage=new BluetoothMessage();
                BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(0x12, 0);
                if (blueMessageBody!=null){
                    blueMessageBody.addPropertys(bytes.length,bytes);
                    bluetoothMessage.encode((int)timeId);
                    list.add( bluetoothMessage.getmBytes());
                    return list;
                }
            }
            if (networkInfos.size()>1){
                for (int i=0;i<networkInfos.size();i++){
                    BluetoothMessage bluetoothMessage=new BluetoothMessage();
                    BluetoothMessage.BlueMessageBody blueMessageBody=null;
                    byte[] bytes = networkInfos.get(i);
                    if (i==0){
                       blueMessageBody = bluetoothMessage.getBlueMessageBody(0x12, 0x01);
                    }else if (i>=networkInfos.size()-1){
                       blueMessageBody = bluetoothMessage.getBlueMessageBody(0x12, 0x11);
                    }else {
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x12, 0x10);
                    }
                    blueMessageBody.addPropertys(bytes.length,bytes);
                    bluetoothMessage.encode((int)timeId);
                    list.add( bluetoothMessage.getmBytes());

                }
                return list;
            }


        }

        return null;
    }

    @Override
    public byte[] HeartbeatEncode() {
        BluetoothMessage bluetoothMessage=new BluetoothMessage();
        BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(1, 1);
        if (blueMessageBody!=null){
            blueMessageBody.addPropertys(4, (int)System.currentTimeMillis()/1000);
            bluetoothMessage.encode();
            return bluetoothMessage.getmBytes();
        }
        return new byte[0];
    }

    @Override
    public byte[] bleSmartConfig() {
        BluetoothMessage bluetoothMessage=new BluetoothMessage();
        BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(7, 1);
        if (blueMessageBody!=null){
            blueMessageBody.addPropertys(1, new byte[]{0x01});
            bluetoothMessage.encode();
            return bluetoothMessage.getmBytes();
        }
        return new byte[0];
    }

    @Override
    public byte[] getDeviceInfo() {
        BluetoothMessage bluetoothMessage=new BluetoothMessage();
        BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(7, 1);
        blueMessageBody.addPropertys(1, 0x01);
        bluetoothMessage.encode();
        return bluetoothMessage.getmBytes();
    }

    @Override
    public byte[] hexEncode(byte[] hex) {
        BluetoothMessage bluetoothMessage=new BluetoothMessage();
        BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(5, 1);
        blueMessageBody.addPropertys(hex.length, hex);
        bluetoothMessage.encode();
        return bluetoothMessage.getmBytes();
    }

    @Override
    public byte[] getVersion() {

        BluetoothMessage bluetoothMessage=new BluetoothMessage();
        BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(9, 1);
        blueMessageBody.addPropertys(4, System.currentTimeMillis()/1000);
        String s = Long.toHexString(System.currentTimeMillis() / 1000);
        bluetoothMessage.encode();
        return bluetoothMessage.getmBytes();
    }

    @Override
    public byte[] unbindPu(int kindId,int modelId) {
        BluetoothMessage bluetoothMessage=new BluetoothMessage();
        BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(0x0c, 1);
        byte [] bytes=new byte[4];
        bytes=BlueTools.Int16ToBytesBE(kindId,bytes,0);
        bytes = BlueTools.Int16ToBytesBE(modelId, bytes, 2);
        blueMessageBody.addPropertys(bytes.length, bytes);
        bluetoothMessage.encode();
        return bluetoothMessage.getmBytes();
    }

    @Override
    public byte[] startOta( File file) throws Exception {
        BluetoothMessage bluetoothMessage=new BluetoothMessage();
        BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(10, 1);

        CRC32 crc32=new CRC32();
        FileInputStream fileinputstream = new FileInputStream(file);
        int available = fileinputstream.available();
        byte[] fileByte = ByteUtils.toByteArray(file);
        crc32.update(fileByte);
        long crcValue = crc32.getValue();
        byte [] bytes=new byte[8];
        bytes=BlueTools.Int32ToBytesUP(available,bytes,0);
        bytes = BlueTools.Long32ToBytesUP(crcValue, bytes, 4);

        blueMessageBody.addPropertys(8, bytes);
        bluetoothMessage.encode();
        return bluetoothMessage.getmBytes();
    }

    @Override
    public byte[] startOtaByte(int index,byte[] files) throws Exception {

        String s = HexUtil.encodeHexStr(files);
        int totalLen=files.length+12;
        int fileLen=files.length+6;
        int startLen=0;
       byte[] start_head={BluetoothConsts.START_HEAD_1,BluetoothConsts.START_HEAD_2};
       byte[] start_end={BluetoothConsts.END_1,BluetoothConsts.END_2};
       byte [] bytes=new byte[totalLen];
       System.arraycopy(start_head,0,bytes, 0, start_head.length);
       startLen+=2;
       bytes = ByteUtils.bytesToInt16BE(fileLen,bytes,startLen);
       startLen+=2;
       bytes = ByteUtils.bytesToInt32BE(index,bytes,startLen);
       startLen+=4;
       System.arraycopy(files,0,bytes, startLen,files.length);
       startLen+=files.length;
       byte [] crcByte=new byte[4+files.length];
       System.arraycopy(bytes,4,crcByte, 0,crcByte.length);
       String crcValue = CRC16Utils.getCRCValue(crcByte);
        bytes[startLen]=(byte)CRC16Utils.getCrcMinLen(crcValue);
        startLen+=1;
        bytes[startLen]=(byte)CRC16Utils.getCrcMaxLen(crcValue);
        startLen+=1;
        System.arraycopy(start_end,0,bytes, startLen, start_end.length);
        return bytes;
    }

    @Override
    public Map<String, Object> decode(byte[] bytes) {
        Map<String, Object> value=new HashMap<>();
        BluetoothMessage bluetoothMessage=new BluetoothMessage();
        BluetoothMessage.BlueMessageBody decode = bluetoothMessage.decode(bytes);
        if (decode!=null){
            try {
                List<Object> propertys = decode.getPropertys(bytes);
                if (propertys!=null){
                    value.put("code", decode.getCode());
                    byte[] bytes1=(byte[]) propertys.get(0);
                    switch (decode.getCode()){
                        case 2:
                            value.put("value",  HexUtil.encodeHexStr(bytes1));
                            value.put("byte",  bytes1);
                            break;
                        case 4:
                            value.put("value",  HexUtil.encodeHexStr(bytes1));
                            value.put("byte",  bytes1);
                            break;
                        case 6:
                            value.put("value", HexUtil.encodeHexStr(bytes1));
                            value.put("byte",  bytes1);
                            break;
                        case 8:
                            value.put("value", HexUtil.encodeHexStr(bytes1));
                            value.put("byte",  bytes1);
                            break;
                        case 0x09:
                            value.put("value", HexUtil.byteToLong(bytes1,0,4,true));
                            value.put("byte",  bytes1);
                            break;
                        case 0x0A:
                        case 0x0B:
                        case 0x0C:
                            value.put("value",(int)bytes1[0]);
                            value.put("byte",  bytes1);
                            break;
                        case 0x13:
                            value.put("value",  HexUtil.encodeHexStr(bytes1));
                            value.put("byte",  bytes1);
                            break;
                        case 0x15:
                            value.put("value",  HexUtil.encodeHexStr(bytes1));
                            value.put("time",  decode.getTimeHex());
                            value.put("byte",  bytes1);
                            break;
                        case 0x17:
                            value.put("value",  HexUtil.encodeHexStr(bytes1));
                            value.put("time",  decode.getTimeHex());
                            value.put("byte",  bytes1);
                            break;
                        case 0x19:
                            value.put("value",  HexUtil.encodeHexStr(bytes1));
                            value.put("time",  decode.getTimeHex());
                            value.put("byte",  bytes1);
                            break;


                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return value;
    }

    @Override
    public List<byte[]> getUartData(String macCode,int mtu,String uart) throws Exception {

        String key="";
        List<byte[]> list=new ArrayList<>();
        int timeId=(int)System.currentTimeMillis() %65536;
        byte[] buff = BlueTools.Int16ToBytes(timeId);
        String timeIdHex = HexUtil.encodeHexStr(buff);
        if (!TextUtils.isEmpty(macCode)){
            key=macCode.replaceAll(":", "").toUpperCase();
        }
        key+=timeIdHex.toUpperCase();
        byte[] encrypt = AesEncryptUtil.encrypt(key,HexUtil.decodeHex(uart));
        List<byte[]> networkInfos= PacketUtils.getPackets(mtu-12, encrypt);
        if (networkInfos!=null){
            if (networkInfos.size()==1){
                byte[] bytes = networkInfos.get(0);
                BluetoothMessage bluetoothMessage=new BluetoothMessage();
                BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(0x14, 0);
                if (blueMessageBody!=null){
                    blueMessageBody.addPropertys(bytes.length,bytes);
                    bluetoothMessage.encode((int)timeId);
                    list.add( bluetoothMessage.getmBytes());
                    return list;
                }
            }
            if (networkInfos.size()>2){
                for (int i=0;i<networkInfos.size();i++){
                    BluetoothMessage bluetoothMessage=new BluetoothMessage();
                    BluetoothMessage.BlueMessageBody blueMessageBody=null;
                    byte[] bytes = networkInfos.get(i);
                    if (i==0){
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x14, 0x01);
                    }else if (i>=networkInfos.size()-1){
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x14, 0x11);
                    }else {
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x14, 0x10);
                    }
                    blueMessageBody.addPropertys(bytes.length,bytes);
                    bluetoothMessage.encode((int)timeId);
                    list.add( bluetoothMessage.getmBytes());

                }
                return list;
            }


        }
        return null;
    }
    @Override
    public List<byte[]> getUartData(String macCode,int mtu,byte[] uart) throws Exception {

        String key="";
        List<byte[]> list=new ArrayList<>();
        int timeId=(int)System.currentTimeMillis() %65536;
        byte[] buff = BlueTools.Int16ToBytes(timeId);
        String timeIdHex = HexUtil.encodeHexStr(buff);
        if (!TextUtils.isEmpty(macCode)){
            key=macCode.replaceAll(":", "").toUpperCase();
        }
        key+=timeIdHex.toUpperCase();
        byte[] encrypt = AesEncryptUtil.encrypt(key,uart);
        List<byte[]> networkInfos= PacketUtils.getPackets(mtu-12, encrypt);
        if (networkInfos!=null){
            if (networkInfos.size()==1){
                byte[] bytes = networkInfos.get(0);
                BluetoothMessage bluetoothMessage=new BluetoothMessage();
                BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(0x14, 0);
                if (blueMessageBody!=null){
                    blueMessageBody.addPropertys(bytes.length,bytes);
                    bluetoothMessage.encode((int)timeId);
                    list.add( bluetoothMessage.getmBytes());
                    return list;
                }
            }
            if (networkInfos.size()>2){
                for (int i=0;i<networkInfos.size();i++){
                    BluetoothMessage bluetoothMessage=new BluetoothMessage();
                    BluetoothMessage.BlueMessageBody blueMessageBody=null;
                    byte[] bytes = networkInfos.get(i);
                    if (i==0){
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x14, 0x01);
                    }else if (i>=networkInfos.size()-1){
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x14, 0x11);
                    }else {
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x14, 0x10);
                    }
                    blueMessageBody.addPropertys(bytes.length,bytes);
                    bluetoothMessage.encode((int)timeId);
                    list.add( bluetoothMessage.getmBytes());

                }
                return list;
            }


        }
        return null;
    }

    @Override
    public List<byte[]> getDeviceInfo(String macCode, int mtu, String uart) throws Exception {
        String key="";
        List<byte[]> list=new ArrayList<>();
        int timeId=(int)System.currentTimeMillis() %65536;
        byte[] buff = BlueTools.Int16ToBytes(timeId);
        String timeIdHex = HexUtil.encodeHexStr(buff);

        if (!TextUtils.isEmpty(macCode)){
            key=macCode.replaceAll(":", "").toUpperCase();
        }
        key+=timeIdHex.toUpperCase();
        byte[] encrypt = AesEncryptUtil.encrypt(key,uart);
        List<byte[]> networkInfos= PacketUtils.getPackets(mtu-12, encrypt);
        if (networkInfos!=null){
            if (networkInfos.size()==1){
                byte[] bytes = networkInfos.get(0);
                BluetoothMessage bluetoothMessage=new BluetoothMessage();
                BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(0x18, 0);
                if (blueMessageBody!=null){
                    blueMessageBody.addPropertys(bytes.length,bytes);
                    bluetoothMessage.encode((int)timeId);
                    list.add( bluetoothMessage.getmBytes());
                    return list;
                }
            }
            if (networkInfos.size()>2){
                for (int i=0;i<networkInfos.size();i++){
                    BluetoothMessage bluetoothMessage=new BluetoothMessage();
                    BluetoothMessage.BlueMessageBody blueMessageBody=null;
                    byte[] bytes = networkInfos.get(i);
                    if (i==0){
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x18, 0x01);
                    }else if (i>=networkInfos.size()-1){
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x18, 0x11);
                    }else {
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x18, 0x10);
                    }
                    blueMessageBody.addPropertys(bytes.length,bytes);
                    bluetoothMessage.encode((int)timeId);
                    list.add( bluetoothMessage.getmBytes());

                }
                return list;
            }


        }
        return null;
    }

    @Override
    public List<byte[]> startOta(String macCode, int mtu, String uart) throws Exception {
        String key="";
        List<byte[]> list=new ArrayList<>();
        int timeId=(int)System.currentTimeMillis() %65536;
        byte[] buff = BlueTools.Int16ToBytes(timeId);
        String timeIdHex = HexUtil.encodeHexStr(buff);

        if (!TextUtils.isEmpty(macCode)){
            key=macCode.replaceAll(":", "").toUpperCase();
        }
        key+=timeIdHex.toUpperCase();
        byte[] encrypt = AesEncryptUtil.encrypt(key,uart);
        List<byte[]> networkInfos= PacketUtils.getPackets(mtu-12, encrypt);
        if (networkInfos!=null){
            if (networkInfos.size()==1){
                byte[] bytes = networkInfos.get(0);
                BluetoothMessage bluetoothMessage=new BluetoothMessage();
                BluetoothMessage.BlueMessageBody blueMessageBody = bluetoothMessage.getBlueMessageBody(0x16, 0);
                if (blueMessageBody!=null){
                    blueMessageBody.addPropertys(bytes.length,bytes);
                    bluetoothMessage.encode((int)timeId);
                    list.add( bluetoothMessage.getmBytes());
                    return list;
                }
            }
            if (networkInfos.size()>2){
                for (int i=0;i<networkInfos.size();i++){
                    BluetoothMessage bluetoothMessage=new BluetoothMessage();
                    BluetoothMessage.BlueMessageBody blueMessageBody=null;
                    byte[] bytes = networkInfos.get(i);
                    if (i==0){
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x16, 0x01);
                    }else if (i>=networkInfos.size()-1){
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x16, 0x11);
                    }else {
                        blueMessageBody = bluetoothMessage.getBlueMessageBody(0x16, 0x10);
                    }
                    blueMessageBody.addPropertys(bytes.length,bytes);
                    bluetoothMessage.encode((int)timeId);
                    list.add( bluetoothMessage.getmBytes());

                }
                return list;
            }


        }
        return null;
    }


}
