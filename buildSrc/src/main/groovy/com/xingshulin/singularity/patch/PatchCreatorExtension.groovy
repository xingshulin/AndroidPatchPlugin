package com.xingshulin.singularity.patch

import org.gradle.api.Project

class PatchCreatorExtension {
    boolean disabled
    String accessKey
    HashMap<String, String> filter = [:]
    HashMap<String, String> buildExtra = [:]

    PatchCreatorExtension(Project project) {
    }
}
