package com.rajatxo.coral.ui.screens.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily

/**
 * ★ SectionHeader — "Title >" with count on the right.
 *
 * Ported from old Coral. Used for "Recent" and "More picks" sections.
 */
@Composable
fun SectionHeader(
    title: String,
    count: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = CalSansFamily
            )
            Icon(
                imageVector = CoralXIcons.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.then(Modifier)
            )
        }
        Text(
            text = if (count > 0) "$count" else "",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontFamily = CalSansFamily
        )
    }
}
