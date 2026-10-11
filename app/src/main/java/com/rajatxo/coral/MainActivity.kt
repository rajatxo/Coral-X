package com.rajatxo.coral

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Coral X — MainActivity.
 *
 * Clean slate. Only the universal layout foundation is in place:
 *   - Manual edge-to-edge (WindowCompat.setDecorFitsSystemWindows)
 *   - Transparent system bars (via theme XML)
 *   - LTR layout direction
 *
 * Everything else (player, screens, navigation, media service) will be
 * built from scratch when we're ready.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ★ Universal Law 2: Manual edge-to-edge.
        //   NOT enableEdgeToEdge() — gives per-screen-state icon control.
        window.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CoralXRoot()
        }
    }
}

/**
 * Root composable — currently just a blank black screen with "Coral X" text.
 * This is the starting point. Everything builds on top of this.
 */
@Composable
private fun CoralXRoot() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Coral X",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
