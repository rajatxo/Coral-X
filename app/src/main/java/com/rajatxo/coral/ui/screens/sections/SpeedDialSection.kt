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
import androidx.compose.foundation.layout.fillMaxHeight
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
 * ★ SpeedDialSection — 3×3 grid of song tiles inside a glass container.
 *
 * Ported from old Coral's SpeedDialSection. Each tile shows real album art
 * with Spiral 2.0 blur-blend (blurred bottom + sharp top + DstIn blend)
 * and the song title overlaid at the bottom (no artist — matches old Coral).
 *
 * Last tile (bottom-right) = shuffle/dice button.
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

        // 3×3 grid — tiles arranged directly, NO surrounding container.
        // Matches old Coral's Astra mode: each tile is open (no glass box around them).
        Column(
            modifier = Modifier.fillMaxWidth()
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
                                    song = song,
                                    onClick = { onTileClick(song) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .height(100.dp)
                                )
                            } else {
                                EmptyTile(
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

private fun songPlaceholderColor(songId: Long): Color {
    val hue = (songId % 360).toFloat()
    return Color.hsv(hue = hue, saturation = 0.45f, value = 0.22f)
}

/**
 * SpeedDialTile — Spiral 2.0 blur-blend album art with song title overlay.
 * Matches old Coral's SpeedDialCard exactly:
 *   - Layer 1: blurred album art (full tile)
 *   - Layer 2: sharp album art (top 85%, DstIn blend at bottom)
 *   - Title at bottom (on the blurred part)
 */
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

    val tileShape = RoundedCornerShape(8.dp)
    val fallbackColor = songPlaceholderColor(song.id)

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
        // ═══ Spiral 2.0-style album art blur-blend ═══
        // Layer 1 (bottom): BLURRED album art
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

        // Title at the bottom (on the blurred part) — song title only, no artist
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
    }
}

@Composable
private fun EmptyTile(modifier: Modifier = Modifier) {
    val tileShape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(tileShape)
            .background(Color(0xFF1A1A1A).copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            color = Color.White.copy(alpha = 0.2f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
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
