package com.xingshulin.singularity.core

import android.content.Context
import android.util.Log
import com.xingshulin.singularity.utils.ArrayUtils.concat
import con.xingshulin.singularity.utils.*
import dalvik.system.DexClassLoader
import java.io.File

val TAG = "hotfix"
val KEY_URI = "uri"
val KEY_SHA = "sha1"
val DOMAIN = "http://singularity.xingshulin.com"

internal fun checkForUpdates(context: Context) {
    download(context)
}

fun configure(context: Context, token: String) {
    context.saveHotfixConfig("token", token)

    val apkDir = ensureSubDirExists(context.getHotfixRoot(), "default")
    val dexOptDir = ensureSubDirExists(apkDir, "dexOpt")
    val apk = copyHelperAPK(context, apkDir)

    loadPatch(apk, dexOptDir)
}

internal fun discoverAndApply(context: Context) {
    val patch = context.patchFile()
    if (!patch.exists()) return
    if (patch.isValidPatchFile(context)) {
        return apply(patch)
    } else {
        patch.delete()
    }
}

private fun apply(patch: File) {
    val dexOptDir = ensureSubDirExists(patch.parentFile, "dexOpt")
    loadPatch(patch, dexOptDir)
}

private fun loadPatch(dexFile: File, dexOptDir: File) {
    Log.d(TAG, "loading patch file ${dexFile.absolutePath}")
    val dexPath = dexFile.absolutePath
    val defaultDexOptPath = dexOptDir.absolutePath

    val dexClassLoader = DexClassLoader(dexPath, defaultDexOptPath, dexPath, getPathClassLoader())
    val baseDexElements = getDexElements(getPathList(getPathClassLoader()))
    val newDexElements = getDexElements(getPathList(dexClassLoader))

    val concat = concat(newDexElements, baseDexElements) //order is important!
    val pathList = getPathList(getPathClassLoader())
    setField(pathList, pathList.javaClass, "dexElements", concat)
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

