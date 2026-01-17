package com.artic.cqt

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CQTAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    companion object {
        var instance: CQTAccessibilityService? = null
            private set

        private val _isConnected = MutableStateFlow(false)
        val isConnected = _isConnected.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        _isConnected.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isConnected.value = false
    }

    fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun performAction(actionType: String) {
        when (actionType) {
            TilePreferences.ACTION_SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            }
            TilePreferences.ACTION_LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
            TilePreferences.ACTION_POWER_DIALOG -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
        }
    }
}
