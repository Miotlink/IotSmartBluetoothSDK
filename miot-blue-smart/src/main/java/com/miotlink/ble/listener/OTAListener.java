package com.miotlink.ble.listener;

public interface OTAListener {



    /**
     *
     * @param macCode
     * @param version
     * @throws Exception
     */
    public void checkOtaStateListener(String macCode, int version)throws Exception;

    public void firmwareOtaListener(String macCode, int version)throws Exception;

    public void progress(String macCode, int porgress)throws Exception;
}
