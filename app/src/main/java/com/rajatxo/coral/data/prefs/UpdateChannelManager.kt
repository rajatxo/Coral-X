package com.rajatxo.coral.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ★ UpdateChannelManager — user selects Stable or Nightly update channel.
 *
 * Stable channel:
 *   - Only gets notified when a stable release drops (v0.5.0, v0.6.0, v0.7.0)
 *   - Patch version is always 0 in the tag name
 *   - Slow, tested, reliable
 *
 * Nightly channel:
 *   - Gets notified on EVERY push (v0.5.1, v0.5.2, v0.5.3)
 *   - Patch version > 0 in the tag name
 *   - Frequent, may have bugs
 *
 * Version naming convention:
 *   Stable:  v0.5.0 → v0.6.0 → v0.7.0  (minor bump, patch=0)
 *   Nightly: v0.5.1 → v0.5.2 → v0.5.3  (patch increment between stable)
 *
 * When a new stable is ready (v0.6.0), Nightly users also see it
 * (because 0.6.0 > 0.5.3).
 */
object UpdateChannelManager {

    private const val PREFS_NAME = "coral_update_prefs"
    private const val KEY_CHANNEL = "update_channel"

    enum class UpdateChannel(val displayName: String) {
        STABLE("Stable"),
        NIGHTLY("Nightly")
    }

    private lateinit var prefs: android.content.SharedPreferences

    private val _channel = MutableStateFlow(UpdateChannel.STABLE)
    val channel: StateFlow<UpdateChannel> = _channel.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_CHANNEL, null)
        _channel.value = try {
            saved?.let { UpdateChannel.valueOf(it) } ?: UpdateChannel.STABLE
        } catch (_: Exception) {
            UpdateChannel.STABLE
        }
    }

    fun setChannel(channel: UpdateChannel) {
        _channel.value = channel
        prefs.edit().putString(KEY_CHANNEL, channel.name).apply()
    }
}
