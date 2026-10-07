package com.rajatxo.coral.ui.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.rajatxo.coral.ui.icons.CoralIcons
import kotlinx.coroutines.launch

/**
 * ★ CoralMiniPlayer — fixed-height mini player, part of the bottom stack.
 *
 * LAYOUT CONTRACT:
 *   - Width: 259dp (industry-standard mini player width — fits album art
 *     + title/artist + play/pause + next comfortably on every phone)
 *   - Height: 64dp (CoralLayout.MiniPlayerHeight)
 *   - Shape: RoundedCornerShape(50.dp) — full pill / capsule shape
 *   - Glass morphism via Kyant drawBackdrop (real AGSL blur of content behind)
 *
 * GESTURES:
 *   - Tap anywhere → open full player (onMiniPlayerClick)
 *   - Swipe up → open full player (more natural than tap)
 *   - Swipe left/right (past threshold) → dismiss mini player + pause playback
 *
 * POSITIONING:
 *   - Parent (CoralScaffold) handles placement in bottom Column stack
 *   - Mini player does NOT use any alignment/padding for its own position
 */
@Composable
fun CoralMiniPlayer(
    title: String,
    artist: String,
    albumArtUri: android.net.Uri?,
    songId: Long?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit,
    backdrop: LayerBackdrop? = null
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Drag state — swipe up to open, swipe sideways to dismiss
    var dragDirection by remember { mutableStateOf<Int?>(null) }
    var totalDragX by remember { mutableStateOf(0f) }
    var totalDragY by remember { mutableStateOf(0f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    val screenWidthPx = with(density) {
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    // Reset offset when song changes
    LaunchedEffect(songId) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        totalDragX = 0f
        totalDragY = 0f
        dragDirection = null
    }

    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .width(259.dp)
            .height(CoralLayout.MiniPlayerHeight)
            .clip(RoundedCornerShape(50.dp))
            .graphicsLayerOffset(offsetX.value, offsetY.value)
            .pointerInput(songId) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                        dragDirection = null
                    },
                    onDragEnd = {
                        scope.launch {
                            when (dragDirection) {
                                1 -> {  // vertical — swipe up
                                    onClick()
                                    offsetY.snapTo(0f)
                                }
                                2 -> {  // horizontal — dismiss
                                    if (kotlin.math.abs(totalDragX) > screenWidthPx / 4) {
                                        val direction = if (totalDragX > 0) 1f else -1f
                                        offsetX.animateTo(
                                            targetValue = direction * screenWidthPx,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        onClick()  // dismiss
                                    } else {
                                        offsetX.animateTo(0f)
                                    }
                                }
                            }
                            offsetX.snapTo(0f)
                            offsetY.snapTo(0f)
                            totalDragX = 0f
                            totalDragY = 0f
                            dragDirection = null
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragDirection == null) {
                            // Determine direction once user starts moving
                            dragDirection = if (kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)) 1 else 2
                        }
                        when (dragDirection) {
                            1 -> {  // vertical
                                totalDragY += dragAmount.y
                                // Only allow upward swipe (negative y)
                                val targetY = (offsetY.value + dragAmount.y).coerceAtMost(0f)
                                scope.launch { offsetY.snapTo(targetY) }
                            }
                            2 -> {  // horizontal
                                totalDragX += dragAmount.x
                                scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                            }
                        }
                    }
                )
            }
            .pointerInput(songId) {
                detectTapGestures(onTap = { onClick() })
            }
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(50.dp) },
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
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.1f))
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art (left)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                AsyncImage(
                    model = albumArtUri,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Title + artist (center)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play/pause button (right)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPlayPauseClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) CoralIcons.PauseLucide else CoralIcons.PlayLucide,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Progress bar at the very bottom of the pill (1dp line)
        if (durationMs > 0) {
            LinearProgressIndicator(
                progress = { progress },
                color = Color.White,
                trackColor = Color.Transparent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
            )
        }
    }
}

/** Helper: apply offset via graphicsLayer instead of offset() for perf. */
private fun Modifier.graphicsLayerOffset(offsetX: Float, offsetY: Float): Modifier =
    this.then(
        Modifier.graphicsLayer {
            translationX = offsetX
            translationY = offsetY
        }
    )
