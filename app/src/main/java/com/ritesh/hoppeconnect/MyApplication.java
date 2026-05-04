package com.ritesh.hoppeconnect;

import android.app.Application;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppwriteService.INSTANCE.init(this);
    }
}