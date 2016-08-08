package com.xingshulin.singularity.utils;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.xingshulin.singularity.core.Patch.TAG;

public class StringUtils {

    public static String readText(String address, String token) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setRequestProperty("Authorization", "Bearer " + token);
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
