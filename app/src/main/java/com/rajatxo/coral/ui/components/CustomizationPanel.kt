package com.rajatxo.coral.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.coral.ui.foundation.LocalGlassBackdrop
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy

/**
 * ★ CustomizationPanel — a glass card that appears when the user long-presses
 * the mini player, nav bar, or search FAB.
 *
 * Shows options to customize the capsule (width, height, corner radius, etc.).
 * For now, this is a simpler version of old Coral's CynthiaCustomizationPanel.
 *
 * The panel:
 *   - Glass morphism background (drawBackdrop with kyant blur)
 *   - 320×440dp rounded card
 *   - Drag handle at top
 *   - Close on tap outside
 *
 * @param visible Whether the panel is showing
 * @param onDismiss Called when the user taps outside or presses close
 * @param title The title of the panel (e.g. "Mini Player", "Nav Bar", "Search")
 */
@Composable
fun CustomizationPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = Modifier.fillMaxSize()
    ) {
        // ★ RippleDismissContainer — water-ripple dismiss animation
        com.rajatxo.coral.ui.components.RippleDismissContainer(
            onDismiss = onDismiss,
            scrimColor = Color.Black.copy(alpha = 0.4f)
        ) { progress ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Glass panel
            val panelShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .padding(horizontal = 36.dp)
                    .size(width = 320.dp, height = 440.dp)
                    .rippleFadeOut(progress)  // ★ fade out in sync with ripple
                    .clip(panelShape)
                    .then(
                        if (backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { panelShape },
                                effects = {
                                    vibrancy()
                                    colorControls(
                                        brightness = 0.1f,
                                        contrast = 1f,
                                        saturation = 1.3f
                                    )
                                    blur(30f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.Black.copy(alpha = 0.45f))
                                }
                            )
                        } else {
                            Modifier.background(Color(0xFF1A1A1A).copy(alpha = 0.92f))
                        }
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), panelShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // consume click so it doesn't dismiss
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drag handle bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.4f))
                        )
                    }

                    // Title + close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = CalSansFamily
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onDismiss
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CoralXIcons.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Placeholder content — will add sliders for width/height/corner etc.
                    Text(
                        text = "Customization options coming soon",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontFamily = CalSansFamily
                    )
                }
            }
        }
        }
    }
}
