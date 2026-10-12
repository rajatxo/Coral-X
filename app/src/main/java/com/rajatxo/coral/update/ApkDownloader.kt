package com.rajatxo.coral.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * ★ ApkDownloader — downloads a release APK straight from GitHub Releases
 * and launches the Android system package installer when done.
 *
 * Flow:
 *   Idle → Downloading(progress%) → Ready → Install intent fired
 *                                       ↘ Error
 *
 * The APK lands in [Context.getExternalCacheDir]/coral_update.apk.
 * A FileProvider shares that file with the system installer.
 *
 * This avoids the user needing Telegram at all — the only external dependency
 * is a working internet connection.
 */
object ApkDownloader {

    private const val TAG = "ApkDownloader"
    private const val APK_FILENAME = "coral_update.apk"

    /** Current UI-visible state of any in-flight download. */
    sealed class State {
        object Idle : State()
        data class Downloading(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : State()
        data class Ready(val file: File) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var currentJob: Job? = null

    /**
     * Start (or restart) a download. Cancels any in-flight job first.
     */
    fun start(context: Context, url: String) {
        currentJob?.cancel()
        currentJob = CoroutineScope(Dispatchers.IO).launch {
            downloadInternal(context.applicationContext, url)
        }
    }

    /** User dismissed the dialog / cancelled the download. */
    fun cancel() {
        currentJob?.cancel()
        currentJob = null
        _state.value = State.Idle
    }

    private suspend fun downloadInternal(context: Context, url: String) {
        try {
            val target = File(context.externalCacheDir, APK_FILENAME)
            // Wipe any previous (maybe partial) APK first
            if (target.exists()) target.delete()

            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Coral-Updater")
                connectTimeout = 15000
                readTimeout = 60000
                instanceFollowRedirects = true // GitHub release assets 302 → objects.githubusercontent.com
            }

            try {
                if (connection.responseCode !in 200..299) {
                    _state.value = State.Error("Server returned ${connection.responseCode}")
                    return
                }

                val total = connection.contentLengthLong.let { if (it > 0) it else 0L }
                var downloaded = 0L
                var lastReported = -1

                connection.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read

                            // Throttle state updates — don't emit every byte
                            val progress = if (total > 0) ((downloaded * 100) / total).toInt() else -1
                            if (progress != lastReported) {
                                lastReported = progress
                                _state.value = State.Downloading(progress, downloaded, total)
                            }
                        }
                    }
                }

                Log.d(TAG, "Downloaded ${target.length()} bytes to ${target.absolutePath}")
                _state.value = State.Ready(target)

                // Fire the system installer immediately
                launchInstaller(context, target)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _state.value = State.Error(e.message ?: "Download failed")
        }
    }

    /**
     * Launch Android's "Install app?" prompt for the cached APK.
     *
     * On Android 8+ the user will be asked to allow Coral to install unknown apps
     * the first time. Subsequent updates skip that prompt.
     */
    fun launchInstaller(context: Context, apk: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apk
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer", e)
            _state.value = State.Error("Could not open installer: ${e.message}")
        }
    }
}
