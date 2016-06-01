package com.xingshulin.singularity.hotfix;

import android.content.Context;
import com.xingshulin.singularity.core.PatchKt;

import static com.xingshulin.singularity.core.PatchKt.configure;
import static com.xingshulin.singularity.core.PatchKt.discoverAndApply;

public class PatchManager {
    public static void install(Context context) {
        configure(context);
        discoverAndApply(context);
    }

    public static void checkUpdates(Context context) {
        PatchKt.download(context);
    }
}
