package com.xingshulin.singularity

import org.gradle.api.Project

class PatchExtension {
    String accessKey

    HashSet<String> excludeClass = []

    PatchExtension(Project project) {
    }
}
