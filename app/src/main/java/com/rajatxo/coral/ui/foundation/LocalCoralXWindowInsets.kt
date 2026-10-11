package com.rajatxo.coral.ui.foundation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.staticCompositionLocalOf

val LocalCoralXWindowInsets = staticCompositionLocalOf<WindowInsets> {
    error("CoralXWindowInsets not provided — wrap your screen in CoralXScaffold")
}
