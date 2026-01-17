package com.artic.cqt

import android.content.Context
import android.content.SharedPreferences

class TilePreferences(context: Context, val tileId: Int = -1) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cqt_prefs", Context.MODE_PRIVATE)

    companion object {
        const val ACTION_NONE = "NONE"
        const val ACTION_TOGGLE = "TOGGLE"
        const val ACTION_OPEN_APP = "OPEN_APP"
        const val ACTION_OPEN_URL = "OPEN_URL"
        const val ACTION_FLASHLIGHT = "FLASHLIGHT"
        const val ACTION_SCREENSHOT = "SCREENSHOT"
        const val ACTION_LOCK_SCREEN = "LOCK_SCREEN"
        const val ACTION_POWER_DIALOG = "POWER_DIALOG"
        const val ACTION_REBOOT = "REBOOT"
        const val ACTION_REBOOT_RECOVERY = "REBOOT_RECOVERY"
        const val ACTION_SHELL_COMMAND = "SHELL_COMMAND"
        const val ACTION_ADB_WIFI = "ADB_WIFI"
        const val ACTION_KILL_APP = "KILL_APP"
        const val ACTION_CLEAR_DATA = "CLEAR_DATA"
        const val ACTION_MOBILE_DATA = "MOBILE_DATA"
        const val ACTION_NFC = "NFC"

        val actionOptions = listOf(
            ACTION_NONE to "None",
            ACTION_TOGGLE to "Simple Toggle",
            ACTION_FLASHLIGHT to "Flashlight / Torch",
            ACTION_OPEN_APP to "Open App / Activity",
            ACTION_OPEN_URL to "Open Web Page",
            ACTION_SCREENSHOT to "Take Screenshot",
            ACTION_LOCK_SCREEN to "Lock Screen",
            ACTION_POWER_DIALOG to "Power Menu",
            ACTION_REBOOT to "Reboot",
            ACTION_REBOOT_RECOVERY to "Reboot to Recovery",
            ACTION_SHELL_COMMAND to "Run Shell Command",
            ACTION_ADB_WIFI to "Wireless ADB (Toggle)",
            ACTION_KILL_APP to "Kill Foreground/Specific App",
            ACTION_CLEAR_DATA to "Clear App Data",
            ACTION_MOBILE_DATA to "Mobile Data (Toggle)",
            ACTION_NFC to "NFC (Toggle)"
        )

        val builtinIcons = listOf(
            "ic_flashlight" to R.drawable.ic_flashlight,
            "ic_screenshot" to R.drawable.ic_screenshot,
            "ic_wifi" to R.drawable.ic_wifi,
            "ic_reboot" to R.drawable.ic_reboot,
            "ic_rocket" to R.drawable.ic_rocket,
            "ic_power" to R.drawable.ic_power,
            "ic_bolt" to R.drawable.ic_bolt,
            "ic_speed" to R.drawable.ic_speed,
            "ic_star" to R.drawable.ic_star,
            "ic_gamepad" to R.drawable.ic_gamepad,
            "ic_launcher_foreground" to R.drawable.ic_launcher_foreground
        )

        private val iconMap = builtinIcons.toMap()

        fun getResIdByName(name: String): Int {
            return iconMap[name] ?: 0
        }

        fun getBuiltinIconRes(actionType: String): Int {
            return when (actionType) {
                ACTION_FLASHLIGHT -> R.drawable.ic_flashlight
                ACTION_SCREENSHOT -> R.drawable.ic_screenshot
                ACTION_ADB_WIFI -> R.drawable.ic_wifi
                ACTION_REBOOT -> R.drawable.ic_reboot
                ACTION_REBOOT_RECOVERY -> R.drawable.ic_rocket
                ACTION_POWER_DIALOG -> R.drawable.ic_power
                ACTION_LOCK_SCREEN -> R.drawable.ic_bolt
                ACTION_SHELL_COMMAND -> R.drawable.ic_speed
                ACTION_MOBILE_DATA -> R.drawable.ic_speed
                ACTION_NFC -> R.drawable.ic_star
                ACTION_TOGGLE -> R.drawable.ic_gamepad
                else -> R.drawable.ic_launcher_foreground
            }
        }
    }

    private fun key(suffix: String) = if (tileId == -1) "assistant_$suffix" else "tile_${tileId}_$suffix"

    var tileLabel: String
        get() = prefs.getString(key("label"), if (tileId == -1) "Assistant" else "Tile $tileId") ?: ""
        set(value) = prefs.edit().putString(key("label"), value).apply()

    var tileSubtitle: String
        get() = prefs.getString(key("subtitle"), "") ?: ""
        set(value) = prefs.edit().putString(key("subtitle"), value).apply()

    var tileActionType: String
        get() = prefs.getString("action_type_$tileId", ACTION_NONE) ?: ACTION_NONE
        set(value) = prefs.edit().putString("action_type_$tileId", value).apply()

    var tileActionValue: String
        get() = prefs.getString("action_value_$tileId", "") ?: ""
        set(value) = prefs.edit().putString("action_value_$tileId", value).apply()

    var iconType: String
        get() = prefs.getString("icon_type_$tileId", "DEFAULT") ?: "DEFAULT"
        set(value) = prefs.edit().putString("icon_type_$tileId", value).apply()

    var iconValue: String
        get() = prefs.getString("icon_value_$tileId", "") ?: ""
        set(value) = prefs.edit().putString("icon_value_$tileId", value).apply()

    var isActive: Boolean
        get() = prefs.getBoolean("is_active_$tileId", false)
        set(value) = prefs.edit().putBoolean("is_active_$tileId", value).apply()

    var useAnimation: Boolean
        get() = prefs.getBoolean("use_animation_$tileId", true)
        set(value) = prefs.edit().putBoolean("use_animation_$tileId", value).apply()
}
