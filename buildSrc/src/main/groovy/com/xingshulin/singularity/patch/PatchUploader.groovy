package com.xingshulin.singularity.patch

import com.xingshulin.singularity.utils.DateUtils
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
import static org.apache.commons.codec.digest.DigestUtils.shaHex

class PatchUploader {
    public static final String KEY_BUILD_TIMESTAMP = 'buildTimestamp'
    public static final String KEY_REVISION_CODE = 'revisionCode'
    public static final String KEY_PACKAGE_NAME = 'packageName'
    public static final String KEY_VERSION_CODE = 'versionCode'
    public static final String KEY_VERSION_NAME = 'versionName'
    public static final String KEY_BUILD_DEVICE_ID = 'buildDeviceId'

    static private OkHttpClient client = new OkHttpClient()
    static private String host = "http://singularity.xingshulin.com"
    static private Logger logger = LoggerFactory.getLogger('android-patch')

    static private String securityKey

    static void setAccessKey(String accessKey) {
        if (accessKey == null || accessKey.trim().length() == 0) {
            def info = 'Access key cannot be null.'
            logger.quiet(fatal(info))
            throw new GradleException(info)
        }
        securityKey = accessKey
    }

    private static Object downloadBuildHistories(HashMap<String, String> buildOptions) {
        def params = buildOptions.collect { key, value ->
            return "${key}=${encode(value.toString(), "UTF-8")}"
        }
        def request = new Request.Builder()
                .url("${host}/buildHistories?${params.join('&')}")
                .header("Authorization", "Bearer ${securityKey}")
                .build()
        def response = client.newCall(request).execute()
        failBuildOnError(response)
        new JsonSlurper().parse(response.body().byteStream())
    }

    static void uploadBuildHistory(HashMap<String, String> buildHistorySettings, String fileName) {
        logger.quiet('Upload build params ' + buildHistorySettings.inspect())
        def builder = new Request.Builder()
                .url("${host}/buildHistories")
                .header("Authorization", "Bearer ${securityKey}")
        def formBuilder = new FormBody.Builder()
        for (String key : buildHistorySettings.keySet()) {
            formBuilder.addEncoded(key, buildHistorySettings.get(key))
        }
        formBuilder.addEncoded("dexMapping", fileName)
        def request = builder.post(formBuilder.build()).build()
        def response = client.newCall(request).execute()
        failBuildOnError(response)
        logger.debug(response.body().string())
    }

    private static String getToken(String tokenType, String fileName) {
        def builder = new Request.Builder()
                .url("${host}/tokens?type=${tokenType}&key=${fileName}")
                .header("Authorization", "Bearer ${securityKey}")
        def response = client.newCall(builder.build()).execute()

        failBuildOnError(response)
        String uploadToken = response.body().string()
        if (!uploadToken) {
            def info = 'Cannot get upload token, please check your network.'
            logger.quiet(fatal(info))
            throw new GradleException(info)
        }
        uploadToken
    }

    static void uploadPatch(Map<String, String> patchOptions, File patchFile) {
        logger.quiet('Patch file created @ ' + patchFile.absolutePath)
        String uploadToken = getToken("put", patchFile.name)
        uploadFile(uploadToken, patchFile)
        uploadPatchInfo(patchOptions, patchFile)
    }

    static void uploadPatchInfo(Map<String, String> patchOptions, File patchFile) {
        logger.quiet('Upload patch info ' + patchOptions.inspect())
        def builder = new Request.Builder()
                .url("${host}/patches")
                .header("Authorization", "Bearer ${securityKey}")
        def formBuilder = new FormBody.Builder()
        formBuilder.addEncoded('packageName', patchOptions.get(KEY_PACKAGE_NAME))
        formBuilder.addEncoded('appVersion', patchOptions.get(KEY_VERSION_NAME))
        formBuilder.addEncoded('appBuild', patchOptions.get(KEY_VERSION_CODE))
        formBuilder.addEncoded('version', '1')
        formBuilder.addEncoded("uri", patchFile.name)
        formBuilder.addEncoded("sha1", shaHex(patchFile.bytes))
        formBuilder.addEncoded("buildDeviceId", patchOptions.get(KEY_BUILD_DEVICE_ID))
        formBuilder.addEncoded("buildTimestamp", patchOptions.get(KEY_BUILD_TIMESTAMP))
        def request = builder.post(formBuilder.build()).build()
        def response = client.newCall(request).execute()
        failBuildOnError(response)
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
        failBuildOnError(response)
        logger.debug(response.body().string())
    }

    static HashMap<String, String> downloadBuildHistory(HashMap<String, String> buildOptions, String patchDir) {
        if (buildOptions.size() < 1) {
            throw new GradleException('Need to specify more than 1 params')
        }
        Object result = downloadBuildHistories(buildOptions)
        def resultMessage = validateBuildHistories(result)
        if (resultMessage) {
            throw new GradleException(resultMessage)
        }
        if (!result[0]) return new HashMap<String, String>(0)
        String mapping = result[0]["dexMapping"]
        logger.debug("Found mapping file ${mapping}")
        def token = getToken("get", mapping)

        def request = new Request.Builder().url(token).build()
        def response = client.newCall(request).execute()

        def patchedTxt = new File("${patchDir}/${mapping}")
        if (patchedTxt.exists()) patchedTxt.delete()

        failBuildOnError(response)
        patchedTxt.bytes = response.body().bytes()
        logger.quiet("Downloaded mapping file ${patchedTxt.absolutePath}")
        return (HashMap<String, String>) Eval.me(patchedTxt.text)
    }

    static void saveBuildHistory(HashMap<String, String> buildOptions, File patchClasses) {
        String uploadToken = getToken("put", patchClasses.name)
        uploadFile(uploadToken, patchClasses)
        uploadBuildHistory(buildOptions, patchClasses.name)
    }

    private static void failBuildOnError(Response response) {
        if (!response.successful) {
            def error = response.body().string()
            logger.quiet(fatal(error))
            throw new GradleException(error)
        }
    }

    static String validateBuildHistories(Object jsonArray) {
        if (jsonArray.size() == 0) {
            return fatal('No build histories found, please adjust your filters.')
        }
        if (jsonArray.size() > 1) {
            StringBuilder builder = new StringBuilder()
            builder.append("\r\nAvailable build histories:").append("\r\n")
            for (int i = 0; i < jsonArray.size(); i++) {
                def item = jsonArray[i]
                builder.append("buildTimestamp: ${item.buildTimestamp} - built at ${DateUtils.format(item.buildTimestamp)}").append("\r\n")
            }
            return fatal("Found ${jsonArray.size()} build histories, please adjust your filters. ${builder.toString()}")
        }
        null
    }

    private static String fatal(String info) {
        return "==================\r\nFatal: $info \r\n================="
    }
}
