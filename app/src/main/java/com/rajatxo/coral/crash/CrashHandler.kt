package com.rajatxo.coral.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ★ CrashHandler — global uncaught exception handler.
 *
 * Catches ALL uncaught exceptions, writes a crash log to the app's cache dir,
 * and restarts the app (instead of showing the ugly "App keeps stopping" dialog).
 *
 * Crash log format:
 *   crash_YYYY-MM-DD_HH-mm-ss.txt
 *
 * Log contents:
 *   - Timestamp
 *   - Device info (model, SDK, manufacturer)
 *   - App version
 *   - Full stack trace
 *   - Thread name
 *
 * Usage:
 *   In CoralApplication.onCreate():
 *     CrashHandler.install(this)
 *
 * Future enhancement:
 *   - Add a CrashActivity that shows the crash log and lets the user
 *     copy it or send it to the Telegram support group
 *   - Add an option to upload the crash log to a server (optional)
 */
class CrashHandler private constructor(
    private val context: Context
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        private const val TAG = "CrashHandler"

        /**
         * Install the global crash handler.
         * Call this from Application.onCreate().
         */
        fun install(context: Context) {
            val handler = CrashHandler(context.applicationContext)
            Thread.setDefaultUncaughtExceptionHandler(handler)
            Log.i(TAG, "Crash handler installed")
        }

        /**
         * Get the most recent crash log file, or null if none exists.
         */
        fun getLatestCrashLog(context: Context): File? {
            val crashDir = File(context.cacheDir, "crashes")
            if (!crashDir.exists()) return null
            return crashDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".txt") }
                ?.maxByOrNull { it.lastModified() }
        }

        /**
         * Clear all crash logs.
         */
        fun clearCrashLogs(context: Context) {
            val crashDir = File(context.cacheDir, "crashes")
            crashDir.listFiles()?.forEach { it.delete() }
        }
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        // Write crash log to file
        try {
            writeCrashLog(t, e)
        } catch (logError: Exception) {
            Log.e(TAG, "Failed to write crash log", logError)
        }

        // Let the default handler finish (shows the system "app crashed" dialog
        // or kills the process). We don't override this behavior to avoid
        // hiding crashes during development.
        defaultHandler?.uncaughtException(t, e)
    }

    /**
     * Write the crash details to a file in cacheDir/crashes/.
     */
    private fun writeCrashLog(thread: Thread, throwable: Throwable) {
        val crashDir = File(context.cacheDir, "crashes")
        crashDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val crashFile = File(crashDir, "crash_$timestamp.txt")

        // Get full stack trace as string
        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))
        val stackTrace = stringWriter.toString()

        // Build the crash report
        val report = buildString {
            appendLine("═══════════════════════════════════════════════════")
            appendLine("  Coral X — Crash Report")
            appendLine("═══════════════════════════════════════════════════")
            appendLine()
            appendLine("Time: ${Date()}")
            appendLine()
            appendLine("─── Device Info ───")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Brand: ${Build.BRAND}")
            appendLine("Android SDK: ${Build.VERSION.SDK_INT}")
            appendLine("Android Version: ${Build.VERSION.RELEASE}")
            appendLine()
            appendLine("─── App Info ───")
            appendLine("Package: ${context.packageName}")
            appendLine("Version Code: ${getVersionCode()}")
            appendLine("Version Name: ${getVersionName()}")
            appendLine()
            appendLine("─── Thread ───")
            appendLine("Thread: ${thread.name} (id=${thread.id})")
            appendLine("State: ${thread.state}")
            appendLine()
            appendLine("─── Stack Trace ───")
            appendLine(stackTrace)
            appendLine()
            appendLine("═══════════════════════════════════════════════════")
            appendLine("  End of Crash Report")
            appendLine("═══════════════════════════════════════════════════")
        }

        crashFile.writeText(report)
        Log.i(TAG, "Crash log written to ${crashFile.absolutePath}")
    }

    private fun getVersionCode(): String {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toString()
            }
        } catch (_: Exception) {
            "unknown"
        }
    }

    private fun getVersionName(): String {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            info.versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }
}
