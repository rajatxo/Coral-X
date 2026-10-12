package com.rajatxo.coral.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil3.request.transformations
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.data.prefs.SectionToggleManager
import com.rajatxo.coral.data.scanner.MusicScanner
import com.rajatxo.coral.data.scanner.SongCache
import com.rajatxo.coral.domain.model.Song
import com.rajatxo.coral.ui.foundation.LocalCoralXWindowInsets
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.screens.sections.GreetingMoodHeader
import com.rajatxo.coral.ui.screens.sections.SectionHeader
import com.rajatxo.coral.ui.screens.sections.SpeedDialSection
import com.rajatxo.coral.ui.screens.sections.TodaysTopSection
import com.rajatxo.coral.ui.theme.CalSansFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Placeholder Song data ───────────────────────────────────
data class PlaceholderSong(
    val id: Long,
    val title: String,
    val artist: String,
    val color: Color,
    val albumArtUri: android.net.Uri? = null
)

/**
 * Convert a real [Song] (from MediaStore) to a [PlaceholderSong] so it can
 * be rendered by the existing SquareCard / LandscapeCard components.
 *
 * The color is derived deterministically from the song's id (hash → hue)
 * so each song gets a stable, distinct placeholder color.
 * albumArtUri is passed through for Coil3 to load real album art.
 */
private fun Song.toPlaceholder(): PlaceholderSong {
    val hue = (id % 360).toFloat()
    val color = Color.hsv(hue = hue, saturation = 0.35f, value = 0.18f)
    return PlaceholderSong(
        id = id,
        title = title,
        artist = artist,
        color = color,
        albumArtUri = albumArtUri
    )
}

private val placeholderSongs = listOf(
    PlaceholderSong(1, "Blinding Lights", "The Weeknd", Color(0xFF1a0a2e)),
    PlaceholderSong(2, "Starboy", "The Weeknd", Color(0xFF16213e)),
    PlaceholderSong(3, "Save Your Tears", "The Weeknd", Color(0xFF0f3460)),
    PlaceholderSong(4, "After Hours", "The Weeknd", Color(0xFF533483)),
    PlaceholderSong(5, "Die For You", "The Weeknd", Color(0xFF2d1b4e)),
    PlaceholderSong(6, "Call Out My Name", "The Weeknd", Color(0xFF1a1a2e)),
    PlaceholderSong(7, "The Hills", "The Weeknd", Color(0xFF2c1810)),
    PlaceholderSong(8, "Can't Feel My Face", "The Weeknd", Color(0xFF1e3a5f)),
    PlaceholderSong(9, "I Feel It Coming", "The Weeknd", Color(0xFF252850)),
    PlaceholderSong(10, "Pray For Me", "The Weeknd", Color(0xFF2a1f3d)),
    PlaceholderSong(11, "In The Night", "The Weeknd", Color(0xFF1a2a3a)),
    PlaceholderSong(12, "Secrets", "The Weeknd", Color(0xFF2d2d2d)),
    PlaceholderSong(13, "Often", "The Weeknd", Color(0xFF1a0a1a)),
    PlaceholderSong(14, "Earned It", "The Weeknd", Color(0xFF251a2e)),
    PlaceholderSong(15, "Wicked Games", "The Weeknd", Color(0xFF1e1e2e)),
    PlaceholderSong(16, "False Alarm", "The Weeknd", Color(0xFF1a2e1a)),
    PlaceholderSong(17, "Party Monster", "The Weeknd", Color(0xFF2e1a1a)),
    PlaceholderSong(18, "Reminder", "The Weeknd", Color(0xFF1a2e2e)),
    PlaceholderSong(19, "Rockin'", "The Weeknd", Color(0xFF2e2a1a)),
    PlaceholderSong(20, "Secrets", "The Weeknd", Color(0xFF1a1a3e)),
    PlaceholderSong(21, "Attention", "The Weeknd", Color(0xFF3e1a2e)),
    PlaceholderSong(22, "ordinary", "The Weeknd", Color(0xFF1a3e2e)),
    PlaceholderSong(23, "Dawn FM", "The Weeknd", Color(0xFF2e1a3e)),
    PlaceholderSong(24, "Out of Time", "The Weeknd", Color(0xFF3e2e1a)),
    PlaceholderSong(25, "Sacrifice", "The Weeknd", Color(0xFF1a3e3e)),
    PlaceholderSong(26, "Take My Breath", "The Weeknd", Color(0xFF3e3e1a))
)

/**
 * ★ QuickPicksScreen — Coral-style design with toggleable sections.
 *
 * Sections (toggleable via SectionToggleManager):
 *   1. GreetingMoodHeader — "Hey, Rajat" + mood text
 *   2. TodaysTopSection — 3 capsule stories
 *   3. SpeedDialSection — 3×3 grid
 *   4. Recent — LazyRow of SquareCards (140×200dp)
 *   5. More picks — LazyRow of LandscapeCards (160×220dp)
 *
 * Content padding: top=108dp (clears blur header), bottom=200dp (clears
 * nav bar + future mini player), horizontal=20dp, spacing=6dp.
 * Same values as old Coral.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPicksScreen(
    onTopCardClick: (com.rajatxo.coral.domain.model.Song) -> Unit = {}
) {
    val context = LocalContext.current
    val greetingEnabled by SectionToggleManager.greetingEnabled.collectAsState()
    val todaysTopEnabled by SectionToggleManager.todaysTopEnabled.collectAsState()
    val speedDialEnabled by SectionToggleManager.speedDialEnabled.collectAsState()
    val recentEnabled by SectionToggleManager.recentEnabled.collectAsState()
    val morePicksEnabled by SectionToggleManager.morePicksEnabled.collectAsState()

    // ★ Real songs from MediaStore (with cache for instant render)
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }

    // ★ Refresh key — bumped on pull-to-refresh. Re-rolls the random seed
    //   so Speed Dial + More Picks get new song selections.
    var refreshKey by remember { mutableIntStateOf(0) }
    val launchSeed = remember(refreshKey) { kotlin.random.Random.nextInt() }

    // ★ Palette-based background gradient (same as old Coral)
    // Extracts dominant color from the first song's album art → vibrant
    // gradient at top (behind blur header) fading to near-black at bottom.
    // This gives the blur header something colorful to sample → visible blur.
    var palette by remember { mutableStateOf(com.rajatxo.coral.util.CoralPalette.Default) }

    LaunchedEffect(Unit) {
        // Load cache first (instant, ~5-20ms)
        val cached = withContext(Dispatchers.IO) { SongCache.load(context) }
        songs = cached

        // Then scan MediaStore in background
        val scanned = withContext(Dispatchers.IO) { MusicScanner.scanMusic(context) }
        if (scanned.isNotEmpty()) {
            songs = scanned
            withContext(Dispatchers.IO) { SongCache.save(context, scanned) }
        }
    }

    // ★ Extract palette from the CURRENTLY PLAYING song's album art.
    // When a song plays, the background gradient changes to match its colors.
    // If no song is playing, fall back to the first song in the library.
    val currentSong by com.rajatxo.coral.playback.PlaybackConnection.currentSong.collectAsState()
    LaunchedEffect(currentSong, songs) {
        // Prefer the current playing song; fall back to first song with art
        val songForPalette = currentSong ?: songs.firstOrNull { it.albumArtUri != null }
        val artUri = songForPalette?.albumArtUri
        if (artUri != null) {
            // Check cache first (instant — no extraction needed)
            com.rajatxo.coral.util.PaletteCache.get(artUri)?.let { palette = it }
            // Extract in background
            val extracted = withContext(Dispatchers.IO) {
                com.rajatxo.coral.util.extractPalette(context, artUri)
            }
            if (extracted != null) {
                palette = extracted
                com.rajatxo.coral.util.PaletteCache.put(artUri, extracted)
            }
        }
    }

    // ★ Animated background gradient colors (smooth transition when palette changes)
    val vibrantTop by animateColorAsState(
        targetValue = palette.primary.copy(alpha = 0.85f),
        animationSpec = tween(800), label = "bgVibrantTop"
    )
    val fade1 by animateColorAsState(
        targetValue = palette.primary.copy(alpha = 0.65f),
        animationSpec = tween(800), label = "bgFade1"
    )
    val fade2 by animateColorAsState(
        targetValue = palette.primary.copy(alpha = 0.45f),
        animationSpec = tween(800), label = "bgFade2"
    )
    val fade3 by animateColorAsState(
        targetValue = palette.primary.copy(alpha = 0.28f),
        animationSpec = tween(800), label = "bgFade3"
    )
    val animatedTop by animateColorAsState(
        targetValue = palette.primary.copy(alpha = 0.18f),
        animationSpec = tween(800), label = "bgTop"
    )
    val animatedMid by animateColorAsState(
        targetValue = palette.primary.copy(alpha = 0.08f),
        animationSpec = tween(800), label = "bgMid"
    )
    val animatedBottom by animateColorAsState(
        targetValue = Color(0xFF05050A),
        animationSpec = tween(800), label = "bgBottom"
    )

    val recentSongs: List<PlaceholderSong> = if (songs.isNotEmpty()) {
        songs.sortedByDescending { it.dateAdded }.take(100).map { it.toPlaceholder() }
    } else {
        placeholderSongs.take(10)
    }
    val moreSongs: List<PlaceholderSong> = if (songs.isNotEmpty()) {
        // ★ Shuffled with launchSeed so refresh re-rolls the selection
        songs.shuffled(kotlin.random.Random(launchSeed + 2)).take(100).map { it.toPlaceholder() }
    } else {
        placeholderSongs.takeLast(10)
    }

    val density = LocalDensity.current
    val insets = LocalCoralXWindowInsets.current
    val topPadding = with(density) { insets.getTop(density).toDp() }
    val bottomPadding = with(density) { insets.getBottom(density).toDp() }

    // ★ Pull-to-refresh state
    val ptrState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ★ Background: solid dark base + palette gradient on top
    // The gradient fills the ENTIRE screen, including behind the blur header.
    // This gives the blur vibrant colors to sample → visible frosted-glass effect.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05050A))
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f  to vibrantTop,
                        0.10f to fade1,
                        0.15f to fade2,
                        0.20f to fade3,
                        0.30f to animatedTop,
                        0.55f to animatedMid,
                        1.0f  to animatedBottom
                    )
                )
            )
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                // Re-scan MediaStore + bump refreshKey (re-rolls speed dial + more picks)
                isRefreshing = true
                refreshKey++
                scope.launch {
                    val scanned = withContext(Dispatchers.IO) { MusicScanner.scanMusic(context) }
                    if (scanned.isNotEmpty()) {
                        songs = scanned
                        withContext(Dispatchers.IO) { SongCache.save(context, scanned) }
                    }
                    delay(500)
                    isRefreshing = false
                }
            },
            state = ptrState,
            modifier = Modifier.fillMaxSize(),
            indicator = {}  // Suppress default indicator — BugLineRefreshIndicator is in Speed Dial header
        ) {
        LazyColumn(
            contentPadding = PaddingValues(
                top = topPadding,
                bottom = bottomPadding,
                start = 20.dp,
                end = 20.dp
            ),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
        // 1. Greeting + Mood
        if (greetingEnabled) {
            item { GreetingMoodHeader(userName = "Rajat", modifier = Modifier.padding(bottom = 6.dp)) }
        }

        // 2. Today's Top — reads from PlaybackHistory.dailyPlays internally
        if (todaysTopEnabled) {
            item {
                TodaysTopSection(
                    songs = songs,
                    onCapsuleClick = { tappedSong ->
                        onTopCardClick(tappedSong)
                    },
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }

        // 3. Speed Dial — pass 26 songs + launchSeed for refresh re-roll
        if (speedDialEnabled) {
            item {
                SpeedDialSection(
                    songs = songs.take(26),
                    launchSeed = launchSeed,
                    pullProgress = ptrState.distanceFraction,
                    isRefreshing = isRefreshing,
                    onTileClick = { song ->
                        com.rajatxo.coral.playback.PlaybackConnection.playSong(song, songs)
                    },
                    onDiceClick = {
                        if (songs.isNotEmpty()) {
                            val random = songs.random()
                            com.rajatxo.coral.playback.PlaybackConnection.playSong(random, songs)
                        }
                    },
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }

        // 4. Recent (LazyRow of SquareCards)
        if (recentEnabled) {
            item { SectionHeader(title = "Recent", count = recentSongs.size) }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    itemsIndexed(recentSongs, key = { _, s -> s.id }) { _, song ->
                        SquareCard(
                            title = song.title,
                            artist = song.artist,
                            color = song.color,
                            albumArtUri = song.albumArtUri,
                            onClick = {
                                // Find the real Song from the songs list and play it
                                val realSong = songs.firstOrNull { it.id == song.id }
                                if (realSong != null) {
                                    com.rajatxo.coral.playback.PlaybackConnection.playSong(realSong, songs)
                                }
                            },
                            modifier = Modifier.width(140.dp).height(200.dp)
                        )
                    }
                }
            }
        }

        // 5. More picks (LazyRow of LandscapeCards)
        if (morePicksEnabled) {
            item { SectionHeader(title = "More picks", count = moreSongs.size) }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    itemsIndexed(moreSongs, key = { _, s -> s.id }) { _, song ->
                        LandscapeCard(
                            title = song.title,
                            artist = song.artist,
                            color = song.color,
                            albumArtUri = song.albumArtUri,
                            onClick = {
                                val realSong = songs.firstOrNull { it.id == song.id }
                                if (realSong != null) {
                                    com.rajatxo.coral.playback.PlaybackConnection.playSong(realSong, songs)
                                }
                            },
                            modifier = Modifier.width(160.dp).height(220.dp)
                        )
                    }
                }
            }
        }
        }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// SQUARE CARD — Coral's design (shadow + gradient placeholder + text overlay)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun SquareCard(
    title: String,
    artist: String,
    color: Color,
    albumArtUri: android.net.Uri? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = cardShape, clip = false)
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // ═══ Spiral 2.0-style album art blur-blend ═══
        // Layer 1 (bottom): BLURRED album art — pre-computed via Coil
        // BlurTransformation (cached in memory) for smooth scrolling.
        if (albumArtUri != null) {
            val blurredRequest = remember(albumArtUri) {
                coil3.request.ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .transformations(com.rajatxo.coral.util.BlurTransformation(36.dp))
                    .build()
            }
            coil3.compose.AsyncImage(
                model = blurredRequest,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CoralXIcons.Music,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Layer 2 (top): SHARP album art — top 75%, DstIn blend at bottom
        if (albumArtUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        val blendHeightPx = 30.dp.toPx()
                        val imageHeight = size.height
                        val blendStartY = (imageHeight - blendHeightPx).coerceAtLeast(0f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startY = blendStartY,
                                endY = imageHeight
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                coil3.compose.AsyncImage(
                    model = coil3.request.ImageRequest.Builder(context)
                        .data(albumArtUri)
                        .build(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Text on the blurred bottom part
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = CalSansFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = CalSansFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// LANDSCAPE CARD — same as SquareCard but taller, bigger text
// ════════════════════════════════════════════════════════════════════

@Composable
private fun LandscapeCard(
    title: String,
    artist: String,
    color: Color,
    albumArtUri: android.net.Uri? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = cardShape, clip = false)
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // ═══ Spiral 2.0-style album art blur-blend ═══
        // Layer 1 (bottom): BLURRED album art — pre-computed via Coil
        if (albumArtUri != null) {
            val blurredRequest = remember(albumArtUri) {
                coil3.request.ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .transformations(com.rajatxo.coral.util.BlurTransformation(36.dp))
                    .build()
            }
            coil3.compose.AsyncImage(
                model = blurredRequest,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CoralXIcons.Music,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Layer 2 (top): SHARP album art — top 75%, DstIn blend at bottom
        if (albumArtUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        val blendHeightPx = 30.dp.toPx()
                        val imageHeight = size.height
                        val blendStartY = (imageHeight - blendHeightPx).coerceAtLeast(0f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startY = blendStartY,
                                endY = imageHeight
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                coil3.compose.AsyncImage(
                    model = coil3.request.ImageRequest.Builder(context)
                        .data(albumArtUri)
                        .build(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Text on the blurred bottom part
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = CalSansFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontFamily = CalSansFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Songs + Playlists screens (unchanged)
// ════════════════════════════════════════════════════════════════════

@Composable
fun SongsScreen() {
    val density = LocalDensity.current
    val insets = LocalCoralXWindowInsets.current
    val topPadding = with(density) { insets.getTop(density).toDp() }
    val bottomPadding = with(density) { insets.getBottom(density).toDp() }
    LazyColumn(
        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding, start = 20.dp, end = 20.dp),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(placeholderSongs) { song ->
            SongListItem(song)
        }
    }
}

@Composable
fun PlaylistsScreen() {
    val playlists = listOf(
        "Chill Mix" to 12, "Workout" to 25, "Road Trip" to 30,
        "Late Night" to 8, "Favorites" to 45, "Discover Weekly" to 30,
        "Throwback" to 50, "Focus" to 18
    )
    val density = LocalDensity.current
    val insets = LocalCoralXWindowInsets.current
    val topPadding = with(density) { insets.getTop(density).toDp() }
    val bottomPadding = with(density) { insets.getBottom(density).toDp() }
    LazyColumn(
        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding, start = 20.dp, end = 20.dp),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(playlists) { (name, count) ->
            PlaylistCard(name, count)
        }
    }
}

@Composable
private fun SongListItem(song: PlaceholderSong) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A).copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art placeholder (gradient)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(listOf(song.color, song.color.copy(alpha = 0.6f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CoralXIcons.Music,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = CalSansFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontFamily = CalSansFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaylistCard(name: String, songCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A).copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Playlist art placeholder
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFF6B6B).copy(alpha = 0.3f), Color(0xFFFFD166).copy(alpha = 0.3f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CoralXIcons.ListMusic,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = CalSansFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$songCount songs",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontFamily = CalSansFamily
            )
        }
    }
}
