package com.xingshulin.singularity.core

import android.content.Context
import android.content.pm.PackageManager.GET_CONFIGURATIONS
import android.util.Log
import com.xingshulin.singularity.utils.ArrayUtils.concat
import com.xingshulin.singularity.utils.DigestUtils.shaHex
import dalvik.system.DexClassLoader
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

val TAG = "hotfix"
val KEY_URI = "uri"
val KEY_SHA = "sha1"
val DOMAIN = "http://localhost:8080"

fun download(context: Context) {
    thread {
        try {
            val result = URL("$DOMAIN/patches?appName=${context.packageName}&appBuild=${context.appVersionCode()}").readText()
            if (JSONArray(result).length() == 0) return@thread

            val patchConfig = JSONArray(result).getJSONObject(0)
            if (patchConfig.isValidPatch() && patchConfig.needDownload(context)) {
                doDownload(patchConfig, context.patchFile())
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
    }
}

fun doDownload(patchConfig: JSONObject, patchFile: File) {
    val fileUrl = URL("$DOMAIN/tokens?type=get&key=${patchConfig.getString(KEY_URI)}").readText()
    val connection = URL(fileUrl).openConnection()
    connection.doInput = true
    connection.connect()
    if (!patchFile.exists()) {
        patchFile.parentFile.mkdirs()
        patchFile.createNewFile()
    }
    patchFile.writeBytes(connection.inputStream.readBytes())
}

fun JSONObject.needDownload(context: Context): Boolean {
    val hotfixConfig = context.getHotfixConfig()
    return !this.getString(KEY_SHA).equals(hotfixConfig.getString(KEY_SHA))
}

fun JSONObject.isValidPatch(): Boolean {
    return !this.isNull(KEY_URI) && !this.isNull(KEY_SHA)
}

fun discoverAndApply(context: Context) {
    val patch = context.patchFile()
    if (patch.exists() && patch.isValidPatchFile(context)) {
        apply(patch)
    }
}

private fun Context.patchFile() = File("${this.filesDir}/hotfix/${this.appVersionCode()}/", "patch.jar")

fun apply(patch: File) {
    val dexOptDir = ensureDirExists(patch.parentFile, "dexOpt")
    loadPatch(patch, dexOptDir)
}

fun File.isValidPatchFile(context: Context): Boolean {
    val patch = context.getHotfixConfig()
    val sha1 = shaHex(this.readBytes())
    return sha1.equals(patch.getString(KEY_SHA))
}

private fun Context.getHotfixConfig(): JSONObject {
    val preferences = this.getSharedPreferences("hotfix", Context.MODE_PRIVATE)
    val json = preferences.getString("patch", "{}")
    val patch = JSONObject(json)
    return patch
}

fun Context.appVersionCode(): Int {
    val packageManager = this.packageManager;
    return packageManager.getPackageInfo(this.packageName, GET_CONFIGURATIONS).versionCode;
}

fun configure(context: Context) {
    val apkDir = ensureDirExists(getHotfixRoot(context), "default")
    val dexOptDir = ensureDirExists(apkDir, "dexOpt")
    val apk = copyHelperAPK(context, apkDir)

    loadPatch(apk, dexOptDir)
}

private fun getHotfixRoot(context: Context) = ensureDirExists(context.filesDir, "hotfix")

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

