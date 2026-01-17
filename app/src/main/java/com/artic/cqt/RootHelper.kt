package com.artic.cqt

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.DataOutputStream
import java.io.IOException

object RootHelper {

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: IllegalStateException) {
            false
        }
    }

    fun runCommand(command: String): Boolean {
        if (isShizukuAvailable() && hasShizukuPermission()) {
            return runShizukuCommand(command)
        }
        return runSuCommand(command)
    }

    private fun runSuCommand(command: String): Boolean {
        var process: Process? = null
        var os: DataOutputStream? = null
        return try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (_: IOException) {
            false
        } catch (_: InterruptedException) {
            false
        } finally {
            try {
                os?.close()
                process?.destroy()
            } catch (_: Exception) {
            }
        }
    }

    private fun runShizukuCommand(command: String): Boolean {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as ShizukuRemoteProcess
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }
}
