package com.artic.cqt

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object ActionHandler {

    fun showToast(context: Context, message: String) {
        // Route through Accessibility Service if available for better background reliability
        if (CQTAccessibilityService.isConnected.value) {
            CQTAccessibilityService.instance?.showToast(message)
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Executes the requested action.
     * @param launcher A function to handle Intent launches (e.g., startActivityAndCollapse for tiles)
     */
    fun execute(
        context: Context, 
        actionType: String, 
        actionValue: String = "", 
        launcher: ((Intent) -> Unit)? = null
    ) {
        var successMessage: String? = null
        
        when (actionType) {
            TilePreferences.ACTION_OPEN_APP -> {
                if (actionValue.isNotEmpty()) {
                    val intent = if (actionValue.contains("/")) {
                        val parts = actionValue.split("/")
                        val pkg = parts[0]
                        val cls = if (parts[1].startsWith(".")) pkg + parts[1] else parts[1]
                        Intent().setComponent(ComponentName(pkg, cls))
                    } else {
                        context.packageManager.getLaunchIntentForPackage(actionValue)
                    }
                    
                    intent?.let { 
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (launcher != null) {
                            launcher(it)
                        } else {
                            context.startActivity(it)
                        }
                        successMessage = "Opening app..."
                    }
                }
            }

            TilePreferences.ACTION_OPEN_URL -> {
                var url = actionValue
                if (url.isNotEmpty()) {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (launcher != null) {
                        launcher(intent)
                    } else {
                        context.startActivity(intent)
                    }
                    successMessage = "Opening URL..."
                }
            }

            TilePreferences.ACTION_FLASHLIGHT -> {
                val enabled = actionValue == "true"
                toggleFlashlight(context, enabled)
                successMessage = if (enabled) "Flashlight On" else "Flashlight Off"
            }

            TilePreferences.ACTION_SCREENSHOT,
            TilePreferences.ACTION_LOCK_SCREEN,
            TilePreferences.ACTION_POWER_DIALOG -> {
                if (CQTAccessibilityService.isConnected.value) {
                    CQTAccessibilityService.instance?.performAction(actionType)
                    // No toast here as the action itself is visible
                } else {
                    val shellCmd = when (actionType) {
                        TilePreferences.ACTION_SCREENSHOT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) "input keyevent 120" else "screencap -p /sdcard/screenshot.png"
                        TilePreferences.ACTION_LOCK_SCREEN -> "input keyevent 26"
                        TilePreferences.ACTION_POWER_DIALOG -> "input keyevent 26 --longpress"
                        else -> ""
                    }
                    if (shellCmd.isNotEmpty()) {
                        RootHelper.runCommand(shellCmd)
                        successMessage = "Triggered via Shell"
                    }
                }
            }

            TilePreferences.ACTION_REBOOT -> {
                RootHelper.runCommand("reboot")
                successMessage = "Rebooting..."
            }
            TilePreferences.ACTION_REBOOT_RECOVERY -> {
                RootHelper.runCommand("reboot recovery")
                successMessage = "Rebooting to Recovery..."
            }
            TilePreferences.ACTION_SHELL_COMMAND -> {
                if (actionValue.isNotEmpty()) {
                    RootHelper.runCommand(actionValue)
                    successMessage = "Command executed"
                }
            }
            
            TilePreferences.ACTION_ADB_WIFI -> {
                val active = actionValue == "true"
                val port = if (active) "5555" else "-1"
                RootHelper.runCommand("setprop service.adb.tcp.port $port && stop adbd && start adbd")
                successMessage = if (active) "Wireless ADB Enabled" else "Wireless ADB Disabled"
            }

            TilePreferences.ACTION_KILL_APP -> {
                val pkg = actionValue.ifEmpty { "\$(dumpsys activity activities | grep mResumedActivity | cut -d '{' -f2 | cut -d '/' -f1 | cut -d ' ' -f3)" }
                RootHelper.runCommand("am force-stop $pkg")
                successMessage = "App killed"
            }

            TilePreferences.ACTION_CLEAR_DATA -> {
                if (actionValue.isNotEmpty()) {
                    val pkg = if (actionValue.contains("/")) actionValue.split("/")[0] else actionValue
                    RootHelper.runCommand("pm clear $pkg")
                    successMessage = "Data cleared for $pkg"
                }
            }

            TilePreferences.ACTION_MOBILE_DATA -> {
                val state = if (actionValue == "true") "enable" else "disable"
                RootHelper.runCommand("svc data $state")
                successMessage = "Mobile Data $state"
            }

            TilePreferences.ACTION_NFC -> {
                val state = if (actionValue == "true") "enable" else "disable"
                RootHelper.runCommand("svc nfc $state")
                successMessage = "NFC $state"
            }
        }

        successMessage?.let { showToast(context, it) }
    }

    private fun toggleFlashlight(context: Context, enabled: Boolean) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, enabled)
        } catch (e: Exception) { e.printStackTrace() }
    }
}
