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
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rajatxo.coral.playback.PlaybackConnection
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily
import com.rajatxo.coral.util.CoralPalette
import com.rajatxo.coral.util.PaletteCache
import com.rajatxo.coral.util.adaptiveContrastColor
import com.rajatxo.coral.util.extractPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ★ Spiral2Player — ported from old Coral's Spiral2Player, stripped down to
 *   LAYOUT ONLY (no lyrics, no queue, no sleep-timer, no delete-song).
 *
 * Layout (BitChord-style Column flow, top → bottom):
 *   1. Opaque black base (fades together with whole player during drag-down).
 *   2. 96dp blurred album art fills entire screen as background.
 *   3. Column flow:
 *        a. COVER + HEADER (Box, weight 1f) — cover is FULL-BLEED edge-to-edge
 *           (top of screen, under status bar). Header Row floats OVER the
 *           cover via statusBarsPadding + align(TopCenter). NO solid black
 *           strip at the top. Cover's bottom edge dissolves into the blurred
 *           bg via a DstIn mask (no harsh line).
 *           On a tall phone: cover is large.
 *           On a small floating window: cover shrinks so the title + seek bar
 *           + transport fit below WITHOUT overlapping (fixes ViVi/Coral bug).
 *        b. Song title + 3-dot menu icon (LEFT-aligned, flows below cover)
 *        c. Artist name
 *        d. Seek bar (drag-to-scrub, thickens 4dp → 10dp on hold)
 *        e. Time labels: current / remaining
 *        f. Triple-circle transport pod: Prev · Play/Pause · Next
 *
 * Universal fitting (BitChord approach):
 *   - Column flow with weight(1f) on the cover (NO absolute offset positioning)
 *   - statusBarsPadding for the header overlay (floats over the cover)
 *   - navigationBarsPadding for bottom (real system inset, NOT forced ≥48dp
 *     which would be huge on a floating window)
 *   - graphicsLayer translation/alpha for drag-down-to-dismiss
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

    // ─── Lyrics state ─────────────────────────────────────────────
    // ★ Lyrics source priority (top to bottom):
    //   1. EMBEDDED in the audio file (via TagLib — TTML, ID3 SYLT/USLT, Vorbis LYRICS)
    //      → No network needed, 100% offline, the user's own file's lyrics.
    //   2. IMPORTED .lrc / pasted lyrics (user explicitly chose these via Import/Paste)
    //   3. CACHED fetched lyrics (from previous network fetch — instant, offline)
    //   4. NETWORK: LrcLib → KuGou → NetEase
    val lyricsRepository = remember { com.rajatxo.coral.data.lyrics.LyricsRepository(context) }
    val embeddedReader = remember { com.rajatxo.coral.data.lyrics.EmbeddedLyricsReader(context) }
    var lyricData by remember { mutableStateOf<com.rajatxo.coral.data.lyrics.Lyric?>(null) }
    var isLyricsLoading by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }

    // ★ AUTO-FETCH on song change. Keyed on song?.uri (so switching songs
    //   re-fetches) + song?.title + song?.artist (so title-only edits also
    //   re-fetch). Uses TagLib for embedded — separate native handle from
    //   ExoPlayer, so it's safe to call during playback (no silence bug).
    LaunchedEffect(song?.id, song?.title, song?.artist) {
        val title = song?.title
        val artist = song?.artist
        if (title.isNullOrBlank() || artist.isNullOrBlank()) {
            lyricData = null
            isLyricsLoading = false
            return@LaunchedEffect
        }
        // ★ Capture non-null locals — Kotlin doesn't smart-cast Compose state
        //   vals across withContext lambda boundaries.
        val safeTitle = title
        val safeArtist = artist
        val safeAlbum = song?.album
        val safeArtUri = song?.albumArtUri
        val safeSongUri = song?.uri
        isLyricsLoading = true

        // 1. EMBEDDED lyrics via TagLib (PRIMARY source — user's own file)
        //    TagLib reads ID3 SYLT/USLT, Vorbis LYRICS, M4A ©lyr — any format.
        //    Returns raw text (could be plain, LRC, Enhanced LRC, or TTML).
        //    The downstream parser handles all of these.
        if (safeSongUri != null) {
            val embeddedRaw = withContext(kotlinx.coroutines.Dispatchers.IO) {
                embeddedReader.readEmbeddedLyrics(safeSongUri)
            }
            if (!embeddedRaw.isNullOrBlank()) {
                val parsedLines = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.rajatxo.coral.data.lyrics.LrcParser.parse(embeddedRaw)
                }
                if (parsedLines.isNotEmpty()) {
                    val hasWordSync = parsedLines.any { it.hasWordSync }
                    val hasTimestamps = parsedLines.any { it.timeMs >= 0 }
                    lyricData = com.rajatxo.coral.data.lyrics.Lyric(
                        synced = hasTimestamps,
                        lines = parsedLines,
                        source = com.rajatxo.coral.data.lyrics.LyricSource.EMBEDDED,
                        trackName = safeTitle,
                        artistName = safeArtist,
                        hasWordSync = hasWordSync,
                        rawText = embeddedRaw
                    )
                    isLyricsLoading = false
                    return@LaunchedEffect
                }
            }
        }

        // 2. IMPORTED .lrc (user explicitly imported/pasted these — high priority)
        val imported = withContext(kotlinx.coroutines.Dispatchers.IO) {
            lyricsRepository.getImportedLrc(safeTitle, safeArtist)
        }
        if (imported != null) {
            lyricData = imported
            isLyricsLoading = false
            return@LaunchedEffect
        }
        // 3. CACHED fetched lyrics (instant, offline)
        val cached = withContext(kotlinx.coroutines.Dispatchers.IO) {
            lyricsRepository.getLyrics(safeTitle, safeArtist, safeAlbum, durationMs)
        }
        if (cached != null) {
            lyricData = cached
            isLyricsLoading = false
            return@LaunchedEffect
        }
        // 4. NETWORK fetch: LrcLib → KuGou → NetEase
        try {
            val fetched = withContext(kotlinx.coroutines.Dispatchers.IO) {
                lyricsRepository.fetchFromNetwork(safeTitle, safeArtist, safeAlbum, durationMs)
            }
            lyricData = fetched
        } catch (_: Exception) { }
        isLyricsLoading = false
    }

    // ★ Active line index — binary search for the current line based on position
    val activeLineIndex = if (lyricData != null && lyricData!!.synced && lyricData!!.lines.isNotEmpty()) {
        findActiveLineIndex(lyricData!!.lines, positionMs)
    } else -1

    // ★ Current lyric line plain text (used to compute the strip display)
    val lyricLineText = when {
        isLyricsLoading && lyricData == null -> "Loading..."
        lyricData != null && lyricData!!.synced && activeLineIndex >= 0 ->
            lyricData!!.lines[activeLineIndex].text.ifBlank { "♪" }
        lyricData != null && lyricData!!.synced && lyricData!!.lines.isNotEmpty() -> {
            val idx = activeLineIndex.coerceIn(0, lyricData!!.lines.lastIndex)
            lyricData!!.lines[idx].text.ifBlank { "♪" }
        }
        lyricData != null && !lyricData!!.synced && lyricData!!.lines.isNotEmpty() -> "♪"
        else -> "No Lyrics Available"
    }

    // ★ Build word-by-word annotated string for the lyrics strip
    //   - If the active line has word sync (Enhanced LRC / TTML):
    //     active/past words = WHITE BOLD, future words = WHITE 35%
    //   - Otherwise: whole line as WHITE
    //   - Recomputes on every positionMs update so the highlight tracks the
    //     playhead in real time (per-word granularity).
    val lyricStripText: androidx.compose.ui.text.AnnotatedString = if (
        lyricData != null && lyricData!!.synced && activeLineIndex >= 0
    ) {
        val activeLine = lyricData!!.lines[activeLineIndex]
        if (activeLine.hasWordSync && activeLine.words != null) {
            buildAnnotatedString {
                activeLine.words.forEach { word ->
                    val isWordActiveOrPast = positionMs >= word.startTime
                    if (isWordActiveOrPast) {
                        withStyle(SpanStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )) { append(word.text) }
                    } else {
                        withStyle(SpanStyle(
                            color = Color.White.copy(alpha = 0.35f)
                        )) { append(word.text) }
                    }
                    append(" ")
                }
            }
        } else {
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White)) {
                    append(lyricLineText)
                }
            }
        }
    } else {
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White)) {
                append(lyricLineText)
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // ROOT — Box (holds blurred bg + Column flow) + drag-dismiss
    //
    // ★ BITCHORD APPROACH (fixes floating-window overlap):
    //   The whole player is a Column flowing top→bottom:
    //     1. Blurred bg fills entire Box (absolute, behind everything)
    //     2. Column {
    //          Top header (fixed ~56dp)
    //          Cover Box (weight 1f — takes ALL remaining space)
    //          Title + artist (fixed, flows below cover)
    //          Seek bar + time labels (fixed)
    //          Transport row (fixed)
    //          Bottom padding (navigationBarsPadding)
    //        }
    //   The cover SHRINKS on small screens (floating window) so the title
    //   and seek bar NEVER overlap. On a full-screen phone, the cover is
    //   large (like the old 65% fraction). No absolute offset positioning.
    // ═════════════════════════════════════════════════════════════════
    Box(
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
        // ═════════════════════════════════════════════════════════════════
        // (1) BLURRED BACKGROUND — 96dp blur fills entire screen (absolute)
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
        // (2) HEART POP OVERLAY (double-tap to favorite — visual only)
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
        // (3) COLUMN FLOW — top → bottom (BitChord approach)
        //
        // ★ Cover is FULL-BLEED (edge-to-edge, top of screen) — NOT below the
        //   header. The header floats OVER the cover via statusBarsPadding,
        //   matching old Coral's Spiral layout. This avoids the solid black
        //   strip at the top that the previous Column-flow had.
        // ═════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // ── (a) COVER + HEADER (overlaid) — weight(1f), full-bleed ──
            //   Box holds: [cover fills entire Box] + [header Row overlaid on top]
            //   Cover extends edge-to-edge (under status bar); header floats over it.
            if (artUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Cover — fills the entire Box, edge-to-edge (full-bleed)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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

                    // Header Row — floats OVER the cover, status-bar aware
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
                }
            } else {
                // No album art — header alone + spacer keeps the layout flow correct
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    Text(
                        text = "NOW PLAYING",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = CalSansFamily,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                        style = TextStyle(shadow = textShadow)
                    )
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
                Spacer(modifier = Modifier.weight(1f))
            }

            // ── (c) SONG TITLE + 3-DOT MENU — flows below cover ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 8.dp, bottom = 12.dp)
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

            // ── (d) BOTTOM PANEL — lyrics strip + seek bar + transport pod ──
            // Flows naturally below the title block. No absolute BottomCenter
            // alignment — uses Column flow with Spacers.
            var seekbarWidthPx by remember { mutableFloatStateOf(1f) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 24.dp)
            ) {
                // ── Lyrics strip (1-line synced, word-by-word highlight, marquee) ──
                // ★ Crossfade ONLY on lyricLineText (changes once per line, ~every 3-5s).
                //   The word-by-word AnnotatedString is rebuilt inside the crossfade
                //   on every positionMs tick (200ms) for live word highlighting —
                //   but this does NOT re-trigger the crossfade animation.
                //   Previous bug: crossfade targeted the full AnnotatedString, which
                //   re-triggered the 400ms fade every 200ms → stuttery/flickering.
                androidx.compose.animation.Crossfade(
                    targetState = lyricLineText,
                    animationSpec = tween(400),
                    label = "lyricsLineFade"
                ) { _lineText ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showLyricsSheet = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Marquee holds Text + Chevron together so the chevron
                        // scrolls with the text and always sits at its end.
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee(
                                    initialDelayMillis = 1_200,
                                    velocity = 40.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lyricStripText,
                                fontSize = 18.sp,
                                fontFamily = CalSansFamily,
                                maxLines = 1,
                                style = TextStyle(shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    offset = Offset(1f, 1f),
                                    blurRadius = 4f
                                ))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = CoralXIcons.ChevronRight,
                                contentDescription = "Open lyrics",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

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

        // ─── Lyrics Sheet overlay (full-screen karaoke page) ────────
        // Opens when user taps the lyrics strip. Renders the full cinematic
        // lyrics sheet with word-by-word karaoke animation, tap-to-seek,
        // and the 3-dot menu (Fetch, Import LRC, Paste Lyrics).
        if (showLyricsSheet) {
            com.rajatxo.coral.ui.lyrics.LyricsSheet(
                trackName = song?.title ?: "",
                artistName = song?.artist ?: "",
                albumName = song?.album,
                durationMs = durationMs,
                currentPositionMs = positionMs,
                isPlaying = isPlaying,
                onDismiss = { showLyricsSheet = false },
                onSeek = { ms -> playback.seekTo(ms) },
                albumArtUri = artUri,
                onLyricsFetched = { fetched ->
                    if (fetched != null) lyricData = fetched
                }
            )
        }
    }
}

/**
 * Binary search for the active lyric line at the given position.
 * Returns -1 if no line is active yet (position before first line).
 */
private fun findActiveLineIndex(
    lines: List<com.rajatxo.coral.data.lyrics.LyricLine>,
    positionMs: Long
): Int {
    if (lines.isEmpty()) return -1
    var lo = 0
    var hi = lines.lastIndex
    var result = -1
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        if (lines[mid].timeMs in 0..positionMs) {
            result = mid
            lo = mid + 1
        } else if (lines[mid].timeMs > positionMs) {
            hi = mid - 1
        } else {
            lo = mid + 1
        }
    }
    return result
}

/** m:ss formatter — used for both elapsed and remaining times. */
private fun formatTime(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
