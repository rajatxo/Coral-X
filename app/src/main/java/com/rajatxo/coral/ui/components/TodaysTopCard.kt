package com.rajatxo.coral.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.rajatxo.coral.data.prefs.SpeedDialPinStore
import com.rajatxo.coral.domain.model.Song
import com.rajatxo.coral.playback.PlaybackConnection
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily
import kotlinx.coroutines.delay

/**
 * ★ TodaysTopCard — floating glass capsule shown when user taps a Today's Top cover.
 *
 * Ported from old Coral's TodaysTopCard:
 *   - Size: 188×61dp (old Coral defaults)
 *   - Corner radius: 50dp (pill shape)
 *   - Glass morphism via kyant drawBackdrop (same as nav bar + mini player)
 *   - Layout: [Cover + pin] [Title + artist] [Play/Pause] [More (•••)]
 *   - Tap more (•••) → opens a glass customization card
 *   - Auto-dismiss after 10 seconds
 *   - Positioned at top-right (statusBarsPadding + 72dp below status bar)
 *
 * @param backdrop The kyant LayerBackdrop from the scaffold (for glass blur)
 * @param onMenuClick Called when user taps the more (•••) icon
 */
@Composable
fun TodaysTopCard(
    visible: Boolean,
    song: Song?,
    backdrop: LayerBackdrop? = null,
    onDismiss: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    // Auto-dismiss after 10 seconds
    LaunchedEffect(visible, song) {
        if (visible && song != null) {
            delay(10000)
            onDismiss()
        }
    }

    // ★ Card enter/exit animation — smooth scale + fade on the CARD only.
    // The scrim (from RippleDismissContainer) appears instantly at 30% opacity
    // (no fade-in on the dark overlay — matches old Coral).
    val enterScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible && song != null) 1f else 0.8f,
        animationSpec = androidx.compose.animation.core.tween(250),
        label = "cardEnterScale"
    )
    val enterAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible && song != null) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(250),
        label = "cardEnterAlpha"
    )

    if (visible && song != null) {
        com.rajatxo.coral.ui.components.RippleDismissContainer(
            onDismiss = onDismiss,
            scrimColor = Color.Black.copy(alpha = 0.3f)
        ) { progress ->
        // Card content — positioned at top-end, NOT full-screen.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            // ★ Card size: 188×61dp with 50dp corner radius (old Coral defaults)
            val cardShape = RoundedCornerShape(50.dp)
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 72.dp, end = 16.dp)
                    .width(188.dp)
                    .height(61.dp)
                    .graphicsLayer {
                        // ★ Enter animation (scale + fade) combined with ripple fade-out
                        compositingStrategy = CompositingStrategy.Offscreen
                        val totalAlpha = enterAlpha * (1f - progress)
                        alpha = totalAlpha
                        scaleX = enterScale
                        scaleY = enterScale
                    }
                    .clip(cardShape)
                    .then(
                        if (backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { cardShape },
                                effects = {
                                    vibrancy()
                                    colorControls(
                                        brightness = 0.05f,
                                        contrast = 1f,
                                        saturation = 1.5f
                                    )
                                    blur(12f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.Black.copy(alpha = 0.25f))
                                }
                            )
                        } else {
                            Modifier.background(Color(0xFF1A1A1A).copy(alpha = 0.88f))
                        }
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), cardShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // consume click so it doesn't dismiss
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ─── Cover circle + pin badge (LEFT) ───
                    val pinnedIds by SpeedDialPinStore.pinnedIds.collectAsState()
                    val isSongPinned = song != null && song.id in pinnedIds

                    // Cover size scales with card height (old Coral: heightDp - 18f)
                    val coverSize = 43.dp  // 61dp - 18dp

                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(coverSize)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val artUri = song?.albumArtUri
                            if (artUri != null) {
                                AsyncImage(
                                    model = artUri,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF1A1A1A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CoralXIcons.Music,
                                        contentDescription = null,
                                        tint = Color(0xFFB0B0B0),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Pin badge — top-right of cover
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSongPinned) Color(0xFFFF6B6B)
                                    else Color.Black.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSongPinned) Color.White
                                            else Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false)
                                ) {
                                    song?.let { SpeedDialPinStore.togglePin(it.id) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CoralXIcons.Pin,
                                contentDescription = if (isSongPinned) "Unpin" else "Pin",
                                tint = if (isSongPinned) Color.White else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    // ─── Title + artist (MIDDLE) ───
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = song?.title ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = CalSansFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song?.artist ?: "",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontFamily = CalSansFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // ─── Play/Pause (RIGHT) ───
                    val currentSong by PlaybackConnection.currentSong.collectAsState()
                    val isPlaying by PlaybackConnection.isPlaying.collectAsState()
                    val isCurrent = song != null && currentSong?.id == song.id

                    Icon(
                        imageVector = if (isCurrent && isPlaying) CoralXIcons.PauseLucide
                                      else CoralXIcons.PlayLucide,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false)
                            ) {
                                song?.let { theSong ->
                                    if (isCurrent) {
                                        PlaybackConnection.playPause()
                                    } else {
                                        PlaybackConnection.playSong(theSong, listOf(theSong))
                                    }
                                }
                            }
                    )

                    // ─── More / three-dot menu (RIGHT) ───
                    Icon(
                        imageVector = CoralXIcons.MoreVertical,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(20.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false)
                            ) { onMenuClick() }
                    )
                }
            }
        }
        }
    }
}
