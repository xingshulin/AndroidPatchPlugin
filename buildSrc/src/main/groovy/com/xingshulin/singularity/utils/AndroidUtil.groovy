package com.xingshulin.singularity.utils

import groovy.xml.Namespace
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project

import static java.lang.System.currentTimeMillis
import static java.util.UUID.randomUUID

class AndroidUtil {

    private static final androidTag = new Namespace("http://schemas.android.com/apk/res/android", 'android')
    private static final String SDK_DIR = "sdk.dir"

    public static AppInfo getAppInfo(File manifestFile) {
        def info = new AppInfo()
        def manifest = new XmlParser().parse(manifestFile)
        info.applicationClass = getApplication(manifest)
        info.packageName = getPackage(manifest)
        info.versionCode = getVersionCode(manifest)
        info.versionName = getVersionName(manifest)
        info.revisionCode = getRevisionCode(manifest)
        return info
    }

    static String getRevisionCode(Node manifest) {
        return manifest.attribute(androidTag.revisionCode)
    }

    static String getVersionName(Node manifest) {
        return manifest.attribute(androidTag.versionName)
    }

    static String getVersionCode(Node manifest) {
        return manifest.attribute(androidTag.versionCode)
    }

    public static String getApplication(manifest) {
        def applicationName = manifest.application[0].attribute(androidTag.name)

        if (applicationName != null) {
            return applicationName.replace(".", "/") + ".class"
        }
        return null;
    }

    public static String getPackage(manifest) {
        return manifest.attribute('package')
    }

    static File dex(Project project, File patchDir) {
        if (!patchDir.listFiles().size()) return
        def sdkDir = getSdkDir(project)
        if (!sdkDir) {
            throw new InvalidUserDataException('$ANDROID_HOME is not defined')
        }
        def patchFile = new File(patchDir.getParent(), "patch.${randomUUID()}.jar")
        def cmdExtension = Os.isFamily(Os.FAMILY_WINDOWS) ? ".bat" : ""
        def stdout = new ByteArrayOutputStream()
        project.exec {
            commandLine "${sdkDir}/build-tools/${project.android.buildToolsVersion}/dx${cmdExtension}",
                    '--dex',
                    "--output=${patchFile.absolutePath}",
                    "${patchDir.absolutePath}"
            standardOutput = stdout
        }
        def error = stdout.toString().trim()
        if (error) {
            println "dex error:" + error
        }
        patchFile
    }

    public static String getSdkDir(Project project) {
        Properties properties = new Properties()
        File localProperties = project.rootProject.file("local.properties")
        if (localProperties.exists()) {
            properties.load(localProperties.newDataInputStream())
            properties.getProperty(SDK_DIR)
        } else {
            System.getenv("ANDROID_HOME")
        }
    }
}
