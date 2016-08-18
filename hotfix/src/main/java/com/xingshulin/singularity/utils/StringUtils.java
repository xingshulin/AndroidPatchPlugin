package com.xingshulin.singularity.utils;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.os.Build.BRAND;
import static android.os.Build.MODEL;
import static android.os.Build.VERSION.RELEASE;
import static android.os.Build.VERSION.SDK_INT;
import static com.xingshulin.singularity.core.Patch.TAG;
import static com.xingshulin.singularity.utils.Configs.getDeviceId;
import static com.xingshulin.singularity.utils.Configs.getHotfixSha;
import static com.xingshulin.singularity.utils.ContextUtils.appVersionCode;
import static com.xingshulin.singularity.utils.ContextUtils.appVersionName;
import static java.lang.String.format;

public class StringUtils {

    public static String readText(Context context, String address, String token) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("x-device-id", getDeviceId(context));
            connection.setRequestProperty("x-device-brand", BRAND);
            connection.setRequestProperty("x-device-model", MODEL);
            connection.setRequestProperty("x-device-os", format("%s(%s)", RELEASE, SDK_INT));
            connection.setRequestProperty("x-app-package", context.getPackageName());
            connection.setRequestProperty("x-app-version", format("%s(%s)", appVersionName(context), appVersionCode(context)));
            connection.setRequestProperty("x-hotfix-sha", getHotfixSha(context));
            connection.setDoInput(true);
            connection.connect();

            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            return new String(outputStream.toByteArray(), "UTF-8");
        } catch (Exception e) {
            Log.w(TAG, e.getMessage(), e);
        }
        return "";
    }
}
