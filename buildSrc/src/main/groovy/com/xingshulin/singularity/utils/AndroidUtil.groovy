package com.xingshulin.singularity.utils

import groovy.xml.Namespace

class AndroidUtil {

    private static final androidTag = new Namespace("http://schemas.android.com/apk/res/android", 'android')

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
}
