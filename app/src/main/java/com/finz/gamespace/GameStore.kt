package com.finz.gamespace

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo

object GameStore {

    fun allApps(ctx: Context): List<ResolveInfo> {
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
    }

    fun saved(ctx: Context): List<ResolveInfo> {
        val all = allApps(ctx)
        val prefs = ctx.getSharedPreferences("gs", Context.MODE_PRIVATE)
        val raw = prefs.getString("games", null)
        if (raw == null) {
            val auto = all.filter {
                it.activityInfo.applicationInfo.category == ApplicationInfo.CATEGORY_GAME
            }
            save(ctx, auto.map { it.activityInfo.packageName })
            return auto
        }
        val set = raw.split(",").filter { it.isNotEmpty() }
        return all.filter { set.contains(it.activityInfo.packageName) }
    }

    fun save(ctx: Context, pkgs: List<String>) {
        ctx.getSharedPreferences("gs", Context.MODE_PRIVATE)
            .edit().putString("games", pkgs.joinToString(",")).apply()
    }
}
