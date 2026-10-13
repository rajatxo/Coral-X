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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    onNavBarLongPress: () -> Unit = {},
    onSearchLongPress: () -> Unit = {},
    miniPlayer: @Composable (() -> Unit) = {},
    onBackdropReady: (com.kyant.backdrop.backdrops.LayerBackdrop) -> Unit = {},
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

    // ★ Notify the parent (MainActivity) that the backdrop is ready.
    //   This lets TodaysTopCard (rendered outside the scaffold) use it for
    //   drawBackdrop blur.
    androidx.compose.runtime.LaunchedEffect(glassBackdrop) {
        onBackdropReady(glassBackdrop)
    }

    CompositionLocalProvider(
        LocalCoralXWindowInsets provides insets,
        LocalGlassBackdrop provides glassBackdrop
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
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
                            // 20dp blur — matches old Coral exactly.
                            renderEffect = BlurEffect(20.dp.toPx(), 20.dp.toPx())
                        }
                    }
                    .drawWithContent {
                        if (useRE) {
                            drawLayer(graphicsLayer)
                        } else {
                            drawContent()
                        }
                        // ★ Exact same DstIn gradient as old Coral:
                        //   0.0 → Black (fully opaque)
                        //   0.6 → Black (fully opaque until 60%)
                        //   1.0 → Transparent (smoothly fades the bottom 40%)
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
                                    blur(20f.dp.toPx())
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

            // ★ Read Cynthia customization prefs
            val navCustom by com.rajatxo.coral.data.prefs.CynthiaNavBarCustomization
                .customization.collectAsState()
            val searchCustom by com.rajatxo.coral.data.prefs.CynthiaSearchFabCustomization
                .customization.collectAsState()
            val navBarPos by com.rajatxo.coral.data.prefs.CynthiaTabCapsulePosition
                .position.collectAsState()
            val searchPos by com.rajatxo.coral.data.prefs.CynthiaSearchFabPosition
                .position.collectAsState()
            val miniPlayerCustom by com.rajatxo.coral.data.prefs.CynthiaMiniPlayerCustomization
                .customization.collectAsState()
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

            // ★ Nav bar — positioned using screen fractions. Universal — adapts to every phone.
            // X fraction * screenWidth = center X. Y fraction * screenHeight = center Y from TOP.
            // All positioned from BottomStart (0,0 = bottom-left corner).
            com.rajatxo.coral.ui.components.CountdownLongPress(
                onLongPressComplete = onNavBarLongPress,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset {
                        // X: navBarPos.first is a fraction (0-1) of screen width
                        val x = (navBarPos.first * screenWidthPx - navCustom.widthDp.dp.toPx() / 2f).toInt()
                        // Y: navBarPos.second is fraction from TOP. From BOTTOM = (1 - frac) * screenHeight
                        // effBottomDp is the system nav bar inset (gesture/button nav)
                        val yFromBottom = (1f - navBarPos.second) * screenHeightPx
                        // Position: from BottomStart, negative Y pushes UP
                        val y = -(yFromBottom - effBottomDp.toPx()).toInt()
                        androidx.compose.ui.unit.IntOffset(x, y)
                    }
            ) {
                CoralXTabCapsule(
                    tabs = CoralTab.entries.toList(),
                    activeTab = activeTab,
                    onTabSelected = onTabSelected,
                    backdrop = glassBackdrop,
                    customWidth = navCustom.widthDp.dp,
                    customHeight = navCustom.heightDp.dp,
                    customShape = navCustom.shape.toComposeShape(navCustom.cornerRadiusDp, navCustom.widthDp)
                )
            }

            // ★ Search FAB — same positioning approach as nav bar
            com.rajatxo.coral.ui.components.CountdownLongPress(
                onLongPressComplete = onSearchLongPress,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset {
                        val x = (searchPos.first * screenWidthPx - searchCustom.sizeDp.dp.toPx() / 2f).toInt()
                        val yFromBottom = (1f - searchPos.second) * screenHeightPx
                        val y = -(yFromBottom - effBottomDp.toPx()).toInt()
                        androidx.compose.ui.unit.IntOffset(x, y)
                    }
            ) {
                val fabShape = searchCustom.shape.toComposeShape(searchCustom.cornerRadiusDp, searchCustom.sizeDp)
                Box(
                    modifier = Modifier
                        .size(searchCustom.sizeDp.dp)
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

            // ★ Mini player — positioned ABOVE the nav bar using screen fractions.
            // Sits at the same X as the nav bar but higher Y.
            // Default: same X as nav bar (43%), Y = nav bar Y - nav bar height - mini player height - 10dp gap
            val miniPlayerY = navBarPos.second - (navCustom.heightDp / screenHeightPx * density.density) - (miniPlayerCustom.heightDp / screenHeightPx * density.density) - (10f / screenHeightPx * density.density)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset {
                        val x = (navBarPos.first * screenWidthPx - miniPlayerCustom.widthDp.dp.toPx() / 2f).toInt()
                        val yFromBottom = (1f - miniPlayerY) * screenHeightPx
                        val y = -(yFromBottom - effBottomDp.toPx()).toInt()
                        androidx.compose.ui.unit.IntOffset(x, y)
                    }
            ) {
                miniPlayer()
            }
        }
    }
}
