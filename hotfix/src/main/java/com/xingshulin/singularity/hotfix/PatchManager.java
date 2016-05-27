package com.xingshulin.singularity.hotfix;

import com.xingshulin.singularity.core.PatchDownloaderKt;

public class PatchManager {
    public static void install() {
        PatchDownloaderKt.download("Hello Kotlin");
    }
}
