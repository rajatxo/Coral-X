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
 * ★ UpdateChecker — checks for new versions and redirects to Telegram.
 *
 * Strategy:
 *   1. Checks GitHub Releases API for the latest release tag
 *   2. Compares version codes
 *   3. If update available → shows dialog with "Download" button
 *   4. "Download" button opens the Telegram support group
 *      (https://t.me/coralmusicsupport) where APKs are posted
 *
 * This keeps the project closed-source — users never see the repo,
 * they just download APKs from Telegram.
 *
 * The Telegram link is hardcoded so even if GitHub is down, the
 * Telegram group always works as the distribution channel.
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val RELEASES_URL =
        "https://api.github.com/repos/rajatxo/Coral-X/releases/latest"

    /**
     * ★ Telegram support group — where APKs are posted for download.
     * Users tap "Download" → this opens in browser/Telegram app.
     */
    const val TELEGRAM_DOWNLOAD_URL = "https://t.me/coralmusicsupport"

    /**
     * Result of an update check.
     */
    sealed class UpdateResult {
        data class UpdateAvailable(
            val versionName: String,
            val releaseNotes: String
        ) : UpdateResult()

        object UpToDate : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }

    /**
     * Check GitHub for the latest release.
     * Must be called on a background thread.
     */
    suspend fun checkForUpdate(currentVersionCode: Int): UpdateResult {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(RELEASES_URL).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "CoralX-UpdateChecker")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val responseCode = connection.responseCode
                if (responseCode != 200) {
                    return@withContext UpdateResult.Error("GitHub API returned $responseCode")
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "")
                val releaseBody = json.optString("body", "New version available.")

                val remoteVersionCode = parseVersionCodeFromTag(tagName)

                Log.d(TAG, "Local: $currentVersionCode, Remote: $remoteVersionCode ($tagName)")

                if (remoteVersionCode > currentVersionCode) {
                    UpdateResult.UpdateAvailable(
                        versionName = tagName,
                        releaseNotes = releaseBody
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

    private fun parseVersionCodeFromTag(tag: String): Int {
        val cleanTag = tag.removePrefix("v").removePrefix("V")
        val parts = cleanTag.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return major * 10000 + minor * 100 + patch
    }

    /**
     * Open the Telegram support group in browser/Telegram app.
     * This is where users download the latest APK.
     */
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
