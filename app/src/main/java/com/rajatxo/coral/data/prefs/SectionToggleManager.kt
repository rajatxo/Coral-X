package com.rajatxo.coral.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ★ SectionToggleManager — controls which sections appear on Quick Picks.
 *
 * User can enable/disable each section in Settings.
 * All sections default to ON.
 *
 * Sections:
 *   - GREETING_MOOD: "Hey, {name}" + time-based mood text
 *   - TODAYS_TOP: 3 capsule stories with top-played-today songs
 *   - SPEED_DIAL: 3x3 paginated grid of quick-access songs
 *   - RECENT: horizontal scroll of recently added songs
 *   - MORE_PICKS: horizontal scroll of more song suggestions
 */
object SectionToggleManager {
    private const val PREFS_NAME = "coral_section_toggles"
    private const val KEY_GREETING = "section_greeting"
    private const val KEY_TODAYS_TOP = "section_todays_top"
    private const val KEY_SPEED_DIAL = "section_speed_dial"
    private const val KEY_RECENT = "section_recent"
    private const val KEY_MORE_PICKS = "section_more_picks"

    private lateinit var prefs: android.content.SharedPreferences

    private val _greetingEnabled = MutableStateFlow(true)
    val greetingEnabled: StateFlow<Boolean> = _greetingEnabled.asStateFlow()

    private val _todaysTopEnabled = MutableStateFlow(true)
    val todaysTopEnabled: StateFlow<Boolean> = _todaysTopEnabled.asStateFlow()

    private val _speedDialEnabled = MutableStateFlow(true)
    val speedDialEnabled: StateFlow<Boolean> = _speedDialEnabled.asStateFlow()

    private val _recentEnabled = MutableStateFlow(true)
    val recentEnabled: StateFlow<Boolean> = _recentEnabled.asStateFlow()

    private val _morePicksEnabled = MutableStateFlow(true)
    val morePicksEnabled: StateFlow<Boolean> = _morePicksEnabled.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _greetingEnabled.value = prefs.getBoolean(KEY_GREETING, true)
        _todaysTopEnabled.value = prefs.getBoolean(KEY_TODAYS_TOP, true)
        _speedDialEnabled.value = prefs.getBoolean(KEY_SPEED_DIAL, true)
        _recentEnabled.value = prefs.getBoolean(KEY_RECENT, true)
        _morePicksEnabled.value = prefs.getBoolean(KEY_MORE_PICKS, true)
    }

    fun setGreetingEnabled(enabled: Boolean) {
        _greetingEnabled.value = enabled
        prefs.edit().putBoolean(KEY_GREETING, enabled).apply()
    }
    fun setTodaysTopEnabled(enabled: Boolean) {
        _todaysTopEnabled.value = enabled
        prefs.edit().putBoolean(KEY_TODAYS_TOP, enabled).apply()
    }
    fun setSpeedDialEnabled(enabled: Boolean) {
        _speedDialEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SPEED_DIAL, enabled).apply()
    }
    fun setRecentEnabled(enabled: Boolean) {
        _recentEnabled.value = enabled
        prefs.edit().putBoolean(KEY_RECENT, enabled).apply()
    }
    fun setMorePicksEnabled(enabled: Boolean) {
        _morePicksEnabled.value = enabled
        prefs.edit().putBoolean(KEY_MORE_PICKS, enabled).apply()
    }
}
