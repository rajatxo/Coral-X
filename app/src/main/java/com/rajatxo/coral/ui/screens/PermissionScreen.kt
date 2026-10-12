package com.rajatxo.coral.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily
import kotlinx.coroutines.delay

/**
 * ★ PermissionScreen — the first thing the user sees when opening Coral X
 *   for the first time (before permissions are granted).
 *
 * Two mandatory permission cards (Notifications + Music files) + one
 * optional card (All files access — for embedding cover art into file
 * metadata). The user must flip BOTH mandatory toggles to ON before the
 * app unlocks and proceeds to the main screen.
 *
 * Visual style: Coral X dark theme — black background, frosted dark glass
 * cards, white text, coral-pink accent on the toggle.
 *
 * Auto-flow:
 *   - Page appears → both toggles OFF (gray)
 *   - User taps Notifications card → toggle flips to coral, system dialog
 *   - User taps Music files card → toggle flips to coral, system dialog
 *   - Both granted → 800ms delay → fade-out → onPermissionsGranted() called
 *   - If denied → toggle flips back OFF, hint appears
 */
@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current

    // Track each permission's state independently
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true  // pre-Android 13: notifications granted at install
        )
    }
    var musicGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }
    var allFilesGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else true
        )
    }

    // Hint state: shows when user denies a permission
    var showHint by remember { mutableStateOf(false) }
    var hintTarget by remember { mutableStateOf("") }

    // Fade-out animation when both granted
    var fadingOut by remember { mutableStateOf(false) }
    val fadeAlpha by animateFloatAsState(
        targetValue = if (fadingOut) 0f else 1f,
        animationSpec = tween(600),
        label = "permissionFade"
    )

    // When required permissions granted (NOT including allFilesGranted —
    // that one is optional), wait 800ms then fade out + proceed.
    LaunchedEffect(notifGranted, musicGranted) {
        if (notifGranted && musicGranted) {
            delay(800)
            fadingOut = true
            delay(600)
            onPermissionsGranted()
        }
    }

    // --- Permission launchers ---
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            notifGranted = true
        } else {
            showHint = true
            hintTarget = "notifications"
        }
    }

    val musicLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it }
        if (granted) {
            musicGranted = true
        } else {
            showHint = true
            hintTarget = "music"
        }
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notifGranted = true
        }
    }

    fun requestMusic() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        musicLauncher.launch(perms)
    }

    fun requestAllFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        } else {
            allFilesGranted = true
        }
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    // Re-check allFilesGranted on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                allFilesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top)
        ) {
            // ─── "Coral X" hero typography ──────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Coral X",
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = CalSansFamily
            )

            Text(
                text = "Allow Access to begin",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Dark glass permission cards ───────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DarkGlassPermissionCard(
                    icon = {
                        Icon(
                            imageVector = CoralXIcons.BellRing,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    title = "Notifications",
                    subtitle = "For playback controls",
                    isOn = notifGranted,
                    onClick = {
                        if (notifGranted) openAppSettings()
                        else requestNotifications()
                    }
                )

                DarkGlassPermissionCard(
                    icon = {
                        Icon(
                            imageVector = CoralXIcons.FileHeadphone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    title = "Music files",
                    subtitle = "To scan your library",
                    isOn = musicGranted,
                    onClick = {
                        if (musicGranted) openAppSettings()
                        else requestMusic()
                    }
                )

                // All files access — optional, Android 11+ only
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    DarkGlassPermissionCard(
                        icon = {
                            Icon(
                                imageVector = CoralXIcons.Wallpaper,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        title = "All files access",
                        subtitle = "Embed covers into file metadata",
                        isOn = allFilesGranted,
                        isOptional = true,
                        onClick = {
                            if (allFilesGranted) openAppSettings()
                            else requestAllFiles()
                        }
                    )
                }
            }

            // --- Hint ---
            AnimatedVisibility(
                visible = showHint,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(250))
            ) {
                Text(
                    text = when (hintTarget) {
                        "notifications" -> "Coral needs notifications to control playback."
                        "music" -> "Coral needs access to your audio files."
                        else -> ""
                    },
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            // --- Footer (scrollable, never cut off) ---
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "We respect your privacy.",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Coral never collects your data — your music stays on your device.",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
            )
            // Bottom padding so the last text never gets cut by the nav bar
            Spacer(modifier = Modifier.height(48.dp))
        }

        // Fade-out overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 1f - fadeAlpha))
        )
    }
}

/**
 * Dark glass permission card — matches Coral X's dark aesthetic.
 *
 * Layered:
 *   1. Dark translucent background (the "frost")
 *   2. Subtle white border
 *   3. Content (icon + title + subtitle + toggle)
 */
@Composable
private fun DarkGlassPermissionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    isOn: Boolean,
    onClick: () -> Unit,
    isOptional: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1A1A1A).copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon in a small dark circle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            // Title + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = CalSansFamily
                    )
                    if (isOptional) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Optional",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            // Toggle
            CoralToggle(isOn = isOn)
        }
    }
}

/**
 * iOS-style toggle — dark theme variant.
 * OFF: dark gray track, white thumb on left
 * ON:  coral-pink track, white thumb on right
 */
@Composable
private fun CoralToggle(isOn: Boolean) {
    val thumbOffset by animateDpAsState(
        targetValue = if (isOn) 20.dp else 0.dp,
        animationSpec = tween(200),
        label = "toggleThumb"
    )

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isOn) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .offset(x = thumbOffset + 2.dp, y = 2.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
