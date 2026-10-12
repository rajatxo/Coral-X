package com.rajatxo.coral

import android.app.Application
import com.rajatxo.coral.crash.CrashHandler
import com.rajatxo.coral.data.prefs.CrossfadeManager
import com.rajatxo.coral.data.prefs.PlaybackHistory
import com.rajatxo.coral.data.prefs.SectionToggleManager
import com.rajatxo.coral.data.prefs.SpeedDialModeManager
import com.rajatxo.coral.data.prefs.SpeedDialPinStore
import com.rajatxo.coral.data.prefs.UpdateChannelManager

class CoralApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)

        // ★ Init all preference stores — must happen before any screen reads them
        SectionToggleManager.init(this)
        UpdateChannelManager.init(this)
        CrossfadeManager.init(this)
        SpeedDialPinStore.init(this)
        SpeedDialModeManager.init(this)
        PlaybackHistory.init(this)
    }
}
