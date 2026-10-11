package com.rajatxo.coral

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.rajatxo.coral.theme.CoralXTheme
import com.rajatxo.coral.ui.foundation.CoralXScaffold
import com.rajatxo.coral.ui.screens.PlaylistsScreen
import com.rajatxo.coral.ui.screens.QuickPicksScreen
import com.rajatxo.coral.ui.screens.SongsScreen
import com.rajatxo.coral.ui.util.CoralTab
import com.rajatxo.coral.update.UpdateChecker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { CoralXTheme { CoralXRoot() } }
    }
}

@Composable
private fun CoralXRoot() {
    LaunchedEffect(Unit) { UpdateChecker.checkForUpdate(BuildConfig.VERSION_CODE) }
    var activeTab by remember { mutableStateOf(CoralTab.QuickPicks) }
    val title = when (activeTab) { CoralTab.QuickPicks -> "Quick picks"; CoralTab.Songs -> "Songs"; CoralTab.Playlists -> "Playlists" }
    CoralXScaffold(title, activeTab, onTabSelected = { activeTab = it }) {
        when (activeTab) { CoralTab.QuickPicks -> QuickPicksScreen(); CoralTab.Songs -> SongsScreen(); CoralTab.Playlists -> PlaylistsScreen() }
    }
}
