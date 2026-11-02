package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramGraph

/**
 * Определяет непосредственных предшественников выбранного
 * логического состояния и входящие в него переходы.
 *
 * Учитывает визуальные копии через logicalId.
 */
internal class DiagramFocusResolver {

    /**
     * Формирует контекст подсветки для выбранного состояния.
     * Если состояние не выбрано, возвращает null.
     */
    fun resolve(
        graph: DiagramGraph,
        selectedLogicalId: String?
    ): DiagramFocus? {
        if (selectedLogicalId == null) {
            return null
        }

        val logicalIdByVisualId = graph.nodes.associate {
            it.id to it.logicalId
        }

        // Все рёбра, направленные в выбранное логическое состояние.
        val incomingEdges = graph.edges.filter { edge ->
            logicalIdByVisualId[edge.target] == selectedLogicalId
        }

        val sourceLogicalIds = incomingEdges
            .mapNotNull { edge ->
                logicalIdByVisualId[edge.source]
            }
            .filterNot { sourceLogicalId ->
                sourceLogicalId == selectedLogicalId
            }
            .toSet()

        return DiagramFocus(
            selectedLogicalId = selectedLogicalId,
            incomingSourceLogicalIds = sourceLogicalIds,
            incomingEdgeIds = incomingEdges.map { it.id }.toSet()
        )
    }
}
