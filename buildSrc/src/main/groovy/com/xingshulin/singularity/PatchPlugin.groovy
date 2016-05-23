package com.xingshulin.singularity

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static com.xingshulin.singularity.utils.AndroidUtil.dex
import static com.xingshulin.singularity.utils.AndroidUtil.getAppInfo
import static com.xingshulin.singularity.utils.ClassUtil.guessClassName
import static com.xingshulin.singularity.utils.ClassUtil.patchClass
import static com.xingshulin.singularity.utils.FileUtils.dirFilter
import static com.xingshulin.singularity.utils.MapUtils.merge
import static com.xingshulin.singularity.utils.MapUtils.nullSafePut
import static com.xingshulin.singularity.utils.PatchUploader.*
import static groovy.io.FileType.FILES
import static java.lang.System.currentTimeMillis
import static java.util.UUID.randomUUID

class PatchPlugin implements Plugin<Project> {
    HashSet<String> excludeClass
    HashMap<String, String> transformedFiles = new HashMap<>()
    HashMap<String, String> buildOptions = new HashMap<>()
    static private Logger logger = LoggerFactory.getLogger('android-patch')

    @Override
    void apply(Project project) {
        project.extensions.create('patchCreator', PatchBuilderExtension.class, project)

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
                    loadBuildOptions(project, variant)
                    patchClasses(transformTask)
                }
                transformTask.doLast {
                    def patchedTxt = new File(getPatchDir(project) + "/patch.dex.${randomUUID()}.txt")
                    patchedTxt.text = transformedFiles.inspect()
                    saveBuildHistory(buildOptions, patchedTxt)
                    createRealPatch(project, transformTask)
                }
            }
        }
    }

    private void createRealPatch(Project project, transformTask) {
        def changedFiles = findChangedFiles(project)
        File generatedPatchDir = getPatchOutputDir(project)
        transformTask.inputs.files.each {
            if (it.isFile()) return
            it.traverse(type: FILES, nameFilter: ~/.*\.class/, preDir: dirFilter) { file ->
                def className = guessClassName(it, file)
                if (changedFiles.containsKey(className)) {
                    def classToCopy = new File("${generatedPatchDir}/${className}")
                    classToCopy.getParentFile().mkdirs()
                    classToCopy.bytes = file.bytes
                }
            }
        }

        if (!generatedPatchDir.listFiles().size()) {
            logger.warn('Not changes found for generating patch file, just skip this build.')
            return
        }
        def patchFile = dex(project, generatedPatchDir)
        def patchOptions = new HashMap<String, String>()
        patchOptions.put(KEY_BUILD_TIMESTAMP, '' + currentTimeMillis())
        merge(patchOptions, buildOptions, KEY_PACKAGE_NAME, KEY_VERSION_NAME, KEY_VERSION_CODE, KEY_BUILD_DEVICE_ID)
        uploadPatch(patchOptions, patchFile)
    }

    private static File getPatchOutputDir(Project project) {
        def generatedPatchDir = new File("${getPatchDir(project)}/generated_patch")
        generatedPatchDir.mkdirs()
        generatedPatchDir
    }

    Map<String, String> findChangedFiles(project) {
        HashMap<String, String> filter = project.patchCreator.buildHistoriesFilter
        merge(filter, buildOptions, KEY_PACKAGE_NAME, KEY_VERSION_NAME, KEY_VERSION_CODE, KEY_BUILD_DEVICE_ID)
        def lastTransformedFiles = downloadBuildHistory(filter, getPatchDir(project))
        diff(lastTransformedFiles, transformedFiles)
    }

    static Map<String, String> diff(HashMap<String, String> original, HashMap<String, String> newer) {
        return original.findAll {
            newer.get(it.key, null) != it.value
        }
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

    private void loadBuildOptions(project, variant) {
        def processManifestTask = project.tasks.findByName("process${variant.name.capitalize()}Manifest")
        def manifest = processManifestTask.outputs.files.find { file ->
            return file.absolutePath.endsWith("${variant.name}/AndroidManifest.xml")
        }
        if (manifest) {
            def appInfo = getAppInfo(manifest as File)
            if (appInfo.applicationClass) {
                excludeClass.add(appInfo.applicationClass)
            }
            nullSafePut(buildOptions, KEY_PACKAGE_NAME, appInfo.packageName)
            nullSafePut(buildOptions, KEY_VERSION_CODE, appInfo.versionCode)
            nullSafePut(buildOptions, KEY_VERSION_NAME, appInfo.versionName)
            nullSafePut(buildOptions, KEY_REVISION_CODE, appInfo.revisionCode)
            nullSafePut(buildOptions, KEY_BUILD_DEVICE_ID, getDeviceId())
            nullSafePut(buildOptions, KEY_BUILD_TIMESTAMP, "" + currentTimeMillis())
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