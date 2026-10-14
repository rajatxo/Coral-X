package com.rajatxo.coral.ui.lyrics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.data.lyrics.LyricLine
import com.rajatxo.coral.data.lyrics.WordTimestamp
import com.rajatxo.coral.ui.theme.CalSansFamily
import kotlin.math.abs

/**
 * ★ SpatialFlowLyrics — per-character word-by-word lyrics renderer.
 *
 * Adapted from SpatialFlow (github.com/MythicalSHUB/SpatialFlow, Apache 2.0).
 *
 * Approach:
 *   1. Two stacked Text layers per line: dim base (grey) + bright overlay (white)
 *   2. Bright overlay uses drawWithCache + eraseFutureText to erase unsung characters
 *   3. Per-character progress via calculateCharProgress with easeOutCubic easing
 *   4. Soft gradient at the sweep boundary (horizontalGradient with BlendMode.DstOut)
 *   5. Smooth position via animateFloatAsState (200ms tween, LinearEasing)
 *
 * This handles:
 *   - Word-synced lyrics (Enhanced LRC, TTML) — per-character sweep
 *   - Line-synced lyrics — just dim/bright highlight
 *   - Multi-line wrapping — per-character getBoundingBox + getPathForRange
 *   - No crashes — no init { require() }, no KaraokeSyllable constraints
 */
@Composable
fun SpatialFlowLyrics(
    lines: List<LyricLine>,
    currentPositionMs: Long,
    isPlaying: Boolean,
    fontSizeSp: Float,
    onSeek: (Long) -> Unit
) {
    if (lines.isEmpty()) return

    val listState = rememberLazyListState()

    // ★ Smooth position interpolation — same as Lyricify/SpatialFlow
    var animatedPosition by remember { mutableLongStateOf(currentPositionMs) }
    val latestPosition by rememberUpdatedState(currentPositionMs)
    var lastUpdateTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(currentPositionMs) {
        lastUpdateTime = System.currentTimeMillis()
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                val elapsed = System.currentTimeMillis() - lastUpdateTime
                animatedPosition = latestPosition + elapsed
                withFrameNanos {}
            }
        } else {
            animatedPosition = latestPosition
        }
    }

    // ★ Active line index — binary search
    val activeIndex by remember(lines) {
        derivedStateOf {
            val pos = animatedPosition
            if (lines.isEmpty()) -1
            else {
                var lo = 0
                var hi = lines.lastIndex
                var result = -1
                while (lo <= hi) {
                    val mid = (lo + hi) / 2
                    if (lines[mid].timeMs in 0..pos) {
                        result = mid
                        lo = mid + 1
                    } else if (lines[mid].timeMs > pos) {
                        hi = mid - 1
                    } else {
                        lo = mid + 1
                    }
                }
                result
            }
        }
    }

    // ★ Auto-scroll
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val target = (activeIndex - 3).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 100.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(lines.size) { index ->
            val line = lines[index]
            val isActive = index == activeIndex
            val isPast = index < activeIndex

            SpatialFlowLyricLine(
                line = line,
                isActive = isActive,
                isPast = isPast,
                currentPositionProvider = { animatedPosition.toInt() },
                fontSizeSp = fontSizeSp,
                onSeek = { onSeek(line.timeMs) }
            )
        }
    }
}

/**
 * A single lyric line with per-character sweep.
 * Adapted from SpatialFlow's LyricLineItem (Apache 2.0).
 */
@Composable
private fun SpatialFlowLyricLine(
    line: LyricLine,
    isActive: Boolean,
    isPast: Boolean,
    currentPositionProvider: () -> Int,
    fontSizeSp: Float,
    onSeek: () -> Unit
) {
    val hasWordSync = line.hasWordSync && line.words != null && line.words.isNotEmpty()
    val words = line.words ?: emptyList()

    // ★ Alpha animation
    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1.0f
            isPast -> 0.60f
            else -> 0.38f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "LyricAlpha"
    )

    // ★ Smoothed position for karaoke — 200ms tween for buttery sweep
    val rawPos = if (hasWordSync && isActive) currentPositionProvider() else line.timeMs.toInt()
    val smoothedPos by animateFloatAsState(
        targetValue = rawPos.toFloat(),
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "SmoothKaraokePos"
    )

    val text = line.text.ifBlank { "♪" }
    val fontSize = fontSizeSp.sp
    val dimColor = Color.White.copy(alpha = 0.35f)
    val litColor = Color.White

    val textStyle = TextStyle(
        fontSize = fontSize,
        fontFamily = CalSansFamily,
        fontWeight = FontWeight.Bold,
        lineHeight = (fontSizeSp * 1.3f).sp,
        textAlign = TextAlign.Center,
        shadow = if (isActive && hasWordSync) Shadow(
            color = Color.White.copy(alpha = 0.3f),
            offset = Offset(0f, 0f),
            blurRadius = 12f
        ) else null
    )

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSeek
            )
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        // ★ Dim base layer
        Text(
            text = text,
            style = textStyle,
            color = if (hasWordSync) dimColor else (if (isActive || isPast) litColor else dimColor),
            onTextLayout = { layoutResult.value = it },
            modifier = Modifier.fillMaxWidth()
        )

        // ★ Bright overlay — erased per character based on word timing
        if (hasWordSync && (isActive || isPast)) {
            Text(
                text = text,
                style = textStyle,
                color = litColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithCache {
                        onDrawWithContent {
                            val layout = layoutResult.value
                            if (layout != null) {
                                drawContent()
                                if (isActive) {
                                    eraseFutureText(layout, words, smoothedPos.toLong())
                                }
                                // If isPast: don't erase — full bright
                            }
                        }
                    }
            )
        }
    }
}

/**
 * ★ Erase unsung characters from the bright overlay.
 *
 * For each character in the text:
 *   - If fully sung (charProgress >= 0.99): leave it bright (don't erase)
 *   - If fully future (charProgress < 0.01): erase completely (BlendMode.DstOut)
 *   - If partially sweeping: erase with a soft gradient (smooth edge)
 *
 * Adapted from SpatialFlow's eraseFutureText (Apache 2.0).
 */
private fun eraseFutureText(
    layout: TextLayoutResult,
    words: List<WordTimestamp>,
    pos: Long,
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    val textLength = layout.layoutInput.text.length
    var charOffset = 0

    for (wordIndex in words.indices) {
        val word = words[wordIndex]
        val wordLen = word.text.length

        for (charIdx in 0 until wordLen) {
            val absoluteCharIndex = charOffset + charIdx
            if (absoluteCharIndex >= textLength) break

            val charProgress = calculateCharProgress(charIdx, word, pos)

            if (charProgress >= 0.99f) {
                // Fully sung — leave bright (don't erase)
            } else if (charProgress < 0.01f) {
                // Fully future — erase completely
                try {
                    val path = layout.getPathForRange(absoluteCharIndex, absoluteCharIndex + 1)
                    drawScope.drawPath(path, color = Color.Black, blendMode = BlendMode.DstOut)
                } catch (_: Exception) { }
            } else {
                // ★ Partially sweeping — soft gradient erase
                try {
                    val path = layout.getPathForRange(absoluteCharIndex, absoluteCharIndex + 1)
                    val box = layout.getBoundingBox(absoluteCharIndex)

                    val gradientWidth = box.width * 1.5f
                    val sweepCenter = box.left + (box.width * charProgress)

                    val brush = Brush.horizontalGradient(
                        0.0f to Color.Transparent,
                        1.0f to Color.Black,
                        startX = sweepCenter - (gradientWidth / 2f),
                        endX = sweepCenter + (gradientWidth / 2f)
                    )

                    drawScope.drawPath(path, brush = brush, blendMode = BlendMode.DstOut)
                } catch (_: Exception) { }
            }
        }

        // Account for the space after each word (except the last)
        charOffset += wordLen
        if (wordIndex < words.size - 1) {
            // Skip the space character — it should be bright if the word is sung
            if (absoluteCharIndex + 1 < textLength) {
                // Space character — don't erase it (let it stay bright if word is done)
            }
            charOffset++ // +1 for the space
        }
    }
}

/**
 * Calculate the sweep progress for a character within a word.
 * Returns 0..1 where 0 = not started, 1 = fully sung.
 *
 * Uses easeOutCubic for smooth animation + a 35% sweep width
 * for the gradient feather at the boundary.
 *
 * Adapted from SpatialFlow's calculateCharProgress (Apache 2.0).
 */
private fun calculateCharProgress(
    charIndex: Int,
    word: WordTimestamp,
    pos: Long
): Float {
    val wordStartMs = word.startTime
    val wordEndMs = word.endTime.coerceAtLeast(wordStartMs + 120L)

    val wordProgress = when {
        pos < wordStartMs -> 0f
        pos >= wordEndMs -> 1f
        else -> {
            val duration = (wordEndMs - wordStartMs).toFloat().coerceAtLeast(1f)
            ((pos - wordStartMs).toFloat() / duration).coerceIn(0f, 1f)
        }
    }

    // ★ Eased progress — smooth acceleration/deceleration
    val easedWordProgress = easeOutCubic(wordProgress)

    val wordLength = word.text.length.toFloat().coerceAtLeast(1f)
    val sweepWidth = 0.35f
    val sweepPosition = easedWordProgress * (1f + sweepWidth)

    val charOffsetInWord = charIndex.toFloat()
    val charRelativePosition = charOffsetInWord / wordLength

    return when {
        sweepPosition < charRelativePosition -> 0f
        sweepPosition >= (charRelativePosition + sweepWidth) -> 1f
        else -> (sweepPosition - charRelativePosition) / sweepWidth
    }
}

/** Ease out cubic — smooth deceleration. */
private fun easeOutCubic(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)
