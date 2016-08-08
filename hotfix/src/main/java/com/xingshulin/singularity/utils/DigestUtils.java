package com.xingshulin.singularity.utils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.xingshulin.singularity.utils.IOUtils.closeQuietly;

class DigestUtils {
    private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static char[] encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS_LOWER[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS_LOWER[0x0F & data[i]];
        }
        return out;
    }

    static String shaHex(byte[] data) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA");
            return new String(encodeHex(sha1.digest(data)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    static String shaHex(File patchFile) {
        ByteArrayOutputStream os = null;
        BufferedInputStream is = null;
        try {
            os = new ByteArrayOutputStream();
            is = new BufferedInputStream(new FileInputStream(patchFile));
            int length;
            byte[] buffer = new byte[1024];
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            os.flush();
            return shaHex(os.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }
    }
}
