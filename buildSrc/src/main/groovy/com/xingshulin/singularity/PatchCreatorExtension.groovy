package com.xingshulin.singularity

import org.gradle.api.Project

class PatchCreatorExtension {
    boolean disabled
    HashMap<String, String> buildHistoriesFilter = [[:]]
    String accessKey
    HashSet<String> excludeClass = []

    PatchCreatorExtension(Project project) {
    }
}
