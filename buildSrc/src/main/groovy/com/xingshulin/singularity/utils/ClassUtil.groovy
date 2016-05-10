package com.xingshulin.singularity.utils

import jdk.internal.org.objectweb.asm.ClassReader
import jdk.internal.org.objectweb.asm.ClassVisitor
import jdk.internal.org.objectweb.asm.ClassWriter
import jdk.internal.org.objectweb.asm.MethodVisitor
import jdk.internal.org.objectweb.asm.Opcodes
import jdk.internal.org.objectweb.asm.Type

class ClassUtil {
    static byte[] referHackWhenInit(InputStream inputStream) {
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

    static void patchClass(File inputFile) {
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
}
