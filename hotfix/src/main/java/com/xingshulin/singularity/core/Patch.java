package com.xingshulin.singularity.core;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;
import android.util.Log;
import com.xingshulin.singularity.utils.Configs;
import dalvik.system.DexClassLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.xingshulin.singularity.utils.ArrayUtils.concat;
import static com.xingshulin.singularity.utils.Configs.*;
import static com.xingshulin.singularity.utils.IOUtils.closeQuietly;
import static com.xingshulin.singularity.utils.IOUtils.save;
import static com.xingshulin.singularity.utils.ReflectionUtils.*;

public class Patch {
    public static final String TAG = "hotfix";
    static final String DOMAIN = "http://singularity.xingshulin.com";
    static final String KEY_URI = "uri";
    public static final String KEY_SHA = "sha1";

    public static void checkForUpdates(Context context) {
        if (isOnMainProcess(context)) {
            new DownloadThread(context).start();
        }
    }

    public static void configure(Context context, String token) {
        init(context, token);
        try {
            applyBasePatch(context);
            discoverAndApply(context);
        } catch (Exception e) {
            Log.w(TAG, "Configure failed, found error when applying patch: " + e.getMessage(), e);
        }
    }

    private static void applyBasePatch(Context context) {
        File apk = copyHelperApk(context, Configs.getDefaultPatchDir(context));
        loadPatch(apk.getAbsolutePath(), Configs.getDefaultPatchOptDir(context).getAbsolutePath());
    }

    private static void discoverAndApply(Context context) {
        File patchFile = getPatchFile(context);
        if (!patchFile.exists()) {
            return;
        }
        if (validatePatch(patchFile, getHotfixSha(context))) {
            apply(patchFile);
        } else {
            resetHotfixInfo(context);
            patchFile.delete();
        }
    }

    private static void apply(File patchFile) {
        loadPatch(patchFile.getAbsolutePath(), Configs.getPatchOptDir(patchFile).getAbsolutePath());
    }

    private static void loadPatch(String dexPath, String dexOpt) {
        Log.d(TAG, "loading patch file " + dexPath);
        DexClassLoader dexClassLoader = new DexClassLoader(dexPath, dexOpt, dexPath, getPathClassLoader());
        Object newDexElements = getDexElements(getPathList(dexClassLoader));
        Object baseDexElements = getDexElements(getPathList(getPathClassLoader()));
        Object concat = concat(newDexElements, baseDexElements);// Order is important
        Object pathList = getPathList(getPathClassLoader());
        setField(pathList, pathList.getClass(), "dexElements", concat);
    }

    private static ClassLoader getPathClassLoader() {
        return Patch.class.getClassLoader();
    }

    private static File copyHelperApk(Context context, File patchDir) {
        String apk = "patch_helper.apk";
        File copyTo = new File(patchDir, apk);
        if (copyTo.exists()) {
            return copyTo;
        }

        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(apk);
            save(inputStream, copyTo);
            return copyTo;
        } catch (IOException e) {
            Log.w(TAG, e.getMessage(), e);
            return null;
        } finally {
            closeQuietly(inputStream);
        }
    }

    private static boolean isOnMainProcess(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
        if (infos == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo info : infos) {
            if (info.pid == Process.myPid()) {
                return context.getPackageName().equalsIgnoreCase(info.processName);
            }
        }
        return false;
    }

}
