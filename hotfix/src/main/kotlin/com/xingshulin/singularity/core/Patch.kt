package com.xingshulin.singularity.core

import android.content.Context
import com.xingshulin.singularity.utils.ArrayUtils.concat
import dalvik.system.DexClassLoader
import java.io.File
import java.lang.reflect.Field

fun installPatch(context: Context) {
    val rootDir = ensureDirExists(context.filesDir, "hotfix")
    val apk = copyHelperAPK(context, rootDir)
    val dexOptDir = ensureDirExists(rootDir, "dexOpt")
    val dexDir = ensureDirExists(rootDir, "dex")

    loadApk(apk, dexDir, dexOptDir)
}

fun loadApk(apk: File, dexDir: File, dexOptDir: File) {
    val dexPath = dexDir.absolutePath
    val defaultDexOptPath = dexOptDir.absolutePath

    val dexClassLoader = DexClassLoader(dexPath, defaultDexOptPath, dexPath, getPathClassLoader())
    val baseDexElements = getDexElements(getPathList(getPathClassLoader()))
    val newDexElements = getDexElements(getPathList(dexClassLoader))

    val concat = concat(baseDexElements, newDexElements)
    val pathList = getPathList(getPathClassLoader())
    setField(pathList, pathList.javaClass, "dexElements", concat)
}

fun getDexElements(dexListObject: Any): Array<*> {
    return getFieldValue(dexListObject, dexListObject.javaClass, "dexElements") as Array<*>
}

fun getPathList(baseDexClassLoader: ClassLoader): Any {
    return getFieldValue(baseDexClassLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList")
}

private fun getPathClassLoader() = object {}.javaClass.classLoader

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

fun getFieldValue(target: Any, javaClass: Class<*>, field: String): Any {
    val declaredField = getField(field, javaClass)
    return declaredField.get(target)
}

fun setField(target: Any, javaClass: Class<*>, field: String, value: Any) {
    getField(field, javaClass).set(target, value)
}

private fun getField(field: String, javaClass: Class<*>): Field {
    val declaredField = javaClass.getDeclaredField(field)
    declaredField.isAccessible = true
    return declaredField
}
