package com.rajatxo.coral.ui.player

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rajatxo.coral.playback.PlaybackConnection
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily
import com.rajatxo.coral.ui.util.consistentNavBarPadding
import com.rajatxo.coral.util.CoralPalette
import com.rajatxo.coral.util.PaletteCache
import com.rajatxo.coral.util.adaptiveContrastColor
import com.rajatxo.coral.util.extractPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ★ Spiral2Player — ported from old Coral's Spiral2Player, stripped down to
 *   LAYOUT ONLY (no lyrics, no queue, no sleep-timer, no delete-song).
 *
 * Layout (top → bottom):
 *   1. Opaque black base (fades together with whole player during drag-down).
 *   2. 96dp blurred album art fills entire screen as background.
 *   3. Sharp full-bleed cover at top 65% — bottom edge dissolves into the
 *      blurred bg via a DstIn mask (no black gradient, no harsh line).
 *   4. Top header: chevron-down · NOW PLAYING · ellipsis (3-dot menu).
 *   5. Song title + 3-dot menu icon (LEFT-aligned, sitting at the blend point
 *      where the cover dissolves into the blur).
 *   6. Artist name below the title.
 *   7. Bottom panel (consistentNavBarPadding ≥48dp):
 *        - Seek bar (drag-to-scrub, thickens from 4dp → 10dp on hold).
 *        - Time labels: current / remaining.
 *        - Triple-circle transport pod: Prev (translucent dark) · Play/Pause
 *          (solid white, slightly larger) · Next (translucent dark).
 *
 * Universal fitting:
 *   - BoxWithConstraints + maxHeight * 0.65f (NO screenHeightDp fraction).
 *   - statusBarsPadding for top header.
 *   - consistentNavBarPadding (≥48dp) for bottom panel.
 *   - graphicsLayer translation/alpha for drag-down-to-dismiss.
 *
 * Reads everything from PlaybackConnection — no params needed except onDismiss.
 *
 * @param onDismiss Called when the user taps back, swipes down past 15% of
 *                  screen height, or presses the chevron-down.
 */
@Composable
fun Spiral2Player(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val playback = PlaybackConnection
    val song by playback.currentSong.collectAsState()
    val isPlaying by playback.isPlaying.collectAsState()
    val positionMs by playback.positionMs.collectAsState()
    val durationMs by playback.durationMs.collectAsState()

    // ─── Poll playback position every 200ms while playing ───────────
    LaunchedEffect(isPlaying) {
        while (true) {
            playback.pollPosition()
            delay(if (isPlaying) 200L else 1000L)
        }
    }

    // ─── Text shadows (small for title, smaller for artist) ────────
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.6f),
        offset = Offset(1f, 1f),
        blurRadius = 3f
    )
    val artistShadow = Shadow(
        color = Color.Black.copy(alpha = 0.35f),
        offset = Offset(0.5f, 0.5f),
        blurRadius = 2f
    )

    // ─── Palette (instant from cache, async extract if missing) ────
    val artUri = song?.albumArtUri
    var palette by remember(artUri) {
        mutableStateOf(PaletteCache.get(artUri) ?: CoralPalette.Default)
    }
    LaunchedEffect(artUri) {
        if (artUri != null && PaletteCache.get(artUri) == null) {
            extractPalette(context, artUri)?.let {
                palette = it
                PaletteCache.put(artUri, it)
            }
        }
    }
    // Smooth crossfade when palette changes (no grey flash on song switch)
    val animatedAccentColor by animateColorAsState(palette.accent, tween(600), label = "accent")
    // Adaptive accent — brightens if accent is too close to the bg color
    val adaptiveAccent = adaptiveContrastColor(animatedAccentColor, palette.tertiary)

    // ─── Drag-down-to-dismiss (15% threshold, bg fades with drag) ──
    val dismissDragY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val screenHeightPx = view.rootView.height.toFloat()
    val dismissThreshold = screenHeightPx * 0.15f
    val bgAlpha = (1f - (dismissDragY.value / dismissThreshold)).coerceIn(0f, 1f)

    // ─── Seek bar state ───────────────────────────────────────────
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

    // ─── Double-tap heart pop (visual feedback only, no favorites store yet) ──
    var showHeartPop by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }
    LaunchedEffect(showHeartPop) {
        if (showHeartPop) {
            heartScale.snapTo(0f)
            heartScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            delay(400)
            heartScale.animateTo(
                targetValue = 0f,
                animationSpec = tween(300)
            )
            showHeartPop = false
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // ROOT — BoxWithConstraints (universal fitting rule #7)
    // ═════════════════════════════════════════════════════════════════
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Whole player translates + fades during drag-down.
            .graphicsLayer {
                translationY = dismissDragY.value
                alpha = bgAlpha
            }
            // Black base drawn via drawBehind INSIDE the graphicsLayer alpha
            // scope — fades TOGETHER with the player during drag-down.
            .drawBehind { drawRect(Color.Black) }
            // Drag-down-to-dismiss gesture (anywhere on the player)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dismissDragY.value > dismissThreshold) {
                            // Past threshold — animate down off-screen, then dismiss
                            coroutineScope.launch {
                                dismissDragY.animateTo(
                                    targetValue = screenHeightPx,
                                    animationSpec = tween(200)
                                )
                                onDismiss()
                            }
                        } else {
                            // Below threshold — snap back to 0
                            coroutineScope.launch {
                                dismissDragY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(150)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dismissDragY.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(150)
                            )
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        // Only drag DOWN (positive) dismisses; up = no-op
                        coroutineScope.launch {
                            dismissDragY.snapTo(
                                (dismissDragY.value + dragAmount).coerceAtLeast(0f)
                            )
                        }
                    }
                )
            }
    ) {
        val screenHeightDp = maxHeight  // BoxWithConstraints scope — universal fitting

        // ═════════════════════════════════════════════════════════════════
        // (1) BLURRED BACKGROUND — 96dp blur fills entire screen
        // ═════════════════════════════════════════════════════════════════
        if (artUri != null) {
            AsyncImage(
                model = artUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(96.dp)  // RenderEffect — API 31+. Below: shows sharp (graceful fallback).
            )
        }

        // ═════════════════════════════════════════════════════════════════
        // (2) SHARP COVER (top 65%) — bottom edge dissolves into blur via DstIn
        // ═════════════════════════════════════════════════════════════════
        if (artUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .align(Alignment.TopCenter)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        // Bottom fade: opaque → transparent over 140dp.
                        // NO black gradient — just a DstIn mask so the sharp
                        // cover dissolves into the blurred bg behind it.
                        val fadeHeightPx = 140.dp.toPx()
                        val startY = (size.height - fadeHeightPx).coerceAtLeast(0f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startY = startY,
                                endY = size.height
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .pointerInput(artUri) {
                        detectTapGestures(
                            onDoubleTap = {
                                // Double-tap = visual heart pop (favorites store TBD)
                                showHeartPop = true
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        )
                    }
            ) {
                AsyncImage(
                    model = artUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ═════════════════════════════════════════════════════════════════
        // (3) HEART POP OVERLAY (double-tap to favorite — visual only)
        // ═════════════════════════════════════════════════════════════════
        if (showHeartPop) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    imageVector = CoralXIcons.HeartFilled,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(adaptiveAccent),
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            scaleX = heartScale.value
                            scaleY = heartScale.value
                            alpha = heartScale.value
                        }
                )
            }
        }

        // ═════════════════════════════════════════════════════════════════
        // (4) TOP HEADER — chevron-down · NOW PLAYING · ellipsis
        // ═════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button — translucent dark capsule
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false)
                    ) { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CoralXIcons.ChevronDown,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            // NOW PLAYING label — centered, letter-spaced uppercase
            Text(
                text = "NOW PLAYING",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = CalSansFamily,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                style = TextStyle(shadow = textShadow)
            )
            // More button (3-dot) — placeholder, no popup yet
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false)
                    ) { /* TBD: more menu capsule */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CoralXIcons.Ellipsis,
                    contentDescription = "More",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ═════════════════════════════════════════════════════════════════
        // (5) SONG TITLE + 3-DOT MENU (LEFT-aligned, at the blend point)
        // ═════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                // ★ Sit at the blend point — the title floats right where the
                //   sharp cover dissolves into the blurred bg.
                .offset {
                    IntOffset(
                        x = 0,
                        y = (screenHeightDp * 0.65f).toPx().toInt()
                    )
                }
                .padding(horizontal = 28.dp)
        ) {
            // Title row — title takes available space, menu icon hugs the end
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song?.title ?: "No song",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontFamily = CalSansFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    style = TextStyle(shadow = textShadow),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                // 3-dot menu icon — placeholder for now (no popup yet)
                Icon(
                    imageVector = CoralXIcons.Ellipsis,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false)
                        ) { /* TBD: open menu capsule with shuffle / loop */ }
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            // Artist name (60% white, smaller shadow)
            Text(
                text = song?.artist ?: "",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp,
                fontFamily = CalSansFamily,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                style = TextStyle(shadow = artistShadow),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ═════════════════════════════════════════════════════════════════
        // (6) BOTTOM PANEL — seek bar + time labels + transport pod
        // ═════════════════════════════════════════════════════════════════
        var seekbarWidthPx by remember { mutableFloatStateOf(1f) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .consistentNavBarPadding()  // ≥48dp bottom (universal fitting)
                .padding(horizontal = 28.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Seek bar (drag-to-scrub, thickens on hold, no thumb) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)  // touch target — track is thinner inside
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
                // Track (dim white)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .clip(RoundedCornerShape(trackHeight / 2))
                        .align(Alignment.CenterStart)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                // Progress (bright white)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(displayProgress)
                        .height(trackHeight)
                        .clip(RoundedCornerShape(trackHeight / 2))
                        .align(Alignment.CenterStart)
                        .background(Color.White)
                )
            }

            // ── Time labels (current left, remaining right) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime((displayProgress * durationMs).toLong()),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = CalSansFamily
                )
                Text(
                    text = "-" + formatTime(((1f - displayProgress) * durationMs).toLong()),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontFamily = CalSansFamily
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Triple-circle transport pod (Prev · Play/Pause · Next) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous — translucent dark circle (glass-like)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false)
                        ) {
                            playback.prev()
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CoralXIcons.Rewind,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                // Play/Pause — solid white, slightly larger, center
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false)
                        ) {
                            playback.playPause()
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) CoralXIcons.PauseLucide else CoralXIcons.PlayLucide,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                // Next — translucent dark circle (glass-like)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false)
                        ) {
                            playback.next()
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CoralXIcons.FastForward,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

/** m:ss formatter — used for both elapsed and remaining times. */
private fun formatTime(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
