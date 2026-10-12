package com.rajatxo.coral.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.data.prefs.SectionToggleManager
import com.rajatxo.coral.ui.foundation.LocalCoralXWindowInsets
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.screens.sections.GreetingMoodHeader
import com.rajatxo.coral.ui.screens.sections.SectionHeader
import com.rajatxo.coral.ui.screens.sections.SpeedDialSection
import com.rajatxo.coral.ui.screens.sections.TodaysTopSection
import com.rajatxo.coral.ui.theme.CalSansFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Placeholder Song data ───────────────────────────────────
data class PlaceholderSong(
    val id: Long,
    val title: String,
    val artist: String,
    val color: Color
)

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
@Composable
fun QuickPicksScreen() {
    val greetingEnabled by SectionToggleManager.greetingEnabled.collectAsState()
    val todaysTopEnabled by SectionToggleManager.todaysTopEnabled.collectAsState()
    val speedDialEnabled by SectionToggleManager.speedDialEnabled.collectAsState()
    val recentEnabled by SectionToggleManager.recentEnabled.collectAsState()
    val morePicksEnabled by SectionToggleManager.morePicksEnabled.collectAsState()

    val recentSongs = placeholderSongs.take(10)
    val moreSongs = placeholderSongs.takeLast(10)

    LazyColumn(
        contentPadding = PaddingValues(
            top = 108.dp,
            bottom = 200.dp,
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

        // 2. Today's Top
        if (todaysTopEnabled) {
            item { TodaysTopSection(modifier = Modifier.padding(bottom = 6.dp)) }
        }

        // 3. Speed Dial
        if (speedDialEnabled) {
            item { SpeedDialSection(modifier = Modifier.padding(bottom = 6.dp)) }
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
                            modifier = Modifier.width(160.dp).height(220.dp)
                        )
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
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
        // Background: solid color with gradient (placeholder for album art)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.6f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CoralXIcons.Music,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
        }

        // Text overlay at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(10.dp)
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
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
        // Background gradient placeholder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.6f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CoralXIcons.Music,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(56.dp)
            )
        }

        // Text overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(12.dp)
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
                color = Color.White.copy(alpha = 0.6f),
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
    LazyColumn(
        contentPadding = PaddingValues(top = 108.dp, bottom = 200.dp, start = 20.dp, end = 20.dp),
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
    LazyColumn(
        contentPadding = PaddingValues(top = 108.dp, bottom = 200.dp, start = 20.dp, end = 20.dp),
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
