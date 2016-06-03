package con.xingshulin.singularity.utils

import com.xingshulin.singularity.utils.DigestUtils.shaHex
import android.content.Context
import java.io.File

internal fun File.isValidPatchFile(context: Context): Boolean {
    val patch = context.getHotfixConfig()
    val sha1 = shaHex(this.readBytes())
    return sha1.equals(patch.sha())
}

internal fun ensureSubDirExists(parent: File?, dir: String): File {
    val rootDir = File(parent, dir)
    rootDir.mkdirs()
    return rootDir
}