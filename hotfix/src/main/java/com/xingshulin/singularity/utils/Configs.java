package com.xingshulin.singularity.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.xingshulin.singularity.core.Patch;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.xingshulin.singularity.core.Patch.TAG;
import static com.xingshulin.singularity.utils.ContextUtils.appVersionCode;
import static com.xingshulin.singularity.utils.DigestUtils.shaHex;
import static com.xingshulin.singularity.utils.IOUtils.readAllLines;
import static com.xingshulin.singularity.utils.IOUtils.save;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;

public class Configs {

    private static final String KEY_HOTFIX = "hotfix";
    private static final String PATCH_JAR = "patch.jar";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PATCH = "patch";
    private static final String KEY_DEVICE_ID = "device.id";
    private static String deviceId = null;

    public static void saveHotfixInfo(Context context, JSONObject hotfix) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_HOTFIX, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_PATCH, hotfix.toString()).apply();
    }

    public static String getHotfixToken(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_HOTFIX, Context.MODE_PRIVATE);
        return preferences.getString(KEY_TOKEN, "");
    }

    public static File getPatchFile(Context context) {
        return new File(format("%s/%s/", getPatchRootDir(context).getAbsolutePath(), appVersionCode(context)), PATCH_JAR);
    }

    public static void init(Context context, String token) {
        saveHotfixToken(context, token);
        ensureDirs(context);
    }

    public static File getDefaultPatchOptDir(Context context) {
        return new File(getDefaultPatchDir(context), "dexOpt");
    }

    public static File getDefaultPatchDir(Context context) {
        return new File(getPatchRootDir(context), "default");
    }

    public static boolean validatePatch(File patchFile, String sha1) {
        return shaHex(patchFile).equalsIgnoreCase(sha1);
    }

    public static File getPatchOptDir(File patchFile) {
        File dexOpt = new File(patchFile.getParentFile(), "dexOpt");
        dexOpt.mkdirs();
        return dexOpt;
    }

    static String getDeviceId(Context context) {
        if (deviceId != null) {
            return deviceId;
        }
        File deviceIdFile = new File(getPatchRootDir(context), KEY_DEVICE_ID);
        if (!deviceIdFile.exists()) {
            String id = randomUUID().toString();
            save(id, deviceIdFile);
        }
        Configs.deviceId = readAllLines(deviceIdFile);
        return deviceId;
    }

    public static String getHotfixSha(Context context) {
        try {
            JSONObject hotfix = Configs.getHotfixInfo(context);
            return hotfix.getString(Patch.KEY_SHA);
        } catch (JSONException e) {
            Log.w(TAG, e.getMessage(), e);
        }
        return "(none)";
    }

    public static void resetHotfixInfo(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_HOTFIX, Context.MODE_PRIVATE);
        preferences.edit().remove(KEY_PATCH).apply();
    }

    public static boolean localPatchIsValid(Context context) {
        return validatePatch(getPatchFile(context), getHotfixSha(context));
    }

    private static JSONObject getHotfixInfo(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_HOTFIX, Context.MODE_PRIVATE);
        String json = preferences.getString(KEY_PATCH, "{}");
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            Log.w(TAG, e);
        }
        return new JSONObject();
    }

    private static void saveHotfixToken(Context context, String token) {
        context.getSharedPreferences(KEY_HOTFIX, Context.MODE_PRIVATE)
                .edit().putString(KEY_TOKEN, token).apply();
    }

    private static void ensureDirs(Context context) {
        getPatchRootDir(context).mkdirs();
        getPatchFile(context).getParentFile().mkdirs();
        getDefaultPatchDir(context).mkdirs();
        getDefaultPatchOptDir(context).mkdirs();
    }

    private static File getPatchRootDir(Context context) {
        return new File(format("%s/hotfix/", context.getFilesDir().getAbsolutePath()));
    }
}
