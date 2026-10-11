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
 * ★ UpdateChecker — checks GitHub Releases for new versions.
 *
 * Since Coral X is distributed via Telegram (not Play Store), we need our own
 * update mechanism. This checks the GitHub Releases API for the latest release
 * and compares the version code.
 *
 * Flow:
 *   1. App starts → UpdateChecker.checkForUpdate(context)
 *   2. Fetches https://api.github.com/repos/rajatxo/Coral-X/releases/latest
 *   3. Parses the release tag_name (e.g., "v0.1.0") and body (release notes)
 *   4. Compares with the current versionCode
 *   5. Returns UpdateResult (UpdateAvailable / UpToDate / Error)
 *
 * The UI layer (when built) will show an update dialog based on UpdateResult.
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val RELEASES_URL =
        "https://api.github.com/repos/rajatxo/Coral-X/releases/latest"

    /**
     * Result of an update check.
     */
    sealed class UpdateResult {
        /** A new version is available. */
        data class UpdateAvailable(
            val versionName: String,
            val downloadUrl: String,
            val releaseNotes: String,
            val htmlUrl: String
        ) : UpdateResult()

        /** App is up to date. */
        object UpToDate : UpdateResult()

        /** Check failed (network error, parse error, etc.). */
        data class Error(val message: String) : UpdateResult()
    }

    /**
     * Check GitHub for the latest release.
     * Must be called on a background thread (IO dispatcher).
     *
     * @param currentVersionCode The app's current versionCode (from BuildConfig)
     * @return UpdateResult
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
                    return@withContext UpdateResult.Error(
                        "GitHub API returned $responseCode"
                    )
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "")  // e.g., "v0.1.0"
                val htmlUrl = json.optString("html_url", "")
                val releaseBody = json.optString("body", "No release notes.")

                // Extract version code from tag name (v0.1.0 → parse from versionName)
                // We use the release's assets to find the APK download URL
                val assets = json.optJSONArray("assets")
                var downloadUrl = htmlUrl  // fallback to release page
                if (assets != null && assets.length() > 0) {
                    val firstAsset = assets.getJSONObject(0)
                    downloadUrl = firstAsset.optString("browser_download_url", htmlUrl)
                }

                // Parse version code from tag name
                // Tag format: "v0.1.0" → versionName "0.1.0"
                // We compare version codes, so we need to extract a numeric code
                val remoteVersionCode = parseVersionCodeFromTag(tagName)

                Log.d(TAG, "Local: $currentVersionCode, Remote: $remoteVersionCode ($tagName)")

                if (remoteVersionCode > currentVersionCode) {
                    UpdateResult.UpdateAvailable(
                        versionName = tagName,
                        downloadUrl = downloadUrl,
                        releaseNotes = releaseBody,
                        htmlUrl = htmlUrl
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

    /**
     * Parse a version code from a GitHub release tag name.
     * Tag format: "v0.1.0" → versionCode 1
     * Tag format: "v1.2.3" → versionCode 10203
     *
     * Formula: major * 10000 + minor * 100 + patch
     * This gives unique codes up to version 99.99.99
     */
    private fun parseVersionCodeFromTag(tag: String): Int {
        val cleanTag = tag.removePrefix("v").removePrefix("V")
        val parts = cleanTag.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return major * 10000 + minor * 100 + patch
    }

    /**
     * Open the download URL in the system browser.
     * (Downloads the APK or opens the GitHub release page)
     */
    fun openDownloadUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open download URL", e)
        }
    }
}
