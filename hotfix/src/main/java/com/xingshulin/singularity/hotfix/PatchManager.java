package com.xingshulin.singularity.hotfix;

import android.content.Context;
import com.xingshulin.singularity.core.PatchKt;

public class PatchManager {
    private static Context applicationContext;
    public static void install(Context context) {
        applicationContext = context;
        PatchKt.installPatch(context);
    }
}
