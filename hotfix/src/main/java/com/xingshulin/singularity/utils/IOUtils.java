package com.xingshulin.singularity.utils;

import java.io.*;

public class IOUtils {
    public static void closeQuietly(Closeable outputStream) {
        if (outputStream == null) {
            return;
        }
        try {
            outputStream.close();
        } catch (IOException ignored) {
        }
    }

    public static String readAllLines(File from) {
        if (!from.exists()) {
            return null;
        }
        ByteArrayOutputStream outputStream = null;
        BufferedInputStream inputStream = null;

        try {
            outputStream = new ByteArrayOutputStream();
            inputStream = new BufferedInputStream(new FileInputStream(from));
            int length;
            byte[] buffer = new byte[1024];
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            return new String(outputStream.toByteArray(), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(outputStream);
            closeQuietly(inputStream);
        }
        return null;
    }

    public static void save(String content, File output) {
        save(new ByteArrayInputStream(content.getBytes()), output);
    }

    public static void save(InputStream inputStream, File patchFile) {
        if (patchFile.exists()) {
            patchFile.delete();
        }
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(inputStream);
            out = new BufferedOutputStream(new FileOutputStream(patchFile));

            byte[] buffer = new byte[2048];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(out);
            closeQuietly(in);
        }
    }
}
