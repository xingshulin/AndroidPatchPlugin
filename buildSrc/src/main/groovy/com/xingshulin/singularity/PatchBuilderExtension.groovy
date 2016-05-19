package com.xingshulin.singularity

import org.gradle.api.Project

class PatchBuilderExtension {
    HashMap<String, String> buildHistoriesFilter = [[:]]
    String accessKey
    HashSet<String> excludeClass = []

    PatchBuilderExtension(Project project) {
    }
}
