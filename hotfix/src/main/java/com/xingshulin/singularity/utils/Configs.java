package com.xingshulin.singularity.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.xingshulin.singularity.core.Patch.TAG;
import static com.xingshulin.singularity.utils.ContextUtils.appVersionCode;
import static com.xingshulin.singularity.utils.DigestUtils.shaHex;
import static java.lang.String.format;

public class Configs {

    private static final String KEY_HOTFIX = "hotfix";
    private static final String PATCH_JAR = "patch.jar";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PATCH = "patch";

    public static JSONObject getHotfixInfo(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_HOTFIX, Context.MODE_PRIVATE);
        String json = preferences.getString(KEY_PATCH, "{}");
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            Log.w(TAG, e);
        }
        return new JSONObject();
    }

    public static void saveHotfixInfo(Context context, JSONObject hotfix) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_HOTFIX, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_PATCH, hotfix.toString()).apply();
    }

    public static String getHotfixToken(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(KEY_HOTFIX, Context.MODE_PRIVATE);
        return preferences.getString(KEY_TOKEN, "");
    }

    public static void saveHotfixToken(Context context, String token) {
        context.getSharedPreferences(KEY_HOTFIX, Context.MODE_PRIVATE)
                .edit().putString(KEY_TOKEN, token).apply();
    }

    private static void ensureDirs(Context context) {
        getPatchRootDir(context).mkdirs();
        getPatchFile(context).getParentFile().mkdirs();
        getDefaultPatchDir(context).mkdirs();
        getDefaultPatchOptDir(context).mkdirs();
    }

    public static File getPatchRootDir(Context context) {
        return new File(format("%s/hotfix/", context.getFilesDir().getAbsolutePath()));
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
}
