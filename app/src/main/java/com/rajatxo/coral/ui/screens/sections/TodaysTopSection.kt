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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily

/**
 * ★ TodaysTopSection — 3 capsule stories with placeholder covers.
 *
 * Ported from old Coral. Since we don't have music scanning yet, this
 * shows placeholder capsules. When playback history is added, these will
 * show the top 6 most-played songs today.
 *
 * Each capsule is a 56dp pill with gradient border + 2 cover circles.
 */
@Composable
fun TodaysTopSection(
    modifier: Modifier = Modifier,
    onCapsuleClick: () -> Unit = {}
) {
    val placeholderColors = listOf(
        Color(0xFFFF6B6B), Color(0xFFFFD166), Color(0xFF6B9EFF),
        Color(0xFFFF8FAB), Color(0xFF51CF66), Color(0xFFB197FC)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Top",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = CalSansFamily
            )
            Icon(
                imageVector = CoralXIcons.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3 capsules
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until 3) {
                val color1 = placeholderColors[i * 2]
                val color2 = placeholderColors[i * 2 + 1]
                val capsuleShape = RoundedCornerShape(28.dp)
                val borderBrush = Brush.horizontalGradient(listOf(color1, color2))
                val bgBrush = Brush.horizontalGradient(
                    listOf(color1.copy(alpha = 0.25f), color2.copy(alpha = 0.25f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(capsuleShape)
                        .background(bgBrush)
                        .border(2.5.dp, borderBrush, capsuleShape)
                        .padding(6.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCapsuleClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cover slot 1 (placeholder)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color1.copy(alpha = 0.7f))
                                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "+", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        // Cover slot 2 (placeholder)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color2.copy(alpha = 0.7f))
                                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "+", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
