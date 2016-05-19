package com.xingshulin.singularity.utils

import groovy.json.JsonSlurper
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.gradle.api.GradleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static java.net.URLEncoder.encode
import static okhttp3.MediaType.parse
import static okhttp3.RequestBody.create

class PatchUploader {
    static private OkHttpClient client = new OkHttpClient()
    static private String host = "http://localhost:8080"
    static private Logger logger = LoggerFactory.getLogger('android-patch')

    static HashMap<String, String> downloadBuildHistory(HashMap<String, String> buildOptions, String patchDir) {
        if (buildOptions.size() < 1) {
            throw new GradleException('Need to specify more than 1 params')
        }
        Object object = downloadBuildHistories(buildOptions)
        String mapping = object[0]["dexMapping"]
        logger.debug("Found mapping file ${mapping}")
        def token = getToken("get", mapping)

        println(token)
        def request = new Request.Builder().url(token).build()
        def response = client.newCall(request).execute()

        def patchedTxt = new File("${patchDir}/${mapping}")
        if (patchedTxt.exists()) patchedTxt.delete()

        patchedTxt.bytes = response.body().bytes()
        logger.quiet("Downloaded mapping file ${patchedTxt.absolutePath}")
        return (HashMap<String, String>) Eval.me(patchedTxt.text)
    }

    private static Object downloadBuildHistories(HashMap<String, String> buildOptions) {
        def params = buildOptions.collect { key, value ->
            return "${key}=${encode(value, "UTF-8")}"
        }
        def request = new Request.Builder().url("${host}/buildHistories?${params.join('&')}").build()
        def response = client.newCall(request).execute()
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parse(response.body().byteStream())
        object
    }

    static void saveBuildHistory(HashMap<String, String> buildOptions, File patchClasses) {
        String uploadToken = getToken("put", patchClasses.name)
        uploadFile(uploadToken, patchClasses)
        uploadBuildHistory(buildOptions, patchClasses.name)
    }

    static void uploadBuildHistory(HashMap<String, String> buildHistorySettings, String fileName) {
        logger.quiet('Upload build params')
        def builder = new Request.Builder().url("${host}/buildHistories")
        def formBuilder = new FormBody.Builder()
        for (String key : buildHistorySettings.keySet()) {
            formBuilder.addEncoded(key, buildHistorySettings.get(key))
        }
        formBuilder.addEncoded("dexMapping", fileName)
        def request = builder.post(formBuilder.build()).build()
        def response = client.newCall(request).execute()

        logger.debug(response.body().string())
    }

    private static void uploadFile(String uploadToken, File patchedFiles) {
        def body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart('token', uploadToken)
                .addFormDataPart('file', patchedFiles.name, create(parse('text/plain; charset=utf-8'), patchedFiles))
                .addFormDataPart('key', patchedFiles.name)
                .build()
        def request = new Request.Builder()
                .url("http://upload.qiniu.com/")
                .header('Host', 'upload.qiniu.com')
                .post(body).build()
        def response = client.newCall(request).execute()
        logger.debug(response.body().string())
    }

    private static String getToken(String tokenType, String fileName) {
        def builder = new Request.Builder()
                .url("${host}/tokens?type=${tokenType}&key=${fileName}")
        def response = client.newCall(builder.build()).execute()

        String uploadToken = response.body().string()
        if (!uploadToken) {
            throw new GradleException('Cannot get upload token, please check your network.')
        }
        uploadToken
    }
}
