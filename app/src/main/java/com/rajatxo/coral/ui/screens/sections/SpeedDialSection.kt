package com.rajatxo.coral.ui.screens.sections

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.domain.model.Song
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily

/**
 * ★ SpeedDialSection — 3×3 grid of song tiles inside a glass container.
 *
 * Ported from old Coral's SpeedDialSection, adapted for Coral X.
 * Now accepts real [Song] objects from the music scanner. Each tile shows
 * the song title with a deterministic color derived from the song ID.
 *
 * Glass container: RoundedCornerShape(20dp) with dark translucent bg
 * + subtle border. Same look as old Coral.
 *
 * Last tile (bottom-right) = dice randomize button.
 *
 * @param songs The songs to display (up to 8). If fewer than 8, empty
 *   slots show a "+" placeholder. If more, only first 8 are shown.
 * @param onTileClick Called with the song that was clicked.
 * @param onDiceClick Called when the dice (randomize) button is clicked.
 */
@Composable
fun SpeedDialSection(
    modifier: Modifier = Modifier,
    songs: List<Song> = emptyList(),
    onHeaderClick: () -> Unit = {},
    onTileClick: (Song) -> Unit = {},
    onDiceClick: () -> Unit = {}
) {
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
        }

        // Glass container with 3×3 grid
        val containerShape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(containerShape)
                .background(Color(0xFF1A1A1A).copy(alpha = 0.4f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), containerShape)
                .padding(8.dp)
        ) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0 until 3) {
                        val index = row * 3 + col
                        val isDice = (index == 8)

                        if (isDice) {
                            DiceTile(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                                    .height(100.dp),
                                onClick = onDiceClick
                            )
                        } else {
                            val song = songs.getOrNull(index)
                            if (song != null) {
                                SpeedDialTile(
                                    title = song.title,
                                    artist = song.artist,
                                    color = songPlaceholderColor(song.id),
                                    onClick = { onTileClick(song) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .height(100.dp)
                                )
                            } else {
                                // Empty slot — placeholder
                                SpeedDialTile(
                                    title = "",
                                    artist = "",
                                    color = Color(0xFF1A1A1A),
                                    isEmpty = true,
                                    onClick = {},
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .height(100.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Derive a stable, dark-ish color from a song ID for the tile background.
 * Each song gets a consistent hue based on its ID so tiles look varied
 * but stable across recompositions.
 */
private fun songPlaceholderColor(songId: Long): Color {
    val hue = (songId % 360).toFloat()
    return Color.hsv(hue = hue, saturation = 0.45f, value = 0.22f)
}

@Composable
private fun SpeedDialTile(
    title: String,
    artist: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEmpty: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
        label = "tileScale"
    )

    val tileShape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = 4.dp, shape = tileShape, clip = false)
            .clip(tileShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Background gradient (or flat color for empty slots)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isEmpty) Modifier.background(Color(0xFF1A1A1A).copy(alpha = 0.5f))
                    else Modifier.background(
                        Brush.verticalGradient(listOf(color, color.copy(alpha = 0.5f)))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isEmpty) {
                Text(
                    text = "+",
                    color = Color.White.copy(alpha = 0.2f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = CoralXIcons.Music,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Title at bottom (only for non-empty tiles)
        if (!isEmpty && title.isNotBlank()) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = CalSansFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, end = 4.dp, bottom = 6.dp)
            )
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
