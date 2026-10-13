package com.rajatxo.coral.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.ui.theme.CalSansFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ★ CountdownLongPress — wraps any composable with a 3-2-1 countdown timer.
 *
 * The pointerInput is on the SAME Box as the content — so it only captures
 * touches ON the content, NOT the whole screen.
 *
 * When the user holds down on the child:
 *   Phase 1: 2 seconds hold (no UI feedback)
 *   Phase 2: Bubble pops up showing "Hold to move in [3]→[2]→[1]" (3 seconds)
 *   Phase 3: Countdown done → onLongPressComplete() called
 *
 * Release before completion → bubble disappears, countdown cancelled.
 */
@Composable
fun CountdownLongPress(
    modifier: Modifier = Modifier,
    onLongPressComplete: () -> Unit,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var showBubble by remember { mutableStateOf(false) }
    var countdownNumber by remember { mutableStateOf(3) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }

    val bubbleScale by animateFloatAsState(
        targetValue = if (showBubble) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bubbleScale"
    )
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (showBubble) 1f else 0f,
        animationSpec = tween(250),
        label = "bubbleAlpha"
    )

    // ★ Content + pointerInput on the SAME Box — only captures touches on the content
    Box(modifier = modifier) {
        content()

        // Countdown bubble — appears to the LEFT of the content
        if (bubbleAlpha > 0.01f) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        translationX = with(density) { (-200).dp.toPx() }
                        scaleX = bubbleScale
                        scaleY = bubbleScale
                        alpha = bubbleAlpha
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main pill bubble
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1A1A1A))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Hold to move in",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = CalSansFamily
                        )
                        // Small white capsule with countdown number
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = countdownNumber.toString(),
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CalSansFamily
                            )
                        }
                    }
                }
                // Pointed tail (triangle pointing right → toward the content)
                Canvas(
                    modifier = Modifier
                        .size(width = 10.dp, height = 14.dp)
                        .graphicsLayer { alpha = bubbleAlpha }
                ) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, size.height / 2f)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path = path, color = Color(0xFF1A1A1A))
                }
            }
        }

        // ★ Transparent overlay — SAME size as the content (wraps it)
        // Captures press-and-hold ONLY on the content area, not the whole screen.
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            down.consume()

                            countdownJob?.cancel()
                            countdownJob = scope.launch {
                                delay(2000L)
                                showBubble = true
                                countdownNumber = 3
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                delay(1000L)

                                countdownNumber = 2
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                delay(1000L)

                                countdownNumber = 1
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                delay(1000L)

                                showBubble = false
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onLongPressComplete()
                            }

                            // Wait for release
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    countdownJob?.cancel()
                                    showBubble = false
                                    break
                                }
                            }
                        }
                    }
                }
        )
    }
}
