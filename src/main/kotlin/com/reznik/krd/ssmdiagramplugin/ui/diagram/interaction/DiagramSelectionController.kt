package com.reznik.krd.ssmdiagramplugin.ui.diagram.interaction

import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramNode
import com.reznik.krd.ssmdiagramplugin.ui.diagram.model.DiagramEdge

/** Управляет выбором состояний и переходов без изменения модели графа. */
internal class DiagramSelectionController(
    private val interaction: DiagramInteractionState,
    private val onChange: () -> Unit
) {
    /** Переключает выбор логического состояния. */
    fun selectNode(node: DiagramNode) {
        val logicalId = node.logicalId

        interaction.selectedNodeLogicalId =
            if (
                interaction.selectedNodeLogicalId ==
                logicalId
            ) {
                null
            } else {
                logicalId
            }

        interaction.selectedEdgeId = null
        onChange()
    }

    /** Переключает выбор конкретного перехода. */
    fun selectEdge(edge: DiagramEdge) {
        interaction.selectedEdgeId =
            if (
                interaction.selectedEdgeId ==
                edge.id
            ) {
                null
            } else {
                edge.id
            }

        interaction.selectedNodeLogicalId = null
        onChange()
    }

    /** Снимает текущее выделение. */
    fun clearSelection() {
        val changed = interaction.selectedEdgeId != null || interaction.selectedNodeLogicalId != null
        interaction.selectedEdgeId = null
        interaction.selectedNodeLogicalId = null
        if (changed) {
            onChange()
        }
    }

}
