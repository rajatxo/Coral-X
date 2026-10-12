package com.rajatxo.coral

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.rajatxo.coral.theme.CoralXTheme
import com.rajatxo.coral.ui.foundation.CoralXScaffold
import com.rajatxo.coral.ui.screens.PermissionScreen
import com.rajatxo.coral.ui.screens.PlaylistsScreen
import com.rajatxo.coral.ui.screens.QuickPicksScreen
import com.rajatxo.coral.ui.screens.SettingsScreen
import com.rajatxo.coral.ui.screens.SongsScreen
import com.rajatxo.coral.ui.util.CoralTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { CoralXTheme { CoralXRoot() } }
    }

    /**
     * Check if the mandatory permissions (notifications + music) are granted.
     * Used to decide whether to show PermissionScreen or the main app.
     */
    internal fun hasRequiredPermissions(): Boolean {
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true

        val music = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

        return notif && music
    }
}

@Composable
private fun CoralXRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current as android.app.Activity
    var permissionsGranted by remember {
        mutableStateOf(
            (context as MainActivity).hasRequiredPermissions()
        )
    }
    var showSettings by remember { mutableStateOf(false) }

    if (!permissionsGranted) {
        PermissionScreen(
            onPermissionsGranted = { permissionsGranted = true }
        )
        return
    }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    var activeTab by remember { mutableStateOf(CoralTab.QuickPicks) }
    val title = when (activeTab) {
        CoralTab.QuickPicks -> "Quick picks"
        CoralTab.Songs -> "Songs"
        CoralTab.Playlists -> "Playlists"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CoralXScaffold(
            title = title,
            activeTab = activeTab,
            onTabSelected = { activeTab = it },
            onSettingsClick = { showSettings = true }
        ) {
            when (activeTab) {
                CoralTab.QuickPicks -> QuickPicksScreen()
                CoralTab.Songs -> SongsScreen()
                CoralTab.Playlists -> PlaylistsScreen()
            }
        }
    }
}
