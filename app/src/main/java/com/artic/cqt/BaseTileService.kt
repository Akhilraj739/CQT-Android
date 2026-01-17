package com.artic.cqt

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

abstract class BaseTileService(private val tileId: Int) : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val prefs = TilePreferences(this, tileId)
        val tile = qsTile ?: return

        tile.label = prefs.tileLabel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = prefs.tileSubtitle
        }

        // Set the icon
        when (prefs.iconType) {
            "GALLERY" -> {
                if (prefs.iconValue.isNotEmpty()) {
                    try {
                        tile.icon = Icon.createWithContentUri(Uri.parse(prefs.iconValue))
                    } catch (e: Exception) {
                        tile.icon = Icon.createWithResource(this, TilePreferences.getBuiltinIconRes(prefs.tileActionType))
                    }
                } else {
                    tile.icon = Icon.createWithResource(this, TilePreferences.getBuiltinIconRes(prefs.tileActionType))
                }
            }
            "BUILTIN" -> {
                val resId = resources.getIdentifier(prefs.iconValue, "drawable", packageName)
                if (resId != 0) {
                    tile.icon = Icon.createWithResource(this, resId)
                } else {
                    tile.icon = Icon.createWithResource(this, TilePreferences.getBuiltinIconRes(prefs.tileActionType))
                }
            }
            else -> {
                // DEFAULT case: Use the action-specific icon if available
                tile.icon = Icon.createWithResource(this, TilePreferences.getBuiltinIconRes(prefs.tileActionType))
            }
        }
        
        tile.state = when (prefs.tileActionType) {
            TilePreferences.ACTION_NONE -> Tile.STATE_UNAVAILABLE
            TilePreferences.ACTION_TOGGLE, 
            TilePreferences.ACTION_FLASHLIGHT,
            TilePreferences.ACTION_ADB_WIFI,
            TilePreferences.ACTION_MOBILE_DATA,
            TilePreferences.ACTION_NFC -> {
                if (prefs.isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            }
            else -> Tile.STATE_INACTIVE
        }
        
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val prefs = TilePreferences(this, tileId)
        
        var toggleValue = ""
        if (prefs.tileActionType == TilePreferences.ACTION_TOGGLE || 
            prefs.tileActionType == TilePreferences.ACTION_FLASHLIGHT ||
            prefs.tileActionType == TilePreferences.ACTION_ADB_WIFI ||
            prefs.tileActionType == TilePreferences.ACTION_MOBILE_DATA ||
            prefs.tileActionType == TilePreferences.ACTION_NFC) {
            
            prefs.isActive = !prefs.isActive
            toggleValue = prefs.isActive.toString()
            updateTileState(prefs.isActive)
        }

        ActionHandler.execute(
            context = this,
            actionType = prefs.tileActionType,
            actionValue = if (toggleValue.isNotEmpty()) toggleValue else prefs.tileActionValue,
            launcher = { intent -> startActivityUnified(intent) }
        )
    }

    private fun updateTileState(active: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    private fun startActivityUnified(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
