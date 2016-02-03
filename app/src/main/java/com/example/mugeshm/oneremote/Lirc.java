package com.example.mugeshm.oneremote;

import java.io.File;

/**
 * Created by Mugesh M on 27-01-2016.
 */
public class Lirc {
   // public static String POWER_TOGGLE = "Function18";

    static {
        System.loadLibrary("oneremote");
    }
    native int parse(String filename);
    native byte[] getIrBuffer(String irDevice, String irCode, int minBufSize);
    native String[] getDeviceList();
    native String[] getCommandList(String irDevice);

    Lirc (){

        File dir = new File("/data/data/com.example.mugeshm.oneremote/log");
        dir.mkdirs();
    }
}
