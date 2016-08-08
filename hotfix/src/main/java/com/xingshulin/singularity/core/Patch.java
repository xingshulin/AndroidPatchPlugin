package com.xingshulin.singularity.core;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.xingshulin.singularity.utils.Configs;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

import static com.xingshulin.singularity.utils.ArrayUtils.concat;
import static com.xingshulin.singularity.utils.Configs.validatePatch;
import static com.xingshulin.singularity.utils.IOUtils.closeQuietly;
import static com.xingshulin.singularity.utils.IOUtils.save;
import static com.xingshulin.singularity.utils.ReflectionUtils.*;

public class Patch {
    public static final String TAG = "hotfix";
    static final String DOMAIN = "http://singularity.xingshulin.com";
    static final String KEY_URI = "uri";
    static final String KEY_SHA = "sha";

    public static void checkForUpdates(Context context) {
        new DownloadThread(context).start();
    }

    public static void configure(Context context, String token) {
        Configs.init(context, token);

        File apk = copyHelperApk(context, Configs.getDefaultPatchDir(context));
        loadPatch(apk.getAbsolutePath(), Configs.getDefaultPatchOptDir(context).getAbsolutePath());

        try {
            FileInputStream fileInputStream = new FileInputStream(apk);
            BufferedInputStream in = new BufferedInputStream(fileInputStream);
            byte[] buffer = new byte[1024];
            File file = new File(Environment.getExternalStorageDirectory(), "demo.apk");
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedOutputStream os = new BufferedOutputStream(fileOutputStream);
            int read;
            while ((read = in.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
            os.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            getPathClassLoader().loadClass("com.xingshulin.singularity.Hack");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void discoverAndApply(Context context) {
        File patchFile = Configs.getPatchFile(context);
        if (!patchFile.exists()) {
            return;
        }
        JSONObject hotfix = Configs.getHotfixInfo(context);
        String sha1 = "";
        try {
            sha1 = hotfix.getString(Patch.KEY_SHA);
        } catch (JSONException e) {
            Log.w(TAG, e.getMessage(), e);
        }
        if (validatePatch(patchFile, sha1)) {
            apply(patchFile);
        } else {
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

    private static PathClassLoader getPathClassLoader() {
        return (PathClassLoader)Patch.class.getClassLoader();
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
}
