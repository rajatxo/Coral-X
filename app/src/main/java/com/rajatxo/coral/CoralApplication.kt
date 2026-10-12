package com.rajatxo.coral

import android.app.Application
import com.rajatxo.coral.crash.CrashHandler
import com.rajatxo.coral.data.prefs.SectionToggleManager
import com.rajatxo.coral.data.prefs.UpdateChannelManager

class CoralApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        SectionToggleManager.init(this)
        UpdateChannelManager.init(this)
    }
}
