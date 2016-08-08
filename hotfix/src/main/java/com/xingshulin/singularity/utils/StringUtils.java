package com.xingshulin.singularity.utils;

import android.util.Log;
import com.xingshulin.singularity.core.Patch;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

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
            while (inputStream.read(buffer) != -1) {
                outputStream.write(buffer);
            }

            return new String(outputStream.toByteArray(), "UTF-8");
        } catch (Exception e) {
            Log.w(Patch.TAG, e.getMessage(), e);
        }
        return "";
    }
}
