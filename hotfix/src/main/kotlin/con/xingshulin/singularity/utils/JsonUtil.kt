package con.xingshulin.singularity.utils

import com.xingshulin.singularity.core.KEY_SHA
import com.xingshulin.singularity.core.KEY_URI
import org.json.JSONObject

internal fun JSONObject.sha() = this.optString(KEY_SHA, "")

internal fun JSONObject.needDownload(hotfixConfig: JSONObject): Boolean {
    return !this.sha().equals(hotfixConfig.sha())
}

internal fun JSONObject.isValidPatch(): Boolean {
    return !this.isNull(KEY_URI) && !this.isNull(KEY_SHA)
}