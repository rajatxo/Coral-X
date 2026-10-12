package com.rajatxo.coral.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * ★ UpdateChecker — checks for new versions based on user's channel.
 *
 * CURRENT STATE (pre-Play-Store launch):
 *   - GitHub Releases no longer carry APK assets
 *   - This checker will query GitHub but find no APK to download
 *   - In-app updater is effectively paused until Play Store launch
 *
 * ROADMAP:
 *   - Once Coral ships on Google Play, swap this to use Play Core's
 *     In-App Updates API (AppUpdateManager). The UpdateDialog UI stays
 *     exactly the same — only the data source changes.
 *
 * Two channels (kept for UI continuity):
 *   STABLE  — only shows updates where patch=0 (v0.5.0, v0.6.0, v0.7.0)
 *   NIGHTLY — shows ALL updates (v0.5.1, v0.5.2, v0.6.0, etc.)
 *
 * Version naming convention:
 *   Stable:  v0.5.0, v0.6.0, v0.7.0  (minor.X.0 — patch always 0)
 *   Nightly: v0.5.1, v0.5.2, v0.5.3  (minor.X.patch — patch > 0)
 *
 * If a release has no APK asset attached, its downloadUrl will be empty
 * and the UpdateDialog will gracefully fall back to "Up to date".
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    private const val RELEASES_URL =
        "https://api.github.com/repos/rajatxo/Coral-X/releases?per_page=30"

    sealed class UpdateResult {
        data class UpdateAvailable(
            val versionName: String,
            val releaseNotes: String,
            val isStable: Boolean,
            /** Direct APK download URL from GitHub release asset */
            val downloadUrl: String,
            /** APK file size in bytes (for progress display), or 0 if unknown */
            val fileSize: Long
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

                // ★ GitHub API returns a plain JSON array [...], NOT {"releases": [...]}
                val releases = JSONArray(body)

                // Parse all releases
                data class Release(
                    val tagName: String,
                    val versionCode: Int,
                    val patch: Int,
                    val body: String,
                    val isStable: Boolean,
                    val apkUrl: String?,
                    val apkSize: Long
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

                    // ★ Find the APK asset attached to this release
                    var apkUrl: String? = null
                    var apkSize = 0L
                    val assets = release.optJSONArray("assets")
                    if (assets != null) {
                        for (j in 0 until assets.length()) {
                            val asset = assets.getJSONObject(j)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                apkUrl = asset.optString("browser_download_url", "")
                                if (apkUrl.isNullOrBlank()) apkUrl = null
                                apkSize = asset.optLong("size", 0L)
                                break
                            }
                        }
                    }

                    allReleases.add(Release(tagName, versionCode, patch, releaseBody, isStable, apkUrl, apkSize))
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

                Log.d(
                    TAG,
                    "Channel: $channel, Local: $currentVersionCode, Remote: ${latest.versionCode} (${latest.tagName}) APK: ${latest.apkUrl ?: "none"}"
                )

                if (latest.versionCode > currentVersionCode) {
                    UpdateResult.UpdateAvailable(
                        versionName = latest.tagName,
                        releaseNotes = stripReleaseNotes(latest.body),
                        isStable = latest.isStable,
                        downloadUrl = latest.apkUrl ?: "",
                        fileSize = latest.apkSize
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
     * Strip release notes to plain text — remove URLs, markdown, and Telegram links.
     * Users see only what changed, never any links or channel promotion.
     */
    private fun stripReleaseNotes(raw: String): String {
        var text = raw

        // Remove Telegram links (t.me/...)
        text = Regex("https?://t\\.me/\\S+").replace(text, "")
        // Remove GitHub links (github.com/...)
        text = Regex("https?://github\\.com/\\S+").replace(text, "")
        // Remove any remaining URLs
        text = Regex("https?://\\S+").replace(text, "")
        // Remove markdown image syntax ![alt](url)
        text = Regex("!\\[[^\\]]*\\]\\([^)]*\\)").replace(text, "")
        // Remove markdown links [text](url) → keep text
        text = Regex("\\[([^\\]]*)\\]\\([^)]*\\)").replace(text, "$1")
        // Remove markdown headings
        text = Regex("^#{1,6}\\s*", RegexOption.MULTILINE).replace(text, "")
        // Remove bold/italic markers
        text = Regex("[*_`~]").replace(text, "")
        // Collapse multiple blank lines
        text = Regex("\n{3,}").replace(text, "\n\n")
        // Collapse multiple spaces
        text = Regex("[ \\t]{2,}").replace(text, " ")
        return text.trim().ifBlank { "New version available." }
    }
}
