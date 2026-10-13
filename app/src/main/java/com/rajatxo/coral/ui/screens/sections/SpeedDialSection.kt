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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily

/**
 * ★ SpeedDialSection — 3×3 grid of song tiles inside a glass container.
 *
 * Ported from old Coral's SpeedDialSection, adapted for Coral X.
 * Uses placeholder colored tiles (will show real album art when
 * music scanning is added).
 *
 * Glass container: RoundedCornerShape(20.dp) with dark translucent bg
 * + subtle border. Same look as old Coral.
 *
 * Last tile (bottom-right) = dice randomize button.
 */
@Composable
fun SpeedDialSection(
    modifier: Modifier = Modifier,
    onHeaderClick: () -> Unit = {},
    onTileClick: (Int) -> Unit = {}
) {
    // Placeholder colors for the 8 song tiles + 1 dice
    val tileColors = listOf(
        Color(0xFF1a0a2e), Color(0xFF16213e), Color(0xFF0f3460),
        Color(0xFF533483), Color(0xFF2d1b4e), Color(0xFF1a1a2e),
        Color(0xFF2c1810), Color(0xFF1e3a5f)
    )
    val tileTitles = listOf("Blinding", "Starboy", "Tears", "Hours", "Die", "Call", "Hills", "Face")

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
                                    .height(100.dp)
                            )
                        } else {
                            SpeedDialTile(
                                title = tileTitles.getOrElse(index) { "?" },
                                color = tileColors.getOrElse(index) { Color(0xFF333333) },
                                onClick = { onTileClick(index) },
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

@Composable
private fun SpeedDialTile(
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        // Background gradient (placeholder for album art)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(color, color.copy(alpha = 0.5f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CoralXIcons.Music,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Title at bottom
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = CalSansFamily,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 6.dp)
        )
    }
}

@Composable
private fun DiceTile(modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "diceScale"
    )
    val isSpinning = remember { androidx.compose.runtime.mutableStateOf(false) }

    val tileShape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .scale(scale)
            .clip(tileShape)
            .background(Color(0xFF333333))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { /* Will play random song when music is added */ }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🎲",
            fontSize = 32.sp,
            modifier = Modifier.graphicsLayer {
                if (isSpinning.value) {
                    rotationZ = 360f
                }
            }
        )
    }
}
