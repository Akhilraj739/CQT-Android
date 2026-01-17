package com.artic.cqt

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.Toast

class AssistantActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = TilePreferences(this, -1)
        val actionType = prefs.tileActionType
        val actionValue = prefs.tileActionValue

        // Debug Toast to see what action is being triggered
        // Toast.makeText(this, "CQT Assistant: $actionType", Toast.LENGTH_SHORT).show()

        when (actionType) {
            TilePreferences.ACTION_SCREENSHOT -> {
                val accessibility = CQTAccessibilityService.instance
                if (accessibility != null) {
                    accessibility.performAction(actionType)
                } else {
                    // This activity can't take screenshots of other apps directly.
                    // We must rely on the CQTVoiceService which is triggered by the system
                    // when long-pressing home if CQT is the default assistant.
                    Toast.makeText(this, "Please enable Accessibility Service for Screenshot fallback.", Toast.LENGTH_LONG).show()
                }
            }
            TilePreferences.ACTION_OPEN_APP -> {
                if (actionValue.isNotEmpty()) {
                    val intent = packageManager.getLaunchIntentForPackage(actionValue)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
            }
            TilePreferences.ACTION_OPEN_URL -> {
                if (actionValue.isNotEmpty()) {
                    var url = actionValue
                    if (!url.startsWith("http")) url = "https://$url"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { startActivity(intent) } catch (e: Exception) {}
                }
            }
            TilePreferences.ACTION_NONE -> {
                // Only open MainActivity if the user explicitly chose "None" (default behavior)
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            else -> {
                // For system actions (Back, Home, etc.), try Accessibility
                CQTAccessibilityService.instance?.performAction(actionType) ?: run {
                    // If no accessibility, and it's not a special assistant action, open app
                    startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
        finish()
    }
}
