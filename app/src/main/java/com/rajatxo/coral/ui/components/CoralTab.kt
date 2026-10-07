package com.rajatxo.coral.ui.components

/**
 * ★ CoralTab — main destinations in Coral-X.
 *
 * Used by NavBarConfig to persist the user's tab order + default tab.
 * Used by CoralNavRow (the nav bar in the scaffold) to render tab labels.
 */
enum class CoralTab(val label: String) {
    QuickPicks("Quick picks"),
    Songs("Songs"),
    Playlists("Playlists"),
    Albums("Albums"),
    Artists("Artists"),
    Discover("Discover")
}
