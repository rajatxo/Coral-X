package com.rajatxo.coral.ui.screens.sections

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.rajatxo.coral.data.prefs.PlaybackHistory
import com.rajatxo.coral.domain.model.Song
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily

/**
 * ★ TodaysTopSection — 3 capsule stories showing the top-played songs today.
 *
 * Ported from old Coral. Reads from PlaybackHistory.dailyPlays to show the
 * 6 most-played songs today (by accumulated play duration).
 *
 * Each capsule shows 2 album art circles. If fewer than 6 songs have been
 * played today, capsules show placeholder "+" circles.
 *
 * @param songs The full song library (for looking up Song objects by ID)
 */
@Composable
fun TodaysTopSection(
    modifier: Modifier = Modifier,
    songs: List<Song> = emptyList(),
    onCapsuleClick: (Song) -> Unit = {}
) {
    // ★ Read daily play history — shows top-played songs today
    val dailyPlays by PlaybackHistory.dailyPlays.collectAsState()

    // Map daily play entries to actual Song objects from the library
    val topSongs: List<Song> = remember(dailyPlays, songs) {
        dailyPlays.take(6).mapNotNull { entry ->
            songs.find { it.id == entry.songId }
        }
    }

    // Default placeholder colors (used when no songs available)
    val placeholderColors = listOf(
        Color(0xFFFF6B6B), Color(0xFFFFD166), Color(0xFF6B9EFF),
        Color(0xFFFF8FAB), Color(0xFF51CF66), Color(0xFFB197FC)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Top",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = CalSansFamily
            )
            Icon(
                imageVector = CoralXIcons.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3 capsules
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until 3) {
                val song1 = topSongs.getOrNull(i * 2)
                val song2 = topSongs.getOrNull(i * 2 + 1)

                val color1 = if (song1 != null) songPlaceholderColor(song1.id) else placeholderColors[i * 2]
                val color2 = if (song2 != null) songPlaceholderColor(song2.id) else placeholderColors[i * 2 + 1]

                val capsuleShape = RoundedCornerShape(28.dp)
                val borderBrush = Brush.horizontalGradient(listOf(color1, color2))
                val bgBrush = Brush.horizontalGradient(
                    listOf(color1.copy(alpha = 0.25f), color2.copy(alpha = 0.25f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(capsuleShape)
                        .background(bgBrush)
                        .border(2.5.dp, borderBrush, capsuleShape)
                        .padding(6.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                // Pass the first song of this capsule (or the second if first is null)
                                (song1 ?: song2)?.let { onCapsuleClick(it) }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cover slot 1
                        AlbumArtCircle(
                            artUri = song1?.albumArtUri,
                            placeholderColor = color1
                        )
                        // Cover slot 2
                        AlbumArtCircle(
                            artUri = song2?.albumArtUri,
                            placeholderColor = color2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumArtCircle(
    artUri: Uri?,
    placeholderColor: Color
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(placeholderColor.copy(alpha = 0.7f))
            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (artUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artUri)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "+",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun songPlaceholderColor(songId: Long): Color {
    val hue = (songId % 360).toFloat()
    return Color.hsv(hue = hue, saturation = 0.45f, value = 0.22f)
}
