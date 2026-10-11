package com.rajatxo.coral

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.rajatxo.coral.theme.CoralXTheme
import com.rajatxo.coral.update.UpdateChecker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ★ Universal Law 2: Manual edge-to-edge
        window.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CoralXTheme {
                CoralXRoot()
            }
        }
    }
}

@Composable
private fun CoralXRoot() {
    // ★ Check for updates on app launch (non-blocking)
    LaunchedEffect(Unit) {
        val result = UpdateChecker.checkForUpdate(
            currentVersionCode = BuildConfig.VERSION_CODE
        )
        // When UI is built, this result will trigger an update dialog
        // For now, it just logs
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Coral X",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 32.sp
        )
    }
}
