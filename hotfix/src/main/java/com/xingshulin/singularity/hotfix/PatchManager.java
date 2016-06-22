package com.xingshulin.singularity.hotfix;

import android.content.Context;
import com.xingshulin.singularity.core.PatchKt;

import static com.xingshulin.singularity.core.PatchKt.configure;
import static com.xingshulin.singularity.core.PatchKt.discoverAndApply;

public class PatchManager {
    public static void install(Context context, String token) {
        configure(context, token);
        discoverAndApply(context);
    }

    public static void checkForUpdates(Context context) {
        PatchKt.checkForUpdates(context);
    }
}
