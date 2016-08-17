package com.xingshulin.singularity.patch

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.helpers.BasicMarkerFactory

import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import static com.xingshulin.singularity.patch.PatchUploader.*
import static com.xingshulin.singularity.utils.AndroidUtil.dex
import static com.xingshulin.singularity.utils.AndroidUtil.getAppInfo
import static com.xingshulin.singularity.utils.ClassUtil.*
import static com.xingshulin.singularity.utils.FileUtils.copyFile
import static com.xingshulin.singularity.utils.FileUtils.isJar
import static com.xingshulin.singularity.utils.MapUtils.*
import static groovy.io.FileType.FILES
import static groovy.json.JsonOutput.toJson
import static java.lang.System.currentTimeMillis
import static java.util.UUID.randomUUID
import static org.apache.commons.codec.digest.DigestUtils.shaHex

class PatchGeneratorPlugin implements Plugin<Project> {
    public static final Marker TAG = new BasicMarkerFactory().getMarker('PatchGeneratorPlugin')
    HashSet<String> excludes = new HashSet<>()
    HashMap<String, String> includedFiles = new HashMap<>()
    HashMap<String, String> buildOptions = new HashMap<>()
    File patchRootDir
    File patchOutputDir
    static private Logger logger = LoggerFactory.getLogger('android-patch')

    @Override
    void apply(Project project) {
        project.extensions.create('patchCreator', PatchCreatorExtension.class, project)

        project.afterEvaluate {
            if (!project.android) {
                throw new GradleException('Please apply android plugin first')
            }
            if (project.patchCreator.disabled) {
                logger.warn('Patch generation is disabled, will not create and upload patch files')
                logger.warn('And apk created during this build CANNOT be patched in future')
                return
            }
            if (project.patchCreator.buildExtra) {
                def extra = project.patchCreator.buildExtra
                copy(buildOptions, extra)
            }

            setAccessKey(project.patchCreator.accessKey)

            project.android.applicationVariants.each { variant ->
                def transformTask = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")
                if (!transformTask) {
                    throw new GradleException('Cannot find any transform tasks')
                }
                transformTask.doFirst {
                    ensurePatchDirs(project)
                    loadBuildOptions(project, variant)

                    HashMap<String, String> filter = project.patchCreator.filter
                    merge(filter, buildOptions, KEY_PACKAGE_NAME, KEY_VERSION_NAME, KEY_VERSION_CODE, KEY_BUILD_DEVICE_ID)
                    def lastTransformedFiles = downloadBuildHistory(filter, patchRootDir.absolutePath)

                    Closure postPatchAction = getPostPatchAction(lastTransformedFiles)
                    patchClasses(transformTask, postPatchAction)
                    def patchedTxt = new File(patchRootDir.absolutePath + "/patch.dex.${randomUUID()}.txt")
                    patchedTxt.text = toJson(includedFiles)
                    saveBuildHistory(buildOptions, patchedTxt)

                    def patchFile = createRealPatch(project)
                    if (patchFile) {
                        def patchOptions = new HashMap<String, String>()
                        patchOptions.put(KEY_BUILD_TIMESTAMP, '' + currentTimeMillis())
                        merge(patchOptions, buildOptions, KEY_PACKAGE_NAME, KEY_VERSION_NAME, KEY_VERSION_CODE, KEY_BUILD_DEVICE_ID)
                        uploadPatch(patchOptions, patchFile)
                    }
                }
            }
        }
    }

    private Closure getPostPatchAction(HashMap<String, String> lastTransformedFiles) {
        if (lastTransformedFiles.isEmpty()) {
            return { String fileName, String fileSha, byte[] fileInBytes -> }
        }
        return { String fileName, String fileSha, byte[] fileInBytes ->

            if (fileHasChanged(fileName, fileSha, lastTransformedFiles)) {
                copyFile(fileName, fileInBytes, patchOutputDir)
            }
        }
    }

    private File createRealPatch(Project project) {
        if (patchOutputDir.listFiles().size() == 0) {
            logger.warn("No patch generated.")
            return null
        }
        return dex(project, patchOutputDir)
    }

    private void ensurePatchDirs(project) {
        patchRootDir = new File("${project.buildDir}/outputs/patch")
        patchRootDir.mkdirs()
        patchOutputDir = new File("${patchRootDir}/generated_patch")
        patchOutputDir.mkdirs()
    }

    private void patchClasses(transformTask, Closure postPatchAction) {
        def inputFiles = transformTask.inputs.files
        inputFiles.each { File fileOrDir ->
            logger.debug("Processing $fileOrDir.absolutePath")
            if (fileOrDir.isFile()) {
                if (isJar(fileOrDir)) {
                    patchJar(fileOrDir, postPatchAction)
                    return
                }

                logger.warn("Skipped processing file $fileOrDir.absolutePath")
                return
            }

            fileOrDir.traverse(type: FILES) { File file ->
                if (isJar(file)) {
                    patchJar(file, postPatchAction)
                    return
                }

                def className = guessFileName(fileOrDir, file)
                if (shouldSkipTransform(className)) {
                    return
                }
                def bytes = patchClass(file)
                def sha = shaHex(bytes)
                includedFiles.put(className, sha)
                postPatchAction(className, sha, bytes)
            }
        }
    }

    private void patchJar(File fileOrDir, Closure postPatchAction) {
        def optJar = new File(fileOrDir.getParent(), fileOrDir.name + ".opt")
        def jos = new JarOutputStream(new FileOutputStream(optJar))

        def jar = new JarFile(fileOrDir)
        def entries = jar.entries()
        while (entries.hasMoreElements()) {
            def entry = entries.nextElement()
            jos.putNextEntry(new ZipEntry(entry.name))
            def bytes = jar.getInputStream(entry).bytes
            if (!shouldSkipTransform(entry.name)) {
                bytes = referHackWhenInit(bytes)
                def hex = shaHex(bytes)
                includedFiles.put(entry.name, hex)
                postPatchAction(entry.name, hex, bytes)
            }
            jos.write(bytes)
            jos.closeEntry()
        }
        jos.close()
        jar.close()

        fileOrDir.delete()
        optJar.renameTo(fileOrDir)
    }

    private static boolean fileHasChanged(String className, String sha, HashMap<String, String> lastTransformedFiles) {
        !lastTransformedFiles.containsKey(className) || !lastTransformedFiles.get(className).equalsIgnoreCase(sha)
    }

    private byte[] getFileBytes(File file, String className) {

    }

    private boolean shouldSkipTransform(classFullPath) {
        def endWithFilter = { excluded ->
            classFullPath.endsWith(excluded)
        }

        def starts = [
                'com/xingshulin/singularity/',
                'android/support/',
                'com/android/'
        ]
        def startsWithFilter = { excluded ->
            classFullPath.startsWith(excluded)
        }
        !classFullPath.endsWith('.class') || starts.any(startsWithFilter) || excludes.any(endWithFilter)
    }

    private void loadBuildOptions(project, variant) {
        def processManifestTask = project.tasks.findByName("process${variant.name.capitalize()}Manifest")
        def manifest = processManifestTask.outputs.files.find { file ->
            return file.absolutePath.endsWith("${variant.name}/AndroidManifest.xml")
        }
        if (manifest) {
            def appInfo = getAppInfo(manifest as File)
            if (appInfo.applicationClass) {
                excludes.add(appInfo.applicationClass)
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