package com.rajatxo.coral.ui.screens.sections

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import com.rajatxo.coral.domain.model.Song
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily
import com.rajatxo.coral.util.BlurTransformation

/**
 * ★ SpeedDialSection — paginated 3×3 grid of song tiles.
 *
 * Ported from old Coral's SpeedDialSection. Uses HorizontalPager so the
 * user can swipe between pages of songs (like old Coral — 3 pages max).
 *
 * Song selection (ported from old Coral):
 *   - Pinned songs come FIRST (from SpeedDialPinStore)
 *   - Mode RANDOM: completely random songs fill the rest
 *   - Mode LAST_PLAYED: 80% from last-played artist + 20% other
 *
 * Page 1: 8 songs + 1 dice (dice is the last slot on page 1)
 * Pages 2+: 9 songs per page
 * Total: 26 songs (8 + 9 + 9)
 */
@Composable
fun SpeedDialSection(
    modifier: Modifier = Modifier,
    songs: List<Song> = emptyList(),
    pullProgress: Float = 0f,
    isRefreshing: Boolean = false,
    onHeaderClick: () -> Unit = {},
    onTileClick: (Song) -> Unit = {},
    onDiceClick: () -> Unit = {}
) {
    // ★ Observe pinned songs + speed dial mode
    val pinnedIds by com.rajatxo.coral.data.prefs.SpeedDialPinStore.pinnedIds.collectAsState()
    val speedDialMode by com.rajatxo.coral.data.prefs.SpeedDialModeManager.mode.collectAsState()
    val playbackHistory by com.rajatxo.coral.data.prefs.PlaybackHistory.history.collectAsState()

    // Random seed — changes on every app launch (so songs are different each time)
    val launchSeed = remember { kotlin.random.Random.nextInt() }

    // ★ Build the speed dial song list (pinned first, then random/last-played)
    val speedDialSongs = remember(songs.size, pinnedIds, launchSeed, speedDialMode, playbackHistory) {
        if (songs.isEmpty()) emptyList()
        else {
            // 1. Pinned songs (in pin order), filtered to ones that still exist
            val pinnedSongs = pinnedIds.mapNotNull { id ->
                songs.firstOrNull { it.id == id }
            }
            // 2. Pool of available (non-pinned) songs
            val pool = songs.filter { it.id !in pinnedIds }

            // 3. Pick songs based on mode
            val targetCount = 26
            val randomCount = (targetCount - pinnedSongs.size).coerceAtLeast(0)

            val picked: List<Song> = if (speedDialMode == com.rajatxo.coral.data.prefs.SpeedDialModeManager.SpeedDialMode.LAST_PLAYED
                && playbackHistory.isNotEmpty()
            ) {
                // LAST_PLAYED mode — 80% from last-played artist + 20% other
                val lastArtist = playbackHistory.firstOrNull()?.artist
                if (!lastArtist.isNullOrBlank()) {
                    val artistSongs = pool.filter {
                        it.artist.trim().equals(lastArtist.trim(), ignoreCase = true)
                    }.shuffled(kotlin.random.Random(launchSeed + 100))
                    val otherSongs = pool.filter {
                        !it.artist.trim().equals(lastArtist.trim(), ignoreCase = true)
                    }.shuffled(kotlin.random.Random(launchSeed + 200))
                    val eightyPct = (randomCount * 0.8f).toInt()
                    val artistPicked = artistSongs.take(eightyPct)
                    val remainingNeeded = randomCount - artistPicked.size
                    artistPicked + otherSongs.take(remainingNeeded)
                } else {
                    pool.shuffled(kotlin.random.Random(launchSeed + 100)).take(randomCount)
                }
            } else {
                // RANDOM mode — completely random
                pool.shuffled(kotlin.random.Random(launchSeed + 100)).take(randomCount)
            }

            // Failsafe — pad with pool songs if we don't have enough
            val finalPicked = if (picked.size < randomCount && pool.isNotEmpty()) {
                val stillNeeded = randomCount - picked.size
                val fillers = pool.shuffled(kotlin.random.Random(launchSeed + 300))
                    .filter { it.id !in picked.map(Song::id) }
                    .take(stillNeeded)
                picked + fillers
            } else {
                picked
            }
            pinnedSongs + finalPicked
        }
    }

    // ─── Pagination math (same as old Coral) ──
    val columns = 3
    val rows = 3
    val itemsPerPage = columns * rows  // 9
    val totalSlots = speedDialSongs.size + 1    // +1 for the dice
    val pageCount = (totalSlots + itemsPerPage - 1) / itemsPerPage
    val pagerState = rememberPagerState(pageCount = { pageCount.coerceAtLeast(1) })

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Speed dial",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = CalSansFamily,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onHeaderClick
                )
            )
            Box(
                modifier = Modifier.size(24.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onHeaderClick
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CoralXIcons.ChevronRight,
                    contentDescription = "Settings",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
            // ★ Bug-on-a-line pull-to-refresh indicator (in the header, weight 1f)
            com.rajatxo.coral.ui.components.BugLineRefreshIndicator(
                progress = pullProgress,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
            )
        }

        // ★ HorizontalPager — swipe between pages of songs.
        // Fixed tile size (110dp) for BOTH width and height → perfect squares.
        // This is old Coral's exact approach: targetItemSize = 110.dp.
        val tileWidth = 110.dp
        HorizontalPager(
            state = pagerState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(tileWidth * rows)
        ) { page ->
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0 until columns) {
                            val itemIndex = row * columns + col
                            val globalItemIndex = page * itemsPerPage + itemIndex

                            // The dice button is the last slot on the FIRST page
                            val isDiceSlot = (globalItemIndex == itemsPerPage - 1)

                            if (isDiceSlot) {
                                DiceTile(
                                    modifier = Modifier
                                        .width(tileWidth)
                                        .height(tileWidth)
                                        .padding(4.dp),
                                    onClick = onDiceClick
                                )
                            } else {
                                // Adjust index to account for the dice slot on page 1
                                val actualIndex = if (globalItemIndex < itemsPerPage - 1) {
                                    globalItemIndex
                                } else {
                                    globalItemIndex - 1
                                }
                                val song = speedDialSongs.getOrNull(actualIndex)
                                if (song != null) {
                                    SpeedDialTile(
                                        song = song,
                                        onClick = { onTileClick(song) },
                                        modifier = Modifier
                                            .width(tileWidth)
                                            .height(tileWidth)
                                            .padding(4.dp)
                                    )
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .width(tileWidth)
                                            .height(tileWidth)
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun songPlaceholderColor(songId: Long): Color {
    val hue = (songId % 360).toFloat()
    return Color.hsv(hue = hue, saturation = 0.45f, value = 0.22f)
}

@Composable
private fun SpeedDialTile(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
        label = "tileScale"
    )

    // ★ Pin state — long-press shows the pin capsule
    var showPinCapsule by remember { mutableStateOf(false) }
    val isPinned = com.rajatxo.coral.data.prefs.SpeedDialPinStore.isPinned(song.id)

    val tileShape = RoundedCornerShape(8.dp)
    val fallbackColor = songPlaceholderColor(song.id)

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = 4.dp, shape = tileShape, clip = false)
            .clip(tileShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = { showPinCapsule = true }
            )
    ) {
        // ═══ Spiral 2.0-style album art blur-blend ═══
        if (song.albumArtUri != null) {
            val blurredRequest = remember(song.albumArtUri) {
                ImageRequest.Builder(context)
                    .data(song.albumArtUri)
                    .transformations(BlurTransformation(20.dp))
                    .build()
            }
            AsyncImage(
                model = blurredRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CoralXIcons.Music,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Layer 2 (top): SHARP album art — top 85%, DstIn blend at bottom
        if (song.albumArtUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        val blendHeightPx = 24.dp.toPx()
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
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(song.albumArtUri)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Title at the bottom (song title only, no artist — matches old Coral)
        Text(
            text = song.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = CalSansFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, end = 4.dp, bottom = 4.dp)
        )

        // Pinned indicator (top-left, small pin icon)
        if (isPinned) {
            Icon(
                imageVector = CoralXIcons.Pin,
                contentDescription = "Pinned",
                tint = Color.White,
                modifier = Modifier
                    .padding(6.dp)
                    .size(12.dp)
                    .align(Alignment.TopStart)
            )
        }

        // ★ Long-press pin capsule overlay
        SpeedDialPinCapsule(
            visible = showPinCapsule,
            isPinned = isPinned,
            onPin = {
                if (isPinned) {
                    com.rajatxo.coral.data.prefs.SpeedDialPinStore.unpin(song.id)
                } else {
                    com.rajatxo.coral.data.prefs.SpeedDialPinStore.pin(song.id)
                }
                showPinCapsule = false
            },
            onTimeout = { showPinCapsule = false }
        )
    }
}

@Composable
private fun SpeedDialPinCapsule(
    visible: Boolean,
    isPinned: Boolean,
    onPin: () -> Unit,
    onTimeout: () -> Unit
) {
    // Auto-hide after 5 seconds
    if (visible) {
        androidx.compose.runtime.LaunchedEffect(visible) {
            kotlinx.coroutines.delay(5000)
            onTimeout()
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(animationSpec = tween(200)),
        exit = androidx.compose.animation.fadeOut(animationSpec = tween(300)),
        modifier = Modifier.fillMaxSize()
    ) {
        // Dim the card behind the capsule + center the capsule
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            // Thin capsule with pin icon
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPin
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CoralXIcons.Pin,
                    contentDescription = if (isPinned) "Unpin" else "Pin",
                    tint = if (isPinned) Color(0xFFFF6B6B) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DiceTile(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "diceScale"
    )

    val tileShape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .scale(scale)
            .clip(tileShape)
            .background(Color(0xFF333333))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CoralXIcons.Shuffle,
            contentDescription = "Randomize",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(28.dp)
        )
    }
}
