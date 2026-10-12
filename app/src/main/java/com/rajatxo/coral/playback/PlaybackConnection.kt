package com.rajatxo.coral.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.rajatxo.coral.domain.model.Song
import com.rajatxo.coral.service.CoralPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ★ PlaybackConnection — manages the MediaController lifecycle and exposes
 * playback state as Compose-observable StateFlows.
 *
 * Singleton so all composables (MiniPlayer, FullPlayer, QuickPicksScreen)
 * share the same controller + state without passing it through params.
 *
 * Lifecycle:
 *   - connect(context) → builds MediaController via SessionToken
 *   - Controller listener updates StateFlows on every playback event
 *   - disconnect() → releases the controller (called from MainActivity.onDispose)
 *
 * Usage from composables:
 *   val playback = PlaybackConnection
 *   val isPlaying by playback.isPlaying.collectAsState()
 *   val currentSong by playback.currentSong.collectAsState()
 *   playback.playSong(song, allSongs)  // set queue + play
 */
object PlaybackConnection {

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // ★ Compose-observable playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    /** The full song list currently in the queue (for next/prev navigation). */
    private var queue: List<Song> = emptyList()

    /**
     * Connect to the CoralPlaybackService and start observing playback state.
     * Call from MainActivity's LaunchedEffect.
     */
    fun connect(context: Context) {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, CoralPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync().also { future ->
            future.addListener({
                controller = future.get()
                controller?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val songId = mediaItem?.mediaId?.toLongOrNull()
                        _currentSong.value = queue.firstOrNull { it.id == songId }
                        _positionMs.value = 0L
                        _durationMs.value = 0L
                    }
                })
            }, Runnable::run)
        }
    }

    /**
     * Poll the controller for current position + duration.
     * Call from a LaunchedEffect every 200ms while playing.
     */
    fun pollPosition() {
        controller?.let { c ->
            _positionMs.value = c.currentPosition.coerceAtLeast(0L)
            _durationMs.value = c.duration.coerceAtLeast(0L)
        }
    }

    /**
     * Play a song from a list. Sets the entire list as the queue, seeks to
     * the clicked song, and starts playback.
     */
    fun playSong(song: Song, allSongs: List<Song>) {
        queue = allSongs
        val controller = controller ?: return
        val mediaItems = allSongs.map { s ->
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
        val index = allSongs.indexOf(song).coerceAtLeast(0)
        controller.setMediaItems(mediaItems, index, 0)
        controller.prepare()
        controller.play()
        _currentSong.value = song
    }

    fun playPause() {
        val controller = controller ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun prev() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    /**
     * Release the controller. Call from MainActivity's DisposableEffect.
     */
    fun disconnect() {
        controllerFuture?.let { future ->
            if (!future.isDone) {
                future.cancel(true)
            }
        }
        controller?.release()
        controller = null
        controllerFuture = null
        _isPlaying.value = false
        _currentSong.value = null
    }
}
