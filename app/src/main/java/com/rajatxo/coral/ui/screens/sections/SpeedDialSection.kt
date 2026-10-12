package com.rajatxo.coral.ui.screens.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily

/**
 * ★ SpeedDialSection — 3x3 grid of quick-access song tiles.
 *
 * Ported from old Coral. Since we don't have music scanning yet, this
 * shows placeholder tiles. When music scanning is added, these will show
 * album art for quick-access songs + a dice randomize button.
 *
 * Layout: 3 columns × 3 rows = 9 tiles per page.
 * Last tile of page 1 is the dice (randomize) button.
 */
@Composable
fun SpeedDialSection(
    modifier: Modifier = Modifier,
    onHeaderClick: () -> Unit = {},
    onTileClick: (Int) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
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
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onHeaderClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CoralXIcons.ChevronRight,
                    contentDescription = "Open Speed Dial settings",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3x3 grid (single page for now, pager will be added with real data)
        val tileShape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1A1A).copy(alpha = 0.4f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .padding(8.dp)
        ) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0 until 3) {
                        val index = row * 3 + col
                        val isDiceSlot = (index == 8) // last slot = dice

                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .padding(4.dp)
                                .clip(tileShape)
                                .background(
                                    if (isDiceSlot) Color(0xFF333333)
                                    else Color(0xFF2A2A2A)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onTileClick(index) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDiceSlot) {
                                Text(
                                    text = "🎲",
                                    fontSize = 32.sp
                                )
                            } else {
                                Text(
                                    text = "♪",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
