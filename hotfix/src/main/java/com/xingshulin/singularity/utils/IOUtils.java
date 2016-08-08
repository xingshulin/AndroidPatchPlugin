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
            int totalSize = 0;
            int length;
            while ((length = in.read(buffer)) != -1) {
                totalSize += length;
                out.write(buffer, 0, length);
            }
            System.out.println("totalSize = " + totalSize);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(out);
            closeQuietly(in);
        }
    }
}
