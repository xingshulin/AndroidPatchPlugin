package com.example.samplewithmultidex;

import android.app.Application;
import android.content.Context;

import com.xingshulin.singularity.hotfix.PatchManager;

public class MultiDexApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        PatchManager.install(this, "Sk5koHow");
    }
}
