package com.rajatxo.coral.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Coral brand colors — same across the whole app.
 */
object CoralColors {
    /** Main app surface — pure black. */
    val Surface: Color = Color(0xFF000000)

    /** Slightly lighter for cards / mini player / nav rail pills. */
    val SurfaceVariant: Color = Color(0xFF1A1A1A)

    /** Primary text — pure white. */
    val TextPrimary: Color = Color.White

    /** Secondary text — muted gray for inactive items, subtitles, etc. */
    val TextMuted: Color = Color(0xFF888888)

    /** Coral accent — the brand color, used for active states + buttons. */
    val Coral: Color = Color(0xFFFF6B6B)
}
