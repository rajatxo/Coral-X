package com.rajatxo.coral.ui.foundation

/**
 * ★ CoralCustomShape — shape options for the customizable components.
 *
 * (Was CynthiaCustomShape in the previous Coral codebase. Renamed to remove
 * the Cynthia branding since this is used by CoralScaffold which is shared
 * across all UI modes.)
 */
enum class CoralCustomShape(val label: String) {
    CIRCLE("Circle"),
    PILL("Pill"),
    SQUIRCLE("Squircle"),
    RECTANGLE("Rectangle")
}
