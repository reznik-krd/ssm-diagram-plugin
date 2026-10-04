package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

/**
 * Хранит текущее состояние hover и выбора элементов диаграммы.
 */
class DiagramInteractionState {
    var hoveredNodeId: String? = null
    var hoveredEventEdgeId: String? = null
    var hoveredGuardEdgeId: String? = null
    var hoveredAction: ActionKey? = null
    var selectedEdgeId: String? = null
    var selectedNodeLogicalId: String? = null

    /**
     * Сбрасывает текущее состояние объекта.
     */
    fun clear() {
        clearHover()
        selectedEdgeId = null
        selectedNodeLogicalId = null
    }

    fun hasHover(): Boolean =
        hoveredNodeId != null ||
                hoveredEventEdgeId != null ||
                hoveredGuardEdgeId != null ||
                hoveredAction != null

    /**
     * Сбрасывает состояние hover.
     */
    fun clearHover() {
        hoveredNodeId = null
        hoveredEventEdgeId = null
        hoveredGuardEdgeId = null
        hoveredAction = null
    }
}

/**
 * Однозначно идентифицирует action по переходу и индексу внутри него.
 */
data class ActionKey(
    val edgeId: String,
    val actionIndex: Int
)
