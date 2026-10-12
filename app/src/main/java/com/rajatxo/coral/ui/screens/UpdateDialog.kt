package com.rajatxo.coral.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.update.ApkDownloader

/**
 * ★ UpdateDialog — full-screen overlay showing:
 *   - Version name + stable/nightly badge
 *   - Plain-text release notes (no links)
 *   - State-driven action area:
 *       Idle      → Download / Later
 *       Downloading → progress bar + bytes + %
 *       Ready     → Install / Close  (system installer already fired)
 *       Error     → message + Retry / Close
 *
 * ⚠️ CURRENTLY UNUSED — Coral is in pre-Play-Store development.
 *    Users install APKs manually from GitHub Actions.
 *
 *    This file is preserved because the exact same UI will be reused
 *    when we swap UpdateChecker from GitHub Releases to Play Core's
 *    In-App Updates API next year. Only the data source changes —
 *    the dialog, progress UI, and state machine stay identical.
 */
@Composable
fun UpdateDialog(
    versionName: String,
    releaseNotes: String,
    isStable: Boolean,
    downloadUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val downloadState by ApkDownloader.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1A1A1A))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Update Available",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = versionName,
                color = Color(0xFFFF6B6B),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isStable) Color(0xFF51CF66).copy(alpha = 0.2f)
                        else Color(0xFFFFD166).copy(alpha = 0.2f)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isStable) "Stable" else "Nightly",
                    color = if (isStable) Color(0xFF51CF66) else Color(0xFFFFD166),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = releaseNotes.take(200),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            when (val state = downloadState) {
                is ApkDownloader.State.Idle -> {
                    ActionRow(
                        leftLabel = "Later",
                        rightLabel = "Download",
                        onLeft = onDismiss,
                        onRight = {
                            if (downloadUrl.isNotBlank()) {
                                ApkDownloader.start(context, downloadUrl)
                            }
                        }
                    )
                }

                is ApkDownloader.State.Downloading -> {
                    DownloadingRow(
                        progress = state.progress,
                        downloadedBytes = state.downloadedBytes,
                        totalBytes = state.totalBytes
                    )
                }

                is ApkDownloader.State.Ready -> {
                    ActionRow(
                        leftLabel = "Close",
                        rightLabel = "Install",
                        onLeft = onDismiss,
                        onRight = {
                            ApkDownloader.launchInstaller(context, state.file)
                        }
                    )
                }

                is ApkDownloader.State.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = Color(0xFFFF6B6B),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ActionRow(
                            leftLabel = "Close",
                            rightLabel = "Retry",
                            onLeft = onDismiss,
                            onRight = {
                                if (downloadUrl.isNotBlank()) {
                                    ApkDownloader.start(context, downloadUrl)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    leftLabel: String,
    rightLabel: String,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLeft
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = leftLabel, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFFF6B6B))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRight
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = rightLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DownloadingRow(
    progress: Int,
    downloadedBytes: Long,
    totalBytes: Long
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (progress >= 0) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFFFF6B6B),
                trackColor = Color.White.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (totalBytes > 0) "${humanSize(downloadedBytes)} / ${humanSize(totalBytes)}" else "${humanSize(downloadedBytes)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Text(
                    text = "$progress%",
                    color = Color(0xFFFF6B6B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color(0xFFFF6B6B),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${humanSize(downloadedBytes)} downloaded",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

private fun humanSize(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}
