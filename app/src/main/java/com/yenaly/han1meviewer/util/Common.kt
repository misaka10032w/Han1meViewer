package com.yenaly.han1meviewer.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.GlobalToasts
import java.security.MessageDigest

fun isLegalBuild(context: Context, sha: String): Boolean {
  //  if (BuildConfig.DEBUG) return true
    return try {
        val pm = context.packageManager
        val packageName = context.packageName
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val packageInfo = pm.getPackageInfo(packageName, flags)

        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }

        signatures?.any { sig ->
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(sig.toByteArray())
            digest.joinToString("") { "%02X".format(it) } == sha
        } ?: false
    } catch (_: Exception) {
        false
    }
}

fun getSha(context: Context, res: Int): String {
    val input = context.resources.openRawResource(res)
    val totalSize = input.available()
    val buffer = ByteArray(32)
    input.skip((totalSize - 32).toLong())
    input.read(buffer)
    input.close()
    return buffer.joinToString("") { "%02X".format(it) }
}
fun checkBadGuy(context: Context, res: Int): IntArray {
    try {
        val sha = getSha(context, res)
        return if (!isLegalBuild(context, sha)){
    //            Preferences.preferenceSp.edit {
    //                putString(NetworkSettingsFragment.DOMAIN_NAME,"http://hanime.c0m")
    //            }
            intArrayOf(R.string.app_tampered, R.string.app_tampered)
        } else {
            intArrayOf(R.string.introduction, R.string.comment)
        }
    } catch (e: java.lang.Exception){
        GlobalToasts.show("${e.message}", level = GlobalToasts.ToastLevel.ERROR)
        return intArrayOf(R.string.introduction, R.string.comment)
    }
}
