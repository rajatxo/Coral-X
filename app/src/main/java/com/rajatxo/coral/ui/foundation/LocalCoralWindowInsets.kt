package com.rajatxo.coral.ui.foundation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ★ CoralWindowInsets — the single source of truth for screen insets.
 *
 * PROBLEM THIS SOLVES:
 *   Coral's previous screens (Astra, Cynthia, Nyx) each managed their own
 *   paddings — `padding(start = 20.dp)` for greeting, `padding(start = 24.dp)`
 *   for grid, etc. Different screens, different paddings → misaligned layout
 *   that LOOKED DIFFERENT on every phone.
 *
 *   The other problem: gesture nav has ~16dp bottom inset, button nav has
 *   ~48dp. If we just use `navigationBarsPadding()`, the mini player sits
 *   at different heights on different phones.
 *
 * METROLIST/ARCHIVETUNE APPROACH (we follow this):
 *   One `WindowInsets` object that bundles together:
 *     - System status bar (top) — for the header
 *     - System nav bar (bottom) — forced to AT LEAST 48dp (button nav height)
 *     - App's nav row (mini player + nav bar) — fixed dp values
 *
 *   Every screen reads `LocalCoralWindowInsets.current.asPaddingValues()`
 *   as its `contentPadding`. Result: identical layouts on every phone, because
 *   the same insets flow into every screen.
 *
 * USAGE in screens:
 *   LazyColumn(
 *       contentPadding = LocalCoralWindowInsets.current.asPaddingValues(),
 *       ...
 *   )
 */

/** Consistent dp values — same on every phone. */
object CoralLayout {
    /** Horizontal padding for all content (greeting, headers, grids, cards). */
    val ContentPaddingHorizontal: Dp = 20.dp

    /** Top header height (the row with profile icon + title + settings). */
    val HeaderHeight: Dp = 56.dp

    /** Top blur overlay height (fades behind header for glass effect). */
    val TopBlurHeight: Dp = 120.dp

    /** Mini player fixed height. */
    val MiniPlayerHeight: Dp = 64.dp

    /** Nav bar fixed height (also the search FAB diameter). */
    val NavBarHeight: Dp = 52.dp

    /** Gap between mini player and nav bar in the bottom stack. */
    val BottomStackGap: Dp = 10.dp

    /** Horizontal gap between nav bar and search FAB. */
    val NavRowGap: Dp = 10.dp

    /**
     * Minimum bottom inset — guarantees button-nav-like spacing on gesture
     * nav phones. Prevents mini player from overlapping system gesture bar.
     */
    val MinBottomSystemInset: Dp = 48.dp
}

/**
 * The CompositionLocal that exposes the current screen insets.
 *
 * Provided by CoralScaffold. Read by every screen via:
 *   LocalCoralWindowInsets.current.asPaddingValues()
 */
val LocalCoralWindowInsets = staticCompositionLocalOf<WindowInsets> {
    error("CoralWindowInsets not provided — wrap your screen in CoralScaffold")
}
