package com.miotlink.ble.utils;

import java.util.ArrayList;
import java.util.List;

public class PacketUtils {

    public static List<byte[]> getPackets(int mtuLength,byte [] data){
        List<byte[]> list=new ArrayList<>();
        int length = data.length;
        int availableLength = length;
        int packLength = mtuLength;
        int index=0;
        while (index < length){
                int onePackLength = packLength;
                onePackLength = (availableLength >= packLength ? packLength : availableLength);
                byte[] txBuffer = new byte[onePackLength];
                for (int i=0; i<onePackLength; i++){
                    if(index < length){
                        txBuffer[i] = data[index++];
                    }
                }
                availableLength-=onePackLength;
                list.add(txBuffer);
        }
        return list;
    }


}
