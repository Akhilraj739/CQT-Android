package com.artic.cqt

import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.os.Bundle
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.Toast
import java.io.OutputStream
import android.content.ContentValues
import android.os.Build
import android.content.Context
import android.content.Intent

class CQTVoiceService : VoiceInteractionService()

class CQTVoiceSessionService : android.service.voice.VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return CQTVoiceSession(this)
    }
}

class CQTVoiceSession(context: Context) : VoiceInteractionSession(context) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        
        val prefs = TilePreferences(context, -1)
        val actionType = prefs.tileActionType
        
        if (actionType == TilePreferences.ACTION_SCREENSHOT) {
            // We want the system to handle the screenshot. 
            // The session is shown, and if "Use screenshot" is enabled in system settings,
            // onHandleScreenshot will be called shortly.
            Toast.makeText(context, "Assistant: Taking Screenshot...", Toast.LENGTH_SHORT).show()
        } else if (actionType != TilePreferences.ACTION_NONE) {
            // Bridge to AssistantActivity for other custom actions
            val intent = Intent(context, AssistantActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            hide()
        } else {
            // Default behavior: Open the app
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            hide()
        }
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        if (screenshot != null) {
            saveScreenshot(screenshot)
            Toast.makeText(context, "Screenshot Saved to Gallery", Toast.LENGTH_SHORT).show()
        } else {
            // Fallback: If Assistant Bitmap is null, try the Accessibility action if available
            val accessibility = CQTAccessibilityService.instance
            if (accessibility != null) {
                accessibility.performAction(TilePreferences.ACTION_SCREENSHOT)
            } else {
                Toast.makeText(context, "Screenshot failed. Please enable 'Use Screenshot' in Google Assistant settings or enable CQT Accessibility Service.", Toast.LENGTH_LONG).show()
            }
        }
        hide()
    }

    private fun saveScreenshot(bitmap: Bitmap) {
        val filename = "CQT_${System.currentTimeMillis()}.jpg"
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CQT_Screenshots")
            }
        }

        try {
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            val fos: OutputStream? = imageUri?.let { resolver.openOutputStream(it) }
            fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
