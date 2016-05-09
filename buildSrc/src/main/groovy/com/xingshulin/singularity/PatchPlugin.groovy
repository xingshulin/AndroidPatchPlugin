package com.xingshulin.singularity

import org.gradle.api.Plugin
import org.gradle.api.Project

class PatchPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.task('hello') << {
            println 'hello world'
        }
    }
}