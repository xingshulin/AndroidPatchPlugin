package com.xingshulin.singularity.core

import android.content.Context
import android.content.pm.PackageManager.GET_CONFIGURATIONS
import com.xingshulin.singularity.utils.ArrayUtils.concat
import com.xingshulin.singularity.utils.DigestUtils.shaHex
import dalvik.system.DexClassLoader
import org.json.JSONObject
import java.io.File

fun discoverAndApply(context: Context) {
    val patch = File("${context.filesDir}/hotfix/${appVersionCode(context)}/", "patch.jar")
    if (patch.exists() && patch.isValidPatchFile(context)) {
        apply(patch)
    }
}

fun apply(patch: File) {
    val dexOptDir = ensureDirExists(patch.parentFile, "dexOpt")
    loadPatch(patch, dexOptDir)
}

fun File.isValidPatchFile(context: Context): Boolean {
    val preferences = context.getSharedPreferences("hotfix", Context.MODE_PRIVATE)
    val json = preferences.getString("patch", "{}")
    val patch = JSONObject(json)
    val sha1 = shaHex(this.readBytes())
    return sha1.equals(patch.getString("sha1"))
}

fun appVersionCode(context: Context): Int {
    val packageManager = context.packageManager;
    return packageManager.getPackageInfo(context.packageName, GET_CONFIGURATIONS).versionCode;
}

fun configure(context: Context) {
    val rootDir = ensureDirExists(context.filesDir, "hotfix")
    val apkDir = ensureDirExists(rootDir, "default")
    val dexOptDir = ensureDirExists(apkDir, "dexOpt")
    val apk = copyHelperAPK(context, apkDir)

    loadPatch(apk, dexOptDir)
}

fun loadPatch(dexFile: File, dexOptDir: File) {
    val dexPath = dexFile.absolutePath
    val defaultDexOptPath = dexOptDir.absolutePath

    val dexClassLoader = DexClassLoader(dexPath, defaultDexOptPath, dexPath, getPathClassLoader())
    val baseDexElements = getDexElements(getPathList(getPathClassLoader()))
    val newDexElements = getDexElements(getPathList(dexClassLoader))

    val concat = concat(baseDexElements, newDexElements)
    val pathList = getPathList(getPathClassLoader())
    setField(pathList, pathList.javaClass, "dexElements", concat)
}

private fun ensureDirExists(parent: File?, dir: String): File {
    val rootDir = File(parent, dir)
    rootDir.mkdirs()
    return rootDir
}

private fun copyHelperAPK(context: Context, toDir: File): File {
    val apk = "patch_helper.apk"
    val input = context.assets.open(apk)
    val copyTo = File(toDir, apk)
    val out = copyTo.outputStream()
    input.copyTo(out)
    out.close()
    input.close()
    return copyTo
}

