package com.rajatxo.coral.ui.foundation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.rajatxo.coral.ui.theme.CalSansFamily
import com.rajatxo.coral.ui.util.CoralTab

@Composable
fun CoralXTabCapsule(
    tabs: List<CoralTab>,
    activeTab: CoralTab,
    onTabSelected: (CoralTab) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    customWidth: Dp? = null,
    customHeight: Dp? = null,
    customShape: Shape? = null
) {
    val view = LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeIndex = tabs.indexOf(activeTab).coerceAtLeast(0)
    var slideDirection by remember { mutableIntStateOf(1) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 60f

    // ★ SoundPool for tick sound on tab switch
    val soundPool = remember {
        android.media.SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }
    var soundLoaded by remember { mutableStateOf(false) }
    val tickSoundId = remember {
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) soundLoaded = true
        }
        soundPool.load(context, com.rajatxo.coral.R.raw.wheel_tick, 1)
    }

    val hapticsOn by com.rajatxo.coral.data.prefs.SoundHapticsManager.hapticsEnabled.collectAsState()
    val soundsOn by com.rajatxo.coral.data.prefs.SoundHapticsManager.soundsEnabled.collectAsState()
    val soundVolume by com.rajatxo.coral.data.prefs.SoundHapticsManager.soundVolume.collectAsState()

    fun tickHaptic() {
        if (hapticsOn) {
            try {
                view.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            } catch (_: Exception) { }
        }
        if (soundsOn && soundLoaded) {
            try {
                val vol = soundVolume / 100f
                soundPool.play(tickSoundId, vol, vol, 1, 0, 1f)
            } catch (_: Exception) { }
        }
    }

    val capsuleShape: Shape = customShape ?: RoundedCornerShape(26.dp)
    val actualWidth = customWidth ?: 240.dp
    val actualHeight = customHeight ?: 52.dp

    val glassModifier = if (backdrop != null) {
        modifier.width(actualWidth).height(actualHeight).clip(capsuleShape).drawBackdrop(
            backdrop = backdrop, shape = { capsuleShape },
            effects = { vibrancy(); colorControls(brightness = 0.05f, contrast = 1f, saturation = 1.5f); blur(12f.dp.toPx()) },
            onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.25f)) }
        )
    } else {
        modifier.width(actualWidth).height(actualHeight).clip(capsuleShape).background(Color.Black.copy(alpha = 0.5f))
    }

    Box(modifier = glassModifier.pointerInput(tabs, activeTab) {
        detectHorizontalDragGestures(
            onDragEnd = { dragAccumulator = 0f },
            onHorizontalDrag = { _, dragAmount ->
                dragAccumulator += dragAmount
                if (dragAccumulator < -dragThreshold) {
                    slideDirection = 1
                    onTabSelected(tabs[(activeIndex + 1) % tabs.size])
                    tickHaptic()
                    dragAccumulator = 0f
                } else if (dragAccumulator > dragThreshold) {
                    slideDirection = -1
                    val prev = if (activeIndex - 1 < 0) tabs.size - 1 else activeIndex - 1
                    onTabSelected(tabs[prev])
                    tickHaptic()
                    dragAccumulator = 0f
                }
            }
        )
    }) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            drawRect(
                brush = Brush.horizontalGradient(colorStops = arrayOf(
                    0.0f to Color.Transparent, 0.1f to Color.White.copy(alpha = 0.15f),
                    0.25f to Color.White.copy(alpha = 0.6f), 0.75f to Color.White.copy(alpha = 0.6f),
                    0.9f to Color.White.copy(alpha = 0.15f), 1.0f to Color.Transparent
                )),
                topLeft = Offset(0f, centerY - 0.75f), size = Size(size.width, 1.5f)
            )
        }
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                if (slideDirection == 1) {
                    slideInHorizontally(tween(250)) { it } togetherWith slideOutHorizontally(tween(250)) { -it }
                } else {
                    slideInHorizontally(tween(250)) { -it } togetherWith slideOutHorizontally(tween(250)) { it }
                }
            },
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center, label = "tabText"
        ) { tab ->
            Box(Modifier.wrapContentSize(Alignment.Center).background(Color.Transparent).padding(horizontal = 12.dp, vertical = 2.dp)) {
                Text(
                    text = tab.label,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = CalSansFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}
