package com.xingshulin.singularity.utils

import groovy.io.FileVisitResult

class FileUtils {
    public static def dirFilter = {
        if (it.absolutePath.contains("com/xingshulin/singularity/"))
            return FileVisitResult.SKIP_SUBTREE
        return FileVisitResult.CONTINUE
    }

    public static void copyFile(String filePath, byte[] bytes, File outputDir) {
        def classToCopy = new File("${outputDir}/${filePath}")
        classToCopy.getParentFile().mkdirs()
        classToCopy.bytes = bytes
    }

    public static boolean isJar(File fileOrDir) {
        fileOrDir.absolutePath.endsWith('.jar')
    }
}
