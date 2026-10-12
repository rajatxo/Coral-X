package com.rajatxo.coral.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.rajatxo.coral.playback.PlaybackConnection
import com.rajatxo.coral.ui.foundation.LocalGlassBackdrop
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily
import com.rajatxo.coral.ui.util.consistentNavBarPadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * ★ MiniPlayer — ported from old Coral's HomeScreen.MiniPlayer.
 *
 * A glass pill at the bottom of the screen showing the current song.
 * Layout: [circular album art + progress ring] [title + artist] [heart]
 *
 * Gestures:
 *   - Tap → opens FullPlayer (onExpand callback)
 *   - Swipe UP → opens FullPlayer
 *   - Swipe LEFT/RIGHT → dismisses (pauses + hides)
 *
 * Uses PlaybackConnection for playback state — no params needed.
 *
 * @param onExpand Called when the user taps or swipes up to open the full player.
 * @param onDismiss Called when the user swipes the mini player away.
 */
@Composable
fun MiniPlayer(
    onExpand: () -> Unit,
    onDismiss: () -> Unit
) {
    val playback = PlaybackConnection
    val song by playback.currentSong.collectAsState()
    val isPlaying by playback.isPlaying.collectAsState()
    val positionMs by playback.positionMs.collectAsState()
    val durationMs by playback.durationMs.collectAsState()

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidthPx = with(density) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    val maxSwipeUpPx = with(density) { 60.dp.toPx() }

    var dragDirection: Int? by remember { mutableStateOf(null) }
    var totalDragX by remember { mutableStateOf(0f) }
    var totalDragY by remember { mutableStateOf(0f) }

    val pillShape: Shape = RoundedCornerShape(32.dp)

    // ★ Read the glass backdrop from the scaffold (for real frosted-glass blur)
    val backdrop = LocalGlassBackdrop.current

    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            // ★ NO consistentNavBarPadding() here — the scaffold's Box
            // already positions the mini player 10dp above the tab capsule.
            // Adding consistentNavBarPadding here would double-pad it,
            // pushing it way too high (48dp extra).
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value.coerceAtLeast(-maxSwipeUpPx)
                val s = scale.value
                scaleX = s
                scaleY = s
                alpha = (1f - abs(offsetX.value) / screenWidthPx).coerceIn(0f, 1f) *
                    (1f - (abs(offsetY.value) / maxSwipeUpPx)).coerceIn(0f, 1f)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragDirection = null
                        totalDragX = 0f
                        totalDragY = 0f
                        scope.launch { scale.snapTo(0.96f) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        if (dragDirection == null) {
                            if (abs(totalDragX) > 20f || abs(totalDragY) > 20f) {
                                dragDirection = if (abs(totalDragX) > abs(totalDragY)) 0 else 1
                            }
                        }

                        when (dragDirection) {
                            0 -> scope.launch { offsetX.snapTo(totalDragX) }
                            1 -> {
                                if (totalDragY < 0) {
                                    scope.launch { offsetY.snapTo(totalDragY * 0.4f) }
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        scope.launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                        when (dragDirection) {
                            0 -> {
                                if (abs(totalDragX) > screenWidthPx * 0.4f) {
                                    val target = if (totalDragX < 0) -screenWidthPx else screenWidthPx
                                    scope.launch {
                                        offsetX.animateTo(target, tween(200))
                                        onDismiss()
                                        delay(100)
                                        offsetX.snapTo(0f)
                                    }
                                } else {
                                    scope.launch {
                                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                }
                            }
                            1 -> {
                                if (totalDragY < -30f) {
                                    scope.launch {
                                        offsetY.animateTo(-maxSwipeUpPx * 2f, tween(200))
                                        onExpand()
                                    }
                                } else {
                                    scope.launch {
                                        offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    }
                )
            }
    ) {
        // Frosted-glass pill background — uses kyant drawBackdrop when available
        // (same AGSL real-time blur as the nav bar + search FAB).
        // Falls back to flat dark background when no backdrop (shouldn't happen).
        val bodyModifier = if (backdrop != null) {
            Modifier
                .width(240.dp)
                .height(64.dp)
                .clip(pillShape)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { pillShape },
                    effects = {
                        vibrancy()
                        colorControls(
                            brightness = 0.05f,
                            contrast = 1f,
                            saturation = 1.3f
                        )
                        blur(18f.dp.toPx())  // AGSL real-time backdrop blur
                    },
                    onDrawSurface = {
                        drawRect(Color.Black.copy(alpha = 0.35f))
                    }
                )
                .border(1.dp, Color.White.copy(alpha = 0.2f), pillShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onExpand
                )
        } else {
            Modifier
                .width(240.dp)
                .height(64.dp)
                .clip(pillShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), pillShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onExpand
                )
        }
        Box(modifier = bodyModifier) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- Circular album art + progress ring (LEFT) ---
                val progress = if (durationMs > 0) {
                    (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(56.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { playback.playPause() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Progress ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 2.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = androidx.compose.ui.geometry.Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )
                        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                        drawArc(
                            color = Color.White.copy(alpha = 0.15f),
                            startAngle = -90f, sweepAngle = 360f, useCenter = false,
                            topLeft = topLeft, size = arcSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                        drawArc(
                            color = Color.White,
                            startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                            topLeft = topLeft, size = arcSize,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }

                    // Album art
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val artUri = song?.albumArtUri
                        if (artUri != null) {
                            AsyncImage(
                                model = artUri,
                                contentDescription = "Album art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CoralXIcons.Music,
                                    contentDescription = null,
                                    tint = Color(0xFFB0B0B0),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        // Play/pause overlay
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) CoralXIcons.Pause else CoralXIcons.Play,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // --- Title + artist ---
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = song?.title ?: "",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = CalSansFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song?.artist ?: "",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontFamily = CalSansFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // --- Next button (RIGHT) ---
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { playback.next() }
                        ),
                contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CoralXIcons.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
