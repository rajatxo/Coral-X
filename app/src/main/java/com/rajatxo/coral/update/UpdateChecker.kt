package com.rajatxo.coral.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * ★ UpdateChecker — checks for new versions based on user's channel.
 *
 * Two channels:
 *   STABLE  — only shows updates where patch=0 (v0.5.0, v0.6.0, v0.7.0)
 *   NIGHTLY — shows ALL updates (v0.5.1, v0.5.2, v0.6.0, etc.)
 *
 * Version naming convention:
 *   Stable:  v0.5.0, v0.6.0, v0.7.0  (minor.X.0 — patch always 0)
 *   Nightly: v0.5.1, v0.5.2, v0.5.3  (minor.X.patch — patch > 0)
 *
 * So the logic is:
 *   - Fetch ALL releases from GitHub
 *   - If STABLE: filter to only releases where patch=0, pick the latest
 *   - If NIGHTLY: pick the latest release (any patch version)
 *   - Compare with current versionCode
 *
 * Download always opens Telegram group (closed-source distribution).
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val RELEASES_URL =
        "https://api.github.com/repos/rajatxo/Coral-X/releases"

    const val TELEGRAM_DOWNLOAD_URL = "https://t.me/coralmusicsupport"

    sealed class UpdateResult {
        data class UpdateAvailable(
            val versionName: String,
            val releaseNotes: String,
            val isStable: Boolean
        ) : UpdateResult()

        object UpToDate : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }

    /**
     * Check for updates based on the user's channel preference.
     *
     * @param currentVersionCode The app's current versionCode
     * @param channel STABLE (only patch=0 releases) or NIGHTLY (all releases)
     */
    suspend fun checkForUpdate(
        currentVersionCode: Int,
        channel: com.rajatxo.coral.data.prefs.UpdateChannelManager.UpdateChannel
    ): UpdateResult {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(RELEASES_URL).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "Coral-UpdateChecker")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                if (connection.responseCode != 200) {
                    return@withContext UpdateResult.Error("GitHub API returned ${connection.responseCode}")
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val json = JSONObject(body)
                val releases = json.optJSONArray("releases") ?: return@withContext UpdateResult.Error("No releases found")

                // Parse all releases
                data class Release(
                    val tagName: String,
                    val versionCode: Int,
                    val patch: Int,
                    val body: String,
                    val isStable: Boolean
                )

                val allReleases = mutableListOf<Release>()
                for (i in 0 until releases.length()) {
                    val release = releases.getJSONObject(i)
                    val tagName = release.optString("tag_name", "")
                    val releaseBody = release.optString("body", "New version available.")
                    val parts = tagName.removePrefix("v").removePrefix("V").split(".")
                    val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
                    val versionCode = major * 10000 + minor * 100 + patch
                    val isStable = (patch == 0)

                    allReleases.add(Release(tagName, versionCode, patch, releaseBody, isStable))
                }

                if (allReleases.isEmpty()) {
                    return@withContext UpdateResult.UpToDate
                }

                // ★ Filter based on channel
                val filteredReleases = when (channel) {
                    com.rajatxo.coral.data.prefs.UpdateChannelManager.UpdateChannel.STABLE -> {
                        // Only stable releases (patch=0)
                        allReleases.filter { it.isStable }
                    }
                    com.rajatxo.coral.data.prefs.UpdateChannelManager.UpdateChannel.NIGHTLY -> {
                        // All releases (including nightly)
                        allReleases
                    }
                }

                if (filteredReleases.isEmpty()) {
                    return@withContext UpdateResult.UpToDate
                }

                // Pick the latest
                val latest = filteredReleases.maxByOrNull { it.versionCode }
                    ?: return@withContext UpdateResult.UpToDate

                Log.d(TAG, "Channel: $channel, Local: $currentVersionCode, Remote: ${latest.versionCode} (${latest.tagName})")

                if (latest.versionCode > currentVersionCode) {
                    UpdateResult.UpdateAvailable(
                        versionName = latest.tagName,
                        releaseNotes = latest.body,
                        isStable = latest.isStable
                    )
                } else {
                    UpdateResult.UpToDate
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
                UpdateResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun openTelegramDownload(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_DOWNLOAD_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Telegram", e)
        }
    }
}
