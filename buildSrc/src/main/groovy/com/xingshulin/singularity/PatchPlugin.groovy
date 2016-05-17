package com.xingshulin.singularity

import groovy.io.FileVisitResult
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.xingshulin.singularity.utils.AndroidUtil.getAppInfo
import static com.xingshulin.singularity.utils.ClassUtil.guessClassName
import static com.xingshulin.singularity.utils.ClassUtil.patchClass
import static com.xingshulin.singularity.utils.PatchUploader.downloadBuildHistory
import static com.xingshulin.singularity.utils.PatchUploader.saveBuildHistory
import static groovy.io.FileType.FILES
import static java.lang.System.currentTimeMillis
import static java.util.UUID.randomUUID

class PatchPlugin implements Plugin<Project> {
    public static final String KEY_BUILD_TIMESTAMP = 'buildTimestamp'
    public static final String KEY_REVISION_CODE = 'revisionCode'
    HashSet<String> excludeClass
    HashMap<String, String> transformedFiles = new HashMap<>()
    HashMap<String, String> buildOptions = new HashMap<>()
    static private Logger logger = LoggerFactory.getLogger('android-patch')

    @Override
    void apply(Project project) {
        project.extensions.create('Patch', PatchExtension.class, project)

        project.afterEvaluate {
            if (!project.android) {
                throw new GradleException('Please apply android plugin first')
            }
            project.android.applicationVariants.each { variant ->
                def transformTask = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")
                if (!transformTask) {
                    throw new GradleException('Cannot find any transform tasks')
                }
                transformTask.doFirst {
                    ensurePatchDir(project)
                    cacheBuildOptions(project, variant)
                    patchClasses(transformTask)
                }
                transformTask.doLast {
                    def patchedTxt = new File(getPatchDir(project) + "/patch.dex.${randomUUID()}.txt")
                    patchedTxt.text = transformedFiles.inspect()
                    saveBuildHistory(buildOptions, patchedTxt)
                    generatePatchFile()
                }
            }
        }
    }

    void generatePatchFile() {
        buildOptions.remove(KEY_BUILD_TIMESTAMP)
        buildOptions.remove(KEY_REVISION_CODE)
        buildOptions.put("buildTimestamp", "1463490606804")
        def lastTransformedFiles = downloadBuildHistory(buildOptions)
    }

    private static void ensurePatchDir(project) {
        new File(getPatchDir(project)).mkdirs()
    }

    private static String getPatchDir(project) {
        "${project.buildDir}/outputs/patch"
    }

    private void patchClasses(transformTask) {
        def inputFiles = transformTask.inputs.files
        inputFiles.each { fileOrDir ->
            if (fileOrDir.isFile()) {
                logger.debug("${fileOrDir.absolutePath} is skipped.")
                return
            }
            def dirFilter = {
                if (it.absolutePath.contains("com/xingshulin/singularity/"))
                    return FileVisitResult.SKIP_SUBTREE
                return FileVisitResult.CONTINUE
            }
            fileOrDir.traverse(type: FILES, nameFilter: ~/.*\.class/, preDir: dirFilter) { file ->
                if (excludeClass.any { excluded ->
                    file.absolutePath.endsWith(excluded)
                }) {
                    return
                }
                transformedFiles.put(guessClassName(fileOrDir, file), patchClass(file))
            }
        }
    }

    private void cacheBuildOptions(project, variant) {
        def processManifestTask = project.tasks.findByName("process${variant.name.capitalize()}Manifest")
        def manifest = processManifestTask.outputs.files.find { file ->
            return file.absolutePath.endsWith("${variant.name}/AndroidManifest.xml")
        }
        if (manifest) {
            def appInfo = getAppInfo(manifest as File)
            if (appInfo.applicationClass) {
                excludeClass.add(appInfo.applicationClass)
            }
            nullSafeSavePatchOptions('packageName', appInfo.packageName)
            nullSafeSavePatchOptions('versionCode', appInfo.versionCode)
            nullSafeSavePatchOptions('versionName', appInfo.versionName)
            nullSafeSavePatchOptions(KEY_REVISION_CODE, appInfo.revisionCode)
            nullSafeSavePatchOptions('buildDeviceId', getDeviceId())
            nullSafeSavePatchOptions(KEY_BUILD_TIMESTAMP, "" + currentTimeMillis())
        }
    }

    private void nullSafeSavePatchOptions(String key, String value) {
        if (value == null || key == null) return
        if (!buildOptions.containsKey(key)) {
            buildOptions[key] = value
        }
    }

    private static String getDeviceId() {
        def deviceIdFile = new File("${System.getProperty("user.home")}/.android_patch/device_id")
        if (!deviceIdFile.exists()) {
            deviceIdFile.getParentFile().mkdirs()
            deviceIdFile.text = randomUUID()
        }
        deviceIdFile.text
    }
}