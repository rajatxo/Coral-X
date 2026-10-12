package com.rajatxo.coral.ui.foundation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.staticCompositionLocalOf
import com.kyant.backdrop.backdrops.LayerBackdrop

val LocalCoralXWindowInsets = staticCompositionLocalOf<WindowInsets> {
    error("CoralXWindowInsets not provided — wrap your screen in CoralXScaffold")
}

/**
 * ★ LocalGlassBackdrop — shares the scaffold's kyant LayerBackdrop with
 * child composables (MiniPlayer, TabCapsule, etc.) so they can call
 * drawBackdrop() for real-time frosted-glass blur.
 *
 * Provided by CoralXScaffold. Defaults to null so composables can fall
 * back to a flat dark background when not inside a scaffold (previews).
 */
val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }
