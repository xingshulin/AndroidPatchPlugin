package com.xingshulin.singularity

import groovy.io.FileType
import groovy.io.FileVisitResult
import jdk.internal.org.objectweb.asm.ClassReader
import jdk.internal.org.objectweb.asm.ClassVisitor
import jdk.internal.org.objectweb.asm.ClassWriter
import jdk.internal.org.objectweb.asm.MethodVisitor
import jdk.internal.org.objectweb.asm.Opcodes
import jdk.internal.org.objectweb.asm.Type
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.xingshulin.singularity.utils.AndroidUtil

import static groovy.io.FileType.FILES

class PatchPlugin implements Plugin<Project> {
    HashSet<String> excludeClass

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
                        if (fileOrDir.isDirectory()) {
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
                                processClass(file)
                            }
                        }
                    }
                }
            }
        }
    }

    private static void processClass(File inputFile) {
        println 'processing class ' + inputFile.absolutePath
        def optClass = new File(inputFile.getParent(), inputFile.getName() + '.opt')
        FileInputStream inputStream = new FileInputStream(inputFile)
        def bytes = referHackWhenInit(inputStream)
        FileOutputStream outputStream = new FileOutputStream(optClass)
        outputStream.write(bytes)
        inputStream.close()
        outputStream.close()
        if (inputFile.exists()) {
            inputFile.delete()
        }
        optClass.renameTo(inputFile)
    }

    private static byte[] referHackWhenInit(InputStream inputStream) {
        ClassReader reader = new ClassReader(inputStream)
        ClassWriter writer = new ClassWriter(reader, 0)
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM4, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {

                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                mv = new MethodVisitor(Opcodes.ASM4, mv) {
                    @Override
                    void visitInsn(int opcode) {
                        if ("<init>".equals(name) && opcode == Opcodes.RETURN) {
                            super.visitLdcInsn(Type.getType("Lcom/xingshulin/zeus/Hack;"));
                        }
                        super.visitInsn(opcode);
                    }
                }
                return mv;
            }
        };
        reader.accept(visitor, 0)
        return writer.toByteArray()
    }
}