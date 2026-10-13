package com.rajatxo.coral

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.rajatxo.coral.playback.PlaybackConnection
import com.rajatxo.coral.theme.CoralXTheme
import com.rajatxo.coral.ui.foundation.CoralXScaffold
import com.rajatxo.coral.ui.player.FullPlayer
import com.rajatxo.coral.ui.player.MiniPlayer
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
    val context = LocalContext.current
    var permissionsGranted by remember {
        mutableStateOf((context as MainActivity).hasRequiredPermissions())
    }
    var showSettings by remember { mutableStateOf(false) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var customizationTitle by remember { mutableStateOf("") }
    var showCustomization by remember { mutableStateOf(false) }
    // ★ Cynthia customization mode — which component to customize
    var customizationMode by remember {
        mutableStateOf<com.rajatxo.coral.ui.cynthia.CustomizationMode?>(null)
    }

    // ★ TodaysTopCard state — hoisted OUTSIDE the scaffold (like old Coral).
    // The card is a SIBLING of the scaffold content, so drawBackdrop samples
    // the content behind it — no recursion, no crash.
    var topCardSong by remember { mutableStateOf<com.rajatxo.coral.domain.model.Song?>(null) }
    var showTopCard by remember { mutableStateOf(false) }

    // ★ Capture the scaffold's glass backdrop so TodaysTopCard (rendered outside
    //   the scaffold) can use it for drawBackdrop blur.
    var glassBackdrop by remember {
        mutableStateOf<com.kyant.backdrop.backdrops.LayerBackdrop?>(null)
    }

    // ★ Connect to CoralPlaybackService on launch
    LaunchedEffect(Unit) {
        PlaybackConnection.connect(context)
    }

    // ★ Disconnect when the composable leaves (prevents controller leaks)
    DisposableEffect(Unit) {
        onDispose {
            PlaybackConnection.disconnect()
        }
    }

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
            onSettingsClick = { showSettings = true },
            onNavBarLongPress = {
                customizationMode = com.rajatxo.coral.ui.cynthia.CustomizationMode.NAV_BAR
                showCustomization = true
            },
            onSearchLongPress = {
                customizationMode = com.rajatxo.coral.ui.cynthia.CustomizationMode.SEARCH_FAB
                showCustomization = true
            },
            onBackdropReady = { backdrop -> glassBackdrop = backdrop },
            miniPlayer = {
                // ★ Only show mini player when a song is loaded
                val currentSong by PlaybackConnection.currentSong.collectAsState()
                if (currentSong != null) {
                    MiniPlayer(
                        onExpand = { showFullPlayer = true },
                        onDismiss = { /* Mini player dismissed — stays hidden until next song */ },
                        onLongPress = {
                            customizationMode = com.rajatxo.coral.ui.cynthia.CustomizationMode.MINI_PLAYER
                            showCustomization = true
                        }
                    )
                }
            }
        ) {
            when (activeTab) {
                CoralTab.QuickPicks -> QuickPicksScreen(
                    onTopCardClick = { song ->
                        topCardSong = song
                        showTopCard = true
                    }
                )
                CoralTab.Songs -> SongsScreen()
                CoralTab.Playlists -> PlaylistsScreen()
            }
        }

        // ★ Full player overlay — slides up when showFullPlayer is true
        AnimatedVisibility(
            visible = showFullPlayer,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            FullPlayer(
                onDismiss = { showFullPlayer = false }
            )
        }

        // ★ TodaysTopCard — rendered BEFORE the customization panel so the
        // panel appears ON TOP of the card (correct z-order).
        // Card reads from CynthiaTodaysTopCardCustomization prefs — arc dial
        // changes apply LIVE to the card below.
        com.rajatxo.coral.ui.components.TodaysTopCard(
            visible = showTopCard,
            song = topCardSong,
            backdrop = glassBackdrop,
            onMenuClick = {
                customizationMode = com.rajatxo.coral.ui.cynthia.CustomizationMode.TOP_CARD
                showCustomization = true
            },
            onDismiss = { showTopCard = false }
        )

        // ★ Cynthia Customization Panel — rendered AFTER the card so it's ON TOP.
        // The panel floats over the card — arc dial changes apply live.
        com.rajatxo.coral.ui.cynthia.CynthiaCustomizationPanel(
            visible = showCustomization,
            onDismiss = { showCustomization = false },
            backdrop = glassBackdrop,
            mode = customizationMode
        )
    }
}
