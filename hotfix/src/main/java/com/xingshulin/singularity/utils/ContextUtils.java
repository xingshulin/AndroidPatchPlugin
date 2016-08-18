package com.xingshulin.singularity.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import static android.content.pm.PackageManager.GET_CONFIGURATIONS;

public class ContextUtils {

    public static String appVersionCode(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            return String.valueOf(packageManager.getPackageInfo(context.getPackageName(), GET_CONFIGURATIONS).versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String appVersionName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getPackageInfo(context.getPackageName(), GET_CONFIGURATIONS).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }
}
