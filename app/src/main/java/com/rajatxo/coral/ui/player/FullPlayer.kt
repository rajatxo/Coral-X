package com.rajatxo.coral.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rajatxo.coral.playback.PlaybackConnection
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily
import com.rajatxo.coral.ui.util.consistentNavBarPadding
import com.rajatxo.coral.util.CoralPalette
import com.rajatxo.coral.util.extractPalette
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * ★ FullPlayer — ported from old Coral's FullPlayer.
 *
 * Layout (top → bottom):
 *   1. Full-bleed album art (top 65% of screen, edge-to-edge)
 *   2. Top-left: back button (circular, translucent dark)
 *      Top-right: more button (circular, translucent dark)
 *   3. Right-side vertical pill: skip-prev / favorite / skip-next
 *   4. Bottom panel (solid black):
 *      - Seek bar (drag-to-scrub, thickens on hold)
 *      - Time labels (current / remaining)
 *      - Play/pause circle (white, centered)
 *      - Song title + artist + album
 *
 * Uses PlaybackConnection for all playback state — no params needed.
 *
 * @param onDismiss Called when the user taps back / swipes down.
 */
@Composable
fun FullPlayer(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val playback = PlaybackConnection
    val song by playback.currentSong.collectAsState()
    val isPlaying by playback.isPlaying.collectAsState()
    val positionMs by playback.positionMs.collectAsState()
    val durationMs by playback.durationMs.collectAsState()

    // Poll position every 200ms while playing
    LaunchedEffect(isPlaying) {
        while (true) {
            playback.pollPosition()
            delay(if (isPlaying) 200L else 1000L)
        }
    }

    // Subtle text shadow for readability on bright album art
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.6f),
        offset = Offset(1f, 1f),
        blurRadius = 3f
    )

    // Palette extraction from album art
    var palette by remember { mutableStateOf(CoralPalette.Default) }
    LaunchedEffect(song?.albumArtUri) {
        val artUri = song?.albumArtUri
        if (artUri != null) {
            extractPalette(context, artUri)?.let { palette = it }
        }
    }
    val animatedAccentColor by animateColorAsState(palette.accent, tween(600), label = "accent")

    // Seek bar state
    var isDragging by remember { mutableStateOf(false) }
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 4.dp,
        animationSpec = tween(200),
        label = "trackHeight"
    )
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val progress = if (durationMs > 0)
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    else 0f
    val displayProgress = dragFraction ?: progress

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // (1) Full-bleed album art — top 65% of screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .align(Alignment.TopCenter)
                .pointerInput(song?.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            // Double-tap could toggle favorite in the future
                        }
                    )
                }
        ) {
            val artUri = song?.albumArtUri
            if (artUri != null) {
                AsyncImage(
                    model = artUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Gradient overlay at bottom of art: transparent → solid black
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black)
                        )
                    )
            )
        }

        // (2) Top buttons — back (left) + more (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CoralXIcons.ChevronDown,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            // More button (placeholder)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CoralXIcons.Ellipsis,
                    contentDescription = "More",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // (3) Right-side vertical pill — skip-prev / skip-next
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Skip previous
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false)
                        ) { playback.prev() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CoralXIcons.SkipPrev,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                // Skip next
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false)
                        ) { playback.next() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CoralXIcons.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // (4) Bottom panel — seek bar + play/pause + song info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .consistentNavBarPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Seek bar
            var seekbarWidthPx by remember { mutableFloatStateOf(1f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .onSizeChanged { seekbarWidthPx = it.width.toFloat() }
                    .pointerInput(durationMs) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                dragFraction?.let { frac ->
                                    if (durationMs > 0) playback.seekTo((frac * durationMs).toLong())
                                }
                                dragFraction = null
                            },
                            onDragCancel = { isDragging = false; dragFraction = null },
                            onDrag = { change, _ ->
                                if (durationMs > 0 && seekbarWidthPx > 0) {
                                    val frac = (change.position.x / seekbarWidthPx).coerceIn(0f, 1f)
                                    dragFraction = frac
                                }
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .clip(RoundedCornerShape(trackHeight / 2))
                        .align(Alignment.CenterStart)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(displayProgress)
                        .height(trackHeight)
                        .clip(RoundedCornerShape(trackHeight / 2))
                        .align(Alignment.CenterStart)
                        .background(Color.White)
                )
            }

            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime((displayProgress * durationMs).toLong()),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    style = TextStyle(shadow = textShadow)
                )
                Text(
                    text = "-" + formatTime(((1f - displayProgress) * durationMs).toLong()),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    style = TextStyle(shadow = textShadow)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Play/pause circle (white, centered)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false)
                    ) { playback.playPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) CoralXIcons.PauseLucide else CoralXIcons.PlayLucide,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Song title
            Text(
                text = song?.title ?: "No song",
                color = Color.White,
                fontSize = 24.sp,
                fontFamily = CalSansFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = textShadow)
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Artist name
            Text(
                text = song?.artist ?: "",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontFamily = CalSansFamily,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = textShadow)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Album name (with music icon)
            val albumName = song?.album
            if (albumName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Image(
                        imageVector = CoralXIcons.Music,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.5f)),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = albumName,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(shadow = textShadow)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(
                    text = if (durationMs > 0) formatTime(durationMs) else "--:--",
                    textShadow = textShadow
                )
                InfoChip(
                    text = "Offline",
                    textShadow = textShadow
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    text: String,
    accentColor: Color = Color.White,
    textShadow: Shadow
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (accentColor == Color.White) Color.White.copy(alpha = 0.8f) else accentColor,
            fontSize = 12.sp,
            style = TextStyle(shadow = textShadow)
        )
    }
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * Helper — collectAsState without lifecycle-runtime-compose dependency.
 */
@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsState() =
    androidx.compose.runtime.collectAsState(initial = value)
