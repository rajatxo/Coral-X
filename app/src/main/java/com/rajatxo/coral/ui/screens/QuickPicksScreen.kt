package com.rajatxo.coral.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rajatxo.coral.data.prefs.PlaybackHistory
import com.rajatxo.coral.data.prefs.SpeedDialPinStore
import com.rajatxo.coral.data.prefs.UserName
import com.rajatxo.coral.domain.model.Song
import com.rajatxo.coral.ui.foundation.CoralLayout
import com.rajatxo.coral.ui.foundation.LocalCoralWindowInsets
import com.rajatxo.coral.ui.icons.CoralIcons
import com.rajatxo.coral.ui.theme.CalSansFamily
import kotlinx.coroutines.launch

/**
 * ★ QuickPicksScreen — first screen on CoralScaffold.
 *
 * GUARANTEES (same on every phone):
 *   - LazyColumn uses LocalCoralWindowInsets.current.asPaddingValues() for
 *     top/bottom insets — scaffold computes the correct values
 *   - 20dp horizontal contentPadding (CoralLayout.ContentPaddingHorizontal)
 *     applied to LazyColumn — every item inherits the same left edge
 *   - All children use Modifier.fillMaxWidth() — NO per-item padding chaos
 */
@Composable
fun QuickPicksScreen(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit
) {
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.6f)

    val dailyPlays by PlaybackHistory.dailyPlays.collectAsState()
    val todayTopSongs = remember(songs, dailyPlays) {
        dailyPlays
            .sortedByDescending { it.playCount }
            .mapNotNull { entry -> songs.find { it.id == entry.songId } }
            .take(5)
    }

    val recentHistory by PlaybackHistory.history.collectAsState()
    val recentSongs = remember(songs, recentHistory) {
        recentHistory
            .mapNotNull { entry -> songs.find { it.id == entry.songId } }
            .distinctBy { it.id }
            .take(10)
    }

    val pinnedIdsList by SpeedDialPinStore.pinnedIds.collectAsState()
    val pinnedIds: Set<Long> = pinnedIdsList.toSet()
    val speedDialSongs = remember(songs, pinnedIds) {
        val pinned = songs.filter { it.id in pinnedIds }
        val others = songs.filter { it.id !in pinnedIds }.shuffled()
        (pinned + others).take(20)
    }

    val insets = LocalCoralWindowInsets.current
    val contentPadding = insets.asPaddingValues()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
            start = CoralLayout.ContentPaddingHorizontal,
            end = CoralLayout.ContentPaddingHorizontal
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { GreetingHeader(textPrimary = textPrimary, textSecondary = textSecondary) }

        if (todayTopSongs.isNotEmpty()) {
            item { SectionHeader(title = "Today's top", textPrimary = textPrimary) }
            item {
                StoryCapsules(
                    songs = todayTopSongs,
                    currentSongId = currentSongId,
                    onSongClick = onSongClick
                )
            }
        }

        if (speedDialSongs.isNotEmpty()) {
            item { SpeedDialHeader(textPrimary = textPrimary, textSecondary = textSecondary) }
            item {
                SpeedDialGrid(
                    songs = speedDialSongs,
                    allSongs = songs,
                    currentSongId = currentSongId,
                    pinnedIds = pinnedIds,
                    onSongClick = onSongClick
                )
            }
        }

        if (recentSongs.isNotEmpty()) {
            item { SectionHeader(title = "Recent", textPrimary = textPrimary) }
            item {
                RecentRow(
                    songs = recentSongs,
                    currentSongId = currentSongId,
                    onSongClick = onSongClick
                )
            }
        }
    }
}

@Composable
private fun GreetingHeader(textPrimary: Color, textSecondary: Color) {
    val hour = remember {
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }
    val userName by UserName.name.collectAsState()
    val greeting = if (userName.isNotBlank()) "Hey, $userName" else "Hey,"
    val moodText = when (hour) {
        in 5..11 -> "Start your day with music"
        in 12..16 -> "What's on your mind?"
        in 17..21 -> "Ready for some music?"
        else -> "Late night vibes"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = greeting,
            color = textPrimary,
            fontFamily = CalSansFamily,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                    blurRadius = 3f
                )
            )
        )
        Text(
            text = moodText,
            color = textSecondary,
            fontFamily = CalSansFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(top = 2.dp),
            style = androidx.compose.ui.text.TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                    blurRadius = 2f
                )
            )
        )
    }
}

@Composable
private fun SectionHeader(title: String, textPrimary: Color) {
    Text(
        text = title,
        color = textPrimary,
        fontFamily = CalSansFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SpeedDialHeader(textPrimary: Color, textSecondary: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Speed dial",
            color = textPrimary,
            fontFamily = CalSansFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = CoralIcons.ChevronRight,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StoryCapsules(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        songs.forEach { song ->
            val isCurrent = song.id == currentSongId
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isCurrent) Color.White.copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSongClick(song) }
                    )
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.5f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
                Text(
                    text = song.title.take(14),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun SpeedDialGrid(
    songs: List<Song>,
    allSongs: List<Song>,
    currentSongId: Long?,
    pinnedIds: Set<Long>,
    onSongClick: (Song) -> Unit
) {
    val columns = 3
    val rows = 3
    val itemsPerPage = columns * rows
    val totalSlots = songs.size + 1
    val pageCount = (totalSlots + itemsPerPage - 1) / itemsPerPage
    val pagerState = rememberPagerState(pageCount = { pageCount.coerceAtLeast(1) })
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gridSpacing = 6.dp
        val itemPadding = 2.dp
        val availableWidth = maxWidth - (gridSpacing * (columns - 1))
        val itemWidth = availableWidth / columns

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
        ) {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 12.dp,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemWidth * rows + gridSpacing * (rows - 1))
            ) { page ->
                Column(modifier = Modifier.fillMaxSize()) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(gridSpacing)
                        ) {
                            for (col in 0 until columns) {
                                val itemIndex = row * columns + col
                                val globalItemIndex = page * itemsPerPage + itemIndex
                                val isDiceSlot = (globalItemIndex == itemsPerPage - 1)

                                if (isDiceSlot) {
                                    DiceButton(
                                        onClick = {
                                            scope.launch {
                                                kotlinx.coroutines.delay(400)
                                                onSongClick(allSongs.random())
                                            }
                                        },
                                        modifier = Modifier
                                            .size(itemWidth - itemPadding * 2)
                                            .padding(itemPadding)
                                    )
                                } else {
                                    val actualIndex = if (globalItemIndex < itemsPerPage - 1) {
                                        globalItemIndex
                                    } else {
                                        globalItemIndex - 1
                                    }
                                    val song = songs.getOrNull(actualIndex)
                                    if (song != null) {
                                        SpeedDialCardItem(
                                            song = song,
                                            isCurrent = song.id == currentSongId,
                                            isPinned = song.id in pinnedIds,
                                            onClick = { onSongClick(song) },
                                            modifier = Modifier
                                                .size(itemWidth - itemPadding * 2)
                                                .padding(itemPadding)
                                        )
                                    } else {
                                        Spacer(
                                            modifier = Modifier
                                                .size(itemWidth - itemPadding * 2)
                                                .padding(itemPadding)
                                        )
                                    }
                                }
                            }
                        }
                        if (row < rows - 1) {
                            Spacer(modifier = Modifier.height(gridSpacing))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedDialCardItem(
    song: Song,
    isCurrent: Boolean,
    isPinned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCurrent) Color.White.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.05f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.5f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            if (isPinned) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = song.title.take(14),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist.take(14),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DiceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CoralIcons.ShuffleLucide,
            contentDescription = "Randomize",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun RecentRow(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = songs,
            key = { it.id }
        ) { song ->
            val isCurrent = song.id == currentSongId
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isCurrent) Color.White.copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSongClick(song) }
                    )
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.5f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = song.title.take(14),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist.take(14),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
