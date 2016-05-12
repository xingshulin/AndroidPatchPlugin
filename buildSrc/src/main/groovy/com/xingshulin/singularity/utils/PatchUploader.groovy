package com.xingshulin.singularity.utils

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.GradleException

import static okhttp3.MediaType.parse
import static okhttp3.RequestBody.create

class PatchUploader {
    static private OkHttpClient client = new OkHttpClient()

    static void uploadPatch(File patchedFiles) {
        String uploadToken = getUploadToken(patchedFiles)
        uploadFile(uploadToken, patchedFiles)
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
        println response.body().string()
    }

    private static String getUploadToken(File patchedFiles) {
        def builder = new Request.Builder()
                .url("http://localhost:8080/tokens?type=put&key=${patchedFiles.name}")
        def response = client.newCall(builder.build()).execute()

        String uploadToken = response.body().string()
        if (!uploadToken) {
            throw new GradleException('Cannot get upload token, please check your network.')
        }
        uploadToken
    }
}
