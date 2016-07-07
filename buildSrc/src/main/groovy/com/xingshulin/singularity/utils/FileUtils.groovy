package com.xingshulin.singularity.utils

import groovy.io.FileVisitResult

class FileUtils {
    public static def dirFilter = {
        if (it.absolutePath.contains("com/xingshulin/singularity/"))
            return FileVisitResult.SKIP_SUBTREE
        return FileVisitResult.CONTINUE
    }
}
