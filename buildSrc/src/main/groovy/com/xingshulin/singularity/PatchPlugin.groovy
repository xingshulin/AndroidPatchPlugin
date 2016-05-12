package com.xingshulin.singularity

import groovy.io.FileVisitResult
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

import static com.xingshulin.singularity.utils.AndroidUtil.getAppInfo
import static com.xingshulin.singularity.utils.ClassUtil.guessClassName
import static com.xingshulin.singularity.utils.ClassUtil.patchClass
import static com.xingshulin.singularity.utils.PatchUploader.uploadPatch
import static groovy.io.FileType.FILES
import static java.util.UUID.randomUUID

class PatchPlugin implements Plugin<Project> {
    HashSet<String> excludeClass
    HashMap<String, String> patchedFiles = new HashMap<>()
    HashMap<String, String> patchOptions = new HashMap<>()

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
                    def processManifestTask = project.tasks.findByName("process${variant.name.capitalize()}Manifest")
                    def manifest = processManifestTask.outputs.files.find { file ->
                        return file.absolutePath.endsWith("${variant.name}/AndroidManifest.xml")
                    }
                    if (manifest) {
                        def appInfo = getAppInfo(manifest as File)
                        if (appInfo.applicationClass) {
                            excludeClass.add(appInfo.applicationClass)
                        }
                        savePatchOptions('packageName', appInfo.packageName)
                        savePatchOptions('versionCode', appInfo.versionCode)
                        savePatchOptions('versionName', appInfo.versionName)
                        savePatchOptions('revisionCode', appInfo.revisionCode)
                        savePatchOptions('buildingDeviceId', getDeviceId())
                    }
                }
                transformTask.doLast {
                    def inputFiles = transformTask.inputs.files
                    inputFiles.each { fileOrDir ->
                        if (fileOrDir.isFile()) {
                            println fileOrDir.absolutePath + ' is skipped.'
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
                            patchedFiles.put(guessClassName(fileOrDir, file), patchClass(file))
                        }
                    }
                    def patchedTxt = new File("${project.buildDir}/outputs/patch/patch.${randomUUID()}.txt")
                    patchedTxt.getParentFile().delete()
                    patchedTxt.getParentFile().mkdirs()
                    patchedTxt.text = patchedFiles.inspect()
                    uploadPatch(patchOptions, patchedTxt)
                }
            }
        }
    }

    private void savePatchOptions(String key, String value) {
        if (value == null || key == null) return
        if (!patchOptions.containsKey(key)) {
            patchOptions[key] = value
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