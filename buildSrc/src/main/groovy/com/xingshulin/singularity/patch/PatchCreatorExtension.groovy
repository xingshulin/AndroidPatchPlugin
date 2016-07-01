package com.xingshulin.singularity.patch

import org.gradle.api.Project

class PatchCreatorExtension {
    boolean disabled
    HashMap<String, String> filter = [:]
    String accessKey

    PatchCreatorExtension(Project project) {
    }
}
