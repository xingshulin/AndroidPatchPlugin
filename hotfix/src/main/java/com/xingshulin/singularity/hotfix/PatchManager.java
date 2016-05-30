package com.xingshulin.singularity.hotfix;

import android.content.Context;
import com.xingshulin.singularity.core.PatchKt;

public class PatchManager {
    public static void install(Context context) {
        PatchKt.configure(context);
    }

    public static void applyPatch(Context context) {
        PatchKt.discoverAndApply(context);
    }
}
