package com.rajatxo.coral.ui.foundation

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.rajatxo.coral.data.prefs.PlayerStyleManager
import com.rajatxo.coral.domain.model.Song
import com.rajatxo.coral.ui.icons.CoralIcons
import com.rajatxo.coral.ui.player.CoralGlassPlayer
import com.rajatxo.coral.ui.player.CoralPlayer
import com.rajatxo.coral.ui.player.FullPlayer
import com.rajatxo.coral.ui.player.Spiral2Player
import com.rajatxo.coral.ui.player.Spiral3Player
import com.rajatxo.coral.ui.player.SpiralPlayer
import com.rajatxo.coral.ui.theme.CalSansFamily

/**
 * ★ CoralScaffold — the foundation every screen plugs into.
 *
 * PROVIDES:
 *   1. Glass backdrop (Kyant) — captures the content layer for AGSL blur.
 *   2. Top header — profile icon + screen title + settings cog (fixed 56dp,
 *      statusBarsPadding). Sits ABOVE a 120dp blur overlay for glass effect.
 *   3. Content slot — fills the screen between header and bottom stack.
 *      The slot is provided with `LocalCoralWindowInsets.current` so screens
 *      can use the SAME contentPadding on every phone.
 *   4. Bottom stack — Column at BottomCenter: mini player (when song playing)
 *      → 10dp gap → nav bar + search FAB Row. Cannot overlap (Column stacks
 *      vertically by definition).
 *   5. Full player overlay — fades in when `showFullPlayer` is true.
 *
 * LAYOUT CONTRACT (same on every phone):
 *   ┌─────────────────────────────────────────┐
 *   │ statusBarsPadding                       │
 *   │ [profile]  Quick picks  [settings]   ← header (56dp)
 *   ├─────────────────────────────────────────┤
 *   │ 120dp top blur fade                     │
 *   │                                         │
 *   │   ┌──── content slot ────┐              │
 *   │   │  LazyColumn / Box     │  ← uses LocalCoralWindowInsets
 *   │   │  with contentPadding  │     for contentPadding
 *   │   └───────────────────────┘             │
 *   │                                         │
 *   │   [mini player]  ← 64dp height          │
 *   │   10dp gap                              │
 *   │   [nav bar 152dp] [gap] [search 52dp]   │
 *   │   safeBottomInset (≥48dp)               │
 *   └─────────────────────────────────────────┘
 *
 * WHY THIS WORKS ON EVERY PHONE:
 *   - All sizes are FIXED dp (CoralLayout constants)
 *   - All positions use Alignment (TopCenter, BottomCenter) — not fractions
 *   - System insets are NORMALIZED — bottom is always ≥48dp regardless of
 *     gesture vs button nav
 *   - Every screen reads the SAME LocalCoralWindowInsets — no per-screen
 *     padding chaos
 */
@Composable
fun CoralScaffold(
    songs: List<Song>,
    mediaController: MediaController?,
    currentSongId: Long?,
    currentSongTitle: String?,
    currentSongArtist: String?,
    currentSongAlbum: String?,
    currentSongArt: android.net.Uri?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSongClick: (Song) -> Unit,
    onMiniPlayerClick: () -> Unit,
    showFullPlayer: Boolean,
    onFullPlayerDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenProfile: () -> Unit,
    title: String,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val systemBottomInset = WindowInsets.navigationBars.getBottom(density)
    val minBottomPx = with(density) { CoralLayout.MinBottomSystemInset.toPx().toInt() }
    val effectiveBottomInset = maxOf(systemBottomInset, minBottomPx)
    val effectiveBottomInsetDp = with(density) { effectiveBottomInset.toDp() }

    val systemTopInset = WindowInsets.statusBars.getTop(density)
    val effectiveTopInsetDp = with(density) { systemTopInset.toDp() }

    // Bottom stack total height — passed to every screen as bottom contentPadding
    val miniPlayerVisible = currentSongId != null
    val bottomStackHeightDp = if (miniPlayerVisible) {
        effectiveBottomInsetDp +
        CoralLayout.NavBarHeight +
        CoralLayout.BottomStackGap +
        CoralLayout.MiniPlayerHeight +
        CoralLayout.BottomStackGap
    } else {
        effectiveBottomInsetDp +
        CoralLayout.NavBarHeight +
        CoralLayout.BottomStackGap
    }

    // ★ Build the WindowInsets every screen will read.
    //   - top: system status bar + header height (so content starts below header)
    //   - bottom: nav row + mini player + safe inset
    //   - horizontal: zero — screens apply CoralLayout.ContentPaddingHorizontal themselves
    val coralWindowInsets = WindowInsets(
        top = effectiveTopInsetDp + CoralLayout.HeaderHeight + 8.dp,
        bottom = bottomStackHeightDp + 16.dp  // +16dp breathing room above bottom stack
    )

    // ─── Glass backdrop (Kyant) — captures the content layer ───
    val graphicsLayer = rememberGraphicsLayer()
    val glassBackdrop = rememberLayerBackdrop(graphicsLayer) {
        drawContent()
    }

    // ─── Mini player position/duration polling ───
    var miniPlayerPositionMs by remember { mutableStateOf(0L) }
    var miniPlayerDurationMs by remember { mutableStateOf(0L) }
    LaunchedEffect(mediaController, isPlaying) {
        while (true) {
            mediaController?.let { controller ->
                miniPlayerPositionMs = controller.currentPosition.coerceAtLeast(0L)
                miniPlayerDurationMs = controller.duration.coerceAtLeast(0L)
            }
            kotlinx.coroutines.delay(500)
        }
    }

    CompositionLocalProvider(
        LocalCoralWindowInsets provides coralWindowInsets
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // ═══ INNER BOX — layerBackdrop captures page content for glass blur ═══
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(glassBackdrop)
            ) {
                content()
            }

            // ═══ TOP FADE BLUR — 120dp, gradient fade at bottom ═══
            val useRenderEffect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(CoralLayout.TopBlurHeight)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        clip = true
                        if (useRenderEffect) {
                            renderEffect = BlurEffect(
                                radiusX = 20.dp.toPx(),
                                radiusY = 20.dp.toPx()
                            )
                        }
                    }
                    .drawWithContent {
                        if (useRenderEffect) {
                            drawLayer(graphicsLayer)
                        } else {
                            drawContent()
                        }
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black,
                                    0.6f to Color.Black,
                                    1.0f to Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                if (!useRenderEffect) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBackdrop(
                                backdrop = glassBackdrop,
                                shape = { RectangleShape },
                                effects = {
                                    vibrancy()
                                    colorControls(
                                        brightness = 0f,
                                        contrast = 1f,
                                        saturation = 1.1f
                                    )
                                    blur(20f.dp.toPx())
                                }
                            )
                    )
                }
            }

            // ═══ HEADER ROW — profile icon + title + settings cog ═══
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = effectiveTopInsetDp)
                    .height(CoralLayout.HeaderHeight)
                    .padding(horizontal = CoralLayout.ContentPaddingHorizontal),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile icon (left)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOpenProfile
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CoralIcons.CircleUser,
                        contentDescription = "Account",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title (center, takes remaining width)
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CalSansFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                // Settings cog (right)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOpenSettings
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CoralIcons.Cog,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // ═══ BOTTOM STACK — mini player + nav row, Column at BottomCenter ═══
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = effectiveBottomInsetDp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CoralLayout.BottomStackGap)
            ) {
                // ─── Mini player (above nav row, only when a song is playing) ───
                if (miniPlayerVisible) {
                    CoralMiniPlayer(
                        title = currentSongTitle ?: "",
                        artist = currentSongArtist ?: "",
                        albumArtUri = currentSongArt,
                        songId = currentSongId,
                        isPlaying = isPlaying,
                        positionMs = miniPlayerPositionMs,
                        durationMs = miniPlayerDurationMs,
                        onPlayPauseClick = onPlayPauseClick,
                        onClick = onMiniPlayerClick,
                        backdrop = glassBackdrop
                    )
                }

                // ─── Nav row: nav bar (tabs) + search FAB ───
                CoralNavRow(
                    onSearchClick = onOpenSearch,
                    backdrop = glassBackdrop
                )
            }

            // ═══ Full player overlay — fades in when showFullPlayer is true ═══
            androidx.compose.animation.AnimatedVisibility(
                visible = showFullPlayer,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                val playerStyle by PlayerStyleManager.playerStyle.collectAsState()
                when (playerStyle) {
                    PlayerStyleManager.CORAL_GLASS -> CoralGlassPlayer(
                        mediaController = mediaController,
                        songId = currentSongId,
                        title = currentSongTitle ?: "",
                        artist = currentSongArtist ?: "",
                        albumName = currentSongAlbum,
                        albumArtUri = currentSongArt,
                        isPlaying = isPlaying,
                        onPlayPauseClick = onPlayPauseClick,
                        onNextClick = onNextClick,
                        onPrevClick = onPrevClick,
                        onSeek = onSeek,
                        onDismiss = onFullPlayerDismiss,
                        onAddToPlaylist = { }
                    )
                    PlayerStyleManager.CORAL -> CoralPlayer(
                        mediaController = mediaController,
                        songId = currentSongId,
                        title = currentSongTitle ?: "",
                        artist = currentSongArtist ?: "",
                        albumName = currentSongAlbum,
                        albumArtUri = currentSongArt,
                        isPlaying = isPlaying,
                        onPlayPauseClick = onPlayPauseClick,
                        onNextClick = onNextClick,
                        onPrevClick = onPrevClick,
                        onSeek = onSeek,
                        onDismiss = onFullPlayerDismiss,
                        onAddToPlaylist = { }
                    )
                    PlayerStyleManager.SPIRAL -> SpiralPlayer(
                        mediaController = mediaController,
                        songId = currentSongId,
                        title = currentSongTitle ?: "",
                        artist = currentSongArtist ?: "",
                        albumName = currentSongAlbum,
                        albumArtUri = currentSongArt,
                        isPlaying = isPlaying,
                        onPlayPauseClick = onPlayPauseClick,
                        onNextClick = onNextClick,
                        onPrevClick = onPrevClick,
                        onSeek = onSeek,
                        onDismiss = onFullPlayerDismiss,
                        onAddToPlaylist = { },
                        onSongDelete = { }
                    )
                    PlayerStyleManager.SPIRAL_2 -> Spiral2Player(
                        mediaController = mediaController,
                        songId = currentSongId,
                        title = currentSongTitle ?: "",
                        artist = currentSongArtist ?: "",
                        albumName = currentSongAlbum,
                        albumArtUri = currentSongArt,
                        isPlaying = isPlaying,
                        onPlayPauseClick = onPlayPauseClick,
                        onNextClick = onNextClick,
                        onPrevClick = onPrevClick,
                        onSeek = onSeek,
                        onDismiss = onFullPlayerDismiss,
                        onAddToPlaylist = { },
                        onSongDelete = { }
                    )
                    PlayerStyleManager.SPIRAL_3 -> Spiral3Player(
                        mediaController = mediaController,
                        songId = currentSongId,
                        title = currentSongTitle ?: "",
                        artist = currentSongArtist ?: "",
                        albumName = currentSongAlbum,
                        albumArtUri = currentSongArt,
                        isPlaying = isPlaying,
                        onPlayPauseClick = onPlayPauseClick,
                        onNextClick = onNextClick,
                        onPrevClick = onPrevClick,
                        onSeek = onSeek,
                        onDismiss = onFullPlayerDismiss,
                        onAddToPlaylist = { },
                        onSongDelete = { }
                    )
                    else -> FullPlayer(
                        mediaController = mediaController,
                        songId = currentSongId,
                        title = currentSongTitle ?: "",
                        artist = currentSongArtist ?: "",
                        albumName = currentSongAlbum,
                        albumArtUri = currentSongArt,
                        isPlaying = isPlaying,
                        onPlayPauseClick = onPlayPauseClick,
                        onNextClick = onNextClick,
                        onPrevClick = onPrevClick,
                        onSeek = onSeek,
                        onDismiss = onFullPlayerDismiss,
                        onAddToPlaylist = { }
                    )
                }
            }
        }
    }
}
