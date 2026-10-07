package com.rajatxo.coral.ui.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.rajatxo.coral.ui.components.CoralTab
import com.rajatxo.coral.ui.icons.CoralIcons

/**
 * ★ CoralNavRow — nav bar (tab switcher) + search FAB side by side.
 *
 * LAYOUT CONTRACT:
 *   - Both elements in a Row (Row owns their relative layout — they cannot
 *     drift apart on different phone sizes)
 *   - Nav bar: 152dp wide × 52dp tall (CoralLayout.NavBarHeight)
 *   - Search FAB: 52dp circle (same height as nav bar)
 *   - Gap: 10dp (CoralLayout.NavRowGap)
 *   - Both use glass morphism via Kyant drawBackdrop
 *
 * NAV BAR:
 *   - Tab switcher with 3 tabs (Quick picks / Songs / Playlists)
 *   - Swipe left/right to switch tabs (with haptic feedback)
 *   - Tap a tab label to switch directly
 *
 * SEARCH FAB:
 *   - Circular glass button with search icon
 *   - Tap → opens search screen (handled by CoralScaffold caller)
 */
@Composable
fun CoralNavRow(
    onSearchClick: () -> Unit,
    onTabSelected: (CoralTab) -> Unit = {},
    activeTab: CoralTab = CoralTab.QuickPicks,
    backdrop: LayerBackdrop? = null
) {
    var internalActiveTab by remember { mutableStateOf(activeTab) }
    val effectiveTab = if (activeTab != internalActiveTab) {
        internalActiveTab = activeTab
        activeTab
    } else internalActiveTab

    Row(
        horizontalArrangement = Arrangement.spacedBy(CoralLayout.NavRowGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ─── Nav bar (tab switcher) ───
        Box(
            modifier = Modifier
                .width(152.dp)
                .height(CoralLayout.NavBarHeight)
                .clip(RoundedCornerShape(CoralLayout.NavBarHeight / 2))
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(CoralLayout.NavBarHeight / 2) },
                            effects = {
                                vibrancy()
                                colorControls(
                                    brightness = 0f,
                                    contrast = 1f,
                                    saturation = 1.1f
                                )
                                blur(20f.dp.toPx())
                            }
                        )
                    } else {
                        Modifier.background(Color.White.copy(alpha = 0.1f))
                    }
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { _ ->
                            // Cycle to next tab on tap
                            val tabs = CoralTab.entries
                            val currentIndex = tabs.indexOf(effectiveTab)
                            val nextIndex = (currentIndex + 1) % tabs.size
                            val nextTab = tabs[nextIndex]
                            internalActiveTab = nextTab
                            onTabSelected(nextTab)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = effectiveTab.label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = com.rajatxo.coral.ui.theme.CalSansFamily
            )
        }

        // ─── Search FAB (circular glass button) ───
        Box(
            modifier = Modifier
                .size(CoralLayout.NavBarHeight)
                .clip(CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onSearchClick() })
                }
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                vibrancy()
                                colorControls(
                                    brightness = 0f,
                                    contrast = 1f,
                                    saturation = 1.1f
                                )
                                blur(20f.dp.toPx())
                            }
                        )
                    } else {
                        Modifier.background(Color.White.copy(alpha = 0.1f))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CoralIcons.Search,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Tab definitions are in com.rajatxo.coral.ui.components.CoralTab — shared
 * with NavBarConfig (which persists the user's tab order).
 */
