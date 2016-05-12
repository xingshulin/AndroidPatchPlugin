package com.xingshulin.singularity

import com.xingshulin.singularity.utils.AndroidUtil
import groovy.io.FileVisitResult
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

import static com.xingshulin.singularity.utils.ClassUtil.guessClassName
import static com.xingshulin.singularity.utils.ClassUtil.patchClass
import static com.xingshulin.singularity.utils.PatchUploader.uploadPatche
import static groovy.io.FileType.FILES
import static java.util.UUID.randomUUID

class PatchPlugin implements Plugin<Project> {
    HashSet<String> excludeClass
    HashMap<String, String> patchedFiles = new HashMap<>()

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
                        return file.absolutePath.endsWith("${variant.name.capitalize()}/AndroidManifest.xml")
                    }
                    if (manifest) {
                        def applicationClass = AndroidUtil.getApplication(manifest as File)
                        if (applicationClass) {
                            excludeClass.add(applicationClass)
                        }
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
                    uploadPatche(patchedTxt)
                }
            }
        }
    }


}