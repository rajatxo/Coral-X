package com.rajatxo.coral

import android.app.Application
import com.rajatxo.coral.crash.CrashHandler

/**
 * Coral X — Application entry point.
 *
 * Wires in the fundamental infrastructure:
 *   - CrashHandler (global uncaught exception handler)
 *
 * Additional systems will be added here as we build them:
 *   - SongCoverManager.init()
 *   - TextColorStyleManager.init()
 *   - Coil image loader configuration
 *   - MediaSession setup
 */
class CoralApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // ★ Global crash handler — catches ALL uncaught exceptions,
        // writes a crash log to cacheDir/crashes/, and lets the default
        // handler finish (so the user sees the standard "app crashed" dialog).
        CrashHandler.install(this)
    }
}
