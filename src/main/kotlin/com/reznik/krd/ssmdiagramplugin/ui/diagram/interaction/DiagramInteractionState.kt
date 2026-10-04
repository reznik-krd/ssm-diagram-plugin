package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

/**
 * Stores the diagram element currently highlighted by mouse hover.
 */
class DiagramInteractionState {
    var hoveredNodeId: String? = null
    var hoveredEventEdgeId: String? = null
    var hoveredGuardEdgeId: String? = null
    var hoveredAction: ActionKey? = null

    /**
     * Concrete selected transition.
     */
    var selectedEdgeId: String? = null

    /**
     * Logical state selected by the user.
     *
     * We store logicalId instead of node.id because one logical state
     * may have several visual copies in the diagram.
     */
    var selectedNodeLogicalId: String? = null

    fun clear() {
        clearHover()
        selectedEdgeId = null
        selectedNodeLogicalId = null
    }

    /** Returns true when at least one diagram element is highlighted. */
    fun hasHover(): Boolean =
        hoveredNodeId != null ||
                hoveredEventEdgeId != null ||
                hoveredGuardEdgeId != null ||
                hoveredAction != null

    fun clearHover() {
        hoveredNodeId = null
        hoveredEventEdgeId = null
        hoveredGuardEdgeId = null
        hoveredAction = null
    }
}

/** Identifies one action among all actions attached to an edge. */
data class ActionKey(
    val edgeId: String,
    val actionIndex: Int
)
