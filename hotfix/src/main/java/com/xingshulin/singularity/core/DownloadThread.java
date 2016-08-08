package com.xingshulin.singularity.core;

import android.content.Context;
import android.util.Log;
import com.xingshulin.singularity.utils.Configs;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URL;

import static com.xingshulin.singularity.core.Patch.TAG;
import static com.xingshulin.singularity.utils.Configs.*;
import static com.xingshulin.singularity.utils.ContextUtils.appVersionCode;
import static com.xingshulin.singularity.utils.IOUtils.save;
import static com.xingshulin.singularity.utils.StringUtils.readText;
import static java.lang.String.format;

class DownloadThread extends Thread {
    private Context context;

    DownloadThread(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        String address = format("%s/patches?packageName=%s&appBuild=%s", Patch.DOMAIN, context.getPackageName(), appVersionCode(context));
        Log.i(TAG, "fetching hotfix info " + address);
        String token = getHotfixToken(context);
        JSONObject hotfix = fetchHotfixInfo(address, token);
        Log.d(TAG, "fetching hotfix info response " + hotfix.toString());
        if (isValid(hotfix) && shouldDownload(hotfix)) {
            doDownload(hotfix, token);
        }
    }

    private void doDownload(JSONObject hotfix, String token) {
        try {
            String fileDownloadToken = format("%s/tokens?type=get&key=%s", Patch.DOMAIN, hotfix.getString(Patch.KEY_URI));
            String patchUrl = readText(fileDownloadToken, token);
            Log.d(TAG, "downloading patch file " + patchUrl);
            File patchFile = getPatchFile(context);
            save(new URL(patchUrl).openStream(), patchFile);
            if (validatePatch(patchFile, hotfix.getString(Patch.KEY_SHA))) {
                saveHotfixInfo(context, hotfix);
                Log.i(TAG, "patch file downloaded successfully.");
            } else {
                Log.w(TAG, "patch file does not pass sha1 check");
            }
        } catch (Exception e) {
            Log.w(TAG, e.getMessage(), e);
        }
    }

    private boolean shouldDownload(JSONObject hotfix) {
        JSONObject existed = Configs.getHotfixInfo(context);
        try {
            return !hotfix.getString(Patch.KEY_SHA).equalsIgnoreCase(existed.getString(Patch.KEY_SHA));
        } catch (JSONException e) {
            Log.w(TAG, e.getMessage(), e);
        }
        return true;
    }

    private boolean isValid(JSONObject hotfix) {
        return hotfix.has(Patch.KEY_URI) && hotfix.has(Patch.KEY_SHA);
    }

    private JSONObject fetchHotfixInfo(String address, String token) {
        try {
            JSONArray array = new JSONArray(readText(address, token));
            return array.length() > 0 ? array.getJSONObject(0) : new JSONObject();
        } catch (JSONException e) {
            Log.w(TAG, e.getMessage(), e);
        }
        return new JSONObject();
    }

}
