package com.hyperlauncher.ai.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Scans installed launcher apps. Uses QUERY_ALL_PACKAGES (offline flavor has
 * no INTERNET permission — scanning is fully local).
 */
object AppScanner {

    fun getLauncherApps(context: Context): List<LauncherAppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val infos = try {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            emptyList()
        }
        return infos.mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null
            val label = try {
                info.loadLabel(pm).toString()
            } catch (e: Exception) {
                pkg
            }
            LauncherAppInfo(packageName = pkg, label = label)
        }.sortedBy { it.label.lowercase() }
    }
}

data class LauncherAppInfo(val packageName: String, val label: String)
