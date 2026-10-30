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

    /** Проверяет, подсвечен ли сейчас какой-либо элемент диаграммы. */
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
