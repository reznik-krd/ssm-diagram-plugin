package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

/**
 * Описывает элементы, участвующие в подсветке выбранного состояния.
 *
 * Идентификаторы состояний логические, поэтому визуальные копии
 * одного состояния подсвечиваются совместно.
 */
internal data class DiagramFocus(
    val selectedLogicalId: String,
    val incomingSourceLogicalIds: Set<String>,
    val incomingEdgeIds: Set<String>
) {
    /** Проверяет, является ли состояние источником входящего перехода. */
    fun isIncomingSource(logicalId: String): Boolean =
        logicalId in incomingSourceLogicalIds

    /** Проверяет, участвует ли состояние в текущей подсветке. */
    fun isFocusedNode(logicalId: String): Boolean =
        logicalId == selectedLogicalId ||
                isIncomingSource(logicalId)

    /** Проверяет, является ли переход входящим. */
    fun isFocusedEdge(edgeId: String): Boolean =
        edgeId in incomingEdgeIds
}
