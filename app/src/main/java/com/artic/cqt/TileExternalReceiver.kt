package com.artic.cqt

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import android.util.Log

class TileExternalReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.artic.cqt.ACTION_UPDATE_TILE") return

        val tileId = intent.getIntExtra("tileId", -1)
        if (tileId !in 1..20) {
            Log.e("TileExternalReceiver", "Invalid tileId: $tileId. Must be between 1 and 20.")
            return
        }

        val callerPackage = intent.getStringExtra("caller_package") 
            ?: context.packageManager.getNameForUid(getSendingUid()) 
            ?: "External App"

        val bridgeSettings = BridgeSettings(context)

        if (bridgeSettings.isWhitelisted(callerPackage)) {
            // App is already whitelisted, apply changes immediately
            applyChanges(context, intent, tileId)
        } else {
            // Not whitelisted, launch approval activity
            val approvalIntent = Intent(context, BridgeApprovalActivity::class.java).apply {
                putExtras(intent)
                putExtra("callerPackage", callerPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            try {
                context.startActivity(approvalIntent)
            } catch (e: Exception) {
                Log.e("TileExternalReceiver", "Failed to start BridgeApprovalActivity", e)
            }
        }
    }

    private fun applyChanges(context: Context, intent: Intent, tileId: Int) {
        val prefs = TilePreferences(context, tileId)
        
        intent.getStringExtra("label")?.let { prefs.tileLabel = it }
        intent.getStringExtra("subtitle")?.let { prefs.tileSubtitle = it }
        intent.getStringExtra("actionType")?.let { prefs.tileActionType = it }
        intent.getStringExtra("actionValue")?.let { prefs.tileActionValue = it }
        intent.getStringExtra("iconUri")?.let { 
            prefs.iconType = "GALLERY"
            prefs.iconValue = it 
        }

        val className = "com.artic.cqt.QuickTileService$tileId"
        TileService.requestListeningState(context, ComponentName(context, className))
    }

    private fun getSendingUid(): Int {
        return try {
            val getSendingUidMethod = BroadcastReceiver::class.java.getDeclaredMethod("getSendingUid")
            getSendingUidMethod.isAccessible = true
            getSendingUidMethod.invoke(this) as Int
        } catch (e: Exception) {
            -1
        }
    }
}
