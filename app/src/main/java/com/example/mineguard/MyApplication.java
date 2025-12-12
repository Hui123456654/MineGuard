package com.example.mineguard;

import android.app.Application;

public class MyApplication extends Application {
      // 定义全局变量
    public static String token;
    public static String globalIP;
    public static String globalIP1;
    public static String globalRtsp;

    @Override
    public void onCreate() {
        super.onCreate();
        // 在这里可以对全局变量进行初始化
//        globalIP1 = "10.34.8.66";
        globalIP = "192.168.31.30";
//        globalRtsp= "rtsp://admin:cs123456@192.168.1.108";
    }

    public static void setGlobalIP(String globalIP) {
        MyApplication.globalIP = globalIP;
    }
    public static void setGlobalIP1(String globalIP1) {
        MyApplication.globalIP1 = globalIP1;
    }
}
