package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

import java.awt.Rectangle

/**
 * Keeps hit-test bounds produced during the latest diagram render pass.
 */
class DiagramHitRegistry {
    val eventAreas = mutableMapOf<String, Rectangle>()
    val guardAreas = mutableMapOf<String, Rectangle>()
    val actionAreas = mutableListOf<ActionHitArea>()

    /** Removes bounds from the previous render pass. */
    fun clear() {
        eventAreas.clear()
        guardAreas.clear()
        actionAreas.clear()
    }
}

/** Hit-test bounds for a concrete action attached to an edge. */
data class ActionHitArea(
    val edgeId: String,
    val actionIndex: Int,
    val bounds: Rectangle
)
