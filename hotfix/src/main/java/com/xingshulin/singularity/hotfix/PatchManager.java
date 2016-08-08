package com.xingshulin.singularity.hotfix;

import android.content.Context;
import com.xingshulin.singularity.core.Patch;

public class PatchManager {
    public static void install(Context context, String token) {
        Patch.configure(context, token);
        Patch.discoverAndApply(context);
    }

    public static void checkForUpdates(Context context) {
        Patch.checkForUpdates(context);
    }
}
