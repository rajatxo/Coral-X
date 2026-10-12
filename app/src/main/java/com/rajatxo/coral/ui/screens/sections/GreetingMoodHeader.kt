package com.rajatxo.coral.ui.screens.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.ui.theme.CalSansFamily

/**
 * ★ GreetingMoodHeader — "Hey, {name}" + time-based mood subtitle.
 *
 * Ported from old Coral's QuickPicksScreen, simplified.
 *
 * Greeting: "Hey, Rajat" (or just "Hey," if no name set)
 * Mood text (based on hour of day):
 *   5-11: "Start your day with music"
 *   12-16: "What's on your mind?"
 *   17-21: "Ready for some music?"
 *   22-4: "Late night vibes"
 */
@Composable
fun GreetingMoodHeader(
    userName: String = "",
    modifier: Modifier = Modifier
) {
    val hour = remember {
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }

    val greeting = if (userName.isNotBlank()) "Hey, $userName" else "Hey,"

    val moodText = when (hour) {
        in 5..11 -> "Start your day with music"
        in 12..16 -> "What's on your mind?"
        in 17..21 -> "Ready for some music?"
        else -> "Late night vibes"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp)
    ) {
        Text(
            text = greeting,
            color = Color.White,
            fontFamily = CalSansFamily,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = Offset(1f, 1f),
                    blurRadius = 3f
                )
            ),
            letterSpacing = (-1).sp
        )
        Text(
            text = moodText,
            fontFamily = CalSansFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.6f),
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = Offset(0.5f, 0.5f),
                    blurRadius = 2f
                )
            ),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
