package com.example.facedoor;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.example.facedoor.db.DBManage;
import com.iflytek.cloud.SpeechUtility;


public class MyApp extends Application {

    public static final String adminId = "admin1";
    public static final String DBIP_KEY = "dbip";
    public static final String DOORIP_KEY = "doorip";
    public static final String DOOR_CONTROLLER = "doorcontroller";
    public static final String OPEN_TIME = "opentime";
    public static final String PLATFORM_IP = "platformip";
    public static final String DOOR_NUM = "doornum";
    public static final String DB_AGENT = "dbagent";
    public static final String VOICE_VALUE = "voiceValue";
    public static final String CONFIG = "config";
    public static final String BAIDU_TOKEN = "baidutoken";
    private static MyApp instance;
    public static DBManage db;
    Intent serviceIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        //得到一个该application的实例
        instance = this;
        //语音初始化
        SpeechUtility.createUtility(this, "appid=" + getString(R.string.appid));
        //启动dds服务
        /*serviceIntent = new Intent(this, DDSService.class);
        serviceIntent.setAction("start");
        startService(serviceIntent);*/
    }

    public MyApp getApplication() {
        return instance;
    }

    public static DBManage getDBManage(Context context) {
        if (db == null) {
            db = new DBManage(context);
        }
        return db;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        db.closeDB();
        //stopService(serviceIntent);
        System.exit(1);
    }
}
