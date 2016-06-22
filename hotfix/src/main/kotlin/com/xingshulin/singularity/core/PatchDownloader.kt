package com.xingshulin.singularity.core

import android.content.Context
import android.util.Log
import con.xingshulin.singularity.utils.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import com.xingshulin.singularity.utils.DigestUtils.shaHex
import java.nio.charset.Charset
import kotlin.concurrent.thread

fun download(context: Context) {
    thread {
        try {
            val address = "$DOMAIN/patches?packageName=${context.packageName}&appBuild=${context.appVersionCode()}"
            Log.i(TAG, "Start pulling patch @ " + address)
            val token = context.getHotfixConfig("token")
            val result = readText(address, token)
            Log.v(TAG, result)
            if (JSONArray(result).length() == 0) return@thread

            val patchConfig = JSONArray(result).getJSONObject(0)
            if (patchConfig.isValidPatch() && patchConfig.needDownload(context.getHotfixConfig())) {
                doDownload(patchConfig, context.patchFile(), token)
                context.saveHotfixConfig(patchConfig)
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
    }
}

fun doDownload(patchConfig: JSONObject, patchFile: File, token: String) {
    val fileUrl = readText("${DOMAIN}/tokens?type=get&key=${patchConfig.getString(KEY_URI)}", token)
    Log.v(TAG, "downloading patch file " + fileUrl)
    val connection = URL(fileUrl).openConnection()
    connection.doInput = true
    connection.connect()

    if (patchFile.exists()) {
        patchFile.delete()
    }
    patchFile.parentFile.mkdirs()
    val inputStream = connection.inputStream
    patchFile.writeBytes(inputStream.readBytes())
    inputStream.close()
    val fileSha1 = shaHex(patchFile.readBytes())
    if (fileSha1.equals(patchConfig.sha())) {
        Log.v(TAG, "file downloaded, sha1 is $fileSha1")
    } else {
        Log.w(TAG, "the downloaded file failed to pass sha check")
    }
}

private fun readText(address: String, token: String): String {
    val connection = URL(address).openConnection()
    connection.setRequestProperty("Authorization", "Bearer $token")
    connection.doInput = true
    connection.connect()
    val result = connection.inputStream.use {
        it.readBytes()
    }.toString(Charset.defaultCharset())
    return result
}
