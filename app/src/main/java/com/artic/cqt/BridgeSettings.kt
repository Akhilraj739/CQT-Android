package com.artic.cqt

import android.content.Context
import android.content.SharedPreferences

class BridgeSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bridge_whitelist", Context.MODE_PRIVATE)

    fun isWhitelisted(packageName: String): Boolean {
        return prefs.getBoolean(packageName, false)
    }

    fun setWhitelisted(packageName: String, allowed: Boolean) {
        prefs.edit().putBoolean(packageName, allowed).apply()
    }

    fun getWhitelistedApps(): List<String> {
        return prefs.all.filter { it.value == true }.keys.toList()
    }
}
