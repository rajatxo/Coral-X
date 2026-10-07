package com.rajatxo.coral

import android.Manifest
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.rajatxo.coral.audio.CrossfadeVisualState
import com.rajatxo.coral.data.scanner.MusicScanner
import com.rajatxo.coral.domain.model.Song
import com.rajatxo.coral.service.CoralPlaybackService
import com.rajatxo.coral.ui.foundation.CoralScaffold
import com.rajatxo.coral.ui.screens.QuickPicksScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.hashCode()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.hashCode())
        )
        setContent {
            CoralApp()
        }
    }
}

@Composable
fun CoralApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(false) }
    val songs = remember {
        androidx.compose.runtime.mutableStateListOf<Song>().apply {
            addAll(com.rajatxo.coral.data.scanner.SongCache.load(context))
        }
    }

    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var currentSongId by remember { mutableStateOf<Long?>(null) }
    var currentSongTitle by remember { mutableStateOf<String?>(null) }
    var currentSongArtist by remember { mutableStateOf<String?>(null) }
    var currentSongAlbum by remember { mutableStateOf<String?>(null) }
    var currentSongArt by remember { mutableStateOf<android.net.Uri?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var permissionChecked by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Check if permissions are already granted
    LaunchedEffect(Unit) {
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
        val musicOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        hasPermission = notifOk && musicOk
        permissionChecked = true

        if (hasPermission) {
            scope.launch {
                val scannedSongs = withContext(Dispatchers.IO) { MusicScanner.scanMusic(context) }
                if (scannedSongs != songs.toList()) {
                    songs.clear()
                    songs.addAll(scannedSongs)
                    withContext(Dispatchers.IO) {
                        com.rajatxo.coral.data.scanner.SongCache.save(context, scannedSongs)
                    }
                }
                withContext(Dispatchers.IO) {
                    com.rajatxo.coral.data.lyrics.LyricsIndex.scan(context)
                }
            }
        }
    }

    // MediaController setup
    LaunchedEffect(Unit) {
        try {
            val sessionToken = SessionToken(context, ComponentName(context, CoralPlaybackService::class.java))
            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            val controller = controllerFuture.await()
            mediaController = controller

            controller.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentSongTitle = mediaItem?.mediaMetadata?.title?.toString()
                    currentSongArtist = mediaItem?.mediaMetadata?.artist?.toString()
                    currentSongAlbum = mediaItem?.mediaMetadata?.albumTitle?.toString()
                    currentSongArt = mediaItem?.mediaMetadata?.artworkUri
                    currentSongId = mediaItem?.mediaId?.toLongOrNull()
                    if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                        if (!controller.isPlaying) controller.play()
                    }
                }
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    if (playing) controller.volume = 1f
                }
            })

            controller.repeatMode = com.rajatxo.coral.data.prefs.PlaybackPrefs.repeatMode.value

            val currentMediaItem = controller.currentMediaItem
            if (currentMediaItem != null) {
                currentSongTitle = currentMediaItem.mediaMetadata.title?.toString()
                currentSongArtist = currentMediaItem.mediaMetadata.artist?.toString()
                currentSongAlbum = currentMediaItem.mediaMetadata.albumTitle?.toString()
                currentSongArt = currentMediaItem.mediaMetadata.artworkUri
                currentSongId = currentMediaItem.mediaId.toLongOrNull()
            }
            isPlaying = controller.isPlaying
        } catch (e: Exception) {
            android.util.Log.e("CoralApp", "MediaController setup failed", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try { mediaController?.release() } catch (_: Exception) { }
            mediaController = null
        }
    }

    // Permission launcher
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true
        val musicOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        if (notifOk && musicOk) {
            hasPermission = true
            scope.launch {
                val scannedSongs = withContext(Dispatchers.IO) { MusicScanner.scanMusic(context) }
                songs.clear()
                songs.addAll(scannedSongs)
                withContext(Dispatchers.IO) {
                    com.rajatxo.coral.data.scanner.SongCache.save(context, scannedSongs)
                }
                withContext(Dispatchers.IO) {
                    com.rajatxo.coral.data.lyrics.LyricsIndex.scan(context)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            !permissionChecked -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
            !hasPermission -> {
                PermissionGate(
                    onGrantClick = {
                        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(
                                Manifest.permission.READ_MEDIA_AUDIO,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(perms)
                    }
                )
            }
            else -> {
                CoralScaffold(
                    songs = songs,
                    mediaController = mediaController,
                    currentSongId = currentSongId,
                    currentSongTitle = currentSongTitle,
                    currentSongArtist = currentSongArtist,
                    currentSongAlbum = currentSongAlbum,
                    currentSongArt = currentSongArt,
                    isPlaying = isPlaying,
                    onPlayPauseClick = {
                        if (isPlaying) mediaController?.pause() else mediaController?.play()
                    },
                    onNextClick = {
                        CrossfadeVisualState.clearIncoming()
                        mediaController?.seekToNextMediaItem()
                    },
                    onPrevClick = {
                        CrossfadeVisualState.clearIncoming()
                        mediaController?.seekToPreviousMediaItem()
                    },
                    onSeek = { positionMs -> mediaController?.seekTo(positionMs) },
                    onSongClick = { song ->
                        CrossfadeVisualState.clearIncoming()
                        mediaController?.let { controller ->
                            val allMediaItems = songs.map { s ->
                                MediaItem.Builder()
                                    .setUri(s.uri)
                                    .setMediaId(s.id.toString())
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(s.title)
                                            .setArtist(s.artist)
                                            .setAlbumTitle(s.album)
                                            .setArtworkUri(s.albumArtUri)
                                            .build()
                                    )
                                    .build()
                            }
                            val index = songs.indexOf(song)
                            controller.setMediaItems(allMediaItems, index, 0)
                            controller.prepare()
                            controller.play()
                            com.rajatxo.coral.data.prefs.PlaybackPrefs.saveQueue(
                                songs.map { it.id }, index, 0L
                            )
                        }
                    },
                    onMiniPlayerClick = { showFullPlayer = true },
                    showFullPlayer = showFullPlayer,
                    onFullPlayerDismiss = { showFullPlayer = false },
                    onOpenSettings = { showSettings = true },
                    onOpenSearch = { showSearch = true },
                    onOpenProfile = { },
                    title = "Quick picks"
                ) {
                    QuickPicksScreen(
                        songs = songs,
                        currentSongId = currentSongId,
                        onSongClick = { song ->
                            CrossfadeVisualState.clearIncoming()
                            mediaController?.let { controller ->
                                val allMediaItems = songs.map { s ->
                                    MediaItem.Builder()
                                        .setUri(s.uri)
                                        .setMediaId(s.id.toString())
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(s.title)
                                                .setArtist(s.artist)
                                                .setAlbumTitle(s.album)
                                                .setArtworkUri(s.albumArtUri)
                                                .build()
                                        )
                                        .build()
                                }
                                val index = songs.indexOf(song)
                                controller.setMediaItems(allMediaItems, index, 0)
                                controller.prepare()
                                controller.play()
                                com.rajatxo.coral.data.prefs.PlaybackPrefs.saveQueue(
                                    songs.map { it.id }, index, 0L
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionGate(onGrantClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Button(onClick = onGrantClick) {
            Text("Grant permissions")
        }
    }
}
