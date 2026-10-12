package com.rajatxo.coral.ui.foundation

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily
import com.rajatxo.coral.ui.util.CoralTab

@Composable
fun CoralXScaffold(
    title: String,
    activeTab: CoralTab,
    onTabSelected: (CoralTab) -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    // ★ ConsistentNavBarPadding — always at least 48dp (button nav height).
    // This ensures the tab capsule + FAB sit at the same height on gesture-nav
    // phones (thin gesture bar ~16dp) and button-nav phones (~48dp).
    val systemBottom = WindowInsets.navigationBars.getBottom(density)
    val minBottom = with(density) { Dimensions.MinBottomSystemInset.toPx().toInt() }
    val effBottomDp = with(density) { maxOf(systemBottom, minBottom).toDp() }
    val effTopDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }

    // ★ Content top padding = status bar + header row height + gap.
    // Matches old Coral: 108dp total from screen top to first content.
    // (statusBar ~24dp + header 56dp + gap 28dp = ~108dp)
    val insets = WindowInsets(
        top = effTopDp + Dimensions.TopBarHeight + Dimensions.ContentTopGap,
        bottom = effBottomDp + Dimensions.MiniPlayerHeight + Dimensions.BottomStackGap
    )

    val graphicsLayer = rememberGraphicsLayer()
    val glassBackdrop = rememberLayerBackdrop(graphicsLayer = graphicsLayer) {
        drawContent()
    }

    CompositionLocalProvider(LocalCoralXWindowInsets provides insets) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF1A1A2E),  // subtle dark blue at top (behind blur)
                            0.3f to Color(0xFF0A0A12),  // fade to near-black
                            1.0f to Color(0xFF000000)   // pure black at bottom
                        )
                    )
                )
        ) {
            // Content — fills the WHOLE screen (no padding).
            // Each screen reads LocalCoralXWindowInsets to add its own top/bottom
            // padding via LazyColumn's contentPadding. This way content can scroll
            // UNDER the blur header (which is rendered as a sibling below).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(glassBackdrop)
            ) {
                content()
            }

            // Header blur
            val useRE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(Dimensions.TopBlurHeight)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        clip = true
                        if (useRE) {
                            renderEffect = BlurEffect(30.dp.toPx(), 30.dp.toPx())
                        }
                    }
                    .drawWithContent {
                        if (useRE) {
                            drawLayer(graphicsLayer)
                        } else {
                            drawContent()
                        }
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black,
                                    0.6f to Color.Black,
                                    1.0f to Color.Transparent
                                )
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                if (!useRE) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBackdrop(
                                backdrop = glassBackdrop,
                                shape = { RectangleShape },
                                effects = {
                                    vibrancy()
                                    blur(30f.dp.toPx())
                                }
                            )
                    )
                }
            }

            // Header row: profile + title + settings
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(Dimensions.TopBarHeight)
                    .padding(horizontal = Dimensions.NavBarHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.TopBarIconTouchSize)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onProfileClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CoralXIcons.CircleUser,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(Dimensions.TopBarIconSize)
                    )
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = Dimensions.TitleFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CalSansFamily,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(Dimensions.TopBarIconTouchSize)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSettingsClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CoralXIcons.Cog,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(Dimensions.TopBarIconSize)
                    )
                }
            }

            // Bottom: TabCapsule + Search FAB
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.NavBarHorizontalPadding)
                    .padding(bottom = effBottomDp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoralXTabCapsule(
                    tabs = CoralTab.entries.toList(),
                    activeTab = activeTab,
                    onTabSelected = onTabSelected,
                    backdrop = glassBackdrop
                )
                Spacer(modifier = Modifier.width(Dimensions.NavSearchGap))
                val fabShape = CircleShape
                Box(
                    modifier = Modifier
                        .size(Dimensions.SearchFabSize)
                        .clip(fabShape)
                        .drawBackdrop(
                            backdrop = glassBackdrop,
                            shape = { fabShape },
                            effects = {
                                vibrancy()
                                colorControls(brightness = 0.05f, contrast = 1f, saturation = 1.5f)
                                blur(18f.dp.toPx())
                            },
                            onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.35f)) }
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.2f), fabShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSearchClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CoralXIcons.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
