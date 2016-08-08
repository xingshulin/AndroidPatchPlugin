package com.example.samplewithmultidex;

import android.content.Context;
import android.support.multidex.MultiDexApplication;
import com.xingshulin.singularity.hotfix.PatchManager;

public class SampleMultiDexApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        PatchManager.checkForUpdates(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        PatchManager.install(this, "Sk5koHow");
    }
}
