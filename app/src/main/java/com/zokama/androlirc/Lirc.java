
package com.zokama.androlirc;

import android.support.v7.app.AppCompatActivity;

import java.io.File;


public class Lirc extends AppCompatActivity {
    public static String POWER_TOGGLE = "Function18";

    static {
        System.loadLibrary("androlirc");
    }

    public native int parse(String filename);
    public native byte[] getIrBuffer(String irDevice, String irCode, int minBufSize);
    public native String[] getDeviceList();
    public native String[] getCommandList(String irDevice);

    public Lirc (){

        File dir = new File("/data/data/com.example.mugeshm.oneremote/log");
        dir.mkdirs();
    }

}
