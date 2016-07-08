package com.xingshulin.sample;

import android.app.Application;
import android.content.Context;
import com.xingshulin.singularity.hotfix.PatchManager;

public class SampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PatchManager.checkForUpdates(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        PatchManager.install(this, "HJgoqKj8");
    }
}
