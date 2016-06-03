package con.xingshulin.singularity.utils

import android.content.Context
import android.content.pm.PackageManager.GET_CONFIGURATIONS
import org.json.JSONObject
import java.io.File

internal fun Context.saveHotfixConfig(patchConfig: JSONObject) {
    this.getSharedPreferences("hotfix", Context.MODE_PRIVATE).edit().putString("patch", patchConfig.toString()).apply()
}

internal fun Context.getHotfixConfig(): JSONObject {
    val preferences = this.getSharedPreferences("hotfix", Context.MODE_PRIVATE)
    val json = preferences.getString("patch", "{}")
    return JSONObject(json)
}

internal fun Context.appVersionCode(): Int {
    val packageManager = this.packageManager;
    return packageManager.getPackageInfo(this.packageName, GET_CONFIGURATIONS).versionCode;
}

internal fun Context.patchFile() = File("${this.filesDir}/hotfix/${this.appVersionCode()}/", "patch.jar")

internal fun Context.getHotfixRoot() = ensureSubDirExists(this.filesDir, "hotfix")