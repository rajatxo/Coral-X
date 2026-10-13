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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.rajatxo.coral.ui.icons.CoralXIcons
import com.rajatxo.coral.ui.theme.CalSansFamily

/**
 * ★ CustomizationPanel — glass card for customizing capsule properties.
 *
 * Ported from old Coral's CynthiaCustomizationPanel.
 * Layout (top to bottom):
 *   1. Drag handle bar (iOS-style)
 *   2. Header row — title + Reset + Close (✕)
 *   3. Field tabs — selectable capsule rows (Width, Height, Corner, Pos X, Pos Y)
 *   4. +1 / -1 buttons — nudge selected field by exactly 1
 *   5. Shape picker — row of shape buttons
 *
 * Each field shows its current value and can be adjusted.
 * Changes apply live to the target component.
 */
@Composable
fun CustomizationPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    backdrop: LayerBackdrop? = null
) {
    val enterScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = tween(250),
        label = "panelEnterScale"
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(250),
        label = "panelEnterAlpha"
    )

    if (visible) {
        com.rajatxo.coral.ui.components.RippleDismissContainer(
            onDismiss = onDismiss,
            scrimColor = Color.Black.copy(alpha = 0.4f)
        ) { progress ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val panelShape = RoundedCornerShape(24.dp)
            Box(
                modifier = Modifier
                    .padding(horizontal = 36.dp)
                    .size(width = 320.dp, height = 440.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        val totalAlpha = enterAlpha * (1f - progress)
                        alpha = totalAlpha
                        scaleX = enterScale
                        scaleY = enterScale
                    }
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
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ★ Drag handle bar (iOS-style)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.4f))
                        )
                    }

                    // ★ Header row — title + Reset + Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = CalSansFamily
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Reset button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { /* Reset logic will be added */ }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Reset",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CalSansFamily
                                )
                            }
                            // Close button (✕)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onDismiss
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CalSansFamily
                                )
                            }
                        }
                    }

                    // ★ Field tabs — selectable capsule rows
                    data class Field(
                        val label: String,
                        val value: Float,
                        val suffix: String
                    )

                    // Placeholder fields — will be wired to actual customization prefs
                    val fields = listOf(
                        Field("Width", 240f, "dp"),
                        Field("Height", 64f, "dp"),
                        Field("Corner", 32f, "dp"),
                        Field("Pos X", 0f, ""),
                        Field("Pos Y", 0f, "")
                    )
                    var selectedField by remember { mutableStateOf(0) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        fields.forEachIndexed { index, field ->
                            val isSelected = index == selectedField
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { selectedField = index }
                                    )
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = field.label,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = CalSansFamily
                                )
                                Text(
                                    text = "${field.value.toInt()}${field.suffix}",
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = CalSansFamily
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ★ +1 / -1 buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        // -1 button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { /* Decrement logic */ }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CalSansFamily
                            )
                        }
                        // +1 button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { /* Increment logic */ }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CalSansFamily
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ★ Shape picker
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Shape",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontFamily = CalSansFamily
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val shapes = listOf("Pill", "Squircle", "Round", "Square")
                            var selectedShape by remember { mutableStateOf(0) }
                            shapes.forEachIndexed { index, shape ->
                                val isSelected = index == selectedShape
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.25f)
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                                            RoundedCornerShape(13.dp)
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { selectedShape = index }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shape,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontFamily = CalSansFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
