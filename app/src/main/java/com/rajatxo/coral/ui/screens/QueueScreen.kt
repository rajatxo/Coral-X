package com.rajatxo.coral.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.session.MediaController
import com.rajatxo.coral.ui.foundation.LocalCoralWindowInsets

/**
 * ★ QueueScreen — placeholder stub for Coral-X.
 *
 * Referenced by Spiral2/Spiral3/Spiral players when user taps the queue
 * button. Currently shows "Queue coming soon" — will be built out properly
 * when the foundation is stable.
 */
@Composable
fun QueueScreen(
    mediaController: MediaController?,
    onDismiss: () -> Unit
) {
    val insets = LocalCoralWindowInsets.current
    val padding = insets.asPaddingValues()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Queue coming soon",
            color = Color.White
        )
    }
}
