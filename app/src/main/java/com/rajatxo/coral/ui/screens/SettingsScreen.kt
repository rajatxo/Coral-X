package com.rajatxo.coral.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.BuildConfig
import com.rajatxo.coral.data.prefs.SectionToggleManager
import com.rajatxo.coral.data.prefs.UpdateChannelManager
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily

/**
 * ★ SettingsScreen — Coral X settings.
 *
 * Sections:
 *   1. Update Channel — Stable / Nightly toggle
 *   2. Quick Picks Sections — toggle greeting, today's top, speed dial, etc.
 *   3. About — version info
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 200.dp)
        ) {
        // ─── Header ───────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CoralXIcons.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = CalSansFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            // Spacer to balance the back button
            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── Section: Updates ────────────────────────
        SettingsSectionHeader("UPDATES")
        Spacer(modifier = Modifier.height(8.dp))
        UpdateChannelToggle()
        Spacer(modifier = Modifier.height(24.dp))

        // ─── Section: Quick Picks ───────────────────
        SettingsSectionHeader("QUICK PICKS SECTIONS")
        Spacer(modifier = Modifier.height(8.dp))
        SectionToggleRow("Greeting & Mood", "greeting")
        SectionToggleRow("Today's Top", "todays_top")
        SectionToggleRow("Speed Dial", "speed_dial")
        SectionToggleRow("Recent", "recent")
        SectionToggleRow("More picks", "more_picks")
        Spacer(modifier = Modifier.height(24.dp))

        // ─── Section: Speed Dial Mode ───────────
        SettingsSectionHeader("SPEED DIAL")
        Spacer(modifier = Modifier.height(8.dp))
        SpeedDialModeToggle()
        Spacer(modifier = Modifier.height(24.dp))

        // ─── Section: About ──────────────────────────
        SettingsSectionHeader("ABOUT")
        Spacer(modifier = Modifier.height(8.dp))
        AboutRow("Version", BuildConfig.VERSION_NAME)
        AboutRow("Channel", if (UpdateChannelManager.channel.value == UpdateChannelManager.UpdateChannel.STABLE) "Stable" else "Nightly")
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = CalSansFamily,
        modifier = Modifier.padding(horizontal = 20.dp),
        letterSpacing = 1.sp
    )
}

@Composable
private fun SpeedDialModeToggle() {
    val mode by com.rajatxo.coral.data.prefs.SpeedDialModeManager.mode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A).copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Text(
            text = "Speed Dial Mode",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = CalSansFamily
        )
        Text(
            text = "Choose how songs are picked",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontFamily = CalSansFamily
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(4.dp)
        ) {
            ChannelButton(
                text = "Random",
                subtitle = "Shuffle all songs",
                isSelected = mode == com.rajatxo.coral.data.prefs.SpeedDialModeManager.SpeedDialMode.RANDOM,
                accentColor = Color(0xFFFFD166),
                modifier = Modifier.weight(1f),
                onClick = { com.rajatxo.coral.data.prefs.SpeedDialModeManager.setMode(com.rajatxo.coral.data.prefs.SpeedDialModeManager.SpeedDialMode.RANDOM) }
            )
            ChannelButton(
                text = "Last Played",
                subtitle = "Based on recent artist",
                isSelected = mode == com.rajatxo.coral.data.prefs.SpeedDialModeManager.SpeedDialMode.LAST_PLAYED,
                accentColor = Color(0xFF6B9EFF),
                modifier = Modifier.weight(1f),
                onClick = { com.rajatxo.coral.data.prefs.SpeedDialModeManager.setMode(com.rajatxo.coral.data.prefs.SpeedDialModeManager.SpeedDialMode.LAST_PLAYED) }
            )
        }
    }
}

@Composable
private fun UpdateChannelToggle() {
    val channel by UpdateChannelManager.channel.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A).copy(alpha = 0.6f))
            .padding(16.dp)
    ) {
        Text(
            text = "Update Channel",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = CalSansFamily
        )
        Text(
            text = "Choose which updates you receive",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontFamily = CalSansFamily
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Segmented toggle: Stable | Nightly
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(4.dp)
        ) {
            // Stable
            ChannelButton(
                text = "Stable",
                subtitle = "Tested releases only",
                isSelected = channel == UpdateChannelManager.UpdateChannel.STABLE,
                accentColor = Color(0xFF51CF66),
                modifier = Modifier.weight(1f),
                onClick = { UpdateChannelManager.setChannel(UpdateChannelManager.UpdateChannel.STABLE) }
            )
            // Nightly
            ChannelButton(
                text = "Nightly",
                subtitle = "Every update (may have bugs)",
                isSelected = channel == UpdateChannelManager.UpdateChannel.NIGHTLY,
                accentColor = Color(0xFFFFD166),
                modifier = Modifier.weight(1f),
                onClick = { UpdateChannelManager.setChannel(UpdateChannelManager.UpdateChannel.NIGHTLY) }
            )
        }
    }
}

@Composable
private fun ChannelButton(
    text: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Radio dot
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isSelected) accentColor else Color.White.copy(alpha = 0.15f))
                .border(
                    width = 2.dp,
                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = CalSansFamily
        )
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontFamily = CalSansFamily,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionToggleRow(label: String, key: String) {
    val enabled = when (key) {
        "greeting" -> SectionToggleManager.greetingEnabled.collectAsState()
        "todays_top" -> SectionToggleManager.todaysTopEnabled.collectAsState()
        "speed_dial" -> SectionToggleManager.speedDialEnabled.collectAsState()
        "recent" -> SectionToggleManager.recentEnabled.collectAsState()
        "more_picks" -> SectionToggleManager.morePicksEnabled.collectAsState()
        else -> return
    }.value

    val onToggle = when (key) {
        "greeting" -> { v: Boolean -> SectionToggleManager.setGreetingEnabled(v) }
        "todays_top" -> { v: Boolean -> SectionToggleManager.setTodaysTopEnabled(v) }
        "speed_dial" -> { v: Boolean -> SectionToggleManager.setSpeedDialEnabled(v) }
        "recent" -> { v: Boolean -> SectionToggleManager.setRecentEnabled(v) }
        "more_picks" -> { v: Boolean -> SectionToggleManager.setMorePicksEnabled(v) }
        else -> return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A).copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onToggle(!enabled) }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = CalSansFamily,
            modifier = Modifier.weight(1f)
        )
        // Toggle switch
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.15f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onToggle(!enabled) }
                ),
            contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A).copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 15.sp,
            fontFamily = CalSansFamily,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = CalSansFamily
        )
    }
}
