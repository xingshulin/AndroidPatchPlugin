package com.xingshulin.singularity.utils

import groovy.json.JsonSlurper
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
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

    static HashMap<String, String> downloadBuildHistory(HashMap<String, String> buildOptions) {
        if (buildOptions.size() < 1) {
            throw new GradleException('Need to specify more than 1 params')
        }
        def params = buildOptions.collect { key, value ->
            return "${key}=${encode(value, "UTF-8")}"
        }
        def builder = new Request.Builder().url("${host}/buildHistories?${params.join('&')}")
        def response = client.newCall(builder.build()).execute()
        def jsonSlurper = new JsonSlurper()
        def object = jsonSlurper.parse(response.body().byteStream())
        String mapping = object[0]["dexMapping"]
        logger.debug("Found mapping file ${mapping}")
        def token = getToken("get", mapping)
        println(token)
        return new HashMap<String, String>()
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
                .addFormDataPart('file', patchedFiles.name)
                .addFormDataPart('key', patchedFiles.name)
                .addFormDataPart('fileBinaryData', patchedFiles.name,
                create(parse('text/plain; charset=utf-8'), patchedFiles))
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
